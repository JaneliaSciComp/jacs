package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * A single run of the pipeline on an ObjectiveSample. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SamplePipelineRun {

    private Long id;
    private String name;
    private String pipelineProcess;
    private Integer pipelineVersion;
    private Date creationDate;
    private List<PipelineResult> results;
    private PipelineError error;
    private ObjectiveSample parent;

    @JsonIgnore
    public ObjectiveSample getParent() {
        return parent;
    }

    @JsonIgnore
    void setParent(ObjectiveSample parent) {
        this.parent = parent;
    }

    @JsonIgnore
    public boolean hasResults() {
        return results!=null && !results.isEmpty();
    }
    
    @JsonProperty
    public List<PipelineResult> getResults() {
        return results==null?null:Collections.unmodifiableList(results);
    }
    
    @JsonProperty
    public void setResults(List<PipelineResult> results) {
        for(PipelineResult result : results) {
            result.setParentRun(this);
        }
        this.results = results;
    }

    @JsonIgnore
    public void addResult(PipelineResult result) {
        if (results==null) {
            this.results = new ArrayList<>();
        }
        result.setParentRun(this);
        results.add(result);
    }

    @JsonIgnore
    public void removeResult(PipelineResult result) {
        if (results==null) {
            return;
        }
        results.remove(result);
    }
    
    @JsonIgnore
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

    @JsonIgnore
    public PipelineResult getLatestResult() {
        return getLatestResultOfType(null);
    }

    @JsonIgnore
    public SampleProcessingResult getLatestProcessingResult() {
        return (SampleProcessingResult) getLatestResultOfType(SampleProcessingResult.class);
    }

    @JsonIgnore
    public SampleAlignmentResult getLatestAlignmentResult() {
        return (SampleAlignmentResult) getLatestResultOfType(SampleAlignmentResult.class);
    }

    public boolean hasError() {
        return error!=null;
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
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
