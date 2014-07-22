/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.Map;

/**
 *
 * @author murphys
 */
public abstract class ActiveVisitor implements Runnable {
    private Long entityId;
    private ActiveDataClient activeData;
    private Map<String, Object> parameterMap;
    private Map<String, Object> contextMap;
    
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
