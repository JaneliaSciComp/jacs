package org.janelia.it.jacs.model.domain;

public class PipelineError implements HasFilepath {

    private String filepath;

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
