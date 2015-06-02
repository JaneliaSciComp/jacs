package org.janelia.it.jacs.model.entity.json;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class JsonEntityData {

	@JsonIgnore
	private EntityData entityData;
	
	public JsonEntityData(EntityData entityData) {
		this.entityData = entityData;
	}
	
	@JsonProperty
	public String getId() {
		return entityData.getId().toString();
	}

	@JsonProperty
	public String getValue() {
		return entityData.getValue();
	}
	
	@JsonProperty
	public String getOwnerKey() {
		if (!Hibernate.isInitialized(entityData)) return null;
		return entityData.getOwnerKey();
	}

	@JsonProperty
	public String getEntityAttrName() {
		if (!Hibernate.isInitialized(entityData)) return null;
		return entityData.getEntityAttrName();
	}
	
	@JsonProperty
	public Integer getOrderIndex() {
		return entityData.getOrderIndex();
	}
	
	@JsonProperty
	public String getChildEntityId() {
		Entity childEntity = entityData.getChildEntity();
		if (childEntity==null) return null;
		return childEntity.getId().toString();
	}
}
