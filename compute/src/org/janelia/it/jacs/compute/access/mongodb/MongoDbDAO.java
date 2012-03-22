package org.janelia.it.jacs.compute.access.mongodb;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.KeyValuePair;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.compute.access.solr.SimpleEntity;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import com.mongodb.*;

/**
 * Data access to the MongoDB data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDbDAO extends AnnotationDAO {

    protected final static int MONGODB_LOADER_BATCH_SIZE = 50000;
	
	protected static final String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	protected static final String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");

	protected LargeOperations largeOp;
	protected Mongo m;
	protected DB db;
	protected DBCollection ec;
	
    public MongoDbDAO(Logger _logger) {
    	super(_logger);
    }
    
    private void init() throws DaoException {
    	if (m!=null) return;
        try {
        	m = new Mongo(MONGO_SERVER_URL);
        	db = m.getDB(MONGO_DATABASE);
        	ec =  db.getCollection("entity");
        }
		catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host given in MongoDB.ServerURL value in system properties: "+MONGO_SERVER_URL);
		}
    }
    
    public void loadAllEntities() throws DaoException {

    	largeOp = new LargeOperations(this);
    	largeOp.buildAnnotationMap();
    	largeOp.buildAncestorMap();
    	
    	_logger.info("Getting entities");
    	
    	Map<Long,SimpleEntity> entityMap = new HashMap<Long,SimpleEntity>();
        int i = 0;
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select e.id, e.name, e.creation_date, e.updated_date, et.name, u.user_login, ea.name, ed.value, ed.child_entity_id ");
	        sql.append("from entity e ");
	        sql.append("join user_accounts u on e.user_id = u.user_id ");
	        sql.append("join entityType et on e.entity_type_id = et.id ");
	        sql.append("left outer join entityData ed on e.id=ed.parent_entity_id ");
	        sql.append("left outer join entityAttribute ea on ed.entity_att_id = ea.id ");
	        
	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
	        
			rs = stmt.executeQuery();
	    	_logger.info("    Processing results");
			while (rs.next()) {
				Long entityId = rs.getBigDecimal(1).longValue();
				SimpleEntity entity = entityMap.get(entityId);
				if (entity==null) {
					if (i>0) {
		            	if (i%MONGODB_LOADER_BATCH_SIZE==0) {
		            		List<DBObject> l = createEntityDocs(entityMap.values());
		                    entityMap.clear();
		            		_logger.info("    Inserting "+l.size()+" docs (i="+i+")");
		            		ec.insert(l);
		            	}
					}
					
					i++;
					entity = new SimpleEntity();
					entityMap.put(entityId, entity);
					entity.setId(entityId);
					entity.setName(rs.getString(2));
					entity.setCreationDate(rs.getDate(3));
					entity.setUpdatedDate(rs.getDate(4));
					entity.setEntityTypeName(rs.getString(5));
					entity.setUserLogin(rs.getString(6));
				}

				String key = rs.getString(7);
				String value = rs.getString(8);
				BigDecimal childIdBD = rs.getBigDecimal(9);

				if (childIdBD != null) {
					Long childId = childIdBD.longValue();
					entity.getChildIds().add(childId);
					entity.getAttributes().add(new KeyValuePair(key, value, childId));
				}
				else if (key!=null && value!=null) {
					entity.getAttributes().add(new KeyValuePair(key, value));
				}
				
			}

        	if (!entityMap.isEmpty()) {
        		List<DBObject> l = createEntityDocs(entityMap.values());
        		_logger.info("    Inserting "+l.size()+" docs (i="+i+")");
        		ec.insert(l);
        	}
    	}
    	catch (SQLException e) {
    		throw new DaoException(e);
    	}
        finally {
        	try {
        		if (rs!=null) rs.close();
        		if (stmt!=null) stmt.close();
        		if (conn!=null) conn.close();		
        	}
            catch (Exception e) {
        		throw new DaoException(e);
            }
        }

    	_logger.info("Completed adding "+i+" entities");

    	_logger.info("Creating indexes");
        ec.ensureIndex("name");
        ec.ensureIndex("entity_type");
        ec.ensureIndex("username");
//        ec.ensureIndex("attributes");
        ec.ensureIndex("child_ids");
        ec.ensureIndex("ancestor_ids");
        
    	_logger.info("Completed data load");
    }

    protected List<DBObject> createEntityDocs(Collection<SimpleEntity> entities) {
        List<DBObject> docs = new ArrayList<DBObject>();
        for(SimpleEntity se : entities) {
        	docs.add(createDoc(se));
        }
        return docs;
    }
    
    public void dropDatabase() throws DaoException {
    	init();
		try {
	    	db.dropDatabase();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with SOLR",e);
		}
    }

    protected SimpleEntity simpleEntityFromEntity(Entity entity) {
    	SimpleEntity simpleEntity = new SimpleEntity();
    	simpleEntity.setId(entity.getId());
    	simpleEntity.setName(entity.getName());
    	simpleEntity.setCreationDate(entity.getCreationDate());
    	simpleEntity.setUpdatedDate(entity.getUpdatedDate());
    	
    	Set<Long> childrenIds = new HashSet<Long>();
    	for(EntityData ed : entity.getEntityData()) {
    		if (ed.getValue()!=null) {
    			String attr = (ed.getEntityAttribute().getName());
    			simpleEntity.getAttributes().add(new KeyValuePair(attr, ed.getValue()));
    		}
    		else if (ed.getChildEntity()!=null) {
    			childrenIds.add(ed.getChildEntity().getId());
    			simpleEntity.getChildIds().add(ed.getChildEntity().getId());
    		}
    	}
    	
    	return simpleEntity;
    }
    
    protected DBObject createDoc(Entity entity) {
    	SimpleEntity simpleEntity = simpleEntityFromEntity(entity);
    	return createDoc(simpleEntity);
    }

    protected DBObject createDoc(SimpleEntity entity) {

    	Set<SimpleAnnotation> annotations = null;
    	AncestorSet ancestorSet = null;
    	if (largeOp!=null) {
    		annotations = (Set<SimpleAnnotation>)largeOp.getValue("annotationMapCache", entity.getId());
    		ancestorSet = (AncestorSet)largeOp.getValue("ancestorMapCache", entity.getId());
    	}
    	else {
    		_logger.warn("Large operations were not run, so we have no annotations or ancestors.");
    		// TODO: get these another way
    	}
    	
    	BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
		builder.add("_id", entity.getId());
    	builder.add("name", entity.getName());
    	builder.add("creation_date", entity.getCreationDate());
    	builder.add("updated_date", entity.getUpdatedDate());
    	builder.add("username", entity.getUserLogin());
    	builder.add("entity_type", entity.getEntityTypeName());
    
    	Map<String,List<Map<String,Object>>> attrs = new HashMap<String,List<Map<String,Object>>>();

    	for(KeyValuePair kv : entity.getAttributes()) {
    		String key = SolrUtils.getFormattedName(kv.getKey());
    		Map<String,Object> value = new HashMap<String,Object>();
    		if (kv.getValue()!=null) value.put("value", kv.getValue());
    		if (kv.getChildId()!=null) value.put("child_id", kv.getChildId());
    		List<Map<String,Object>> values = attrs.get(key);
    		if (values==null) {
    			values = new ArrayList<Map<String,Object>>();
    			attrs.put(key, values);
    		}
    		values.add(value);
    	}
    	
    	builder.add("attributes", attrs);
    	builder.add("child_ids", entity.getChildIds());
    	
    	if (ancestorSet!=null) {
    		builder.add("ancestor_ids", ancestorSet.getAncestors());
    	}
    	
    	if (annotations != null) {
	    	List<String> annots = new ArrayList<String>();
	    	for(SimpleAnnotation annotation : annotations) {
				annots.add(annotation.getTag());
	    	}
	    	builder.add("annotations", annots);
    	}
    	
    	return builder.get();
    }

    public DBCursor search(BasicDBObject query) throws DaoException {
    	init();
    	try {
            return ec.find(query);
    	}
		catch (Exception e) {
			throw new DaoException("Error searching with MongoDB",e);
		}
    }
}
