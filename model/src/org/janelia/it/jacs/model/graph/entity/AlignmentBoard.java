package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_ALIGNMENT_BOARD)
public class AlignmentBoard extends EntityNode {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)
    private String opticalResolution;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)
    private String pixelResolution;

    @GraphAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE)
    private String alignmentSpace;

    @GraphAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_BOARD_USER_SETTINGS)
    private String userSettings;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_ITEM)
    private List<AlignedItem> alignedItems;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getOpticalResolution() {
        return opticalResolution;
    }

    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }

    public String getPixelResolution() {
        return pixelResolution;
    }

    public void setPixelResolution(String pixelResolution) {
        this.pixelResolution = pixelResolution;
    }

    public String getAlignmentSpace() {
        return alignmentSpace;
    }

    public void setAlignmentSpace(String alignmentSpace) {
        this.alignmentSpace = alignmentSpace;
    }

    public String getUserSettings() {
        return userSettings;
    }

    public void setUserSettings(String userSettings) {
        this.userSettings = userSettings;
    }

    public List<AlignedItem> getAlignedItems() {
        return alignedItems;
    }

    public void setAlignedItems(List<AlignedItem> alignedItems) {
        this.alignedItems = alignedItems;
    }
}
