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
    private long timestamp;
    
    public ActiveDataEntityEvent(String descriptor) {
        this.descriptor=descriptor;
        this.timestamp=new Date().getTime();
    }
    
    public String getDescriptor() {
        return descriptor;
    }
    
    public Date getDate() {
        return new Date(timestamp);
    }
}
