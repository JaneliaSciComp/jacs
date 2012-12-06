package org.janelia.it.jacs.compute.mservice.action;

import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 12/6/12
 * Time: 11:29 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CalledAction implements Callable {
    private String contextKey;

    abstract public CalledAction call() throws Exception;

    public void setContextKey(String contextKey) {
        this.contextKey=contextKey;
    }

    public String getContextKey() {
        return contextKey;
    }

    public void setContextKey(Object o) {
        this.contextKey=(String)o;
    }

}
