package org.janelia.it.jacs.model.graph.entity.support;

import org.janelia.it.jacs.model.graph.entity.EntityNode;
import org.janelia.it.jacs.model.graph.entity.EntityRelationship;

/**
 * Graph node utilties specific to the entity-based implementation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityGraphUtils {

    /**
     * Returns true if all related nodes (target nodes on all relationships) have isThisInit=true.
     * @param node
     * @return
     */
    public static boolean areRelatedNodesLoaded(EntityNode node) {
        for (EntityRelationship rel : node.getRelationships()) {
            if (!rel.getTargetNode().isThisInit()) {
            	return false;
            }
        }
        return true;
    }
    
    /**
     * Copy all properties, attributes, permissions, and relationships from one node instance to another. 
     * @param relatedNode
     * @param relatedStub
     */
	public static void copyNode(EntityNode relatedNode, EntityNode relatedStub) {

		if (!relatedNode.getId().equals(relatedStub.getId())) {
			throw new IllegalArgumentException("Cannot copy node when ids do not match: "+relatedNode.getId()+"!="+relatedStub.getId());
		}
		
		relatedStub.setName(relatedNode.getName());
		relatedStub.setCreationDate(relatedNode.getCreationDate());
		relatedStub.setUpdatedDate(relatedNode.getUpdatedDate());
		relatedStub.setOwnerKey(relatedNode.getOwnerKey());
		
		// TODO: we need to copy other stuff like permissions. For now, lets throw this:
		throw new UnsupportedOperationException();
	}
}
