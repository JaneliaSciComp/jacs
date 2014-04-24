package org.janelia.it.jacs.model.domain;

import java.util.List;

public class SampleTile implements HasMips {
    
    private String name;
    private String anatomicalArea;
    private List<Reference> lsmReferences;
    private String signalMipFilepath;
    private String referenceMipFilepath;

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

    public String getSignalMipFilepath() {
        return signalMipFilepath;
    }

    public void setSignalMipFilepath(String signalMipFilepath) {
        this.signalMipFilepath = signalMipFilepath;
    }

    public String getReferenceMipFilepath() {
        return referenceMipFilepath;
    }

    public void setReferenceMipFilepath(String referenceMipFilepath) {
        this.referenceMipFilepath = referenceMipFilepath;
    }
}
