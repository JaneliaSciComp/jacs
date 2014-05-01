package org.janelia.it.jacs.model.domain;

import java.util.List;
import java.util.Map;

public class SampleTile implements HasImages {
    
    private String name;
    private String anatomicalArea;
    private List<Reference> lsmReferences;
    private ReverseReference masks;
    private Map<ImageType,String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAnatomicalArea() {
        return anatomicalArea;
    }
    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }
    public List<Reference> getLsmReferences() {
        return lsmReferences;
    }
    public void setLsmReferences(List<Reference> lsmReferences) {
        this.lsmReferences = lsmReferences;
    }
    public ReverseReference getMasks() {
        return masks;
    }
    public void setMasks(ReverseReference masks) {
        this.masks = masks;
    }
    public Map<ImageType, String> getImages() {
        return images;
    }
    public void setImages(Map<ImageType, String> images) {
        this.images = images;
    }
    
    
}
