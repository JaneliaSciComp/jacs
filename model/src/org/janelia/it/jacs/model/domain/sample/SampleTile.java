package org.janelia.it.jacs.model.domain.sample;

import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.ImageType;
import org.janelia.it.jacs.model.domain.interfaces.HasImages;

public class SampleTile implements HasImages {
    
    private String name;
    private String anatomicalArea;
    private List<Reference> lsmReferences;
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
    public Map<ImageType, String> getImages() {
        return images;
    }
    public void setImages(Map<ImageType, String> images) {
        this.images = images;
    }
    
    
}
