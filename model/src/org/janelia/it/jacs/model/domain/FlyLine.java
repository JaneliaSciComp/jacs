package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.Set;

import org.jongo.marshall.jackson.oid.Id;

public class FlyLine implements DomainObject {

    @Id
    private Long id;
    private String name;
    private String ownerKey;
    private Set<String> readers;
    private Set<String> writers;
    private Date creationDate;
    private Date updatedDate;
    private Long representativeId;
    private Long balancedLineId;
    private Long originalLineId;
    private Integer robotId;
    private String splitPart;

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
    public Long getRepresentativeId() {
        return representativeId;
    }
    public void setRepresentativeId(Long representativeId) {
        this.representativeId = representativeId;
    }
    public Long getBalancedLineId() {
        return balancedLineId;
    }
    public void setBalancedLineId(Long balancedLineId) {
        this.balancedLineId = balancedLineId;
    }
    public Long getOriginalLineId() {
        return originalLineId;
    }
    public void setOriginalLineId(Long originalLineId) {
        this.originalLineId = originalLineId;
    }
    public Integer getRobotId() {
        return robotId;
    }
    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
    }
    public String getSplitPart() {
        return splitPart;
    }
    public void setSplitPart(String splitPart) {
        this.splitPart = splitPart;
    }
    
}
