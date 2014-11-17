package org.janelia.it.jacs.model.domain.support;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.Folder;
import org.reflections.Reflections;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class MongoUtils {

    private static final Logger log = Logger.getLogger(MongoUtils.class);

    private static final String DOMAIN_OBJECT_PACKAGE_NAME = "org.janelia.it.jacs.model.domain";

    private static final Reflections reflections;
    private static final BiMap<String, Class<? extends DomainObject>> typeClasses = HashBiMap.create();

    static {
        reflections = new Reflections(DOMAIN_OBJECT_PACKAGE_NAME);
        registerAnnotatedClasses();
    }

    private static void registerAnnotatedClasses() {
        for (Class<?> nodeClass : reflections.getTypesAnnotatedWith(MongoMapped.class)) {
            MongoMapped annotation = (MongoMapped) nodeClass.getAnnotation(MongoMapped.class);
            try {
                String collectionName = annotation.collectionName();
                if (typeClasses.containsKey(collectionName)) {
                    log.warn("Overridding existing class mapping ("+typeClasses.get(collectionName).getName()+") for collection '"+collectionName+"'");
                }
                log.info("Registering "+nodeClass.getName()+" as mapped class for type '"+collectionName+"'");
                typeClasses.put(collectionName, (Class<? extends DomainObject>) nodeClass);
            }
            catch (Exception e) {
                log.error("Error registering annotated domain object classes", e);
            }
        }
    }

    public static String getCollectionName(DomainObject domainObject) {
        return getCollectionName(domainObject.getClass());
    }

    public static String getCollectionName(Class<?> domainClazz) {
        MongoMapped mongoMappedAnnotation = null;
        Class<?> clazz = domainClazz;
        while (mongoMappedAnnotation==null&&clazz!=null) {
            mongoMappedAnnotation = clazz.getAnnotation(MongoMapped.class);
            clazz = clazz.getSuperclass();
        }
        if (mongoMappedAnnotation==null) {
            throw new IllegalArgumentException("Cannot get MongoDB collection for class hierarchy not marked with @MongoMapped annotation: "+domainClazz.getName());
        }
        return mongoMappedAnnotation.collectionName();
    }

    public static Class<? extends DomainObject> getObjectClass(String collectionName) {
        return typeClasses.get(collectionName);
    }

    /**
     * Test Harness
     */
    public static void main(String[] args) {
        System.out.println("getObjectClass(treeNode): "+MongoUtils.getObjectClass("treeNode"));
        System.out.println("getObjectClass(sample): "+MongoUtils.getObjectClass("sample"));
        System.out.println("getObjectClass(image): "+MongoUtils.getObjectClass("image"));
        System.out.println("getCollectionName(Sample.class): "+MongoUtils.getCollectionName(Sample.class));
        System.out.println("getCollectionName(Folder.class): "+MongoUtils.getCollectionName(Folder.class));
        System.out.println("getCollectionName(LSMImage): "+MongoUtils.getCollectionName(new LSMImage()));
    }
}
