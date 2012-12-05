package org.janelia.it.jacs.compute.mservice.action;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/12/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class EntityAction {

    private static Logger logger= Logger.getLogger(EntityAction.class);
    private static boolean DEBUG=false;
    private List<Object> contextKeysToClearOnDone=new ArrayList<Object>();

    boolean blocking=true;

    public abstract Callable<Object> getCallable(final Entity parentEntity, final Entity entity, Map<Object, Object> context) throws Exception;

    public void processResult(Object result) {
        if (DEBUG) {
            if (result!=null && result instanceof Entity) {
                logger.info("Received result for entity id="+((Entity) result).getId());
            }
        }
    }

    public void handleFailure() {}

    protected EntityBeanLocal getEntityBean() {
        return EJBFactory.getLocalEntityBean();
    }

    protected ComputeBeanLocal getComputeBean() {
        return EJBFactory.getLocalComputeBean();
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking=blocking;
    }

    public void clearContextOnDone(Object key) {
        contextKeysToClearOnDone.add(key);
    }

    public List<Object> getContextKeysToClearOnDone() {
        return contextKeysToClearOnDone;
    }

}
