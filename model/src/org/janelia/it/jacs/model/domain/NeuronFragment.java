package org.janelia.it.jacs.model.domain;

import java.util.Map;
import java.util.Set;

import org.jongo.marshall.jackson.oid.Id;

public class NeuronFragment implements DomainObject, HasImages, HasFilepath {

    @Id
    private Long id;
    private Long sampleId;
    private Long separationId;
    private String ownerKey;
    private Set<String> readers;
    private Set<String> writers;
    private Integer number;
    private String filepath;
    private Map<ImageType,String> images;

    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
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
    public String getOwnerKey() {
        return ownerKey;
    }
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }
    public Set<String> getReaders() {
        return readers;
    }
    public void setReaders(Set<String> readers) {
        this.readers = readers;
    }
    public Set<String> getWriters() {
        return writers;
    }
    public void setWriters(Set<String> writers) {
        this.writers = writers;
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
