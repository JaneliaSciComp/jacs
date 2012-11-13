package org.janelia.it.jacs.compute.mservice;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/12/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class EntitySearchTrigger {

    public class TriggerResponse {
        public boolean performAction=false;
        public boolean continueSearch=true;
    }

    public abstract TriggerResponse evaluate(Entity parent, Entity entity, int level);

}
