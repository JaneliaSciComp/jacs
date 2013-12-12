package org.janelia.it.jacs.model.domain.impl.entity.common;

import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.common.Folder;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

public class EntityFolder extends EntityDomainObject implements Folder {
    
    public EntityFolder(Entity entity) {
        super(entity);
    }

    @Override
    public boolean isRoot() {
        return getAttributeValue(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null;
    }
}
