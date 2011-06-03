package org.janelia.it.jacs.model.entity;
// Generated May 26, 2011 10:42:23 AM by Hibernate Tools 3.2.1.GA


import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.User;

import javax.xml.crypto.Data;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity generated by hbm2java
 */
public class Entity  implements java.io.Serializable, IsSerializable {
     private Long id;
     private String name;
     private User user;
     private EntityStatus entityStatus;
     private EntityType entityType;
     private Date creationDate;
     private Date updatedDate;
     private Set<EntityData> entityData = new HashSet<EntityData>(0);

    public Entity() {
    }

	
    public Entity(Long id) {
        this.id = id;
    }
    public Entity(Long id, String name, User user, EntityStatus entityStatus, EntityType entityType, Date creationDate,
                  Date updatedDate, Set<EntityData> entityData) {
       this.id = id;
       this.name = name;
       this.user = user;
       this.entityStatus = entityStatus;
       this.entityType = entityType;
       this.creationDate = creationDate;
       this.updatedDate = updatedDate;
       this.entityData = entityData;
    }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    public User getUser() {
        return this.user;
    }
    
    public void setUser(User user) {
        this.user = user;
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

    // Utility methods

    public EntityAttribute getAttributeByName(String name) {
        Set<EntityAttribute> attributeSet = entityType.getAttributes();
        for (EntityAttribute ea : attributeSet) {
            if (ea.getName().equals(name)) {
                return ea;
            }
        }
        return null;
    }

    public boolean addAttributeAsTag(String attributeName) {
        Set<EntityData> data=this.getEntityData();
        EntityAttribute attribute = this.getAttributeByName(attributeName);
        if (attribute==null) {
            return false;
        } else {
            EntityData tag = new EntityData();
            tag.setParentEntity(this);
            tag.setEntityAttribute(attribute);
            tag.setUser(user);
            tag.setValue(attribute.getName());
            data.add(tag);
        }
        return true;
    }

    // This method will return non-null only in the case in which
    // there is a single matching EntityData to the given attribute
    // name AND the value of this entity data is a non-null String
    // rather than an Entity

    public String getValueByAttributeName(String attributeName) {
        Set<EntityData> matchingData=new HashSet<EntityData>();
        for (EntityData ed : entityData) {
            if (ed.getEntityAttribute().getName().matches(attributeName)) {
                matchingData.add(ed);
            }
        }
        if (matchingData.size()==1) {
            EntityData ed=matchingData.iterator().next();
            if (ed.getChildEntity()==null && ed.getValue()!=null) {
                return ed.getValue();
            }
        }
        return null;
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
                return false;
            }
            EntityData ed=new EntityData();
            ed.setParentEntity(this);
            ed.setEntityAttribute(attribute);
            ed.setValue(value);
            ed.setUser(user);
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

    public void addChildEntity(Entity entity) {
        EntityData ed=new EntityData();
        ed.setParentEntity(this);
        ed.setChildEntity(entity);
        ed.setUser(user);
        Date createDate = new Date();
        ed.setCreationDate(createDate);
        ed.setUpdatedDate(createDate);
        EntityAttribute attribute=getAttributeByName(EntityConstants.ATTRIBUTE_ENTITY);
        ed.setEntityAttribute(attribute);
        this.entityData.add(ed);
    }

    public boolean addChildEntity(Entity entity, String attributeName) {
        EntityData ed=new EntityData();
        ed.setParentEntity(this);
        ed.setChildEntity(entity);
        ed.setUser(user);
        Date createDate = new Date();
        ed.setCreationDate(createDate);
        ed.setUpdatedDate(createDate);
        EntityAttribute attribute=getAttributeByName(attributeName);
        if (attribute!=null) {
            ed.setEntityAttribute(attribute);
            this.entityData.add(ed);
            return true;
        } else {
            return false;
        }
    }


}


