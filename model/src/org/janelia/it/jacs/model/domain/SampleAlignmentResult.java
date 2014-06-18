package org.janelia.it.jacs.model.domain;

import java.util.Map;

public class SampleAlignmentResult extends PipelineResult implements HasImages {

	private String name;
    private String imageSize;
    private String opticalResolution;
    private String alignmentSpace;
    private String boundingBox;
    private String channelColors;
    private String chanSpec;
    private String objective;
    private Map<ImageType,String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
    public String getImageSize() {
        return imageSize;
    }
	public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }
    public String getOpticalResolution() {
        return opticalResolution;
    }
    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }
    public String getAlignmentSpace() {
        return alignmentSpace;
    }
    public void setAlignmentSpace(String alignmentSpace) {
        this.alignmentSpace = alignmentSpace;
    }
    public String getBoundingBox() {
        return boundingBox;
    }
    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
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
    public String getObjective() {
        return objective;
    }
    public void setObjective(String objective) {
        this.objective = objective;
    }
    public Map<ImageType, String> getImages() {
        return images;
    }
    public void setImages(Map<ImageType, String> images) {
        this.images = images;
    }
    

}