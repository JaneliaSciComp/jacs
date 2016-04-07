package org.janelia.it.jacs.model.domain.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeValueCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.DateRangeCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.ObjectSetCriteria;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.HasRelativeFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

/**
 * Utility methods for dealing with the domain model. 
 * Uses reflection to find domain model classes and keep track of them, among other things.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainUtils {

    private static final Logger log = LoggerFactory.getLogger(DomainUtils.class);

    private static final String DOMAIN_OBJECT_PACKAGE_NAME = "org.janelia.it.jacs.model.domain";

    private static final BiMap<String, Class<? extends DomainObject>> typeClasses = HashBiMap.create();
    private static final Multimap<Class<? extends DomainObject>, Class<? extends DomainObject>> subClasses = ArrayListMultimap.<Class<? extends DomainObject>, Class<? extends DomainObject>>create();
    private static final List<Class<? extends DomainObject>> searchClasses = new ArrayList<>();
    private static final Map<String,String> searchTypeToClassName = new HashMap<>();
    
    static {
        registerAnnotatedClasses();
    }

    /**
     * Look at all classes with the @MongoMapped annotation and register them as domain classes.
     */
    private static void registerAnnotatedClasses() {
        
        Reflections reflections = new Reflections(DOMAIN_OBJECT_PACKAGE_NAME);
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(MongoMapped.class)) {
            Class<? extends DomainObject> nodeClass = (Class<? extends DomainObject>)clazz;
            MongoMapped annotation = (MongoMapped) nodeClass.getAnnotation(MongoMapped.class);
            try {
                String collectionName = annotation.collectionName();
                if (typeClasses.containsKey(collectionName)) {
                    log.warn("Overridding existing class mapping ("+typeClasses.get(collectionName).getName()+") for collection '"+collectionName+"'");
                }
                log.info("Registering "+nodeClass.getName()+" as mapped class for type '"+collectionName+"'");
                typeClasses.put(collectionName, nodeClass);
                
                for(Class<? extends DomainObject> subclass : reflections.getSubTypesOf(nodeClass)) {
                    log.info("  Registering "+subclass.getName()+" as a subtype");
                    subClasses.put(nodeClass, subclass);
                }
                
            }
            catch (Exception e) {
                log.error("Error registering annotated domain object classes", e);
            }
        }
        for(Class<?> searchClass : reflections.getTypesAnnotatedWith(SearchType.class)) {
            searchClasses.add((Class<? extends DomainObject>)searchClass);
        }
        Collections.sort(searchClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                final String l1 = o1.getAnnotation(SearchType.class).label();
                final String l2 = o2.getAnnotation(SearchType.class).label();
                return l1.compareTo(l2);
            }
        });  
        for(Class<?> searchClazz : searchClasses) {
            String searchTypeKey = searchClazz.getAnnotation(SearchType.class).key();
            searchTypeToClassName.put(searchTypeKey, searchClazz.getName());
        }
    }

    public static String getCollectionName(String className) {
        return getCollectionName(getObjectClassByName(className));
    }
    
    public static String getCollectionName(DomainObject domainObject) {
        return getCollectionName(domainObject.getClass());
    }

    public static String getCollectionName(Class<?> objectClass) {
        if (objectClass==null) return null;
        MongoMapped mongoMappedAnnotation = null;
        Class<?> clazz = objectClass;
        while (mongoMappedAnnotation==null&&clazz!=null) {
            mongoMappedAnnotation = clazz.getAnnotation(MongoMapped.class);
            clazz = clazz.getSuperclass();
        }
        if (mongoMappedAnnotation==null) {
            throw new IllegalArgumentException("Cannot get MongoDB collection for class hierarchy not marked with @MongoMapped annotation: "+objectClass.getName());
        }
        return mongoMappedAnnotation.collectionName();
    }

    public static Set<String> getCollectionNames() {
        return typeClasses.keySet();
    }
    
    public static Class<? extends DomainObject> getBaseClass(String collectionName) {
        return typeClasses.get(collectionName);
    }

    public static Set<Class<? extends DomainObject>> getSubClasses(String collectionName) {
        return getSubClasses(getBaseClass(collectionName));
    }
    
    public static Set<Class<? extends DomainObject>> getSubClasses(Class<? extends DomainObject> objectClass) {
        Set<Class<? extends DomainObject>> classes = new HashSet<>();
        classes.addAll(subClasses.get(objectClass));
        return classes;
    }
    
    /**
     * Returns all the domain classes stored in the specified collection. 
     * @param collectionName name of collection
     * @return list of domain object classes
     */
    public static Set<Class<? extends DomainObject>> getObjectClasses(String collectionName) {
        return getObjectClasses(getBaseClass(collectionName));
    }

    /**
     * Returns the given class and all of its sub-classes. 
     * @param objectClass
     * @return
     */
    public static Set<Class<? extends DomainObject>> getObjectClasses(Class<? extends DomainObject> objectClass) {
        Set<Class<? extends DomainObject>> classes = getSubClasses(objectClass);
        classes.add(objectClass);
        return classes;
    }
    
    public static Class<? extends DomainObject> getObjectClassByName(String className) {
        if (className==null) return null;
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Illegal domain object class: "+className);
        }
        if (!DomainObject.class.isAssignableFrom(clazz)) {
            throw new RuntimeException("Not a domain object class: "+className);
        }
        return (Class<? extends DomainObject>)clazz;
    }

    public static List<Class<? extends DomainObject>> getSearchClasses() {
        return searchClasses;
    }

    public static String getClassNameForSearchType(String type) {
        return searchTypeToClassName.get(type);
    }

    /**
     * Generate a list of DomainObjectAttributes for the given domain object class. DomainObjectAttributes are
     * generated for all fields and methods marked with a @SearchAttribute annotation. 
     * @param clazz a class which extends DomainObject
     * @return a list of DomainObjectAttributes
     */
    public static List<DomainObjectAttribute> getSearchAttributes(Class<? extends DomainObject> clazz) {
        
        List<DomainObjectAttribute> attrs = new ArrayList<>();
        
        // Look for annotated fields 
        for (Field field : ReflectionUtils.getAllFields(clazz)) {
            SearchAttribute searchAttributeAnnot = field.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
                try {
                    Method getter = ReflectionHelper.getGetter(clazz, field.getName());
                    Method setter = ReflectionHelper.getSetter(clazz, field.getName(), getter.getReturnType());
                    DomainObjectAttribute attr = new DomainObjectAttribute(field.getName(), searchAttributeAnnot.label(), searchAttributeAnnot.key(), searchAttributeAnnot.facet(), searchAttributeAnnot.display(), getter, setter);
                    attrs.add(attr);
                }
                catch (Exception e) {
                    log.warn("Error getting field " + field.getName() + " on " + clazz.getName(), e);
                }
            }
        }

        // Look for annotated getters
        for (Method getter : clazz.getMethods()) {
            SearchAttribute searchAttributeAnnot = getter.getAnnotation(SearchAttribute.class);
            if (searchAttributeAnnot!=null) {
                try {
                    String getterName = getter.getName();
                    if (getter.getName().startsWith("get")) {
                        getterName = getterName.substring(3, 4).toLowerCase() + getterName.substring(4);
                        Method setter;
                        try {
                            setter = ReflectionHelper.getSetter(clazz, getterName, getter.getReturnType());
                        }
                        catch (NoSuchMethodException e) {
                            log.trace("Getter has no corresponding setter: "+getterName);
                            setter = null;
                        }
                        DomainObjectAttribute attr = new DomainObjectAttribute(getterName, searchAttributeAnnot.label(), searchAttributeAnnot.key(), searchAttributeAnnot.facet(), searchAttributeAnnot.display(), getter, setter);
                        attrs.add(attr);
                    }
                }
                catch (Exception e) {
                    log.warn("Error getting method " + getter.getName() + " on " + clazz.getName(), e);
                }
            }
        }

        return attrs;
    }
        
    /**
     * Returns the subject name part of a given subject key. For example, for "group:flylight", this returns "flylight".
     * @param subjectKey
     * @return
     */
    public static String getNameFromSubjectKey(String subjectKey) {
        if (subjectKey == null) {
            return null;
        }
        return subjectKey.substring(subjectKey.indexOf(':') + 1);
    }

    /**
     * Returns the type part of the given subject key. For example, for "group:flylight", this returns "group".
     * @param subjectKey
     * @return
     */
    public static String getTypeFromSubjectKey(String subjectKey) {
        if (subjectKey == null) {
            return null;
        }
        return subjectKey.substring(0, subjectKey.indexOf(':'));
    }

    public static boolean equals(DomainObject o1, DomainObject o2) {
        if (o1==null || o2==null) return false;
        if (o1.getId()==null || o2.getId()==null) return false;
        return o1.getId().equals(o2.getId());
    }

    public static boolean equals(PipelineResult o1, PipelineResult o2) {
        if (o1==null || o2==null) return false;
        if (o1.getId()==null || o2.getId()==null) return false;
        return o1.getId().equals(o2.getId());
    }
    
    
    /**
     * Returns a string uniquely identifying the object instance. 
     * @param domainObject
     * @return
     */
    public static String identify(DomainObject domainObject) {
        if (domainObject==null) return "(null)";
        return "("+domainObject.getName()+", @"+System.identityHashCode(domainObject)+")";
    }
    
    public static String getFilepath(HasFiles hasFiles, String role) {
        return getFilepath(hasFiles, FileType.valueOf(role));
    }
    
    public static String getFilepath(HasFiles hasFiles, FileType fileType) {

        if (hasFiles==null) return null;
        Map<FileType,String> files = hasFiles.getFiles();
        if (files==null) return null;
        String filepath = files.get(fileType);
        if (filepath==null) return null;

        if (filepath.startsWith("/")) {
            // Already an absolute path, don't need to add prefix
            return filepath;
        }
        
        StringBuilder urlSb = new StringBuilder();

        // Add prefix
        if (hasFiles instanceof HasRelativeFiles) {
            String rootPath = ((HasRelativeFiles)hasFiles).getFilepath();
            if (rootPath!=null) {
                urlSb.append(rootPath);
                if (!rootPath.endsWith("/")) urlSb.append("/");
            }
        }
        
        // Add relative path
        urlSb.append(filepath);
        
        return urlSb.length()>0 ? urlSb.toString() : null;
    }
    
    public static void setFilepath(HasRelativeFiles hasFiles, FileType fileType, String filepath) {
        if (filepath==null) {
            hasFiles.getFiles().remove(fileType);
        }
        else {
            hasFiles.getFiles().put(fileType, getRelativeFilename(hasFiles, filepath));    
        }
    }

    private static String getRelativeFilename(HasFilepath result, String filepath) {
        if (filepath==null) return null;
        if (result==null) return filepath;
        String parentFilepath = result.getFilepath();
        if (parentFilepath==null) throw new IllegalArgumentException("Result "+filepath+" has null parent filepath");
        String prefix = parentFilepath.endsWith("/") ? parentFilepath : parentFilepath+"/";
        if (!filepath.startsWith(prefix)) {
            return filepath;
        }
        return filepath.replaceFirst(prefix, "");
    }
    
    public static String getDefault3dImageFilePath(HasFiles hasFiles) {
        String path = DomainUtils.getFilepath(hasFiles, FileType.LosslessStack);
        if (path==null) path = DomainUtils.getFilepath(hasFiles, FileType.VisuallyLosslessStack);
        return path;
    }

    public static Multiset<String> get2dTypeNames(HasFileGroups hasGroups) {
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        for(String groupKey : hasGroups.getGroupKeys()) {
            log.debug("Checking group {}",groupKey);
            HasFiles hasFiles = hasGroups.getGroup(groupKey);
            if (hasFiles.getFiles()!=null) {
                countedTypeNames.addAll(get2dTypeNames(hasFiles));
            }
        }
        return countedTypeNames;
    }
    
    public static Multiset<String> get2dTypeNames(HasFiles hasFiles) {
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        if (hasFiles.getFiles()!=null) {
            for(FileType fileType : hasFiles.getFiles().keySet()) {
                if (!fileType.is2dImage()) continue;
                log.debug("  Adding {}",fileType.name());
                countedTypeNames.add(fileType.name());
            }
        }
        return countedTypeNames;
    }
    
    /**
     * Returns true if the collection is null or empty. 
     * @param collection
     * @return
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection==null || collection.isEmpty();
    }
    
    public static String unCamelCase(String s) {
        return s.replaceAll("(?<=\\p{Ll})(?=\\p{Lu})|(?<=\\p{L})(?=\\p{Lu}\\p{Ll})", " ");
    }

    public static boolean hasReadAccess(DomainObject domainObject, String subjectKey) {
        return domainObject.getReaders().contains(subjectKey);
    }
    
    public static boolean hasWriteAccess(DomainObject domainObject, String subjectKey) {
        return domainObject.getWriters().contains(subjectKey);
    }
    
    public static boolean isOwner(DomainObject domainObject, String subjectKey) {
        return domainObject.getOwnerKey().equals(subjectKey);
    }

    /**
     * Sort a list of subjects in this order: 
     * groups then users, alphabetical by full name, alphabetical by name. 
     * @param subjects
     */
    public static void sortSubjects(List<Subject> subjects) {
        Collections.sort(subjects, new Comparator<Subject>() {
            @Override
            public int compare(Subject o1, Subject o2) {
                ComparisonChain chain = ComparisonChain.start()
                        .compare(getTypeFromSubjectKey(o1.getKey()), getTypeFromSubjectKey(o2.getKey()), Ordering.natural())
                        .compare(o1.getFullName(), o2.getFullName(), Ordering.natural().nullsLast())
                        .compare(o1.getName(), o2.getName(), Ordering.natural().nullsFirst());
                return chain.result();
            }
        });
    }

    /**
     * Generate a list of ids for the given domain objects.
     * @param domainObjects
     * @return
     */
    public static List<Long> getIds(Collection<? extends DomainObject> domainObjects) {
        List<Long> ids = new ArrayList<>();
        for(DomainObject domainObject : domainObjects) {
            ids.add(domainObject.getId());
        }
        return ids;
    }
    
    /**
     * Generate a list of references to the given domain objects.
     * @param objects collection of domain objects
     * @return a list of references, one for each domain object
     */
    public static <T extends DomainObject> List<Reference> getReferences(Collection<T> domainObjects) {
        List<Reference> refs = new ArrayList<>();
        for(T domainObject : domainObjects) {
            if (domainObject!=null) {
                refs.add(Reference.createFor(domainObject));
            }
        }
        return refs;
    }

    /**
     * Generate a list of references for the members of an object set.
     * @param objectSet
     * @return
     */
    public static List<Reference> getReferencesForMembers(ObjectSet objectSet) {
        // TODO: I feel like I already wrote this code somewhere else, so when I find it I'll de-dup
        List<Reference> refs = new ArrayList<>();
        for(Long id : objectSet.getMembers()) {
            refs.add(Reference.createFor(objectSet.getClassName(), id));
        }
        return refs;
    }
    
    /**
     * Generate a map by reference to the given domain objects.
     * @param objects collection of domain objects
     * @return a map with the domain objects as values, keyed by reference to each domain object
     */
    public static <T extends DomainObject> Map<Reference, T> getMapByReference(Collection<T> objects) {
        Map<Reference, T> objectMap = new HashMap<>();
        if (objects!=null) {
            for (T domainObject : objects) {
                if (domainObject != null) {
                    objectMap.put(Reference.createFor(domainObject), domainObject);
                }
            }
        }
        return objectMap;
    }

    /**
     * Generate a map by reference to the given domain objects.
     * @param objects collection of domain objects
     * @return a map with the domain objects as values, keyed by reference to each domain object
     */
    public static <T extends DomainObject> Map<Long, T> getMapById(Collection<T> objects) {
        Map<Long, T> objectMap = new HashMap<>();
        if (objects!=null) {
            for (T domainObject : objects) {
                if (domainObject != null) {
                    objectMap.put(domainObject.getId(), domainObject);
                }
            }
        }
        return objectMap;
    }
    
    public static ListMultimap<Long,Annotation> getAnnotationsByDomainObjectId(Collection<Annotation> annotations) {
        ListMultimap<Long,Annotation> annotationsByDomainObjectId = ArrayListMultimap.<Long,Annotation>create();
        for(Annotation annotation : annotations) {
            annotationsByDomainObjectId.put(annotation.getTarget().getTargetId(), annotation);
        }
        return annotationsByDomainObjectId;
    }
    
    /**
     * There are better ways of deep cloning, but this is easier for now. 
     */
    public static Filter cloneFilter(Filter filter) {
        Filter newFilter = new Filter();
        newFilter.setName(filter.getName());
        newFilter.setSearchString(filter.getSearchString());
        newFilter.setSearchClass(filter.getSearchClass());
        newFilter.setSort(filter.getSort());
        if (filter.hasCriteria()) {
            for(Criteria criteria : filter.getCriteriaList()) {
                newFilter.addCriteria(cloneCriteria(criteria));
            }
        }
        
        return newFilter;
    }
    
    private static Criteria cloneCriteria(Criteria criteria) {
        if (criteria instanceof AttributeValueCriteria) {
            AttributeValueCriteria source = (AttributeValueCriteria)criteria;
            AttributeValueCriteria newCriteria = new AttributeValueCriteria();
            newCriteria.setAttributeName(source.getAttributeName());
            newCriteria.setValue(source.getValue());
            return newCriteria;
        }
        else if (criteria instanceof DateRangeCriteria) {
            DateRangeCriteria source = (DateRangeCriteria)criteria;
            DateRangeCriteria newCriteria = new DateRangeCriteria();
            newCriteria.setAttributeName(source.getAttributeName());
            newCriteria.setStartDate(source.getStartDate());
            newCriteria.setEndDate(source.getEndDate());
            return newCriteria;
        }
        else if (criteria instanceof FacetCriteria) {
            FacetCriteria source = (FacetCriteria)criteria;
            FacetCriteria newCriteria = new FacetCriteria();
            newCriteria.setAttributeName(source.getAttributeName());
            newCriteria.setValues(new HashSet<>(source.getValues()));
            return newCriteria;
        }
        else if (criteria instanceof ObjectSetCriteria) {
            ObjectSetCriteria source = (ObjectSetCriteria)criteria;
            ObjectSetCriteria newCriteria = new ObjectSetCriteria();
            newCriteria.setObjectSetName(source.getObjectSetName());
            Reference setReference = new Reference(source.getObjectSetReference().getTargetClassName(), source.getObjectSetReference().getTargetId());
            newCriteria.setObjectSetReference(setReference);
            return newCriteria;
        }
        else {
            throw new IllegalArgumentException("Unknown criteria subtype: "+criteria.getClass().getName());
        }
    }

    public static Object getAttributeValue(DomainObject domainObject, String attrName) throws Exception {
        Method getter = ReflectionHelper.getGetter(domainObject.getClass(), attrName);
        return getter.invoke(domainObject);
    }
    
    /**
     * Test Harness
     */
    public static void main(String[] args) {
        System.out.println("getObjectClass(treeNode): "+DomainUtils.getBaseClass("treeNode"));
        System.out.println("getObjectClass(sample): "+DomainUtils.getBaseClass("sample"));
        System.out.println("getObjectClass(image): "+DomainUtils.getBaseClass("image"));
        System.out.println("getSubClasses(TreeNode.class): " + DomainUtils.getSubClasses(TreeNode.class));
        System.out.println("getObjectClasses(image): "+DomainUtils.getObjectClasses("image"));
        System.out.println("getObjectClasses(LSMImage): " + DomainUtils.getObjectClasses(LSMImage.class));
        System.out.println("getCollectionName(Sample.class): "+DomainUtils.getCollectionName(Sample.class));
        System.out.println("getCollectionName(LSMImage): "+DomainUtils.getCollectionName(new LSMImage()));
    }
}
