package org.janelia.it.jacs.compute.access.mongodb;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.compute.access.solr.AncestorSet;
import org.janelia.it.jacs.compute.access.solr.KeyValuePair;
import org.janelia.it.jacs.compute.access.solr.SimpleAnnotation;
import org.janelia.it.jacs.compute.access.solr.SimpleEntity;
import org.janelia.it.jacs.compute.api.support.SolrUtils;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

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
	protected MongoClient m;
	protected DB db;
	protected DBCollection ec;
	protected DBCollection dc;
	
    public MongoDbDAO(Logger _logger) {
    	super(_logger);
    }
    
    private void init() throws DaoException {
    	if (m!=null) return;
        try {
        	m = new MongoClient(MONGO_SERVER_URL);
        	db = m.getDB(MONGO_DATABASE);
        	ec =  db.getCollection("entity");
        	dc =  db.getCollection("edge");
        }
		catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host given in MongoDB.ServerURL value in system properties: "+MONGO_SERVER_URL);
		}
    }
    
    public void loadAllEntities() throws DaoException {

    	largeOp = new LargeOperations(this);
    	largeOp.buildAnnotationMap();
    	largeOp.buildAncestorMap();
    	
    	log.info("Getting entities");
    	
    	Map<Long,SimpleEntity> entityMap = new HashMap<Long,SimpleEntity>();
    	
        int i = 0;
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
    	try {
	        conn = getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select e.id, e.name, e.creation_date, e.updated_date, e.entity_type, e.owner_key, ed.entity_att, ed.value, ed.id, ed.child_entity_id, p.subject_key ");
	        sql.append("from entity e ");
	        sql.append("left outer join entityData ed on e.id=ed.parent_entity_id ");
	        sql.append("left outer join entity_actor_permission p on p.entity_id = e.id ");
	        
	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        stmt.setFetchSize(Integer.MIN_VALUE);
	        
			rs = stmt.executeQuery();
	    	log.info("    Processing results");
			while (rs.next()) {
				Long entityId = rs.getBigDecimal(1).longValue();
				SimpleEntity entity = entityMap.get(entityId);
				if (entity==null) {
					if (i>0) {
		            	if (i%MONGODB_LOADER_BATCH_SIZE==0) {
		            		List<DBObject> l = createEntityDocs(entityMap.values());
		            		List<DBObject> l2 = createEdgeDocs(entityMap.values());
		                    entityMap.clear();
		            		log.info("    Inserting "+l.size()+" entity docs and "+l2.size()+" edge docs (i="+i+")");
		            		ec.insert(l);
		            		dc.insert(l2);
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
					entity.getSubjectKeys().add(rs.getString(6));
				}

				String key = rs.getString(7);
				String value = rs.getString(8);
				BigDecimal edIdBD = rs.getBigDecimal(9);
				BigDecimal childIdBD = rs.getBigDecimal(10);
				String subjectKey = rs.getString(11);

				if (childIdBD != null) {
					Long childId = childIdBD.longValue();
					Long edId = edIdBD.longValue();
					entity.getChildIds().add(childId);
					entity.getAttributes().add(new KeyValuePair(key, value, childId, edId));
				}
				else if (key!=null && value!=null) {
					entity.getAttributes().add(new KeyValuePair(key, value));
				}
				
				if (subjectKey!=null) {
					entity.getSubjectKeys().add(subjectKey);
				}
				
			}

        	if (!entityMap.isEmpty()) {
        		List<DBObject> l = createEntityDocs(entityMap.values());
        		log.info("    Inserting "+l.size()+" docs (i="+i+")");
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

    	log.info("Completed adding "+i+" entities");

    	log.info("Creating indexes");
    	
        ec.ensureIndex("name");
        ec.ensureIndex("type");
        ec.ensureIndex("subjects");
        ec.ensureIndex("username");
        ec.ensureIndex("children");
        ec.ensureIndex("ancestors");
        ec.ensureIndex("priors");
        
    	log.info("Completed data load");
    }

    protected List<DBObject> createEntityDocs(Collection<SimpleEntity> entities) {
        List<DBObject> docs = new ArrayList<DBObject>();
        for(SimpleEntity se : entities) {
        	docs.add(createEntityDoc(se));
        }
        return docs;
    }

    protected List<DBObject> createEdgeDocs(Collection<SimpleEntity> entities) {
        List<DBObject> docs = new ArrayList<DBObject>();
        for(SimpleEntity se : entities) {
            for(KeyValuePair pair : se.getAttributes()) {
                if (pair.getEdId()!=null) {
                    docs.add(createEdgeDoc(pair.getEdId(), se.getId(), pair.getChildId(), pair.getKey()));
                }
            }
        }
        return docs;
    }
    
    public void dropDatabase() throws DaoException {
    	init();
		try {
	    	db.dropDatabase();
		}
		catch (Exception e) {
			throw new DaoException("Error clearing index with MongoDB",e);
		}
    }

    protected DBObject createEntityDoc(SimpleEntity entity) {

    	Set<SimpleAnnotation> annotations = null;
    	AncestorSet ancestorSet = null;
    	if (largeOp!=null) {
    		annotations = (Set<SimpleAnnotation>)largeOp.getValue("annotationMapCache", entity.getId());
    		ancestorSet = (AncestorSet)largeOp.getValue("ancestorMapCache", entity.getId());
    	}
    	else {
    		log.warn("Large operations were not run, so we have no annotations or ancestors.");
    		// TODO: get these another way
    	}
    	
    	BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
		builder.add("_id", entity.getId());
		builder.add("type", entity.getEntityTypeName());
		builder.add("name", entity.getName());
    	builder.add("creation_date", entity.getCreationDate());
    	builder.add("updated_date", entity.getUpdatedDate());
    	
    	Map<String,String> attrs = new HashMap<String,String>();

    	for(KeyValuePair kv : entity.getAttributes()) {
    		String key = SolrUtils.getFormattedName(kv.getKey());
    		if (kv.getValue()!=null) {
    		    attrs.put(key, kv.getValue());
    		}
    	}
    	
    	Set<String> subjectKeys = new HashSet<String>();
    	subjectKeys.add(entity.getOwnerName());
    	subjectKeys.addAll(entity.getSubjectKeys());
    	
    	builder.add("attributes", attrs);
    	builder.add("children", entity.getChildIds());
    	builder.add("subjects", subjectKeys);
    			
    	if (ancestorSet!=null) {
    		builder.add("ancestors", ancestorSet.getAncestors());
    	}
    	
    	if (annotations != null) {
	    	List<Map<String,Object>> annots = new ArrayList<Map<String,Object>>();
	    	for(SimpleAnnotation annotation : annotations) {
	    	    Map<String,Object> annotMap = new HashMap<String,Object>();
	    	    annotMap.put("subject",annotation.getOwner());
	    	    annotMap.put("key",annotation.getValue());
	    	    annotMap.put("value",annotation.getKey());
	    	    annotMap.put("tag",annotation.getTag());
				annots.add(annotMap);
	    	}
	    	builder.add("annotations", annots);
    	}
    	
    	return builder.get();
    }
    
    protected DBObject createEdgeDoc(Long edgeId, Long sourceEntityId, Long targetEntityId, String type) {
        
        if (edgeId==null) {
            edgeId = (Long)TimebasedIdentifierGenerator.generate(1);
        }
        
        List<Long> priors = new ArrayList<Long>();
        priors.add(edgeId);
        
        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start();
        builder.add("_id", edgeId);
        builder.add("type", type);
        builder.add("entry_edge", edgeId);
        builder.add("direct_edge", edgeId);
        builder.add("exit_edge", edgeId);
        builder.add("source", sourceEntityId);
        builder.add("target", targetEntityId);
        builder.add("hops", edgeId);
        builder.add("priors", priors);
        builder.add("graph_space", "model");
        
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
