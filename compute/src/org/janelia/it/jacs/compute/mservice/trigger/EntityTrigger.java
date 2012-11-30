package org.janelia.it.jacs.compute.mservice.trigger;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.compute.mservice.action.EntityAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/12/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class EntityTrigger {
    boolean recursive=true;
    List<EntityAction> actionList=new ArrayList<EntityAction>();

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

    public List<EntityAction> getActionList() {
        return actionList;
    }

    public void addAction(EntityAction action) {
        actionList.add(action);
    }

}
