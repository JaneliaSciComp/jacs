package org.janelia.it.jacs.compute.mservice.action;

import org.apache.log4j.Logger;

import org.janelia.it.jacs.model.entity.Entity;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/26/12
 * Time: 6:47 PM
 * To change this template use File | Settings | File Templates.
 */

public class EntityChangeNameAction extends EntityAction {
    private static Logger logger= Logger.getLogger(EntityAction.class);
    private String priorExpectedName;
    private String newEntityName;

    public EntityChangeNameAction(String priorExpectedName, String newEntityName) {
        this.priorExpectedName = priorExpectedName;
        this.newEntityName = newEntityName;
    }

    public CalledAction getCallableImpl(final Entity parentEntity, final Entity entity, final Map<String, Object> context) throws Exception {
        return new CalledAction() {
            public CalledAction call() throws Exception {
                if (entity.getName().equals(priorExpectedName)) {
                    entity.setName(newEntityName);
                    logger.info("Changing name of entity id="+entity.getId());
                    getEntityBean().saveOrUpdateEntity(entity);
                } else {
                    logger.info("Skipping entity with non-matching name id="+entity.getId()+" name="+entity.getName());
                }
                return this;
            }
        };
    }

}
