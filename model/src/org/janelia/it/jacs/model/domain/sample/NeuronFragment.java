package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.ImageType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasImages;

public class NeuronFragment extends AbstractDomainObject implements HasImages, HasFilepath {

    private Long sampleId;
    private Long separationId;
    private Integer number;
    private String filepath;
    private Map<ImageType,String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public Long getSampleId() {
        return sampleId;
    }
    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }
    public Long getSeparationId() {
        return separationId;
    }
    public void setSeparationId(Long separationId) {
        this.separationId = separationId;
    }
    public Integer getNumber() {
        return number;
    }
    public void setNumber(Integer number) {
        this.number = number;
    }
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public Map<ImageType, String> getImages() {
        return images;
    }
    public void setImages(Map<ImageType, String> images) {
        this.images = images;
    }
    
}
