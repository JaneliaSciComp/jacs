package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

public class SampleProcessingResult extends PipelineResult implements HasFiles {

	private String name;
    private String anatomicalArea;
    private String imageSize;
    private String opticalResolution;
    private Map<FileType,String> files;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
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
    public Map<FileType, String> getFiles() {
        return files;
    }
    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }
}
