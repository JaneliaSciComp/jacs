package org.janelia.it.jacs.model.domain.sample;

import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * The result of running the Neuron Separator on some input file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparation extends PipelineResult {

    private String name;
    @SearchTraversal({Sample.class})
    @JsonUnwrapped
    private ReverseReference fragments;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ReverseReference getFragmentsReference() {
        return fragments;
    }

    public void setFragmentsReference(ReverseReference fragmentsReference) {
        this.fragments = fragmentsReference;
    }

}
