/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData.visitor;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.Random;

/**
 *
 * @author murphys
 */
public class ActiveEntityTestVisitor extends ActiveVisitor {
    Logger logger = Logger.getLogger(ActiveEntityTestVisitor.class);

    @Override
    public Boolean call() throws Exception {
        logger.info("ActiveDataEntityTestVisitor call() - entityId="+entityId);
        Entity e=null;
        try {
            e = EJBFactory.getLocalEntityBean().getEntityById(entityId);
        } catch (Exception ex) {
            logger.error("Error getting EntityBean and calling getEntityById()");
            logger.info(ex,ex);
        }
        if (e!=null) {
            logger.info("Entity confirmation id="+e.getId());
            double debugRandomValue=new Random().nextDouble();
            if (debugRandomValue<0.05) {
                logger.error("Simulating error");
                return false;
            } else {
                return true;
            }
        } else {
            throw new Exception("Retrieved Entity e is null with id="+entityId);
        }
    }
    
}
