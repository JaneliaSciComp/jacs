package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_SUPPORTING_DATA)
public class SupportingData extends EntityNode {

	private static final long serialVersionUID = 1L;
	
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_ENTITY)
    private List<EntityNode> entities;
    
    /* EVERYTHING BELOW IS AUTO GENERATED */    

    public List<EntityNode> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityNode> entities) {
        this.entities = entities;
    }
}
