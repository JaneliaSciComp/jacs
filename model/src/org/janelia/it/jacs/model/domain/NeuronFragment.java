package org.janelia.it.jacs.model.domain;

import java.util.List;

import org.jongo.marshall.jackson.oid.Id;

public class NeuronFragment implements DomainObject, HasMips, HasMaskChan, HasFilepath {

    @Id
    private Long id;
    private Long sampleId;
    private String ownerKey;
    private List<String> readers;
    private List<String> writers;
    private Integer number;
    private String filepath;
    private String signalMipFilepath;
    private String referenceMipFilepath;
    private String maskFilepath;
    private String chanFilepath;

    @Override
    public String getFilepath() {
        return getSeparationFilepath();
    }

    public String getSeparationFilepath() {
        return filepath;
    }
    public void setSeparationFilepath(String separationFilepath) {
        this.filepath = separationFilepath;
    }

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
    public Integer getNumber() {
        return number;
    }
    public void setNumber(Integer number) {
        this.number = number;
    }
    public String getReferenceMipFilepath() {
        return referenceMipFilepath;
    }
    public void setReferenceMipFilepath(String referenceMipFilepath) {
        this.referenceMipFilepath = referenceMipFilepath;
    }
    public String getSignalMipFilepath() {
        return signalMipFilepath;
    }
    public void setSignalMipFilepath(String signalMipFilepath) {
        this.signalMipFilepath = signalMipFilepath;
    }
    @Override
    public String getMaskFilepath() {
        return maskFilepath;
    }
    public void setMaskFilepath(String maskFilepath) {
        this.maskFilepath = maskFilepath;
    }
    @Override
    public String getChanFilepath() {
        return chanFilepath;
    }
    public void setChanFilepath(String chanFilepath) {
        this.chanFilepath = chanFilepath;
    }
    
}
