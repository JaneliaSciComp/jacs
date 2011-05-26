package org.janelia.it.jacs.model.entity;
// Generated May 26, 2011 10:42:23 AM by Hibernate Tools 3.2.1.GA


import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.User;

import java.util.Date;

/**
 * EntityData generated by hbm2java
 */
public class EntityData  implements java.io.Serializable, IsSerializable {
     private String id;
     private EntityAttribute entityAttribute;
     private Entity entity;
     private User user;
     private String val;
     private Date creationDate;
     private Date updatedDate;

    public EntityData() {
    }

	
    public EntityData(String id) {
        this.id = id;
    }
    public EntityData(String id, EntityAttribute entityAttribute, Entity entity, User User, String val, Date creationDate, Date updatedDate) {
       this.id = id;
       this.entityAttribute = entityAttribute;
       this.entity = entity;
       this.user = user;
       this.val = val;
       this.creationDate = creationDate;
       this.updatedDate = updatedDate;
    }
   
    public String getId() {
        return this.id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    public EntityAttribute getEntityAttribute() {
        return this.entityAttribute;
    }
    
    public void setEntityAttribute(EntityAttribute entityAttribute) {
        this.entityAttribute = entityAttribute;
    }
    public Entity getEntity() {
        return this.entity;
    }
    
    public void setEntity(Entity entity) {
        this.entity = entity;
    }
    public User getUser() {
        return this.user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    public String getVal() {
        return this.val;
    }
    
    public void setVal(String val) {
        this.val = val;
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




}


