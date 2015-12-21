package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The result of some processing. May be nested if further processing is done on this result.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public class PipelineResult implements HasFilepath, HasFiles, HasIdentifier {

    private Long id;
    private String name;
    private String filepath;
    private Date creationDate;
    private List<PipelineResult> results;
    private Map<FileType, String> files;

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

    public List<PipelineResult> getResults() {
        return results;
    }
    
    public void setResults(List<PipelineResult> results) {
        this.results = results;
    }

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }


}
