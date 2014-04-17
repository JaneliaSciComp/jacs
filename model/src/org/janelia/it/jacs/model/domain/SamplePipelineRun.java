package org.janelia.it.jacs.model.domain;

import java.util.Date;

public class SamplePipelineRun extends PipelineResult {

    private String name;
    private String pipelineProcess;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPipelineProcess() {
        return pipelineProcess;
    }
    public void setPipelineProcess(String pipelineProcess) {
        this.pipelineProcess = pipelineProcess;
    }
    
}