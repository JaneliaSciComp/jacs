package org.janelia.it.jacs.model.domain.screen;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.ImageType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasImages;

public class PatternMask extends AbstractDomainObject implements HasImages, HasFilepath {

    private Long screenSampleId;
    private String filepath;
    private String maskSetName;
    private boolean normalized;
    private Integer intensityScore;
    private Integer distributionScore;
    private Map<ImageType,String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Long getScreenSampleId() {
        return screenSampleId;
    }
    public void setScreenSampleId(Long screenSampleId) {
        this.screenSampleId = screenSampleId;
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
    public boolean isNormalized() {
        return normalized;
    }
    public void setNormalized(boolean normalized) {
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
    public Map<ImageType, String> getImages() {
        return images;
    }
    public void setImages(Map<ImageType, String> images) {
        this.images = images;
    }
}
