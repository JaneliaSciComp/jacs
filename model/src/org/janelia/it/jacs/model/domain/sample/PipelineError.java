package org.janelia.it.jacs.model.domain.sample;

import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;

/**
 * An error in processing a Sample. The filepath points to a file 
 * containing the stacktrace of the exception that was thrown.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PipelineError implements HasFilepath {

    private String filepath;
    private String classification;
    private String description;

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}
