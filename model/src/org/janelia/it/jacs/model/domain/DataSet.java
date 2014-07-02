package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jongo.marshall.jackson.oid.Id;

public class DataSet implements DomainObject {

    @Id
    private Long id;
    private String name;
    private String ownerKey;
    private Set<String> readers;
    private Set<String> writers;
    private Date creationDate;
    private Date updatedDate;
    private String identifier;
    private String sampleNamePattern;
    private SampleImageType sampleImageType;
    private Boolean sageSync;
    private List<String> pipelineProcesses;

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
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getSampleNamePattern() {
		return sampleNamePattern;
	}
	public void setSampleNamePattern(String sampleNamePattern) {
		this.sampleNamePattern = sampleNamePattern;
	}
	public SampleImageType getSampleImageType() {
		return sampleImageType;
	}
	public void setSampleImageType(SampleImageType sampleImageType) {
		this.sampleImageType = sampleImageType;
	}
	public Boolean getSageSync() {
		return sageSync;
	}
	public void setSageSync(Boolean sageSync) {
		this.sageSync = sageSync;
	}
	public List<String> getPipelineProcesses() {
		return pipelineProcesses;
	}
	public void setPipelineProcesses(List<String> pipelineProcesses) {
		this.pipelineProcesses = pipelineProcesses;
	}
}
