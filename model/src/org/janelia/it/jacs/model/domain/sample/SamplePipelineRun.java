package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A single run of the pipeline on an ObjectiveSample. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SamplePipelineRun {

    private String name;
    private String pipelineProcess;
    private Integer pipelineVersion;
    private Date creationDate;
    private List<PipelineResult> results;
    private PipelineError error;

    public boolean hasError() {
        return error!=null;
    }
    
    protected PipelineResult getLatestResultOfType(Class<? extends PipelineResult> type) {
        if (results==null) {
            return null;
        }
        for (int i = results.size()-1; i>=0; i--) {
            PipelineResult result = results.get(i);
            if (type==null || type.isAssignableFrom(result.getClass())) {
                return result;
            }
        }
        return null;
    }

    public PipelineResult getLatestResult() {
        return getLatestResultOfType(null);
    }
    
    public SampleProcessingResult getLatestProcessingResult() {
        return (SampleProcessingResult) getLatestResultOfType(SampleProcessingResult.class);
    }

    public SampleAlignmentResult getLatestAlignmentResult() {
        return (SampleAlignmentResult) getLatestResultOfType(SampleAlignmentResult.class);
    }

    public void addResult(PipelineResult result) {
        if (results==null) {
            this.results = new ArrayList<>();
        }
        results.add(result);
    }

    public void removeResult(PipelineResult result) {
        if (results==null) {
            return;
        }
        results.remove(result);
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */

    
    public Date getCreationDate() {
        return creationDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getPipelineVersion() {
        return pipelineVersion;
    }

    public void setPipelineVersion(Integer pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
    }

    public List<PipelineResult> getResults() {
        return results;
    }

    public void setResults(List<PipelineResult> results) {
        this.results = results;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    
    public String getPipelineProcess() {
        return pipelineProcess;
    }

    public void setPipelineProcess(String pipelineProcess) {
        this.pipelineProcess = pipelineProcess;
    }

    public PipelineError getError() {
        return error;
    }

    public void setError(PipelineError error) {
        this.error = error;
    }
}
