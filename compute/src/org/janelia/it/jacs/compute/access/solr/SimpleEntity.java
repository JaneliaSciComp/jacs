package org.janelia.it.jacs.compute.access.solr;

import java.util.*;

/**
 * Simplified entity, holding those things we need to load into the SOLR index.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SimpleEntity {

    private Long id;
    private String name;
    private String owner;
    private String entityTypeName;
    private Date creationDate;
    private Date updatedDate;
    private Set<String> subjectKeys = new HashSet<String>();
    private Set<Long> childIds = new HashSet<Long>();
    private final Set<KeyValuePair> attributes = new HashSet<KeyValuePair>();
    
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
	public String getOwnerName() {
		if (owner==null) return owner;
		if (owner.contains(":")) return owner.split(":")[1];
		return owner;
	}
	public void setOwnerKey(String owner) {
		this.owner = owner;
	}
	public String getEntityTypeName() {
		return entityTypeName;
	}
	public void setEntityTypeName(String entityTypeName) {
		this.entityTypeName = entityTypeName;
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
	public Set<Long> getChildIds() {
		return childIds;
	}
	public Set<KeyValuePair> getAttributes() {
		return attributes;
	}
	public Set<String> getSubjectKeys() {
		return subjectKeys;
	}
	public Set<String> getSubjectNames() {
		Set<String> names = new HashSet<String>();
		for(String key : subjectKeys) {
			names.add(key.contains(":") ? key.split(":")[1] : key);
		}
		return names;
	}
}
