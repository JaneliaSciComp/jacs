package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.interfaces.HasRelativeFiles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Lists;

/**
 * The result of some processing. May be nested if further processing is done on this result.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public class PipelineResult implements HasRelativeFiles, HasIdentifier {

    private Long id;
    private String name;
    private String filepath;
    private Date creationDate;
    private List<PipelineResult> results;
    private Map<FileType, String> files;
    private transient SamplePipelineRun parentRun;
    private transient PipelineResult parentResult;

    @JsonIgnore
    public SamplePipelineRun getParentRun() {
        if (parentRun==null && parentResult!=null) {
            // Populate the parent run, since deserialization won't do it. 
            parentRun = parentResult.getParentRun();
        }
        return parentRun;
    }

    @JsonIgnore
    void setParentRun(SamplePipelineRun parentRun) {
        this.parentRun = parentRun;
    }

    @JsonIgnore
    public PipelineResult getParentResult() {
        return parentResult;
    }

    @JsonIgnore
    void setParentResult(PipelineResult parentResult) {
        this.parentResult = parentResult;
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
            result.setParentRun(parentRun);
            result.setParentResult(this);
        }
        this.results = results;
    }

    @JsonIgnore
    public void addResult(PipelineResult result) {
        if (results==null) {
            this.results = new ArrayList<>();
        }
        result.setParentRun(parentRun);
        result.setParentResult(this);
        results.add(result);
    }

    @JsonIgnore
    public void removeResult(PipelineResult result) {
        if (results==null) {
            return;
        }
        result.setParentRun(null);
        result.setParentResult(null);
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
    public NeuronSeparation getLatestSeparationResult() {
        return (NeuronSeparation) getLatestResultOfType(NeuronSeparation.class);
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
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }
}
