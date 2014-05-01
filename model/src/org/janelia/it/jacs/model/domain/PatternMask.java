package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.jongo.marshall.jackson.oid.Id;

public class PatternMask implements DomainObject, HasImages, HasFilepath {

    @Id
    private Long id;
    private Long screenSampleId;
    private String name;
    private String ownerKey;
    private Set<String> readers;
    private Set<String> writers;
    private Date creationDate;
    private Date updatedDate;
    private String filepath;
    private String maskSetName;
    private boolean normalized;
    private Integer intensityScore;
    private Integer distributionScore;
    private Map<ImageType,String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getScreenSampleId() {
        return screenSampleId;
    }
    public void setScreenSampleId(Long screenSampleId) {
        this.screenSampleId = screenSampleId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getOwnerKey() {
        return ownerKey;
    }
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }
    public Set<String> getReaders() {
        return readers;
    }
    public void setReaders(Set<String> readers) {
        this.readers = readers;
    }
    public Set<String> getWriters() {
        return writers;
    }
    public void setWriters(Set<String> writers) {
        this.writers = writers;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Date getUpdatedDate() {
        return updatedDate;
    }
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
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
