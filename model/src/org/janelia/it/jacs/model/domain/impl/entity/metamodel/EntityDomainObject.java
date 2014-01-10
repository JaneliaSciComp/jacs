package org.janelia.it.jacs.model.domain.impl.entity.metamodel;

import java.util.Date;

import org.janelia.it.jacs.model.domain.impl.metamodel.AbstractDomainObject;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

public class EntityDomainObject extends AbstractDomainObject {

    public EntityDomainObject(String name, String ownerKey, String entityTypeName) {
        setName(name);
        setOwnerKey(ownerKey);
        setEntityTypeName(entityTypeName);
    }
    
    public EntityDomainObject(String name, String ownerKey, String entityTypeName, Date creationDate, Date updatedDate) {
        setName(name);
        setOwnerKey(ownerKey);
        setEntityTypeName(entityTypeName);
        setCreationDate(creationDate);
        setUpdatedDate(updatedDate);
    }
    
    public EntityDomainObject(Entity entity) {
        setGuid(entity.getId());
        setCreationDate(entity.getCreationDate());
        setUpdatedDate(entity.getUpdatedDate());
        setEntityTypeName(entity.getEntityTypeName());
        setName(entity.getName());
        setOwnerKey(entity.getOwnerKey());
        setRelationshipsAreInitialized(false);
        for(EntityData ed : entity.getEntityData()) {
            String value = ed.getValue();
            if (value!=null) {
                getAttributes().put(ed.getEntityAttrName(), value);
            }
        }
    }
}
