/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;

/**
 *
 * @author murphys
 */
public class ActiveEntityTestVisitor extends ActiveVisitor {
    Logger logger = Logger.getLogger(ActiveEntityTestVisitor.class);

    @Override
    public void run() {
        logger.info("ActiveDataEntityTestVisitor run() - entityId="+entityId);
        Entity e=null;
        try {
            e = EJBFactory.getLocalEntityBean().getEntityById(entityId);
        } catch (ComputeException ex) {
            logger.info(ex,ex);
        }
        if (e!=null) {
            logger.info("Entity confirmation id="+e.getId());
        } else {
            logger.info("Entity e is null");
        }
    }
    
}
