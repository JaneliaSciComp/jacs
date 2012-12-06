package org.janelia.it.jacs.compute.mservice.action;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 12/5/12
 * Time: 6:39 PM
 * To change this template use File | Settings | File Templates.
 */

public class SubcontextPopAction extends EntityAction {
    private static final Logger logger = Logger.getLogger(SubcontextPopAction.class);

    public CalledAction getCallableImpl(final Entity parentEntity, final Entity entity, final Map<String, Object> context) throws Exception {
        return new CalledAction() {
            public CalledAction call() {
                String contextKey=(String)context.get(CONTEXT);
                if (contextKey!=null) {
                    int lastPosition=contextKey.lastIndexOf(":");
                    if (lastPosition<0) {
                        context.remove(CONTEXT);
                    } else {
                        contextKey=contextKey.substring(0,lastPosition);
                    }
                }
                context.put(CONTEXT, contextKey);
                return this;
            }
        };
    }

}

