package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.SampleImageType;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

/**
 * A data set definition which controls how Samples are processed. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="dataSet",label="Data Set")
public class DataSet extends AbstractDomainObject {

    private String identifier;
    private String sampleNamePattern;
    private SampleImageType sampleImageType;
    private boolean sageSync;
    private List<String> pipelineProcesses = new ArrayList<>();

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

    public boolean isSageSync() {
        return sageSync;
    }

    public void setSageSync(boolean sageSync) {
        this.sageSync = sageSync;
    }

    public List<String> getPipelineProcesses() {
        return pipelineProcesses;
    }

    public void setPipelineProcesses(List<String> pipelineProcesses) {
        if (pipelineProcesses==null) throw new IllegalArgumentException("Property cannot be null");
        this.pipelineProcesses = pipelineProcesses;
    }
    
}
