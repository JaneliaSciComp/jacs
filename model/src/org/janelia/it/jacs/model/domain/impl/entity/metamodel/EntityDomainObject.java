package org.janelia.it.jacs.model.domain.impl.entity.metamodel;

import org.janelia.it.jacs.model.domain.impl.metamodel.AbstractDomainObject;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

public class EntityDomainObject extends AbstractDomainObject {

    public EntityDomainObject(String name, String ownerKey, String typeName) {
        setName(name);
        setOwnerKey(ownerKey);
        setTypeName(typeName);
    }
    
    public EntityDomainObject(Entity entity) {
        setGuid(entity.getId());
        setCreationDate(entity.getCreationDate());
        setUpdatedDate(entity.getUpdatedDate());
        setTypeName(entity.getEntityTypeName());
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

    public void setRelationshipsAreInitialized(boolean relationshipsAreInitialized) {
        this.relationshipsAreInitialized = relationshipsAreInitialized;
    }
}
