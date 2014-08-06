/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 *
 * @author murphys
 */
public abstract class ActiveVisitor implements Callable<Boolean> {
    protected Long entityId;
    protected ActiveDataClient activeData;
    protected Map<String, Object> parameterMap;
    protected Map<String, Object> contextMap;
    
    public void setEntityId(Long entityId) {
        this.entityId=entityId;
    }
    
    public void setActiveDataClient(ActiveDataClient activeDataClient) {
        this.activeData=activeDataClient;
    }
    
    public void setParameterMap(Map<String, Object> parameterMap) {
        this.parameterMap=parameterMap;
    }
    
    public void setContextMap(Map<String, Object> contextMap) {
        this.contextMap=contextMap;
    }
    
}
