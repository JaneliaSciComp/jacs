package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;

@GraphNode(type=EntityConstants.TYPE_IMAGE_3D)
public class Image3d extends Renderable {

    @GraphAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE)
    private String alignmentSpace;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_COLORS)
    private String channelColors;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)
    private String chanSpec;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_FILE_PATH)
    private String filepath;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_OBJECTIVE)
    private String objective;

    @GraphAttribute(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)
    private String opticalResolution;

    @GraphAttribute(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)
    private String pixelResolution;

    public String getAlignmentSpace() {
        return alignmentSpace;
    }

    /* EVERYTHING BELOW IS AUTO GENERATED */

    public void setAlignmentSpace(String alignmentSpace) {
        this.alignmentSpace = alignmentSpace;
    }

    public String getChannelColors() {
        return channelColors;
    }

    public void setChannelColors(String channelColors) {
        this.channelColors = channelColors;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

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
}
