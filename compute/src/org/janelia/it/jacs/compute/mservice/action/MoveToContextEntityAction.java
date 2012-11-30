package org.janelia.it.jacs.compute.mservice.action;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/29/12
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */

public class MoveToContextEntityAction extends EntityAction {
    private static Logger logger= Logger.getLogger(MoveToContextEntityAction.class);
    String destinationEntityContextKey;

    public MoveToContextEntityAction(String destinationEntityContextKey) {
        this.destinationEntityContextKey=destinationEntityContextKey;
    }

    public Callable getCallable(final Entity parentEntity, final Entity entity, final Map context) throws Exception {
        return new Callable<Object>() {
            public Object call() throws Exception {
                Entity destinationParentEntity=(Entity)context.get(destinationEntityContextKey);
                if (destinationParentEntity==null) {
                    throw new Exception("Could not find expected destinationParentEntity with context key="+destinationEntityContextKey);
                }
                // First, find the existing entity data
                Set<EntityData> edsToRemove=new HashSet<EntityData>();
                Set<EntityData> parentEdSet=parentEntity.getEntityData();
                for (EntityData ed : parentEdSet) {
                    if (ed.getChildEntity().getId().equals(entity.getId())) {
                        edsToRemove.add(ed);
                    }
                }
                if (edsToRemove.size()==0) {
                    throw new Exception("Could not find any expected matching child-entity id="+entity.getId()+" for parent id="+parentEntity.getId());
                }
                // First add new parent
                getEntityBean().addEntityToParent(destinationParentEntity, entity, 0, EntityConstants.ATTRIBUTE_ENTITY);
                // Then, remove the original entity data(s)
                for (EntityData ed : edsToRemove) {
                    getEntityBean().deleteEntityData(ed);
                }
                return entity;
          }
        };
    }

}

