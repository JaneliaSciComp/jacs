package org.janelia.it.jacs.compute.service.activeData.visitor;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;

import java.util.Set;

/**
 * Created by murphys on 10/2/14.
 */
public class IdentityEntityLoader implements AbstractEntityLoader {

    public Set<EntityData> getParents(Entity entity) throws Exception {
        // do nothing because we assume already loaded
        return null;
    }

    public Entity populateChildren(Entity entity) throws Exception {
        // do nothing because we assume already loaded
        return entity;
    }

}

