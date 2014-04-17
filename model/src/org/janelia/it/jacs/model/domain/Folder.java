package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.List;

import org.jongo.marshall.jackson.oid.Id;

public class Folder {

    @Id
    private Long id;
    private String ownerKey;
    private String name;
    private List<String> readers;
    private List<String> writers;
    private Date creationDate;
    private Date updatedDate;
    private String itemType;
    private List<Long> itemIds;
    
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getOwnerKey() {
        return ownerKey;
    }
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
    public String getItemType() {
        return itemType;
    }
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    public List<Long> getItemIds() {
        return itemIds;
    }
    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }
    
    
    
}
