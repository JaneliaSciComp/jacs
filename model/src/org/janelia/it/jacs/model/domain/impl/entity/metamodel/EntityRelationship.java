package org.janelia.it.jacs.model.domain.impl.entity.metamodel;

import org.janelia.it.jacs.model.domain.impl.metamodel.AbstractRelationship;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.entity.EntityData;

public class EntityRelationship extends AbstractRelationship {
    
    public EntityRelationship(EntityData entityData, DomainObject source, DomainObject target) {
        setGuid(entityData.getId());
        setOrderIndex(entityData.getOrderIndex());
        setCreationDate(entityData.getCreationDate());
        setUpdatedDate(entityData.getUpdatedDate());
        setType(entityData.getEntityAttrName());
        setSource(source);
        setTarget(target);
    }    
}
