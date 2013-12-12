package org.janelia.it.jacs.model.domain.impl.entity.metamodel;

import java.util.Collection;

import org.janelia.it.jacs.model.domain.impl.metamodel.AbstractDomainObject;
import org.janelia.it.jacs.model.entity.Entity;

public class EntityDomainObject extends AbstractDomainObject {

    public EntityDomainObject(Entity entity) {
        setGuid(entity.getId());
        setCreationDate(entity.getCreationDate());
        setUpdatedDate(entity.getUpdatedDate());
        setEntityTypeName(entity.getEntityTypeName());
        setName(entity.getName());
        setRelationshipsAreInitialized(false);
    }
}
