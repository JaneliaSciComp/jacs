package org.janelia.it.jacs.model.domain.support;

import static org.janelia.it.jacs.model.domain.enums.FileType.LosslessStack;
import static org.janelia.it.jacs.model.domain.enums.FileType.VisuallyLosslessStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
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

    static {
        registerAnnotatedClasses();
        typeClasses.put("sample", org.janelia.it.jacs.model.domain.sample.Sample.class);
        typeClasses.put("fragment", org.janelia.it.jacs.model.domain.sample.NeuronFragment.class);
        typeClasses.put("treeNode", org.janelia.it.jacs.model.domain.workspace.TreeNode.class);
        typeClasses.put("workspace", org.janelia.it.jacs.model.domain.workspace.Workspace.class);
        typeClasses.put("objectSet", org.janelia.it.jacs.model.domain.workspace.ObjectSet.class);
        typeClasses.put("patternMask", org.janelia.it.jacs.model.domain.screen.PatternMask.class);
        typeClasses.put("filter", org.janelia.it.jacs.model.domain.gui.search.Filter.class);
        typeClasses.put("annotation", org.janelia.it.jacs.model.domain.ontology.Annotation.class);
        typeClasses.put("ontology", org.janelia.it.jacs.model.domain.ontology.Ontology.class);
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
    }

    public static String getCollectionName(String className) {
        // TODO: improve performance with a map keyed by class name
        for (Map.Entry<String, Class<? extends DomainObject>> entry : typeClasses.entrySet()) {
            if (entry.getValue().getName().equals(className)) {
                return entry.getKey();
            }
        }
        return null;
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
    
    public static Class<? extends DomainObject> getObjectClass(String collectionName) {
        return typeClasses.get(collectionName);
    }

    public static Set<Class<? extends DomainObject>> getSubClasses(String collectionName) {
        return getSubClasses(getObjectClass(collectionName));
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
        return getObjectClasses(getObjectClass(collectionName));
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
        if (hasFiles instanceof HasFilepath) {
            String rootPath = ((HasFilepath)hasFiles).getFilepath();
            if (rootPath!=null) {
                urlSb.append(rootPath);
                if (!rootPath.endsWith("/")) urlSb.append("/");
            }
        }
        
        // Add relative path
        urlSb.append(filepath);
        
        return urlSb.length()>0 ? urlSb.toString() : null;
    }
    
    public static String getDefault3dImageFilePath(HasFiles hasFiles) {
        String path = DomainUtils.getFilepath(hasFiles, LosslessStack);
        if (path==null) path = DomainUtils.getFilepath(hasFiles, VisuallyLosslessStack);
        return path;
    }

    /**
     * Return true if the given tree node has the specified domain object as a child. 
     * @param treeNode
     * @param domainObject
     * @return
     */
    public static boolean hasChild(TreeNode treeNode, DomainObject domainObject) {
        if (treeNode.hasChildren()) {
            for(Iterator<Reference> i = treeNode.getChildren().iterator(); i.hasNext(); ) {
                Reference iref = i.next();
                if (iref.getTargetId().equals(domainObject.getId())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Return true if the given ontology term has the specified ontology term as a child. 
     * @param ontologyTerm parent term
     * @param childTerm child term
     * @return
     */
    public static boolean hasChild(OntologyTerm ontologyTerm, OntologyTerm childTerm) {
        if (childTerm==null) return false;
        if (ontologyTerm.hasChildren()) {
            for(Iterator<OntologyTerm> i = ontologyTerm.getTerms().iterator(); i.hasNext(); ) {
                OntologyTerm child = i.next();
                if (child!=null && child.getId()!=null && child.getId().equals(childTerm.getId())) {
                    return true;
                }
            }
        }
        return false;
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
    public static List<Long> getIds(List<? extends DomainObject> domainObjects) {
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
    public static Collection<Reference> getReferences(Collection<? extends DomainObject> domainObjects) {
        Collection<Reference> refs = new ArrayList<>();
        for(DomainObject domainObject : domainObjects) {
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
    public static Map<Reference, DomainObject> getMapByReference(Collection<? extends DomainObject> objects) {
        Map<Reference, DomainObject> objectMap = new HashMap<>();
        if (objects!=null) {
            for (DomainObject domainObject : objects) {
                if (domainObject != null) {
                    objectMap.put(Reference.createFor(domainObject), domainObject);
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
     * Find the ontology term with the given id in the specified ontology tree.
     * @param term ontololgy term tree structure
     * @param termId GUID of the term to find 
     * @return term with the given termId, or null if it cannot be found
     */
    public static OntologyTerm findTerm(OntologyTerm term, Long termId) {
        if (termId==null) return null;
        if (term.getId()!=null && term.getId().equals(termId)) {
            return term;
        }
        if (term.getTerms()!=null) {
            for(OntologyTerm child : term.getTerms()) {
                OntologyTerm found = findTerm(child, termId);
                if (found!=null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static OntologyTerm findTerm(OntologyTerm term, String name) {
        if (name==null) return null;
        if (term.getName()!=null && term.getName().equals(name)) {
            return term;
        }
        if (term.getTerms()!=null) {
            for(OntologyTerm child : term.getTerms()) {
                OntologyTerm found = findTerm(child, name);
                if (found!=null) {
                    return found;
                }
            }
        }
        return null;
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
    
    /**
     * Test Harness
     */
    public static void main(String[] args) {
        System.out.println("getObjectClass(treeNode): "+DomainUtils.getObjectClass("treeNode"));
        System.out.println("getObjectClass(sample): "+DomainUtils.getObjectClass("sample"));
        System.out.println("getObjectClass(image): "+DomainUtils.getObjectClass("image"));
        System.out.println("getSubClasses(TreeNode.class): " + DomainUtils.getSubClasses(TreeNode.class));
        System.out.println("getObjectClasses(image): "+DomainUtils.getObjectClasses("image"));
        System.out.println("getObjectClasses(LSMImage): " + DomainUtils.getObjectClasses(LSMImage.class));
        System.out.println("getCollectionName(Sample.class): "+DomainUtils.getCollectionName(Sample.class));
        System.out.println("getCollectionName(LSMImage): "+DomainUtils.getCollectionName(new LSMImage()));
    }
}
