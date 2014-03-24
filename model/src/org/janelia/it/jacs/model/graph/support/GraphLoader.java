package org.janelia.it.jacs.model.graph.support;

import org.janelia.it.jacs.model.graph.entity.EntityNode;

/**
 * Interface for loading lazy graph nodes and relationships. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface GraphLoader {

	/**
	 * Load the given node's properties, attributes, permissions, and possibly relationships (the last depends on the implementation.)
	 * The given node must have a populated id property. 
	 * @param node
	 * @return
	 * @throws Exception
	 */
    public EntityNode loadNode(EntityNode node) throws Exception;
    
    /**
     * Load the given node's relationships. If the implementation chooses to load relationships along with the rest of the node, then
     * this method may be superfluous. 
     * @param node
     * @return
     * @throws Exception
     */
    public EntityNode loadRelationships(EntityNode node) throws Exception;
    
    /**
     * Load all the related nodes for the given node. That is, perform the equivalent of loadNode on all target nodes of all relationships
     * of the given node. However, the implementation usually does this more efficiently, by batching the loads. 
     * @param node
     * @return
     * @throws Exception
     */
    public EntityNode loadRelatedNodes(EntityNode node) throws Exception;
    
}
