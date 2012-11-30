package org.janelia.it.jacs.compute.mservice.action;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/29/12
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */

public class MoveEntityAction extends EntityAction {
    private static Logger logger= Logger.getLogger(MoveEntityAction.class);

    public Callable getCallable(final Entity parentEntity, final Entity entity, final Map context) throws Exception {
        return new Callable<Object>() {
            public Object call() throws Exception {
                return null;
          }
        };
    }

}

