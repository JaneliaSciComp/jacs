package org.janelia.it.jacs.model.domain;

import java.util.List;

public class SampleTile {
    
    private String name;
    private String anatomicalArea;
    private List<LSMImage> lsms;

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

    public List<LSMImage> getLsms() {
        return lsms;
    }

    public void setLsms(List<LSMImage> lsms) {
        this.lsms = lsms;
    }
    
    
}
