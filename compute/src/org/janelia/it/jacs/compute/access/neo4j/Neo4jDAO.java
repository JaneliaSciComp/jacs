package org.janelia.it.jacs.compute.access.neo4j;

import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.large.LargeOperations;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
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

    protected final static int NEO4J_LOADER_BATCH_SIZE = 3000;
	
	protected static final String NEO4J_SERVER_URL = SystemConfigurationProperties.getString("Neo4j.ServerURL");

	protected GraphAccess ga;
	protected Transaction tx;
	protected int numAdded = 0;
	protected LargeOperations largeOp;
	
    public Neo4jDAO(Logger _logger) {
    	super(_logger);

    	ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
    			new String[] {"neo4j-beans.xml"});
    	BeanFactory factory = (BeanFactory) appContext;
    	this.ga = (GraphAccess)factory.getBean("graphAccess");
    	
    	this.largeOp = new LargeOperations(this);
    }

    public void loadAllEntities() throws DaoException {

    	tx = ga.getGraphDatabaseService().beginTx();
    	_logger.info("Creating references nodes");
    	ga.initRefNodes();
    	
    	try {
    		List<Entity> roots = getUserEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_COMMON_ROOT, EntityConstants.ATTRIBUTE_COMMON_ROOT);
    		
    		_logger.info("Found "+roots.size()+" common roots");
    		
        	for(Entity root : roots) {
        		_logger.info("Loading "+root.getName());
        		loadEntity(root);
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
    	
    	_logger.info("Completed loading "+numAdded+" entities");
    }
    
    private Node loadEntity(Entity entity) throws DaoException {
    	
    	if (numAdded>0 && numAdded % NEO4J_LOADER_BATCH_SIZE == 0) {
    		_logger.info("  Commiting a batch");
    		tx.success();
    		tx.finish();
    		tx = ga.getGraphDatabaseService().beginTx();
    	}
    	
    	Node node = null;
    	
    	Long neo4jId = (Long)largeOp.getValue(LargeOperations.NEO4J_MAP, entity.getId());
    	if (neo4jId!=null) {
    		return ga.getGraphDatabaseService().getNodeById(neo4jId);
    	}
    	else {
    		node = ga.createEntityNode(entity);
    		largeOp.putValue(LargeOperations.NEO4J_MAP, entity.getId(), node.getId());
        	for(Entity child : entity.getChildren()) {
        		Node childNode = loadEntity(child);
        		ga.createChildRelationship(node, childNode);
        	}
        	numAdded++;
    	}
    	
    	return node;
    }
    
    public void dropDatabase() throws DaoException {
    	// How is there no way to do this via the API?
    }

}
