package org.janelia.it.jacs.model.domain.interfaces.imaging;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;

public interface AlignmentContext extends DomainObject {

    public abstract String getAlignmentSpaceName();

    public String getOpticalResolution();
    
    public String getPixelResolution();
    
    public boolean canDisplay(AlignmentContext alignmentContext);
    
}
