package org.janelia.it.jacs.compute.mservice.action;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 12/5/12
 * Time: 6:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class SubcontextPushAction extends EntityAction {
    private static final Logger logger = Logger.getLogger(SubcontextPushAction.class);
    private boolean resetContext=false;

    public SubcontextPushAction() {}

    public SubcontextPushAction(boolean resetContext) {
        this.resetContext=resetContext;
    }

    public CalledAction getCallableImpl(final Entity parentEntity, final Entity entity, final Map<String, Object> context) throws Exception {
        return new CalledAction() {
            public CalledAction call() {
                String contextKey=(String)context.get(CONTEXT);
                if (resetContext || contextKey==null || contextKey.length()==0) {
                    contextKey=entity.getId().toString();
                } else {
                    contextKey=contextKey+":"+entity.getId().toString();
                }
                logger.info("Creating context key="+contextKey);
                context.put(CONTEXT, contextKey);
                return this;
            }
        };
    }

}
