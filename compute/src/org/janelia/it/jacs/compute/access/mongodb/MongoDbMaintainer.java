package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.MongoUtils;
import org.jongo.MongoCollection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.WriteConcern;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbMaintainer {
    
    private static final Logger log = Logger.getLogger(MongoDbMaintainer.class);

    private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
    private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
    private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
    private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");
    
    protected DomainDAO dao;
    
    public MongoDbMaintainer() throws UnknownHostException {
		this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
        // To load as fast as possible, we don't wait to be acknowledged. 
        // This can introduce subtle problems, so it's important to verify the integrity of the load manually (e.g. cross check the entity counts). 
        // TODO: Maybe we should disable this for the eventual production load. 
        dao.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
    }

    public void refreshPermissions() throws DaoException {

        long start = System.currentTimeMillis();
        log.info("Refreshing denormalized permissions");

        Multimap<String,String> groupMap = HashMultimap.<String,String>create();
        for(Subject subject : dao.getCollectionByName("subject").find().as(Subject.class)) {
            for(String groupKey : subject.getGroups()) {
                groupMap.put(groupKey, subject.getKey());
            }
        }
        
        Set<String> collectionNames = MongoUtils.getCollectionNames();
        for(String collectionName : collectionNames) {
            log.info("Refreshing denormalized permissions for "+collectionName);
            
            MongoCollection collection = dao.getCollectionByName(collectionName);
            Class domainClass = MongoUtils.getObjectClass(collectionName);
            Iterable iterable = collection.find().as(domainClass);
            
            if (iterable==null) {
                log.info("Could not iterate collection "+collectionName+" as "+domainClass);
                continue;
            }
            
            for(Object obj : iterable) {
                DomainObject domainObject = (DomainObject)obj;
                String ownerKey = domainObject.getOwnerKey();
                if (ownerKey==null) continue;
                collection.update("{_id:#}",domainObject.getId()).with("{$addToSet:{readers:#,writers:#}}",ownerKey,ownerKey);
                
            }
        }
        log.info("Refreshing permissions took "+(System.currentTimeMillis()-start) + " ms");
    }

    public void ensureIndexes() throws DaoException {
        
        long start = System.currentTimeMillis();
        log.info("Ensuring indexes");

        MongoCollection treeNodeCollection = dao.getCollectionByName("treeNode");
        ensureDomainIndexes(treeNodeCollection);
        treeNodeCollection.ensureIndex("{name:1}");
        treeNodeCollection.ensureIndex("{class:1}");
        treeNodeCollection.ensureIndex("{class:1,writers:1}");
        treeNodeCollection.ensureIndex("{class:1,readers:1}");

        MongoCollection ontologyCollection = dao.getCollectionByName("ontology");
        ensureDomainIndexes(ontologyCollection);
        ontologyCollection.ensureIndex("{name:1}");

        MongoCollection sampleCollection = dao.getCollectionByName("sample");
        ensureDomainIndexes(sampleCollection);
        sampleCollection.ensureIndex("{name:1}");
        sampleCollection.ensureIndex("{dataSet:1}");
        sampleCollection.ensureIndex("{line:1}");
        
        MongoCollection fragmentCollection = dao.getCollectionByName("fragment");
        ensureDomainIndexes(fragmentCollection);
        fragmentCollection.ensureIndex("{separationId:1}");
        fragmentCollection.ensureIndex("{sampleId:1}");
        fragmentCollection.ensureIndex("{sampleId:1,writers:1}");
        fragmentCollection.ensureIndex("{sampleId:1,readers:1}");

        MongoCollection lsmCollection = dao.getCollectionByName("lsm");
        ensureDomainIndexes(lsmCollection);
        lsmCollection.ensureIndex("{sageId:1}");
        lsmCollection.ensureIndex("{slideCode:1}");
        lsmCollection.ensureIndex("{filepath:1}");
        lsmCollection.ensureIndex("{sampleId:1}");
        lsmCollection.ensureIndex("{sampleId:1,writers:1}");
        lsmCollection.ensureIndex("{sampleId:1,readers:1}");

        MongoCollection flyLineCollection = dao.getCollectionByName("flyLine");
        ensureDomainIndexes(flyLineCollection);
        flyLineCollection.ensureIndex("{robotId:1}");

        MongoCollection screenSampleCollection = dao.getCollectionByName("screenSample");
        ensureDomainIndexes(screenSampleCollection);
        screenSampleCollection.ensureIndex("{flyLine:1}");

        MongoCollection patternMaskCollection = dao.getCollectionByName("patternMask");
        ensureDomainIndexes(patternMaskCollection);
        patternMaskCollection.ensureIndex("{screenSampleId:1}");
        patternMaskCollection.ensureIndex("{screenSampleId:1,writers:1}");
        patternMaskCollection.ensureIndex("{screenSampleId:1,readers:1}");

        MongoCollection annotationCollection = dao.getCollectionByName("annotation");
        ensureDomainIndexes(annotationCollection);
        annotationCollection.ensureIndex("{targetId:1}");
        annotationCollection.ensureIndex("{targetId:1,writers:1}");
        annotationCollection.ensureIndex("{targetId:1,readers:1}");
        annotationCollection.ensureIndex("{text:1}");
        
        log.info("Indexing MongoDB took "+(System.currentTimeMillis()-start)+" ms");
    }

    private void ensureDomainIndexes(MongoCollection mc) {
        mc.ensureIndex("{ownerKey:1}");
        mc.ensureIndex("{writers:1}");
        mc.ensureIndex("{readers:1}");
        mc.ensureIndex("{_id:1,writers:1}");
        mc.ensureIndex("{_id:1,readers:1}");
    }
    
    /**
     * Test harness
     */
    public static void main(String[] args) throws Exception {
        MongoDbMaintainer refresh = new MongoDbMaintainer();
        refresh.refreshPermissions();
        refresh.ensureIndexes();
    }
}
