package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jongo.marshall.jackson.oid.Id;

public class ScreenSample implements DomainObject, HasImages, HasFilepath {

    @Id
    private Long id;
    private String name;
    private String ownerKey;
    private List<String> readers;
    private List<String> writers;
    private Date creationDate;
    private Date updatedDate;
    private String flyLine;
    private String filepath;
    private Map<ImageType,String> images;
    private ReverseReference masks;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getOwnerKey() {
        return ownerKey;
    }
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }
    public List<String> getReaders() {
        return readers;
    }
    public void setReaders(List<String> readers) {
        this.readers = readers;
    }
    public List<String> getWriters() {
        return writers;
    }
    public void setWriters(List<String> writers) {
        this.writers = writers;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Date getUpdatedDate() {
        return updatedDate;
    }
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
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
