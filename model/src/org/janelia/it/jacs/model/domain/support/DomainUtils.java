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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.gui.search.criteria.AttributeValueCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.DateRangeCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria;
import org.janelia.it.jacs.model.domain.gui.search.criteria.TreeNodeCriteria;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.HasRelativeFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.util.ModelStringUtil;
import org.janelia.it.jacs.model.util.ReflectionHelper;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Multimap<Class<? extends DomainObject>, Class<? extends DomainObject>> subClasses = ArrayListMultimap.create();
    private static final List<Class<? extends DomainObject>> searchClasses = new ArrayList<>();
    private static final Map<String,String> searchTypeToClassName = new HashMap<>();
    private static final Map<String,String> simpleToQualifiedNames = new HashMap<>();
    
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
            MongoMapped annotation = nodeClass.getAnnotation(MongoMapped.class);
            try {
                String collectionName = annotation.collectionName();
                if (typeClasses.containsKey(collectionName)) {
                    log.warn("Overridding existing class mapping ("+typeClasses.get(collectionName).getName()+") for collection '"+collectionName+"'");
                }
                log.info("Registering "+nodeClass.getName()+" as mapped class for type '"+collectionName+"'");
                typeClasses.put(collectionName, nodeClass);
                simpleToQualifiedNames.put(nodeClass.getSimpleName(), nodeClass.getName());
                
                // TODO: make this recursive to support more than 1 level of subtypes
                for(Class<? extends DomainObject> subclass : reflections.getSubTypesOf(nodeClass)) {
                    log.info("  Registering "+subclass.getName()+" as a subtype");
                    subClasses.put(nodeClass, subclass);
                    simpleToQualifiedNames.put(subclass.getSimpleName(), subclass.getName());
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
            searchTypeToClassName.put(searchTypeKey, searchClazz.getSimpleName());
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
        if (!className.contains(".")) {
            String qualified = simpleToQualifiedNames.get(className);
            if (qualified==null) {
                throw new IllegalArgumentException("Unknown domain object class: "+className);
            }
            className = qualified;
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Unknown domain object class: "+className);
        }
        if (!DomainObject.class.isAssignableFrom(clazz)) {
            throw new RuntimeException("Not a domain object class: "+className);
        }
        return (Class<? extends DomainObject>)clazz;
    }

    public static List<Class<? extends DomainObject>> getSearchClasses() {
        return searchClasses;
    }

    /**
     * Takes a @SearchType.key and returns the corresponding simple class name.
     * @param type
     * @return
     */
    public static String getClassNameForSearchType(String type) {
        return searchTypeToClassName.get(type);
    }

    public static String getTypeName(Class<? extends DomainObject> domainClass) {
        SearchType searchType = domainClass.getAnnotation(SearchType.class);
        if (searchType!=null) {
            return searchType.label();
        }
        MongoMapped mongoMapped = domainClass.getAnnotation(MongoMapped.class);
        if (mongoMapped!=null) {
            return mongoMapped.label();
        }
        return ModelStringUtil.splitCamelCase(domainClass.getSimpleName());
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
    
    /**
     * Returns a string uniquely identifying the object instance. 
     * @param domainObject
     * @return
     */
    public static String identify(DomainObject domainObject) {
        if (domainObject==null) return "(null)";
        return "("+domainObject.getName()+", @"+System.identityHashCode(domainObject)+")";
    }
    
    public static String getFilepath(HasFiles hasFiles, String fileTypeName) {
        try {
            return getFilepath(hasFiles, FileType.valueOf(fileTypeName));
        }
        catch (IllegalArgumentException e) {
            log.error("No such file type: "+fileTypeName,e);
            return null;
        }
    }
    
    public static String getFilepath(HasFiles hasFiles, FileType fileType) {

        if (hasFiles==null) return null;
        Map<FileType,String> files = hasFiles.getFiles();
        if (files==null) return null;

        log.trace("getFilepath(files:{}, fileType:{})",files,fileType);

        String filepath = null;
        if (fileType.equals(FileType.FirstAvailable2d) || fileType.equals(FileType.FirstAvailable3d)) {
            for(FileType type : FileType.values()) {
                if ((fileType.equals(FileType.FirstAvailable2d) && type.is2dImage()) || (fileType.equals(FileType.FirstAvailable3d) && !type.is2dImage())) {
                    filepath = files.get(type);
                    if (filepath!=null) break;
                }
            }
        }
        else {
            filepath = files.get(fileType);
        }

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
        return DomainUtils.getFilepath(hasFiles, FileType.FirstAvailable3d);
    }

    public static Multiset<String> get2dTypeNames(HasFileGroups hasGroups) {
        return getTypeNames(hasGroups, true);
    }

    public static Multiset<String> get2dTypeNames(HasFiles hasFiles) {
        return getTypeNames(hasFiles, true);
    }

    public static Multiset<String> getTypeNames(HasFileGroups hasGroups, boolean only2d) {
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        for(String groupKey : hasGroups.getGroupKeys()) {
            log.trace("Checking group {}",groupKey);
            HasFiles hasFiles = hasGroups.getGroup(groupKey);
            if (hasFiles.getFiles()!=null) {
                countedTypeNames.addAll(getTypeNames(hasFiles, only2d));
            }
        }
        return countedTypeNames;
    }
    
    public static Multiset<String> getTypeNames(HasFiles hasFiles, boolean only2d) {
        Multiset<String> countedTypeNames = LinkedHashMultiset.create();
        if (hasFiles.getFiles()!=null) {
            log.trace("Checking files");
            for(FileType fileType : hasFiles.getFiles().keySet()) {
                if (only2d && !fileType.is2dImage()) continue;
                log.trace("  Adding {}",fileType.name());
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
                return ComparisonChain.start()
                        .compare(getTypeFromSubjectKey(o1.getKey()), getTypeFromSubjectKey(o2.getKey()), Ordering.natural())
                        .compare(o1.getFullName(), o2.getFullName(), Ordering.natural().nullsLast())
                        .compare(o1.getName(), o2.getName(), Ordering.natural().nullsFirst())
                        .result();
            }
        });
    }

    /**
     * Sort the given list of domain objects by the given sort criteria. The sort criteria is a name of a field found
     * on all the domain objects. If any of the domain objects are missing the field, then they will be treated as having
     * a null sort value, and will be sorted to the end of the list. The sortCriteria may be prepended with a + or - to
     * indicate sorting direction.
     * @param domainObjects
     * @param sortCriteria
     */
    public static void sortDomainObjects(List<? extends DomainObject> domainObjects, String sortCriteria) {

        if (StringUtils.isEmpty(sortCriteria)) return;
        final String sortField = (sortCriteria.startsWith("-") || sortCriteria.startsWith("+")) ? sortCriteria.substring(1) : sortCriteria;
        final boolean ascending = !sortCriteria.startsWith("-");

        final Map<DomainObject, Object> fieldValues = new HashMap<>();
        for (DomainObject domainObject : domainObjects) {
            Object value = getFieldValue(domainObject, sortField);
            fieldValues.put(domainObject, value);
        }

        Collections.sort(domainObjects, new Comparator<DomainObject>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public int compare(DomainObject o1, DomainObject o2) {
                Comparable v1 = (Comparable) fieldValues.get(o1);
                Comparable v2 = (Comparable) fieldValues.get(o2);
                Ordering ordering = Ordering.natural().nullsLast();
                if (!ascending) {
                    ordering = ordering.reverse();
                }
                return ComparisonChain.start().compare(v1, v2, ordering).result();
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
     * @param domainObjects collection of domain objects
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
        else if (criteria instanceof TreeNodeCriteria) {
            TreeNodeCriteria source = (TreeNodeCriteria)criteria;
            TreeNodeCriteria newCriteria = new TreeNodeCriteria();
            newCriteria.setTreeNodeName(source.getTreeNodeName());
            Reference setReference = Reference.createFor(source.getTreeNodeReference().getTargetClassName(), source.getTreeNodeReference().getTargetId());
            newCriteria.setTreeNodeReference(setReference);
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
    }

    public static Object getFieldValue(DomainObject o1, String fieldName) {
        try {
            return org.janelia.it.jacs.shared.utils.ReflectionUtils.get(o1, fieldName);
        }
        catch (Exception e) {
            log.error("Error getting field value "+fieldName,e);
            return null;
        }
    }

    public static List<DomainObjectAttribute> getUniqueAttributes(Collection<DomainObject> domainObjects) {
        Set<Class<? extends DomainObject>> domainClasses = new HashSet<>();
        for(DomainObject domainObject : domainObjects) {
            domainClasses.add(domainObject.getClass());
        }
        return getUniqueAttributes(domainClasses.toArray(new Class[domainClasses.size()]));
    }

    public static List<DomainObjectAttribute> getUniqueAttributes(Class<? extends DomainObject>... domainClasses) {

        Set<DomainObjectAttribute> attrSet = new HashSet<>();

        for(Class<? extends DomainObject> domainClass : domainClasses) {
            for (DomainObjectAttribute attr : getSearchAttributes(domainClass)) {
                if (attr.isDisplay()) {
                    attrSet.add(attr);
                }
            }
        }

        List<DomainObjectAttribute> attrs = new ArrayList<>(attrSet);
        Collections.sort(attrs, new Comparator<DomainObjectAttribute>() {
            @Override
            public int compare(DomainObjectAttribute o1, DomainObjectAttribute o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });

        return attrs;
    }

    public static String abbr(Collection ids) {
    	if (ids==null) return "null";
        return ids.size() < 6 ? "" + ids : ids.size() + " items";
    }
}
