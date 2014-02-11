package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_SAMPLE)
public class Sample extends Renderable {
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_LINE)
    private String line;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_AGE)
    private String age;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)
    private String chanSpec;

    @GraphAttribute(EntityConstants.ATTRIBUTE_EFFECTOR)
    private String effector;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_OBJECTIVE)
    private String objective;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_SLIDE_CODE)
    private String slideCode;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_STATUS)
    private String status;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_VISITED)
    private String visited;
    
    @RelatedTo(targetNodeType=EntityConstants.TYPE_PIPELINE_RUN)
    private List<PipelineRun> pipelineRuns;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_SUPPORTING_FILES)
    private SupportingData supportingFiles;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

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

    public String getEffector() {
        return effector;
    }

    public void setEffector(String effector) {
        this.effector = effector;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
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

    public SupportingData getSupportingFiles() {
        return supportingFiles;
    }

    public void setSupportingFiles(SupportingData supportingFiles) {
        this.supportingFiles = supportingFiles;
    }   
}
