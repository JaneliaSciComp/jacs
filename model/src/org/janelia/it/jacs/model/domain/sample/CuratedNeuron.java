package org.janelia.it.jacs.model.domain.sample;

import org.janelia.it.jacs.model.domain.Reference;

/**
 * A neuron fragment merged from two primary fragments.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CuratedNeuron extends NeuronFragment {

    private Reference fragment1;
    
    private Reference fragment2;
    
    public Reference getFragment1() {
        return fragment1;
    }
    
    public void setFragment1(Reference fragment1) {
        this.fragment1 = fragment1;
    }
    
    public Reference getFragment2() {
        return fragment2;
    }
    
    public void setFragment2(Reference fragment2) {
        this.fragment2 = fragment2;
    }
}
