/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.Date;

/**
 *
 * @author murphys
 */
public class ActiveDataEntityEvent {
    private String descriptor;
    private Object data;
    private long timestamp;
    
    public ActiveDataEntityEvent(String descriptor, Object data) {
        this.descriptor=descriptor;
        this.data=data;
        this.timestamp=new Date().getTime();
    }
    
    public String getDescriptor() {
        return descriptor;
    }

    public Object getData() { return data; }
    
    public Date getDate() {
        return new Date(timestamp);
    }
}
