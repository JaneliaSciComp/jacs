package org.janelia.it.jacs.model.domain;


public class SampleAlignmentResult extends PipelineResult implements HasMips {

    private String stackFilepath;
    private String verifyMovieFilepath;
    private String signalMipFilepath;
    private String referenceMipFilepath;
    private String imageSize;
    private String opticalResolution;
    private String alignmentSpace;
    private String boundingBox;
    private String channelColors;
    private String chanSpec;
    private String objective;
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public String getStackFilepath() {
        return stackFilepath;
    }

    public void setStackFilepath(String stackFilepath) {
        this.stackFilepath = stackFilepath;
    }

    public String getMovieFilepath() {
        return verifyMovieFilepath;
    }

    public void setVerifyMovieFilepath(String verifyMovieFilepath) {
        this.verifyMovieFilepath = verifyMovieFilepath;
    }

    @Override
    public String getSignalMipFilepath() {
        return signalMipFilepath;
    }

    public void setSignalMipFilepath(String signalMipFilepath) {
        this.signalMipFilepath = signalMipFilepath;
    }

    @Override
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

}