package org.janelia.it.jacs.model.domain.sample;

import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;

public class PipelineError implements HasFilepath {

    private String filepath;

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
