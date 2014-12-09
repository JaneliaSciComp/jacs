package org.janelia.it.jacs.model.domain.sample;

import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.SampleImageType;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

@MongoMapped(collectionName = "dataSet")
public class DataSet extends AbstractDomainObject {

    private String identifier;
    private String sampleNamePattern;
    private SampleImageType sampleImageType;
    private Boolean sageSync;
    private List<String> pipelineProcesses;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getSampleNamePattern() {
        return sampleNamePattern;
    }

    public void setSampleNamePattern(String sampleNamePattern) {
        this.sampleNamePattern = sampleNamePattern;
    }

    public SampleImageType getSampleImageType() {
        return sampleImageType;
    }

    public void setSampleImageType(SampleImageType sampleImageType) {
        this.sampleImageType = sampleImageType;
    }

    public Boolean getSageSync() {
        return sageSync;
    }

    public void setSageSync(Boolean sageSync) {
        this.sageSync = sageSync;
    }

    public List<String> getPipelineProcesses() {
        return pipelineProcesses;
    }

    public void setPipelineProcesses(List<String> pipelineProcesses) {
        this.pipelineProcesses = pipelineProcesses;
    }
}
