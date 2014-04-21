package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS,property="class")
public class PipelineResult implements HasFilepath {

    private String filepath;
    private Date creationDate;
    private List<PipelineResult> results;

    private PipelineResult getLatestResultOfType(Class<? extends PipelineResult> type) {
        if (results==null) return null;
        for(int i=results.size()-1; i>=0; i--) {
            PipelineResult result = results.get(i);
            if (type.isAssignableFrom(result.getClass())) {
                return result;
            }
        }
        return null;
    }
    
    public SampleProcessingResult getLatestProcessingResult() {
        return (SampleProcessingResult)getLatestResultOfType(SampleProcessingResult.class);
    }

    public SampleAlignmentResult getLatestAlignmentResult() {
        return (SampleAlignmentResult)getLatestResultOfType(SampleAlignmentResult.class);
    }

    public NeuronSeparation getLatestSeparationResult() {
        return (NeuronSeparation)getLatestResultOfType(NeuronSeparation.class);
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    @Override
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public List<PipelineResult> getResults() {
        return results;
    }
    public void setResults(List<PipelineResult> results) {
        this.results = results;
    }
}
