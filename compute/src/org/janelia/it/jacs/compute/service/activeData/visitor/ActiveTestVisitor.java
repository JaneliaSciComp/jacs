/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData.visitor;

import org.apache.log4j.Logger;

/**
 *
 * @author murphys
 */
public class ActiveTestVisitor extends ActiveVisitor {
    Logger logger = Logger.getLogger(ActiveTestVisitor.class);

    @Override
    public Boolean call() throws Exception {
        logger.info("ActiveTestVisitor run() - entityId="+entityId);
        return true;
    }
}
