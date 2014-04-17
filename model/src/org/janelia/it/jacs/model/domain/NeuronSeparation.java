package org.janelia.it.jacs.model.domain;

import java.util.List;

public class NeuronSeparation extends PipelineResult {

    String consolidatedSignalFile;
    String consolidatedLabelFile;
    String referenceFile;
    List<NeuronFragment> neuronFragments;
    
    public String getConsolidatedSignalFile() {
        return consolidatedSignalFile;
    }
    public void setConsolidatedSignalFile(String consolidatedSignalFile) {
        this.consolidatedSignalFile = consolidatedSignalFile;
    }
    public String getConsolidatedLabelFile() {
        return consolidatedLabelFile;
    }
    public void setConsolidatedLabelFile(String consolidatedLabelFile) {
        this.consolidatedLabelFile = consolidatedLabelFile;
    }
    public String getReferenceFile() {
        return referenceFile;
    }
    public void setReferenceFile(String referenceFile) {
        this.referenceFile = referenceFile;
    }
    public List<NeuronFragment> getNeuronFragments() {
        return neuronFragments;
    }
    public void setNeuronFragments(List<NeuronFragment> neuronFragments) {
        this.neuronFragments = neuronFragments;
    }
    
    
    
}
