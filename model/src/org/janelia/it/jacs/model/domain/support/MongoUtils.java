package org.janelia.it.jacs.model.domain.support;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.FilterBuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;

/**
 * Utilities for finding out about Mongo DB domain objects through annotations. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoUtils {

    private static final Logger log = Logger.getLogger(MongoUtils.class);

    private static final String DOMAIN_OBJECT_PACKAGE_NAME = "org.janelia.it.jacs.model.domain";

    private static final Reflections reflections;
    private static final BiMap<String, Class<? extends DomainObject>> typeClasses = HashBiMap.create();
    private static final Multimap<Class<? extends DomainObject>, Class<? extends DomainObject>> subClasses = ArrayListMultimap.<Class<? extends DomainObject>, Class<? extends DomainObject>>create();

    static {
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.addClassLoaders(ClasspathHelper.contextClassLoader());
        config.addClassLoaders(ClasspathHelper.staticClassLoader());
        config.filterInputsBy(new FilterBuilder().include(DOMAIN_OBJECT_PACKAGE_NAME));

        reflections = new Reflections(config);
        //reflections = new Reflections(DOMAIN_OBJECT_PACKAGE_NAME);
        registerAnnotatedClasses();
        typeClasses.put("sample", org.janelia.it.jacs.model.domain.sample.Sample.class);
        typeClasses.put("treeNode", org.janelia.it.jacs.model.domain.workspace.TreeNode.class);
    }

    private static void registerAnnotatedClasses() {
                
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
    
    public static Set<Class<? extends DomainObject>> getObjectClasses(String collectionName) {
        return getObjectClasses(getObjectClass(collectionName));
    }

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
     * Returns the subject name part of a given subject key. For example, for "group:flylight", this will return "flylight".
     *
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
     * Test Harness
     */
    public static void main(String[] args) {
        System.out.println("getObjectClass(treeNode): "+MongoUtils.getObjectClass("treeNode"));
        System.out.println("getObjectClass(sample): "+MongoUtils.getObjectClass("sample"));
        System.out.println("getObjectClass(image): "+MongoUtils.getObjectClass("image"));
        System.out.println("getSubClasses(TreeNode.class): " + MongoUtils.getSubClasses(TreeNode.class));
        System.out.println("getObjectClasses(image): "+MongoUtils.getObjectClasses("image"));
        System.out.println("getObjectClasses(LSMImage): " + MongoUtils.getObjectClasses(LSMImage.class));
        System.out.println("getCollectionName(Sample.class): "+MongoUtils.getCollectionName(Sample.class));
        System.out.println("getCollectionName(LSMImage): "+MongoUtils.getCollectionName(new LSMImage()));
    }
}
