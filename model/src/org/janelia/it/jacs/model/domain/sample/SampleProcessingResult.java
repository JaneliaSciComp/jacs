package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.ImageType;
import org.janelia.it.jacs.model.domain.interfaces.HasImages;

public class SampleProcessingResult extends PipelineResult implements HasImages {

    private String anatomicalArea;
    private String imageSize;
    private String opticalResolution;
    private Map<ImageType,String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public String getAnatomicalArea() {
        return anatomicalArea;
    }
    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }
    public String getImageSize() {
        return imageSize;
    }
    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }
    public String getOpticalResolution() {
        return opticalResolution;
    }
    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }
    public Map<ImageType, String> getImages() {
        return images;
    }
    public void setImages(Map<ImageType, String> images) {
        this.images = images;
    }
}
