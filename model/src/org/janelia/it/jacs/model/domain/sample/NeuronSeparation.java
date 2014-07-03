package org.janelia.it.jacs.model.domain.sample;

import org.janelia.it.jacs.model.domain.ReverseReference;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class NeuronSeparation extends PipelineResult {

    @JsonUnwrapped
    private ReverseReference fragments;
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public ReverseReference getFragmentsReference() {
        return fragments;
    }

    public void setFragmentsReference(ReverseReference fragmentsReference) {
        this.fragments = fragmentsReference;
    }
    
}
