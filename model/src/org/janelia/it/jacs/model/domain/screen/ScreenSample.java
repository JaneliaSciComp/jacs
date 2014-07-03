package org.janelia.it.jacs.model.domain.screen;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

public class ScreenSample extends AbstractDomainObject implements HasFiles, HasFilepath {

    private String flyLine;
    private String filepath;
    private Map<FileType,String> images;
    private ReverseReference masks;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public Map<FileType, String> getFiles() {
        return images;
    }
    public void setImages(Map<FileType, String> images) {
        this.images = images;
    }
    public String getFlyLine() {
        return flyLine;
    }
    public void setFlyLine(String flyLine) {
        this.flyLine = flyLine;
    }
    public ReverseReference getMasks() {
        return masks;
    }
    public void setMasks(ReverseReference masks) {
        this.masks = masks;
    }
}
