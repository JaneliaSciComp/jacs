package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
public class SampleProcessingResult extends Result {

    @GraphAttribute(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
    private String anatomicalArea;
    
    @RelatedTo(targetNodeType=EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
    private NeuronSeparation neuronSeparation;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    public NeuronSeparation getNeuronSeparation() {
        return neuronSeparation;
    }

    public void setNeuronSeparation(NeuronSeparation neuronSeparation) {
        this.neuronSeparation = neuronSeparation;
    }
}
