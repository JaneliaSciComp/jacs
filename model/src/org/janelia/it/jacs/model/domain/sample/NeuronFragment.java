package org.janelia.it.jacs.model.domain.sample;

import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

@MongoMapped(collectionName = "fragment")
public class NeuronFragment extends AbstractDomainObject implements HasFiles, HasFilepath {

    private Long sampleId;
    private Long separationId;
    private Integer number;
    private String filepath;
    private Map<FileType, String> files;

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

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setImages(Map<FileType, String> images) {
        this.files = images;
    }

}
