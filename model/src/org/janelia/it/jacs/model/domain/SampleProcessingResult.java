package org.janelia.it.jacs.model.domain;

public class SampleProcessingResult extends PipelineResult {

    private String anatomicalArea;
    private Image3d resultStack;
    
    public String getAnatomicalArea() {
        return anatomicalArea;
    }
    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }
    public Image3d getResultStack() {
        return resultStack;
    }
    public void setResultStack(Image3d resultStack) {
        this.resultStack = resultStack;
    }
    

}
