package org.janelia.it.jacs.model.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS,property="class")
public class PipelineResult implements HasFilepath {

    private String filepath;
    private Date creationDate;
    private List<PipelineResult> results;
    private PipelineError error;

    public boolean hasError() {
        return error!=null;
    }
    
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

    public void addResult(PipelineResult result) {
    	if (results==null) {
    		this.results = new ArrayList<PipelineResult>();
    	}
    	results.add(result);
    }
    
    public void removeResult(PipelineResult result) {
    	if (results==null) {
    		return;
    	}
    	results.remove(result);
    	if (results.isEmpty()) {
    		results = null;
    	}
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
    public PipelineError getError() {
        return error;
    }
    public void setError(PipelineError error) {
        this.error = error;
    }
    
}
