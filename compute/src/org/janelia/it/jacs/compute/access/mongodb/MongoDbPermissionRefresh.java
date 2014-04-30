package org.janelia.it.jacs.compute.access.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.jongo.MongoCollection;

import com.mongodb.WriteConcern;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbPermissionRefresh {
    
	protected static final String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	protected static final String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
	
    protected Logger log;
    protected DomainDAO domainDao;
    
    public MongoDbPermissionRefresh(Logger log) throws DaoException {
        this.log = log;
        this.domainDao = new DomainDAO(MONGO_SERVER_URL);
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

            for(Object obj : collection.find().as(domainClass)) {
                DomainObject domainObject = (DomainObject)obj;
                Subject owner = subjectMap.get(domainObject.getOwnerKey());
                collection.update("{id:#}",domainObject.getId()).with("{$addToSet:{readers:#},$addToSet:{writers:#}}",owner,owner);
                
                for(String groupKey : owner.getGroups()) {
                    collection.update("{id:#}",domainObject.getId()).with("{$addToSet:{readers:#}}",groupKey);
                }
            }
        }
        log.info("Refreshing permissions took "+(System.currentTimeMillis()-start) + " ms");
    }
}
