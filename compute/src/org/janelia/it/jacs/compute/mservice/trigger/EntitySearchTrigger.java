package org.janelia.it.jacs.compute.mservice.trigger;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/12/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class EntitySearchTrigger {
    boolean recursive=true;

    public class TriggerResponse {
        public boolean performAction=false;
        public boolean continueSearch=true;
    }

    public abstract TriggerResponse evaluate(Entity parent, Entity entity, int level);

    public void setRecursive(boolean recursive) {
        this.recursive=recursive;
    }

    public boolean isRecursive() {
        return recursive;
    }

}
