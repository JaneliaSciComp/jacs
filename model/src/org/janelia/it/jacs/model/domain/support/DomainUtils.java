package org.janelia.it.jacs.model.domain.support;

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
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
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
    }

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

    public static String getCollectionName(DomainObject domainObject) {
        return getCollectionName(domainObject.getClass());
    }

    public static String getCollectionName(Class<?> objectClass) {
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
    
    /**
     * Returns a string uniquely identifying the object instance. 
     * @param domainObject
     * @return
     */
    public static String identify(DomainObject domainObject) {
        if (domainObject==null) return "(null)";
        return "("+domainObject.getName()+", @"+System.identityHashCode(domainObject)+")";
    }
    
    /**
     * @deprecated use the version with FileType instead of this weakly-typed String version
     */
    public static String getFilepath(HasFiles hasFiles, String role) {
        return getFilepath(hasFiles, FileType.valueOf(role));
    }
    
    public static String getFilepath(HasFiles hasFiles, FileType fileType) {
        
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

    public static List<Long> getIdList(Collection<DomainObject> objects) {
        List<Long> list = new ArrayList<>();
        for(DomainObject domainObject : objects) {
            if (domainObject!=null) {
                list.add(domainObject.getId());
            }
        }
        return list;
    }

    public static Map<Long, DomainObject> getMapById(Collection<DomainObject> objects) {
        Map<Long, DomainObject> objectMap = new HashMap<>();
        for (DomainObject domainObject : objects) {
            if (domainObject != null) {
                objectMap.put(domainObject.getId(), domainObject);
            }
        }
        return objectMap;
    }
    
    public static Collection<Reference> getReferences(Collection<DomainObject> domainObjects) {
        Collection<Reference> refs = new ArrayList<>();
        for(DomainObject obj : domainObjects) {
            Reference ref = new Reference();
            ref.setTargetId(obj.getId());
            ref.setCollectionName(getCollectionName(obj));
            refs.add(ref);
        }
        return refs;
    }
    
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
