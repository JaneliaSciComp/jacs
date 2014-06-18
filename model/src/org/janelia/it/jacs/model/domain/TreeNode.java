package org.janelia.it.jacs.model.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jongo.marshall.jackson.oid.Id;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS,property="class")
public class TreeNode implements DomainObject {

    @Id
    private Long id;
    private String name;
    private String ownerKey;
    private Set<String> readers;
    private Set<String> writers;
    private Date creationDate;
    private Date updatedDate;
    private List<Reference> children;

    public int getNumChildren() {
        return children==null? 0 : children.size();
    }
    
    public void addChild(Reference ref) {
    	if (children==null) {
    		this.children = new ArrayList<Reference>();
    	}
    	children.add(ref);
    }

    public void insertChild(int index, Reference ref) {
    	if (children==null) {
    		this.children = new ArrayList<Reference>();
    	}
    	children.add(index, ref);
    }
    
    public void removeChild(Reference ref) {
    	if (children==null) {
    		return;
    	}
    	children.remove(ref);
    	if (children.isEmpty()) {
    		children = null;
    	}
    }
    
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
    public List<Reference> getChildren() {
        return children;
    }
    public void setChildren(List<Reference> children) {
        this.children = children;
    }
}
