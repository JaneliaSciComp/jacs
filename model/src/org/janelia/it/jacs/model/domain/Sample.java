package org.janelia.it.jacs.model.domain;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.jongo.marshall.jackson.oid.Id;

public class Sample implements DomainObject {

    @Id
    private Long id;
    private String name;
    private String ownerKey;
    private Set<String> readers;
    private Set<String> writers;
    private Date creationDate;
    private Date updatedDate;
    private String age;
    private String chanSpec;
    private String dataSet;
    private String effector;
    private String gender;
    private String line;
    private String slideCode;
    private String status;
    private String visited;
    private Map<String,ObjectiveSample> objectives;
    
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
    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
    }
    public String getChanSpec() {
        return chanSpec;
    }
    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }
    public String getDataSet() {
        return dataSet;
    }
    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }
    public String getEffector() {
        return effector;
    }
    public void setEffector(String effector) {
        this.effector = effector;
    }
    public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public String getLine() {
        return line;
    }
    public void setLine(String line) {
        this.line = line;
    }
    public String getSlideCode() {
        return slideCode;
    }
    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getVisited() {
        return visited;
    }
    public void setVisited(String visited) {
        this.visited = visited;
    }
    public Map<String, ObjectiveSample> getObjectives() {
        return objectives;
    }
    public void setObjectives(Map<String, ObjectiveSample> objectives) {
        this.objectives = objectives;
    }
    

}
