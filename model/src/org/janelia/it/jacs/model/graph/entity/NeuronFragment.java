package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
@GraphNode(type=EntityConstants.TYPE_NEURON_FRAGMENT)
public class NeuronFragment extends EntityNode {

	private static final long serialVersionUID = 1L;
	
    @GraphAttribute(EntityConstants.ATTRIBUTE_NUMBER)
    private Integer number;

    @GraphAttribute(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)
    private Integer default2dImageFilepath;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_MASK_IMAGE)
    private Integer maskImageFilepath;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHAN_IMAGE)
    private Integer chanImageFilepath;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getDefault2dImageFilepath() {
        return default2dImageFilepath;
    }

    public void setDefault2dImageFilepath(Integer default2dImageFilepath) {
        this.default2dImageFilepath = default2dImageFilepath;
    }

    public Integer getMaskImageFilepath() {
        return maskImageFilepath;
    }

    public void setMaskImageFilepath(Integer maskImageFilepath) {
        this.maskImageFilepath = maskImageFilepath;
    }

    public Integer getChanImageFilepath() {
        return chanImageFilepath;
    }

    public void setChanImageFilepath(Integer chanImageFilepath) {
        this.chanImageFilepath = chanImageFilepath;
    }
    
}
