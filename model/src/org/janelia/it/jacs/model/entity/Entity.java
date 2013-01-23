package org.janelia.it.jacs.model.entity;

import java.util.*;

import javax.xml.bind.annotation.*;

import com.google.gwt.user.client.rpc.IsSerializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="Entity")
public class Entity  implements java.io.Serializable, IsSerializable {
	
	@XmlAttribute(name="guid")
    private Long id;
	
	@XmlElement
    private String name;
	
	@XmlElement
    private String ownerKey;
	
	@XmlElement
    private EntityStatus entityStatus;

	@XmlElement
    private EntityType entityType;
	
	@XmlTransient
    private Date creationDate;

	@XmlTransient
    private Date updatedDate;
    
    @XmlElement(name="entityData")
    @XmlElementWrapper(name="entityDataSet")
    private Set<EntityData> entityData = new HashSet<EntityData>(0);

    @XmlTransient
    private Set<EntityActorPermission> entityActorPermissions = new HashSet<EntityActorPermission>(0);
    
    public Entity() {
    }
	
    public Entity(Long id) {
		this.id = id;
	}

	public Entity(Long id, String name, String ownerKey, EntityStatus entityStatus, EntityType entityType, Date creationDate,
			Date updatedDate, Set<EntityData> entityData) {
		this.id = id;
		this.name = name;
		this.ownerKey = ownerKey;
		this.entityStatus = entityStatus;
		this.entityType = entityType;
		this.creationDate = creationDate;
		this.updatedDate = updatedDate;
		this.entityData = entityData;
	}
	
    public Entity(Long id, String name, String ownerKey, EntityStatus entityStatus, EntityType entityType, Date creationDate,
                  Date updatedDate, Set<EntityData> entityData, Set<EntityActorPermission> entityActorPermissions) {
       this.id = id;
       this.name = name;
		this.ownerKey = ownerKey;
       this.entityStatus = entityStatus;
       this.entityType = entityType;
       this.creationDate = creationDate;
       this.updatedDate = updatedDate;
       this.entityData = entityData;
       this.entityActorPermissions = entityActorPermissions;
    }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public EntityStatus getEntityStatus() {
        return this.entityStatus;
    }
    
    public void setEntityStatus(EntityStatus entityStatus) {
        this.entityStatus = entityStatus;
    }
    public EntityType getEntityType() {
        return this.entityType;
    }
    
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    public Date getCreationDate() {
        return this.creationDate;
    }
    
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Date getUpdatedDate() {
        return this.updatedDate;
    }
    
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
    public Set<EntityData> getEntityData() {
        return this.entityData;
    }
    
    public void setEntityData(Set<EntityData> entityData) {
        this.entityData = entityData;
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

	public Set<EntityActorPermission> getEntityActorPermissions() {
		return entityActorPermissions;
	}

	public void setEntityActorPermissions(Set<EntityActorPermission> entityActorPermissions) {
		this.entityActorPermissions = entityActorPermissions;
	}

	public EntityAttribute getAttributeByName(String name) {
        Set<EntityAttribute> attributeSet = entityType.getAttributes();
        for (EntityAttribute ea : attributeSet) {
            if (ea.getName().equals(name)) {
                return ea;
            }
        }
        return null;
    }
    
	/**
	 * Returns the set of EntityData objects with the given attribute name.
	 */
	public EntityData getEntityDataByAttributeName(String attributeName) {
		Set<EntityData> matchingData = new HashSet<EntityData>();
		for (EntityData ed : entityData) {
			if (ed.getEntityAttribute().getName().matches(attributeName)) {
				matchingData.add(ed);
			}
		}
		if (matchingData.size() == 1) {
			EntityData ed = matchingData.iterator().next();
			return ed;
		}
		if (matchingData.size() > 1) {
			System.out.println("Warning: expected single EntityData for "+attributeName+" in Entity#"+getId()+" but got "+matchingData.size());
		}
		return null;
	}

	/**
	 * Returns the value of the given attribute, if it exists and there is only one.
	 */
	public String getValueByAttributeName(String attributeName) {
		EntityData ed = getEntityDataByAttributeName(attributeName);
		if (ed == null) return null;
		return ed.getValue();
	}

	/**
	 * Returns the child given by the attribute, if it exists and there is only one.
	 */
	public Entity getChildByAttributeName(String attributeName) {
		EntityData ed = getEntityDataByAttributeName(attributeName);
		if (ed == null) return null;
		return ed.getChildEntity();
	}

    // This is the sister method of the above 'getValueByAttributeName'
    // which does the inverse.

    public boolean setValueByAttributeName(String attributeName, String value) {
        Set<EntityData> matchingData=new HashSet<EntityData>();
        for (EntityData ed : entityData) {
            if (ed.getEntityAttribute().getName().matches(attributeName)) {
                matchingData.add(ed);
            }
        }
        if (matchingData.size()==0) {
            // Ok, we will add this
            EntityAttribute attribute=getAttributeByName(attributeName);
            if (attribute==null) {
                throw new IllegalArgumentException("Entity "+getId()+" with type "+getEntityType().getName()+" does not have attribute: "+attributeName);
            }
            EntityData ed=new EntityData();
            ed.setParentEntity(this);
            ed.setEntityAttribute(attribute);
            ed.setValue(value);
            ed.setOwnerKey(ownerKey);
            Date createDate = new Date();
            ed.setCreationDate(createDate);
            ed.setUpdatedDate(createDate);
            this.entityData.add(ed);
            return true;
        } else if (matchingData.size()==1) {
            // Update the value of the existing entry
            EntityData ed=matchingData.iterator().next();
            ed.setValue(value);
            return true;
        }
        // More than one EntityData matching the attribute - do nothing
        return false;
    }

    // This returns the EntityData so it can be persisted
    public EntityData addChildEntity(Entity entity) {
    	return addChildEntity(entity, EntityConstants.ATTRIBUTE_ENTITY);
    }

    // This returns the EntityData so it can be persisted
    public EntityData addChildEntity(Entity entity, String attributeName) {
        EntityData ed=new EntityData();
        ed.setParentEntity(this);
        ed.setChildEntity(entity);
        ed.setOwnerKey(ownerKey);
        Date createDate = new Date();
        ed.setCreationDate(createDate);
        ed.setUpdatedDate(createDate);
        EntityAttribute attribute=getAttributeByName(attributeName);
        if (attribute!=null) ed.setEntityAttribute(attribute);
        this.entityData.add(ed);
        return ed;
    }

	/**
	 * Returns the an ordered list of EntityData objects with the given attribute name.
	 */
	public List<EntityData> getList(String attributeName) {
		List<EntityData> matchingData = new ArrayList<EntityData>();
		for (EntityData ed : getOrderedEntityData()) {
			if (ed.getEntityAttribute().getName().matches(attributeName)) {
				matchingData.add(ed);
			}
		}
		return matchingData;
	}
	
	public Integer getMaxOrderIndex() {
		int max = 0;
		for(EntityData ed : entityData) {
			if (ed.getOrderIndex() != null && ed.getOrderIndex() > max) {
				max = ed.getOrderIndex();
			}
		}
		return max;
	}
	
    public List<EntityData> getOrderedEntityData() {
    	List<EntityData> orderedData = new ArrayList<EntityData>(getEntityData());
    	Collections.sort(orderedData, new Comparator<EntityData>() {
			@Override
			public int compare(EntityData o1, EntityData o2) {
				if (o1.getOrderIndex() == null) {
					if (o2.getOrderIndex() == null) {
						if (o1.getId()==null) {
							if (o2.getId()==null) {
								return 0;
							}
							return -1;
						}
						else if (o2.getId()==null) {
							return 1;
						}
						return o1.getId().compareTo(o2.getId());
					}
					return -1;
				}
				else if (o2.getOrderIndex() == null) {
					return 1;
				}
				return o1.getOrderIndex().compareTo(o2.getOrderIndex());
			}
    		
		});
    	return orderedData;
    }
    
    /**
     * Get a list of children ordered by index order, or id if the index is not available.X
     * @return
     */
    public List<Entity> getOrderedChildren() {
    	List<Entity> children = new ArrayList<Entity>();
    	for(EntityData ed : getOrderedEntityData()) {
        	if (ed.getChildEntity() != null) children.add(ed.getChildEntity());
    	}
    	return children;
    }

	public boolean hasChildren() {
    	for(EntityData ed : entityData) {
        	if (ed.getChildEntity() != null) {
        		return true;
        	}
    	}
    	return false;
	}
    
    public Set<Entity> getChildren() {
    	Set<Entity> children = new HashSet<Entity>();
    	for(EntityData ed : entityData) {
        	if (ed.getChildEntity() != null) children.add(ed.getChildEntity());
    	}
    	return children;
    }
    
    @Override
    public String toString() {
        return getName();
    }
}


