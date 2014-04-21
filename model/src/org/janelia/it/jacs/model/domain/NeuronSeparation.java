package org.janelia.it.jacs.model.domain;

import java.util.List;

public class NeuronSeparation extends PipelineResult {

    List<Long> neuronFragmentIds;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public void setNeuronFragmentIds(List<Long> neuronFragmentIds) {
        this.neuronFragmentIds = neuronFragmentIds;
    }
    public List<Long> getNeuronFragmentIds() {
        return neuronFragmentIds;
    }
    
}
