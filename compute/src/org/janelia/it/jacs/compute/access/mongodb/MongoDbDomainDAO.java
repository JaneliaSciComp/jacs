package org.janelia.it.jacs.compute.access.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.Subject;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbDomainDAO extends AnnotationDAO {

    protected final static int MONGODB_LOADER_BATCH_SIZE = 20000;
    protected final static int MONGODB_INSERTS_PER_SECOND = 5000;
	
	protected static final String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	protected static final String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");

	protected LargeOperations largeOp;
	protected MongoClient m;
	protected DB db;
	protected DBCollection workspaceCollection;
	protected DBCollection sampleCollection;
	
    public MongoDbDomainDAO(Logger _logger) {
    	super(_logger);
    }
    
    private void init() throws DaoException {
    	if (m!=null) return;
        try {
        	m = new MongoClient(MONGO_SERVER_URL);
        	m.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
        	db = m.getDB(MONGO_DATABASE);
        	workspaceCollection =  db.getCollection("workspace");
        	sampleCollection =  db.getCollection("sample");
        }
		catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host given in MongoDB.ServerURL value in system properties: "+MONGO_SERVER_URL);
		}
    }
    
    public void loadAllEntities(String subjectKey) throws DaoException {

//    	largeOp = new LargeOperations(this);
//    	largeOp.buildAnnotationMap();
    	
    	log.info("Adding samples");
    	
    	for(Entity sample : getEntitiesByTypeName(subjectKey, EntityConstants.TYPE_SAMPLE)) {
    	    DBObject sampleObj = getSampleObject(sample);
            sampleCollection.insert(sampleObj);
            log.info("  Added "+sample.getName());
            // Free memory
            sample.setEntityData(null);
    	}

        log.info("Adding workspace");
        DBObject workspace = getWorkspaceObject(getSubjectByNameOrKey(subjectKey));
        workspaceCollection.insert(workspace);
        
        log.info("Creating indexes");
        
        sampleCollection.ensureIndex("name");
        sampleCollection.ensureIndex("owner_key");
        
    }
    
    private DBObject getWorkspaceObject(Subject subject) throws DaoException {

        DBObject workspace = new BasicDBObject();
        workspace.put("_id", subject.getId());
        workspace.put("subject_name", subject.getName());
        workspace.put("subject_key", subject.getKey());
        
        List<DBObject> folders = new ArrayList<DBObject>();
        for(Entity folder : getEntitiesWithTag(subject.getKey(), EntityConstants.ATTRIBUTE_COMMON_ROOT)) {
            folders.add(getMongoFolderHierarchy(folder));
        }

        workspace.put("folders", folders);
        return workspace;
    }
    
    private DBObject getSampleObject(Entity sample) {

        DBObject sampleObj = new BasicDBObject();
        sampleObj.put("_id", sample.getId());
        sampleObj.put("name", sample.getName());
        sampleObj.put("subject_key", sample.getOwnerKey());
        sampleObj.put("creation_date", sample.getCreationDate());
        sampleObj.put("updated_date", sample.getUpdatedDate());
        sampleObj.putAll(getMongoObject(sample));
        
        return sampleObj;
//        
//        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
//        builder.add("_id", sample.getId());
//        addSampleBasics(sample, builder);
//        
//        List<Entity> subsamples = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
//        if (subsamples.isEmpty()) {
//            for(Entity subsample : subsamples) {
//                BasicDBObjectBuilder builder2 = BasicDBObjectBuilder.start();
//                addSampleBasics(subsample, builder2);
//                addSampleDetails(subsample, builder2);    
//            }   
//        }
//        else {
//            addSampleDetails(sample, builder);
//        }
//        
//        return builder.get();
    }
    
    private void addSampleBasics(Entity sample, BasicDBObjectBuilder builder) {
        builder.add("name", sample.getName());
        builder.add("subject_key", sample.getOwnerKey());
        builder.add("creation_date", sample.getCreationDate());
        builder.add("updated_date", sample.getUpdatedDate());
        addAttributes(sample, builder);
    }
    
    private void addAttributes(Entity entity, BasicDBObjectBuilder builder) {
        for(EntityData ed : entity.getEntityData()) {
            String value = ed.getValue();
            if (value==null) continue;
            String key = SolrUtils.getFormattedName(ed.getEntityAttrName());
            builder.add(key, value);
        }
    }
    
//    private void addSampleDetails(Entity sample, BasicDBObjectBuilder builder) {
//        getMongoObject
//    }
    
    private DBObject getMongoObject(Entity entity) {
        
        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
        builder.add("name", entity.getName());
        addAttributes(entity, builder);
        
        List<DBObject> children = new ArrayList<DBObject>();
        for(Entity child : entity.getOrderedChildren()) {
            children.add(getMongoObject(child));
        }

        builder.add("children", children);
        return builder.get();
    }

    
    private DBObject getMongoFolderHierarchy(Entity folder) {
        
        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
        builder.add("name", folder.getName());
        builder.add("subject_key", folder.getOwnerKey());
        addAttributes(folder, builder);
        
        List<DBObject> children = new ArrayList<DBObject>();
        List<Long> childrenIds = new ArrayList<Long>();
        
        for(Entity child : folder.getOrderedChildren()) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                children.add(getMongoObject(child));
            }
            else {
                childrenIds.add(child.getId());
            }
        }

        if (!children.isEmpty()) {
            builder.add("children", children);
        }
        else if (!childrenIds.isEmpty()) {
            builder.add("children", childrenIds);
        }
        
        return builder.get();
    }
    
    
    
//    private void addSampleDetails(Entity sample, BasicDBObjectBuilder builder) {
//        
//        Entity supportingData = EntityUtils.getSupportingData(sample);
//        
//        BasicDBObject tiles = new BasicDBObject();
//        for(Entity tileEntity : supportingData.getChildren()) {
//            
//            BasicDBObject lsms = new BasicDBObject();
//            for(Entity lsmEntity : tileEntity.getChildren()) {
//                BasicDBObjectBuilder lsmBuilder = BasicDBObjectBuilder.start();
//                addAttributes(lsmEntity, lsmBuilder);
//                lsms.put(lsmEntity.getName(), builder.get());
//            }
//            
//            BasicDBObject tile = new BasicDBObject();
//            tile.put("anatomical_area", tileEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA));
//            tile.put("lsms", lsms);
//            tiles.put(tileEntity.getName(), tile);
//        }
//        
//        builder.add("tiles", tiles);
//
//        List<DBObject> runs = new ArrayList<DBObject>();
//        for(Entity runEntity : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN)) {
//
//            List<DBObject> results = new ArrayList<DBObject>();
//            for(Entity resultEntity : EntityUtils.getChildrenForAttribute(runEntity, EntityConstants.ATTRIBUTE_RESULT)) {
//
//                Entity resultSupportingData = EntityUtils.getSupportingData(sample);
//                
//                
//                
//                
//                BasicDBObject result = new BasicDBObject();
//                BasicDBObjectBuilder resultBuilder = BasicDBObjectBuilder.start();
//                result.put("name", resultEntity.getName());
//                result.put("creation_date", resultEntity.getCreationDate());
//                result.put("type", resultEntity.getEntityTypeName());
//                addAttributes(resultEntity, resultBuilder);
//                results.add(result);
//            }
//
//            BasicDBObject run = new BasicDBObject();
//            run.put("name", runEntity.getName());
//            run.put("pipeline_process", runEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS));
//            run.put("creation_date", runEntity.getCreationDate());
//            run.put("results", results);
//            
//            runs.add(run);
//        }
//
//        builder.add("runs", runs);
//    }
    

    public void dropDatabase() throws DaoException {
    	init();
		try {
	    	db.dropDatabase();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with MongoDB",e);
		}
    }
}
