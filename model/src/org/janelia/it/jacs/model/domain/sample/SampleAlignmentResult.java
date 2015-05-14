package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;

public class SampleAlignmentResult extends PipelineResult {

    private String imageSize;
    private String opticalResolution;
    private String alignmentSpace;
    private String boundingBox;
    private String channelColors;
    private String chanSpec;
    private String objective;
    private Map<AlignmentScoreType, String> scores;

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

    public Map<AlignmentScoreType, String> getScores() {
        return scores;
    }

    public void setScores(Map<AlignmentScoreType, String> scores) {
        this.scores = scores;
    }
}
