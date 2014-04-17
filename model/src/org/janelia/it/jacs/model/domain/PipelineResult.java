package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.List;

public class PipelineResult {

    private String filepath;
    private Date creationDate;
    private List<PipelineResult> results;
    
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
