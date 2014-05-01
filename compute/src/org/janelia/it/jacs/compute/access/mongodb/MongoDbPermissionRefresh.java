package org.janelia.it.jacs.compute.access.mongodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.jongo.MongoCollection;

import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbPermissionRefresh {

    protected static final String MONGO_SERVER_URL = "rokicki-ws";
    
    private static final Logger log = Logger.getLogger(MongoDbPermissionRefresh.class);
	
    protected DomainDAO domainDao;
    
    public MongoDbPermissionRefresh(String serverUrl) throws DaoException {
        this.domainDao = new DomainDAO(serverUrl);
        domainDao.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
    }

    public void refreshPermissions() throws DaoException {

        long start = System.currentTimeMillis();
        log.info("Refreshing denormalized permissions");
        
        Map<String,Subject> subjectMap = new HashMap<String,Subject>();
        for(Subject subject : domainDao.getCollection("subject").find().as(Subject.class)) {
            subjectMap.put(subject.getKey(), subject);
        }
        
        Map<String,Class<? extends DomainObject>> domainClassMap = domainDao.getDomainClassMap();
        for(String domainType : domainClassMap.keySet()) {
            Class domainClass = domainClassMap.get(domainType);
                        
            log.info("Refreshing denormalized permissions for "+domainType);
            
            MongoCollection collection = domainDao.getCollection(domainType);
            
            Iterable iterable = collection.find().as(domainClass);
            if (iterable==null) {
                log.info("Could not iterate collection "+domainType+" as "+domainClass);
                continue;
            }
            for(Object obj : iterable) {
                DomainObject domainObject = (DomainObject)obj;
                Subject owner = subjectMap.get(domainObject.getOwnerKey());
                
                Set<String> readers = new HashSet<String>();
                readers.add(owner.getKey());
                for(String groupKey : owner.getGroups()) {
                    readers.add(groupKey);
                }   
                collection.update("{_id:#}",domainObject.getId()).with("{$addToSet:{readers:{$each:#}}}",readers);
                collection.update("{_id:#}",domainObject.getId()).with("{$addToSet:{writers:#}}",owner.getKey());
                
            }
        }
        log.info("Refreshing permissions took "+(System.currentTimeMillis()-start) + " ms");
    }
    
    public static void main(String[] args) throws Exception {
        MongoDbPermissionRefresh refresh = new MongoDbPermissionRefresh(MONGO_SERVER_URL);
        refresh.refreshPermissions();
    }
}
