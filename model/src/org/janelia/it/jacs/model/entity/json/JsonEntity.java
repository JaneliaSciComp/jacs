package org.janelia.it.jacs.model.entity.json;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class JsonEntity {
	
	@JsonIgnore
	private Entity entity;
	
	public JsonEntity(Entity entity) {
		this.entity = entity;
	}
	
	@JsonProperty
	public String getId() {
		return entity.getId().toString();
	}
	
	@JsonProperty
	public String getOwnerKey() {
		if (!Hibernate.isInitialized(entity)) return null;
		return entity.getOwnerKey();
	}
	
	@JsonProperty
	public String getEntityTypeName() {
		if (!Hibernate.isInitialized(entity)) return null;
		return entity.getEntityTypeName();
	}
	
	@JsonProperty
	public List<JsonEntityData> getEntityData() {
		if (!Hibernate.isInitialized(entity)) return null;
		List<JsonEntityData> jed = new ArrayList<JsonEntityData>();
		for(EntityData ed : entity.getEntityData()) {
			jed.add(new JsonEntityData(ed));
		}
		return jed;
	}
}
