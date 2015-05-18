package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@MongoMapped(collectionName = "image")
@SearchType(key="image",label="Image")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public class Image extends AbstractDomainObject implements HasFiles, HasFilepath {

    @SearchAttribute(key="filepath_txt",label="File Path")
    private String filepath;
    
    @SearchAttribute(key="image_size_txt",label="Image Size")
    private String imageSize;

    @SearchAttribute(key="optical_res_txt",label="Optical Resolution")
    private String opticalResolution;

    @SearchAttribute(key="objective_txt",label="Objective", facet=true)
    private String objective;

    @SAGEAttribute(cvName="light_imagery", termName="channels")
    @SearchAttribute(key="num_channels_i",label="Num Channels", facet=true)
    private Integer numChannels;
    
    private Map<FileType, String> files;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    @Override
    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
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

    public Integer getNumChannels() {
        return numChannels;
    }

    public void setNumChannels(Integer numChannels) {
        this.numChannels = numChannels;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    @Override
    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }

}
