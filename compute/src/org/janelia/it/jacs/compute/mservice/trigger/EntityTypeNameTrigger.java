package org.janelia.it.jacs.compute.mservice.trigger;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/26/12
 * Time: 6:30 PM
 * To change this template use File | Settings | File Templates.
 */

public class EntityTypeNameTrigger extends EntityTrigger {
    String entityTypeName;
    String entityName;

    public EntityTypeNameTrigger(String entityTypeName, String entityName) {
        this.entityTypeName=entityTypeName;
        this.entityName=entityName;
    }

    public TriggerResponse evaluate(Entity parent, Entity entity, int level) {
        TriggerResponse response = new TriggerResponse();
        if (entity.getEntityType().getName().equals(entityTypeName) && entity.getName().equals(entityName)) {
            response.continueSearch = false;
            response.performAction = true;
        } else {
            response.continueSearch = true;
            response.performAction = false;
        }
        return response;
    }

}