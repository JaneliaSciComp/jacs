package org.janelia.it.jacs.model.domain.enums;

/**
 * Different types of files which may be associated with an object that implements the HasFiles interface.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum FileType {

    Stack(false, false),
    FastStack(false, false),
    VisuallyLosslessStack(false, false),
    HeatmapStack(true, false),
    AllMip(true, false),
    SignalMip(true, false),
    ReferenceMip(true, false),
    HeatmapMip(true, false),
    AllMovie(false, false),
    SignalMovie(false, false),
    ReferenceMovie(false, false),
    HeatmapMovie(false, false),
    AlignmentVerificationMovie(false, false),
    MaskFile(false, false),
    ChanFile(false, false),
    LsmMetadata(false, true),
    CellCountPlan(false, true),
    CellCountReport(false, true),
    CellCountStack(false, false),
    CellCountStackMip(true, false),
    CellCountImage(false, false),
    CellCountImageMip(true, false),
    Unclassified2d(true, false),
    Unclassified3d(false, false),
    UnclassifiedAscii(false, true);

    private final boolean is2dImage;
    private final boolean isAscii;

    private FileType(boolean is2dImage, boolean isAscii) {
        this.is2dImage = is2dImage;
        this.isAscii = isAscii;
    }

    public boolean isIs2dImage() {
        return is2dImage;
    }

    public boolean isAscii() {
        return isAscii;
    }
}
