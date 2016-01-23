package org.janelia.it.jacs.model.domain.screen;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.enums.AlignmentScoreType;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;
import org.janelia.it.jacs.model.domain.support.SearchType;
import com.fasterxml.jackson.annotation.JsonIgnore;

@MongoMapped(collectionName="screenSample",label="Screen Sample")
@SearchType(key="screenSample",label="Screen Sample")
public class ScreenSample extends AbstractDomainObject implements HasFiles, HasFilepath {

    @SearchAttribute(key="filepath_txt",label="File Path")
    private String filepath;
    
    @SearchAttribute(key="flyline_txt",label="Fly Line")
    private String flyLine;

    @SearchTraversal({})
    private ReverseReference patternMasks;
    
    private Map<FileType, String> images;
    
    private Map<AlignmentScoreType, String> scores;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getFlyLine() {
        return flyLine;
    }

    public void setFlyLine(String flyLine) {
        this.flyLine = flyLine;
    }

    public ReverseReference getPatternMasks() {
        return patternMasks;
    }

    public void setPatternMasks(ReverseReference patternMasks) {
        this.patternMasks = patternMasks;
    }

    @JsonIgnore
    public Map<FileType, String> getFiles() {
        return images;
    }

    // duplicate of getFiles, but need to fulfill get/set for serialization
    public Map<FileType, String> getImages() {
        return images;
    }

    public void setImages(Map<FileType, String> images) {
        this.images = images;
    }

    public Map<AlignmentScoreType, String> getScores() {
        return scores;
    }

    public void setScores(Map<AlignmentScoreType, String> scores) {
        this.scores = scores;
    }
}
