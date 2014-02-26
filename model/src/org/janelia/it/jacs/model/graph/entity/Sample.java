package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_SAMPLE)
public class Sample extends Renderable {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_AGE)
    private String age;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)
    private String chanSpec;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
    private String dataSetIdentifier;

    @GraphAttribute(EntityConstants.ATTRIBUTE_EFFECTOR)
    private String effector;

    @GraphAttribute(EntityConstants.ATTRIBUTE_LINE)
    private String line;
        
    @GraphAttribute(EntityConstants.ATTRIBUTE_OBJECTIVE)
    private String objective;

    @GraphAttribute(EntityConstants.ATTRIBUTE_PROCESSING_BLOCK)
    private EntityNode processingBlock;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_SLIDE_CODE)
    private String slideCode;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_STATUS)
    private String status;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_SUPPORTING_FILES)
    private SupportingData supportingFiles;

    @GraphAttribute(EntityConstants.ATTRIBUTE_TILING_PATTERN)
    private String tilingPattern;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_VISITED)
    private String visited;
    
    @RelatedTo(targetNodeType=EntityConstants.TYPE_PIPELINE_RUN)
    private List<PipelineRun> pipelineRuns;

    /* EVERYTHING BELOW IS AUTO GENERATED */

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public String getDataSetIdentifier() {
        return dataSetIdentifier;
    }

    public void setDataSetIdentifier(String dataSetIdentifier) {
        this.dataSetIdentifier = dataSetIdentifier;
    }

    public String getEffector() {
        return effector;
    }

    public void setEffector(String effector) {
        this.effector = effector;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public EntityNode getProcessingBlock() {
        return processingBlock;
    }

    public void setProcessingBlock(EntityNode processingBlock) {
        this.processingBlock = processingBlock;
    }

    public String getSlideCode() {
        return slideCode;
    }

    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SupportingData getSupportingFiles() {
        return supportingFiles;
    }

    public void setSupportingFiles(SupportingData supportingFiles) {
        this.supportingFiles = supportingFiles;
    }

    public String getTilingPattern() {
        return tilingPattern;
    }

    public void setTilingPattern(String tilingPattern) {
        this.tilingPattern = tilingPattern;
    }

    public String getVisited() {
        return visited;
    }

    public void setVisited(String visited) {
        this.visited = visited;
    }

    public List<PipelineRun> getPipelineRuns() {
        return pipelineRuns;
    }

    public void setPipelineRuns(List<PipelineRun> pipelineRuns) {
        this.pipelineRuns = pipelineRuns;
    }
}
