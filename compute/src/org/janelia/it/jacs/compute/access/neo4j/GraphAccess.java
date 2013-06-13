package org.janelia.it.jacs.compute.access.neo4j;

import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * Access to the Neo4j graph database service defined in the neo4j-beans.xml configuration file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GraphAccess {
	
//	@Autowired Neo4jTemplate template;
	
	private Node subjectReferenceNode;
	private Node rootReferenceNode;
	
	private static Index<Node> entityIndex;
	private static Index<Node> subjectIndex;
	
	private static Map<String,Node> subjectMap = new HashMap<String,Node>();
	
	private static enum RelTypes implements RelationshipType
    {
        SUBJECT_REFERENCE,
		ROOT_REFERENCE,
		OWNER,
        CHILD
    }
	
	public GraphDatabaseService getGraphDatabaseService() {
		return null;//template.getGraphDatabaseService();
	}

	/**
	 * Must be called within a transaction.
	 * @param entity
	 * @return
	 */
	public void initRefNodes() {
		
		GraphDatabaseService gds = null;//template.getGraphDatabaseService();
		Transaction tx = gds.beginTx();
		
		try {
			subjectReferenceNode = gds.createNode();
			gds.getReferenceNode().createRelationshipTo(subjectReferenceNode, RelTypes.SUBJECT_REFERENCE);

			rootReferenceNode = gds.createNode();
			gds.getReferenceNode().createRelationshipTo(rootReferenceNode, RelTypes.ROOT_REFERENCE);
			
			tx.success();
		}
		catch (Exception e) {
			tx.failure();
		}
		finally {
			tx.finish();
		}
	}
	
	/**
	 * Must be called within a transaction.
	 * @param entity
	 * @return
	 */
	public Node createEntityNode(Entity entity) {
		GraphDatabaseService gds = null;//template.getGraphDatabaseService();
		Node node = gds.createNode();
		node.setProperty("entityId", entity.getId());
		node.setProperty("name", entity.getName());
        node.setProperty("entityType", entity.getEntityType().getName());
        
        for(EntityData ed : entity.getEntityData()) {
        	if (ed.getValue()!=null) {
        		node.setProperty(ed.getEntityAttribute().getName(), ed.getValue());
        	}
        }
        
        if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
        	rootReferenceNode.createRelationshipTo(node, RelTypes.ROOT_REFERENCE);	
        }
        
        Node subjectNode = getOrCreateSubjectNode(entity.getOwnerKey());
        node.createRelationshipTo(subjectNode, RelTypes.OWNER);
        
        entityIndex.add(node, "entityId", entity.getId());
        return node;
        
	}
	
	public void createChildRelationship(Node node, Node child) {
		node.createRelationshipTo(child, RelTypes.CHILD);
	}
	
	public Node getOrCreateSubjectNode(String subjectKey) {
		if (subjectMap.containsKey(subjectKey)) {
			return subjectMap.get(subjectKey);
		}
		GraphDatabaseService gds = null;//template.getGraphDatabaseService();
		Node node = gds.createNode();
        node.setProperty("subjectKey", subjectKey);
        subjectIndex.add(node, "subjectKey", subjectKey);
        subjectMap.put(subjectKey, node);
        node.createRelationshipTo(subjectReferenceNode, RelTypes.SUBJECT_REFERENCE);
        return node;
	}
	
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
