package org.janelia.it.jacs.model.domain;

public class SampleProcessingResult extends PipelineResult implements HasMips {

    private String stackFilepath;
    private String anatomicalArea;
    private String signalMipFilepath;
    private String referenceMipFilepath;
    private String imageSize;
    private String opticalResolution;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public String getStackFilepath() {
        return stackFilepath;
    }
    public void setStackFilepath(String stackFilepath) {
        this.stackFilepath = stackFilepath;
    }
    public String getAnatomicalArea() {
        return anatomicalArea;
    }
    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }
    public String getSignalMipFilepath() {
        return signalMipFilepath;
    }
    public void setSignalMipFilepath(String signalMipFilepath) {
        this.signalMipFilepath = signalMipFilepath;
    }
    public String getReferenceMipFilepath() {
        return referenceMipFilepath;
    }
    public void setReferenceMipFilepath(String referenceMipFilepath) {
        this.referenceMipFilepath = referenceMipFilepath;
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
}
