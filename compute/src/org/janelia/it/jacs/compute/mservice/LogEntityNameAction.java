package org.janelia.it.jacs.compute.mservice;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/26/12
 * Time: 6:35 PM
 * To change this template use File | Settings | File Templates.
 */

public class LogEntityNameAction extends EntityAction {
    private static final Logger logger = Logger.getLogger(LogEntityNameAction.class);
    private String logMessage;

    public LogEntityNameAction(String logMessage) {
        this.logMessage=logMessage;
    }

    public Callable getCallable(final Entity parentEntity, final Entity entity) throws Exception {
        return new Callable<Object>() {
            public Object call() {
                logger.info(logMessage+" : entityName="+entity.getName() + " entityId="+entity.getId() + " parentEntityName="+parentEntity.getName());
                return null;
            }
        };
    }

}
