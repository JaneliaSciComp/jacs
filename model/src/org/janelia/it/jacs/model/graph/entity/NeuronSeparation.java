package org.janelia.it.jacs.model.graph.entity;

import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
public class NeuronSeparation extends Result {

    @GraphAttribute(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)
    private String opticalResolution;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)
    private String pixelResolution;

    @GraphAttribute(EntityConstants.ATTRIBUTE_OBJECTIVE)
    private String objective;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_SOURCE_SEPARATION)
    private NeuronSeparation sourceSeparation;
    
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_INPUT_IMAGE)
    private Image3d inputImage;
    
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION)
    private List<NeuronFragment> neuronFragments;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getOpticalResolution() {
        return opticalResolution;
    }

    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }

    public String getPixelResolution() {
        return pixelResolution;
    }

    public void setPixelResolution(String pixelResolution) {
        this.pixelResolution = pixelResolution;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public NeuronSeparation getSourceSeparation() {
        return sourceSeparation;
    }

    public void setSourceSeparation(NeuronSeparation sourceSeparation) {
        this.sourceSeparation = sourceSeparation;
    }

    public Image3d getInputImage() {
        return inputImage;
    }

    public void setInputImage(Image3d inputImage) {
        this.inputImage = inputImage;
    }

    public List<NeuronFragment> getNeuronFragments() {
        return neuronFragments;
    }

    public void setNeuronFragments(List<NeuronFragment> neuronFragments) {
        this.neuronFragments = neuronFragments;
    }
    
}
