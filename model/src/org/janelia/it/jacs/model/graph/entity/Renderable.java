package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;

public abstract class Renderable extends EntityNode {

    @GraphAttribute(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE)
    private String default2dImageFilepath;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)
    private String default3dImageFilepath;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)
    private String defaultFast3dImageFilepath;

    @GraphAttribute(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE)
    private String referenceMipImageFilepath;

    @GraphAttribute(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE)
    private String signalMipImageFilepath;
    
    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getDefault2dImageFilepath() {
        return default2dImageFilepath;
    }

    public void setDefault2dImageFilepath(String default2dImageFilepath) {
        this.default2dImageFilepath = default2dImageFilepath;
    }

    public String getDefault3dImageFilepath() {
        return default3dImageFilepath;
    }

    public void setDefault3dImageFilepath(String default3dImageFilepath) {
        this.default3dImageFilepath = default3dImageFilepath;
    }

    public String getDefaultFast3dImageFilepath() {
        return defaultFast3dImageFilepath;
    }

    public void setDefaultFast3dImageFilepath(String defaultFast3dImageFilepath) {
        this.defaultFast3dImageFilepath = defaultFast3dImageFilepath;
    }
    
    public String getReferenceMipImageFilepath() {
        return referenceMipImageFilepath;
    }

    public void setReferenceMipImageFilepath(String referenceMipImageFilepath) {
        this.referenceMipImageFilepath = referenceMipImageFilepath;
    }

    public String getSignalMipImageFilepath() {
        return signalMipImageFilepath;
    }

    public void setSignalMipImageFilepath(String signalMipImageFilepath) {
        this.signalMipImageFilepath = signalMipImageFilepath;
    }
}
