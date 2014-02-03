package org.janelia.it.jacs.model.graph.entity.support;

import org.janelia.it.jacs.model.graph.entity.EntityNode;

/**
 * Interface for loading lazy graph nodes and relationships. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface GraphLoader {

    public EntityNode loadNode(EntityNode node) throws Exception;
    
    public EntityNode loadRelationships(EntityNode node) throws Exception;
    
    public EntityNode loadRelatedNodes(EntityNode node) throws Exception;
    
}
