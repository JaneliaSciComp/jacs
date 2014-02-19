package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_PIPELINE_RUN)
public class PipelineRun extends Renderable {

    @GraphAttribute(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS)
    private String pipelineProcess;
    
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_RESULT)
    private List<Result> pipelineRuns;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getPipelineProcess() {
        return pipelineProcess;
    }

    public void setPipelineProcess(String pipelineProcess) {
        this.pipelineProcess = pipelineProcess;
    }

    public List<Result> getPipelineRuns() {
        return pipelineRuns;
    }

    public void setPipelineRuns(List<Result> pipelineRuns) {
        this.pipelineRuns = pipelineRuns;
    }
}
