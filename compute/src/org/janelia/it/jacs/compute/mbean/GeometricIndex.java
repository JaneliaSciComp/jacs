/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;

/**
 *
 * @author murphys
 */
public class GeometricIndex implements GeometricIndexMBean {
    
    private static final Logger logger = Logger.getLogger(GeometricIndex.class);


    @Override
    public void start() {
        logger.info("GeometricIndex - start()");
    }

    @Override
    public void stop() {
        logger.info("GeometricIndex - stop()");
    }
    
}
