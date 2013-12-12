package org.janelia.it.jacs.model.domain.impl.entity.imaging;

import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.imaging.AlignmentContext;

public class EntityAlignmentContext extends EntityDomainObject implements AlignmentContext {

    protected String alignmentSpaceName;
    protected String opticalResolution;
    protected String pixelResolution;

    public EntityAlignmentContext(String alignmentSpaceName, String opticalResolution, String pixelResolution) {
        super(null);
        if ( alignmentSpaceName == null || opticalResolution == null || pixelResolution == null ) {
            throw new IllegalArgumentException( "No nulls in constructor." );
        }
        this.alignmentSpaceName = alignmentSpaceName;
        this.opticalResolution = opticalResolution;
        this.pixelResolution = pixelResolution;
    }
    
    public String getAlignmentSpaceName() {
        return alignmentSpaceName;
    }

    public String getOpticalResolution() {
        return opticalResolution;
    }
    
    public String getPixelResolution() {
        return pixelResolution;
    }
    
    public boolean canDisplay(AlignmentContext alignmentContext) {
        return (alignmentSpaceName.equals(alignmentContext.getAlignmentSpaceName()) 
                && opticalResolution.equals(alignmentContext.getOpticalResolution())
                && pixelResolution.equals(alignmentContext.getPixelResolution()));
    }

    @Override
    public String toString() {
        return getAlignmentSpaceName()+" "+getOpticalResolution()+" "+getPixelResolution();
    }
    @Override
    public boolean equals( Object other ) {
        boolean rtnVal;
        if ( other == null || ! ( other instanceof AlignmentContext ) ) {
            rtnVal = false;
        }
        else {
            AlignmentContext otherAlignmentSpace = (AlignmentContext)other;
            rtnVal = otherAlignmentSpace.getAlignmentSpaceName().equals( getAlignmentSpaceName() ) &&
                    otherAlignmentSpace.getPixelResolution().equals( getPixelResolution() ) &&
                    otherAlignmentSpace.getOpticalResolution().equals( getOpticalResolution() );
        }

        return rtnVal;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
    
}
