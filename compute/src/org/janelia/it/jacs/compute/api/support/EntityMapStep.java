package org.janelia.it.jacs.compute.api.support;

import java.io.Serializable;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * A step in the entity graph, across an EntityData to a related Entity. The direction is either up and down the tree
 * hierarchy.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityMapStep implements Serializable {
	
	private String attributeName;
	private String entityName;
	private String entityType;
	private boolean up;

	public EntityMapStep(EntityData entityData, boolean up) {
		Entity parent = up ? entityData.getParentEntity() : entityData.getChildEntity();
		this.attributeName = entityData.getEntityAttribute().getName();
		this.entityName = parent.getName();
		this.entityType = parent.getEntityType().getName();
		this.up = up;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getEntityType() {
		return entityType;
	}
	
	public boolean isUp() {
		return up;
	}
}