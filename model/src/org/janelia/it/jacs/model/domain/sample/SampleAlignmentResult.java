package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

public class SampleAlignmentResult extends PipelineResult implements HasFiles {

	private String name;
    private String imageSize;
    private String opticalResolution;
    private String alignmentSpace;
    private String boundingBox;
    private String channelColors;
    private String chanSpec;
    private String objective;
    private Map<AlignmentScoreType,String> scores;
    private Map<FileType,String> files;

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
    public Map<FileType, String> getFiles() {
        return files;
    }
    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }
	public Map<AlignmentScoreType, String> getScores() {
		return scores;
	}
	public void setScores(Map<AlignmentScoreType, String> scores) {
		this.scores = scores;
	}
}