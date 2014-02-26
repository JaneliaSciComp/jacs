package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_PIPELINE_RUN)
public class PipelineRun extends Renderable {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS)
    private String pipelineProcess;
    
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_RESULT)
    private List<Result> results;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getPipelineProcess() {
        return pipelineProcess;
    }

    public void setPipelineProcess(String pipelineProcess) {
        this.pipelineProcess = pipelineProcess;
    }

	public List<Result> getResults() {
		return results;
	}

	public void setResults(List<Result> results) {
		this.results = results;
	}

}
