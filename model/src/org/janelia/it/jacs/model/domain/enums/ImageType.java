package org.janelia.it.jacs.model.domain.enums;

/**
 * Different types of images which may be associated with an object that implements the HasImages interface. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum ImageType {
	
    Stack(false),
    Mip(true),
    SignalMip(true),
    ReferenceMip(true),
    HeatmapMip(true),
    AlignVerifyMovie(false),
    MaskFile(false),
    ChanFile(false);
    
    private final boolean is2d;
    
    private ImageType(boolean is2d) {
        this.is2d = is2d;
    }
    
    public boolean isIs2d() {
        return is2d;
    }
}
