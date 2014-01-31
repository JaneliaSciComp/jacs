package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_ALIGNMENT_RESULT)
public class AlignmentResult extends Result {

    @RelatedTo(targetNodeType=EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
    private NeuronSeparation neuronSeparation;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public NeuronSeparation getNeuronSeparation() {
        return neuronSeparation;
    }

    public void setNeuronSeparation(NeuronSeparation neuronSeparation) {
        this.neuronSeparation = neuronSeparation;
    }
}
