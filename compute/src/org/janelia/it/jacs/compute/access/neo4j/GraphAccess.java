package org.janelia.it.jacs.compute.access.neo4j;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.jboss.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GraphAccess {
	
	@Autowired Neo4jTemplate template;
	
	private Node userReferenceNode;
	private Node entityReferenceNode;
	
//	private static Index<Node> nodeIndex;
	
	private static enum RelTypes implements RelationshipType
    {
        USER_REFERENCE,
        ENTITY_REFERENCE,
        ENTITY,
        OWNER,
        CHILD
    }
	
	public GraphDatabaseService getGraphDatabaseService() {
		return template.getGraphDatabaseService();
	}

	/**
	 * Must be called within a transaction.
	 * @param entity
	 * @return
	 */
	public void initRefNodes() {
		
		GraphDatabaseService gds = template.getGraphDatabaseService();
//		Transaction tx = gds.beginTx();
		
//		try {
//			Node userReferenceNode = gds.createNode();
//			gds.getReferenceNode().createRelationshipTo(userReferenceNode, RelTypes.USER_REFERENCE);

			entityReferenceNode = gds.createNode();
			gds.getReferenceNode().createRelationshipTo(entityReferenceNode, RelTypes.ENTITY_REFERENCE);
			
//			tx.success();
//		}
//		catch (Exception e) {
//			tx.failure();
//		}
//		finally {
//			tx.finish();
//		}
	}
	
	/**
	 * Must be called within a transaction.
	 * @param entity
	 * @return
	 */
	public Node createEntityNode(Entity entity) {
		GraphDatabaseService gds = template.getGraphDatabaseService();
		Node node = gds.createNode();
		node.setProperty("entityId", entity.getId());
		node.setProperty("name", entity.getName());
        node.setProperty("username", entity.getUser().getUserLogin());
        node.setProperty("entityType", entity.getEntityType().getName());
        
        for(EntityData ed : entity.getEntityData()) {
        	if (ed.getValue()!=null) {
        		node.setProperty(ed.getEntityAttribute().getName(), ed.getValue());
        	}
        }
        
        entityReferenceNode.createRelationshipTo(node, RelTypes.ENTITY);
        return node;
	}
	
	public void addChild(Node node, Node child) {
		node.createRelationshipTo(child, RelTypes.CHILD);
	}
	
//	public Node createUser(String username) {
//		Node node = template.createNode();
//        node.setProperty("username", username);
//        nodeIndex.add(node, "username", username);
//        return node;
//	}
	
//	public void insert(List<GraphEntity> graphEntities) {
//		Transaction tx = template.beginTx();
//		try {
//			for(GraphEntity graphEntity : graphEntities) {
//				insert(graphEntity);
//			}	
//			tx.success();
//		}
//		finally {
//			tx.finish();
//		}
//	}
//	
//	public void insert(GraphEntity graphEntity) {
//		saveGraphEntity(graphEntity);
//	}
//
//	public void saveGraphEntity(GraphEntity graphEntity) {
//		template.save(graphEntity);
//	}
//	
//	public Node getNode(Long entityId) {
//		return template.getNode(entityId);
//	}
//	
//	public void saveGraphRelationship(GraphRelationship graphRel) {
//		template.save(graphRel);
//	}
	
}
