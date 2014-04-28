package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.Annotation;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.jacs.model.domain.PatternMask;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.ScreenSample;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbPermissionRefresh {
    
	protected static final String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	protected static final String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
	protected static final String[] domainTypes = {"folder","sample","screenSample","patternMask","lsm","fragment","annotation","aontology"};
	protected static final Class[] domainClasses = {Folder.class,Sample.class,ScreenSample.class,PatternMask.class,LSMImage.class,NeuronFragment.class,Annotation.class,Ontology.class};
	
    protected Logger log;
    protected Jongo jongo;
    
    public MongoDbPermissionRefresh(Logger log) {
        this.log = log;
    }
    
    private void init() throws DaoException {
        try {
            MongoClient m = new MongoClient(MONGO_SERVER_URL);
        	m.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
        	DB db = m.getDB(MONGO_DATABASE);
        	jongo = new Jongo(db, 
        	        new JacksonMapper.Builder()
        	            .build());
        }
		catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host given in MongoDB.ServerURL value in system properties: "+MONGO_SERVER_URL);
		}
    }

    public void refreshPermissions() throws DaoException {
        
        log.info("Refreshing denormalized permissions");
        
        Map<String,Subject> subjectMap = new HashMap<String,Subject>();
        for(Subject subject : jongo.getCollection("subject").find().as(Subject.class)) {
            subjectMap.put(subject.getKey(), subject);
        }
        
        for(int i=0; i<domainTypes.length; i++) {
            String domainType = domainTypes[i];
            Class domainClass = domainClasses[i];
            
            log.info("Refreshing denormalized permissions for "+domainType);
            
            MongoCollection collection = jongo.getCollection(domainType);

            for(Object obj : collection.find().as(domainClass)) {
                DomainObject domainObject = (DomainObject)obj;
                Subject owner = subjectMap.get(domainObject.getOwnerKey());
                collection.update("{id:#}",domainObject.getId()).with("{$addToSet:{readers:#},$addToSet:{writers:#}}",owner,owner);
                
                for(String groupKey : owner.getGroups()) {
                    collection.update("{id:#}",domainObject.getId()).with("{$addToSet:{readers:#}}",groupKey);
                }
            }
        }
    }
}
