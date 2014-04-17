package org.janelia.it.jacs.model.domain;

public class SampleAlignmentResult extends PipelineResult {

    private String name;
    private String alignmentSpace;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlignmentSpace() {
        return alignmentSpace;
    }

    public void setAlignmentSpace(String alignmentSpace) {
        this.alignmentSpace = alignmentSpace;
    }

}
