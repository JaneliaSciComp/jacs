package org.janelia.it.jacs.model.domain;

import java.util.List;

public class SampleTile {
    
    private String name;
    private String anatomicalArea;
    private List<Long> lsmIds;

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

    public List<Long> getLsmIds() {
        return lsmIds;
    }

    public void setLsmIds(List<Long> lsms) {
        this.lsmIds = lsms;
    }
    
    
}
