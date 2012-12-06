package org.janelia.it.jacs.compute.mservice.action;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/30/12
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */

public class AddChildEntityContextAction extends EntityAction {
    private static Logger logger= Logger.getLogger(AddChildEntityContextAction.class);

    String contextKeyString;
    String childEntityName;

    public AddChildEntityContextAction(String childEntityName, String contextKeyString) {
        this.childEntityName=childEntityName;
        this.contextKeyString=contextKeyString;
    }

    public Callable getCallable(final Entity parentEntity, final Entity entity, final Map<String, Object> context) throws Exception {
        return new Callable<Object>() {
            public Object call() throws Exception {
                logger.info("call() for AddChildEntityContextAction start");
                // We assume the entity is populated, but not its children
                Entity proxyEntity = getEntityBean().getEntityAndChildren(entity.getId());
                Set<EntityData> entityDataSet = proxyEntity.getEntityData();
                if (entityDataSet == null) {
                    logger.info("entityDataSet is null");
                } else {
                    logger.info("Found " + entityDataSet.size() + " entityData elements");
                    for (EntityData childEd : entityDataSet) {
                        Entity child = childEd.getChildEntity();
                        if (child != null) {
                            if (child.getName().equals(childEntityName)) {
                                context.put(contextualKey(contextKeyString, context), child);
                                logger.info("Added context entity child with key=" + contextKeyString + " name=" + childEntityName);
                            }
                            return child;
                        }
                    }
                }
                return null;
            }
        };
    }

}
