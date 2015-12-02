package org.janelia.it.jacs.model.domain.enums;

/**
 * Different types of files which may be associated with an object that implements the HasFiles interface.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum FileType {

    // Stacks
    LosslessStack("Lossless Stack", false, false),
    VisuallyLosslessStack("Visually Lossless Stack", false, false),
    FastStack("Fast-loading Stack", false, false),

    // Metadata files
    LsmMetadata("LSM Metadata", false, true),
    
    // MIPs and movies
    AllMip("Signal+Reference MIP", true, false),
    SignalMip("Signal MIP", true, false),
    ReferenceMip("Reference MIP", true, false),
    AllMovie("Signal+Reference Movie", false, false),
    SignalMovie("Signal Movie", false, false),
    ReferenceMovie("Reference Movie", false, false),
    
    // Alignment outputs
    AlignmentVerificationMovie("Alignment Verification Movie", false, false),

    // Heatmaps for pattern data
    HeatmapStack("Heatmap Stack", false, false),
    HeatmapMip("Heatmap MIP", true, false),
    
    // Mask/chan
    MaskFile("Mask File", false, false),
    ChanFile("Chan File", false, false),
    
    // Cell counting
    CellCountPlan("Cell Counting Plan", false, true),
    CellCountReport("Cell Counting Report", false, true),
    CellCountStack("Cell Counting Stack", false, false),
    CellCountStackMip("Cell Counting Stack MIP", true, false),
    CellCountImage("Cell Counting Image", false, false),
    CellCountImageMip("Cell Counting Image MIP", true, false),
    
    // Legacy files
    Unclassified2d("2D Image", true, false),
    Unclassified3d("3D Image", false, false),
    UnclassifiedAscii("Text File", false, true);

    private final String label;
    private final boolean is2dImage;
    private final boolean isAscii;

    private FileType(String label, boolean is2dImage, boolean isAscii) {
        this.label = label;
        this.is2dImage = is2dImage;
        this.isAscii = isAscii;
    }
    
    public String getLabel() {
        return label;
    }
    
    public boolean is2dImage() {
        return is2dImage;
    }

    public boolean isAscii() {
        return isAscii;
    }
}
