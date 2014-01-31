package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_SUPPORTING_DATA)
public class SupportingData extends EntityNode {

    @RelatedTo(targetNodeType=EntityConstants.TYPE_IMAGE_TILE)
    private List<ImageTile> tiles;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public List<ImageTile> getTiles() {
        return tiles;
    }

    public void setTiles(List<ImageTile> tiles) {
        this.tiles = tiles;
    }
}
