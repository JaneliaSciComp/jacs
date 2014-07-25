/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.mbean;

import org.jboss.system.ServiceMBean;

/**
 *
 * @author murphys
 */
interface GeometricIndexManagerMBean extends ServiceMBean {
    
    public void startGeometricIndexManager();
    
    public void stopGeometricIndexManager();
    
}
