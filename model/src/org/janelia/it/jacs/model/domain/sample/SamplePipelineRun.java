package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.interfaces.HasImageStack;

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
    private transient ObjectiveSample parent;

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
        if (results==null)
            return;
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
    @SuppressWarnings("unchecked")
    public <T extends PipelineResult> T getLatestResultOfType(Class<T> resultClass) {
        if (results==null) {
            return null;
        }
        for (int i = results.size()-1; i>=0; i--) {
            PipelineResult result = results.get(i);
            if (resultClass==null || resultClass.isAssignableFrom(result.getClass())) {
                return (T)result;
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
        return getLatestResultOfType(SampleProcessingResult.class);
    }

    @JsonIgnore
    public HasImageStack getLatestAlignmentResult() {
        return getLatestResultOfType(SampleAlignmentResult.class);
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public <T extends PipelineResult> List<T> getResultsOfType(Class<T> resultClass) {
        List<T> filteredResults = new ArrayList<>();
        if (results==null) {
            return filteredResults;
        }
        for (PipelineResult result : results) {
            if (resultClass==null || resultClass.isAssignableFrom(result.getClass())) {
                filteredResults.add((T)result);
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends PipelineResult> List<T> getResultsById(Class<T> resultClass, Long resultId) {
        List<T> results = new ArrayList<>();
        for(PipelineResult result : getResults()) {
            if (resultId.equals(result.getId()) && (resultClass==null || resultClass.isAssignableFrom(result.getClass()))) {
                results.add((T)result);
            }
            for(T childResult : result.getResultsById(resultClass, resultId)) {
                results.add(childResult);
            }
        }
        return results;
    }
    
    @JsonIgnore
    public List<SampleProcessingResult> getSampleProcessingResults() {
        return getResultsOfType(SampleProcessingResult.class);
    }

    @JsonIgnore
    public List<SampleAlignmentResult> getAlignmentResults() {
        return getResultsOfType(SampleAlignmentResult.class);
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
