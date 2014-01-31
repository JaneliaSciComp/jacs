package org.janelia.it.jacs.model.graph.entity.support;

import org.janelia.it.jacs.model.graph.entity.EntityNode;

public interface GraphLoader {

    public void loadNode(EntityNode node);
    
    public void loadRelationships(EntityNode node);
    
    public void loadRelatedNodes(EntityNode node);
    
}
