package org.janelia.it.jacs.model.entity.cv;

public enum PipelineProcess implements NamedEnum {

    // The enum must match the process file name after PipelineConfig_
    CellCounting("Cell Counting Pipeline"),

    DitpAlignment("DITP Alignment Pipeline"),

    FlyLightCentralBrain("FlyLight Central Brain Pipeline"),
    FlyLightOpticLobe("FlyLight Optic Lobe Pipeline"),
    FlyLightUnaligned("FlyLight Unaligned Pipeline"),
    FlyLightWholeBrain("FlyLight Whole Brain Pipeline"),
    FlyLightWholeBrain64x("FlyLight Whole Brain 63x Pipeline"),

    LeetCentralBrain63x("Lee Central Brain 63x Pipeline"),
    LeetWholeBrain40x("Lee Whole Brain 40x Pipeline"),
    LeetWholeBrain40x512pxINT("Lee Whole Brain 40x 512px INTensity Pipeline"),
    LeetWholeBrain40xImproved("Lee Whole Brain 40x Improved Pipeline"),

    NernaLeftOpticLobe("Aljoscha Left Optic Lobe 63x Pipeline"),
    NernaRightOpticLobe("Aljoscha Right Optic Lobe 63x Pipeline"),

    WolfftMCFOCase1("Tanya Central Brain MCFO Case 1 Pipeline"),
    
    YoshiMB63xFlpout1024pxINT("Yoshi MB Flp-out 63x 1024px INTensity Pipeline"),
    YoshiMB63xFlpout512pxINT("Yoshi MB Flp-out 63x 512px INTensity Pipeline"),
    YoshiMB63xFlpout512pxLM("Yoshi MB Flp-out 63x 512px LandMark Pipeline"),
    YoshiMB63xLexAGal41024pxINT("Yoshi MB LexA-GAL4 63x 1024px INTensity Pipeline"),
    YoshiMB63xLexAGal4512pxINT("Yoshi MB LexA-GAL4 63x 512px INTensity Pipeline"),
    YoshiMB63xLexAGal4512pxLM("Yoshi MB LexA-GAL4 63x 512px LandMark Pipeline"),
    YoshiMBPolarityCase1("Yoshi MB Polarity Case 1 Pipeline"),
    YoshiMBPolarityCase2("Yoshi MB Polarity Case 2 Pipeline"),
    YoshiMBPolarityCase3("Yoshi MB Polarity Case 3 Pipeline"),
    YoshiMBPolarityCase3NoAlignment("Yoshi MB Polarity Case 3 No Alignment Pipeline"),
    YoshiMBPolarityCase4("Yoshi MB Polarity Case 4 Pipeline"),
    YoshiMBSplitMCFOCase1("Yoshi MB Split MCFO Case 1 Pipeline");

    private String name;

    private PipelineProcess(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
