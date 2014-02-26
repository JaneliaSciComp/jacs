package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_IMAGE_TILE)
public class ImageTile extends Renderable {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
    private String anatomicalArea;

    @RelatedTo(targetNodeType=EntityConstants.TYPE_LSM_STACK)
    private List<LsmStack> lsmStacks;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    public List<LsmStack> getLsmStacks() {
        return lsmStacks;
    }

    public void setLsmStacks(List<LsmStack> lsmStacks) {
        this.lsmStacks = lsmStacks;
    }
    
}
