package org.janelia.it.jacs.compute.mservice;

import org.janelia.it.jacs.model.entity.Entity;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/26/12
 * Time: 6:47 PM
 * To change this template use File | Settings | File Templates.
 */

public class EntityChangeNameAction extends EntityAction {
    private String newEntityName;

    public EntityChangeNameAction(String newEntityName) {
        this.newEntityName = newEntityName;
    }

    public Callable getCallable(final Entity parentEntity, final Entity entity) throws Exception {
        return new Callable<Object>() {
            public Object call() throws Exception {
                entity.setName(newEntityName);
                Entity entityCopy = getEntityBean().saveOrUpdateEntity(entity);
                return entityCopy;
            }
        };
    }

}
