package org.janelia.it.jacs.model.domain.sample;

public class SampleProcessingResult extends PipelineResult {

    private String imageSize;
    private String opticalResolution;
    private String channelColors;
    private String chanelSpec;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

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
    
    public String getChannelColors() {
        return channelColors;
    }
    
    public void setChannelColors(String channelColors) {
        this.channelColors = channelColors;
    }
    
    public String getChannelSpec() {
        return chanelSpec;
    }
    
    public void setChannelSpec(String chanSpec) {
        this.chanelSpec = chanSpec;
    }
}
