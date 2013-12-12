package org.janelia.it.jacs.model.entity;


import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.gwt.user.client.rpc.IsSerializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="EntityData")
public class EntityData implements java.io.Serializable, IsSerializable {
	
	@XmlAttribute(name="guid")
    private Long id;

	@XmlElement
    private String entityAttrName;
	
    @XmlTransient
    private Entity parentEntity;

	@XmlElement
    private Entity childEntity;

	@XmlElement
    private String ownerKey;

	@XmlElement
    private String value;
	
    @XmlTransient
    private Date creationDate;
    
    @XmlTransient
    private Date updatedDate;

	@XmlElement
    private Integer orderIndex;

    public EntityData() {
    }

    public EntityData(Long id) {
        this.id = id;
    }

    public EntityData(Long id, String entityAttrName, Entity parentEntity, Entity childEntity, String ownerKey,
                      String value, Date creationDate, Date updatedDate, Integer orderIndex) {
        this.id = id;
        this.entityAttrName = entityAttrName;
        this.parentEntity = parentEntity;
        this.childEntity = childEntity;
        this.ownerKey = ownerKey;
        this.value = value;
        this.creationDate = creationDate;
        this.updatedDate = updatedDate;
        this.orderIndex = orderIndex;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityAttrName() {
        return this.entityAttrName;
    }

    public void setEntityAttrName(String entityAttrName) {
        this.entityAttrName = entityAttrName;
    }

    public String getOwnerKey() {
		return ownerKey;
	}

	public void setOwnerKey(String ownerKey) {
		this.ownerKey = ownerKey;
	}

	public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

    public Entity getParentEntity() {
        return parentEntity;
    }

    public void setParentEntity(Entity parentEntity) {
        this.parentEntity = parentEntity;
    }

    public Entity getChildEntity() {
        return childEntity;
    }

    public void setChildEntity(Entity childEntity) {
        this.childEntity = childEntity;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}


