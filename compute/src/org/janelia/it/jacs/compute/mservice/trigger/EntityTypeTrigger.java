package org.janelia.it.jacs.compute.mservice.trigger;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/26/12
 * Time: 6:27 PM
 * To change this template use File | Settings | File Templates.
 */

public class EntityTypeTrigger extends EntityTrigger {
    String entityTypeName;

    public EntityTypeTrigger(String entityTypeName) {
        this.entityTypeName=entityTypeName;
    }

    public TriggerResponse evaluate(Entity parent, Entity entity, int level) {
        TriggerResponse response = new TriggerResponse();
        if (entity.getEntityTypeName().equals(entityTypeName)) {
            response.continueSearch = false;
            response.performAction = true;
        } else {
            response.continueSearch = true;
            response.performAction = false;
        }
        return response;
    }

}
