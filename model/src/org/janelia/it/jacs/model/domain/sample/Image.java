package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@MongoMapped(collectionName="image")
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS,property="class")
public class Image extends AbstractDomainObject implements HasFiles, HasFilepath {

    private String filepath;
    private String opticalResolution;
    private String pixelResolution;
    private Integer numChannels;
    private String objective;
    private Map<FileType,String> files;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

	@Override
	public String getFilepath() {
		return filepath;
	}
	public void setFilepath(String filepath) {
		this.filepath = filepath;
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
	public Integer getNumChannels() {
		return numChannels;
	}
	public void setNumChannels(Integer numChannels) {
		this.numChannels = numChannels;
	}
	public String getObjective() {
		return objective;
	}
	public void setObjective(String objective) {
		this.objective = objective;
	}
	@Override
	public Map<FileType, String> getFiles() {
		return files;
	}
	public void setFiles(Map<FileType, String> files) {
		this.files = files;
	}
    
}
