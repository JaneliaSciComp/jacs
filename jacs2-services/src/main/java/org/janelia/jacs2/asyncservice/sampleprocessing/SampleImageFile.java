package org.janelia.jacs2.asyncservice.sampleprocessing;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class SampleImageFile {
    private Number id;
    private Number sampleId;
    private String archiveFilePath;
    private String workingFilePath;
    private String metadataFilePath;
    private String chanSpec;
    private String colorSpec;
    private String divSpec;
    private Integer laser;
    private Integer gain;
    private String area;
    private String objective;

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id;
    }

    public Number getSampleId() {
        return sampleId;
    }

    public void setSampleId(Number sampleId) {
        this.sampleId = sampleId;
    }

    public String getArchiveFilePath() {
        return archiveFilePath;
    }

    public void setArchiveFilePath(String archiveFilePath) {
        this.archiveFilePath = archiveFilePath;
    }

    public String getWorkingFilePath() {
        return workingFilePath;
    }

    public void setWorkingFilePath(String workingFilePath) {
        this.workingFilePath = workingFilePath;
    }

    public String getMetadataFilePath() {
        return metadataFilePath;
    }

    public void setMetadataFilePath(String metadataFilePath) {
        this.metadataFilePath = metadataFilePath;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public boolean isChanSpecDefined() {
        return StringUtils.isNotBlank(chanSpec);
    }

    public String getColorSpec() {
        return colorSpec;
    }

    public void setColorSpec(String colorSpec) {
        this.colorSpec = colorSpec;
    }

    public String getDivSpec() {
        return divSpec;
    }

    public void setDivSpec(String divSpec) {
        this.divSpec = divSpec;
    }

    public Integer getLaser() {
        return laser;
    }

    public void setLaser(Integer laser) {
        this.laser = laser;
    }

    public Integer getGain() {
        return gain;
    }

    public void setGain(Integer gain) {
        this.gain = gain;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("area", area)
                .append("objective", objective)
                .append("archiveFilePath", archiveFilePath)
                .append("workingFilePath", workingFilePath)
                .build();
    }
}
