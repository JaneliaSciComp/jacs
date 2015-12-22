package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;

/**
 * The result of running an alignment algorithm on a sample. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleAlignmentResult extends PipelineResult implements HasAnatomicalArea {

	private String anatomicalArea;
    private String imageSize;
    private String opticalResolution;
    private String channelColors;
    private String channelSpec;
    private String objective;
    private String alignmentSpace;
    private String boundingBox;
    private Map<AlignmentScoreType, String> scores;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
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

    public String getChannelColors() {
        return channelColors;
    }

    public void setChannelColors(String channelColors) {
        this.channelColors = channelColors;
    }

    public String getChannelSpec() {
        return channelSpec;
    }

    public void setChannelSpec(String chanSpec) {
        this.channelSpec = chanSpec;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
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

    public Map<AlignmentScoreType, String> getScores() {
        return scores;
    }

    public void setScores(Map<AlignmentScoreType, String> scores) {
        this.scores = scores;
    }
}
