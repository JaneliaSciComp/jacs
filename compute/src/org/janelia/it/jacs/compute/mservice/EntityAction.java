package org.janelia.it.jacs.compute.mservice;

import org.janelia.it.jacs.model.entity.Entity;

import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/12/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class EntityAction {

    public abstract Callable<Object> getCallable(final Entity parentEntity, final Entity entity) throws Exception;

    public void processResult(Object result) {}

    public void handleFailure() {}

}
