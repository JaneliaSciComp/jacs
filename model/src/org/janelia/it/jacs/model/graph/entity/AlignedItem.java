package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;
@GraphNode(type=EntityConstants.TYPE_ALIGNED_ITEM)
public class AlignedItem extends EntityNode {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_VISIBILITY)
    private Boolean visibilty;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_ITEM)
    private EntityNode wrappedItem;

    public Boolean getVisibilty() {
        return visibilty;
    }

    public void setVisibilty(Boolean visibilty) {
        this.visibilty = visibilty;
    }

    public EntityNode getWrappedItem() {
        return wrappedItem;
    }

    public void setWrappedItem(EntityNode wrappedItem) {
        this.wrappedItem = wrappedItem;
    }
}
