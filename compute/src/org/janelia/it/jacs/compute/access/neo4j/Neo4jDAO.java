package org.janelia.it.jacs.compute.access.neo4j;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.solr.KeyValuePair;
import org.janelia.it.jacs.compute.access.solr.SimpleEntity;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Data access to the Neo4j data store.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Neo4jDAO extends AnnotationDAO {

    protected final static int NEO4J_LOADER_BATCH_SIZE = 5000;
	
	protected static final String NEO4J_SERVER_URL = SystemConfigurationProperties.getString("Neo4j.ServerURL");

	protected GraphAccess ga;
	protected Transaction tx;
	protected int numAdded = 0;
	
    public Neo4jDAO(Logger _logger) {
    	super(_logger);

    	ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
    			new String[] {"neo4j-beans.xml"});
    	BeanFactory factory = (BeanFactory) appContext;
    	ga = (GraphAccess)factory.getBean("graphAccess");
    }

    
    public void loadAllEntities() throws DaoException {

    	tx = ga.getGraphDatabaseService().beginTx();
    	_logger.info("Creating references nodes");
    	ga.initRefNodes();
    	
    	Map<Long,Long> visited = new HashMap<Long,Long>();
    	
    	try {
    		List<Entity> roots = getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_COMMON_ROOT, EntityConstants.ATTRIBUTE_COMMON_ROOT);
    		
    		_logger.info("Found "+roots.size()+" common roots");
    		
        	for(Entity root : roots) {
        		_logger.info("Loading "+root.getName());
        		if ("system".equals(root.getUser().getUserLogin())) {
        			_logger.info("  Skipping "+root.getName());
        			continue;
        		}
        		loadEntity(root, visited);
        	}
    		tx.success();
    	}
    	catch (Exception e) {
    		tx.failure();
    		throw new DaoException("Error loading entities",e);
    	}
    	finally {
    		tx.finish();
    		ga.getGraphDatabaseService().shutdown();
    	}
    	
    	_logger.info("Completed loading "+visited.size()+" entities");
    	
    	
    }
    
    private Node loadEntity(Entity entity, Map<Long,Long> visited) throws DaoException {
    	
    	if (numAdded>0 && numAdded % NEO4J_LOADER_BATCH_SIZE == 0) {
    		_logger.info("  Commiting a batch");
    		tx.success();
    		tx.finish();
    		tx = ga.getGraphDatabaseService().beginTx();
    	}
    	
    	Node node = null;
    	Long neo4jId = visited.get(entity.getId());
    	if (neo4jId!=null) {
    		return ga.getGraphDatabaseService().getNodeById(neo4jId);
    	}
    	else {
    		node = ga.createEntityNode(entity);
        	visited.put(entity.getId(), node.getId());
        	for(Entity child : entity.getChildren()) {
        		Node childNode = loadEntity(child, visited);
        		ga.addChild(node, childNode);
        	}
        	numAdded++;
    	}
    	
    	return node;
    }
    	
//    public void loadAllEntities() throws DaoException {
//
//    	_logger.info("Getting entities");
//    	
//    	Map<Long,SimpleEntity> entityMap = new HashMap<Long,SimpleEntity>();
//        int i = 0;
//    	Connection conn = null;
//    	PreparedStatement stmt = null;
//    	ResultSet rs = null;
//    	try {
//	        conn = getJdbcConnection();
//
//	        StringBuffer sql = new StringBuffer();
//	        sql.append("select e.id, e.name, e.creation_date, e.updated_date, et.name, u.user_login, ea.name, ed.value ");
//	        sql.append("from entity e ");
//	        sql.append("join user_accounts u on e.user_id = u.user_id ");
//	        sql.append("join entityType et on e.entity_type_id = et.id ");
//	        sql.append("left outer join entityData ed on e.id=ed.parent_entity_id ");
//	        sql.append("left outer join entityAttribute ea on ed.entity_att_id = ea.id ");
//	        sql.append("where ed.value is not null ");
//	        
//	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
//	        stmt.setFetchSize(Integer.MIN_VALUE);
//	        
//			rs = stmt.executeQuery();
//	    	_logger.info("    Processing results");
//			while (rs.next()) {
//				Long entityId = rs.getBigDecimal(1).longValue();
//				SimpleEntity entity = entityMap.get(entityId);
//				if (entity==null) {
//					if (i>0) {
//		            	if (i%NEO4J_LOADER_BATCH_SIZE==0) {
//		            		List<GraphEntity> l = createGraphEntities(entityMap.values());
//		                    entityMap.clear();
//		            		_logger.info("    Inserting "+l.size()+" entities (i="+i+")");
//		            		ga.insert(l);
//		            	}
//					}
//					
//					i++;
//					entity = new SimpleEntity();
//					entityMap.put(entityId, entity);
//					entity.setId(entityId);
//					entity.setName(rs.getString(2));
//					entity.setCreationDate(rs.getDate(3));
//					entity.setUpdatedDate(rs.getDate(4));
//					entity.setEntityTypeName(rs.getString(5));
//					entity.setUserLogin(rs.getString(6));
//				}
//
//				String key = rs.getString(7);
//				String value = rs.getString(8);
//				if (key!=null && value!=null) {
//					entity.getAttributes().add(new KeyValuePair(key, value));
//				}
//				
//			}
//
//        	if (!entityMap.isEmpty()) {
//        		List<GraphEntity> l = createGraphEntities(entityMap.values());
//        		_logger.info("    Inserting "+l.size()+" entities (i="+i+")");
//        		ga.insert(l);
//        	}
//    	}
//    	catch (SQLException e) {
//    		throw new DaoException(e);
//    	}
//        finally {
//        	try {
//        		if (rs!=null) rs.close();
//        		if (stmt!=null) stmt.close();
//        		if (conn!=null) conn.close();		
//        	}
//            catch (Exception e) {
//        		throw new DaoException(e);
//            }
//        }
//
//    	_logger.info("Completed adding "+i+" entities");
//
//    	_logger.info("Creating indexes");
//        
//    	_logger.info("Completed data load");
//    }

    protected List<GraphEntity> createGraphEntities(Collection<SimpleEntity> entities) {
        List<GraphEntity> docs = new ArrayList<GraphEntity>();
        for(SimpleEntity se : entities) {
        	docs.add(createGraphEntity(se));
        }
        return docs;
    }
    
    public void dropDatabase() throws DaoException {
//		try {
//	    	ga.dropDatabase();
//		}
//		catch (Exception e) {
//			throw new DaoException("Error clearing index with SOLR",e);
//		}
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
    
    protected GraphEntity createGraphEntity(Entity entity) {
    	SimpleEntity simpleEntity = simpleEntityFromEntity(entity);
    	return createGraphEntity(simpleEntity);
    }

    protected GraphEntity createGraphEntity(SimpleEntity entity) {

    	GraphEntity ge = new GraphEntity();
    	ge.setEntityId(entity.getId());
    	ge.setName(entity.getName());
    	ge.setUsername(entity.getUserLogin());
    	ge.setEntityType(entity.getEntityTypeName());
    	
//    	Map<String,List<Map<String,Object>>> attrs = new HashMap<String,List<Map<String,Object>>>();
//
//    	for(KeyValuePair kv : entity.getAttributes()) {
//    		String key = SolrUtils.getFormattedName(kv.getKey());
//    		Map<String,Object> value = new HashMap<String,Object>();
//    		if (kv.getValue()!=null) value.put("value", kv.getValue());
//    		List<Map<String,Object>> values = attrs.get(key);
//    		if (values==null) {
//    			values = new ArrayList<Map<String,Object>>();
//    			attrs.put(key, values);
//    		}
//    		values.add(value);
//    	}
    	
    	
    	return ge;
    }

//    public DBCursor search(BasicDBObject query) throws DaoException {
//    	init();
//    	try {
//            return ec.find(query);
//    	}
//		catch (Exception e) {
//			throw new DaoException("Error searching with MongoDB",e);
//		}
//    }
}
