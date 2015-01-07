package org.janelia.it.jacs.model.domain.screen;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;

@MongoMapped(collectionName = "patternMask")
public class PatternMask extends AbstractDomainObject implements HasFiles, HasFilepath {

    private Reference screenSample;

    @SearchAttribute(key="filepath_txt",label="File Path")
    private String filepath;

    @SearchAttribute(key="mask_set_name_txt",label="Mask Set")
    private String maskSetName;

    @SearchAttribute(key="normalized_b",label="Normalized?")
    private Boolean normalized;

    @SearchAttribute(key="int_score_i",label="MAA Intensity Score")
    private Integer intensityScore;

    @SearchAttribute(key="dist_score_i",label="MAA Distribution Score")
    private Integer distributionScore;
    
    private Map<FileType, String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public Reference getScreenSample() {
		return screenSample;
	}

	public void setScreenSample(Reference screenSample) {
		this.screenSample = screenSample;
	}

    public String getFilepath() {
        return filepath;
    }

	public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getMaskSetName() {
        return maskSetName;
    }

    public void setMaskSetName(String maskSetName) {
        this.maskSetName = maskSetName;
    }

    public Boolean isNormalized() {
        return normalized;
    }

    public void setNormalized(Boolean normalized) {
        this.normalized = normalized;
    }

    public Integer getIntensityScore() {
        return intensityScore;
    }

    public void setIntensityScore(Integer intensityScore) {
        this.intensityScore = intensityScore;
    }

    public Integer getDistributionScore() {
        return distributionScore;
    }

    public void setDistributionScore(Integer distributionScore) {
        this.distributionScore = distributionScore;
    }

    public Map<FileType, String> getFiles() {
        return images;
    }

    public void setImages(Map<FileType, String> images) {
        this.images = images;
    }

	public Map<FileType, String> getImages() {
		return images;
	}
}
