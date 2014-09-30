package org.janelia.it.jacs.compute.service.activeData.visitor;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.Date;
import java.util.Random;

/**
 * Created by murphys on 9/26/14.
 */
public class ActiveEntitySubtreeVisitor extends ActiveVisitor {
    Logger logger = Logger.getLogger(ActiveEntitySubtreeVisitor.class);

    @Override
    public Boolean call() throws Exception {
        logger.info("ActiveEntitySubtreeVisitor call() - entityId="+entityId);
        Entity e=null;
        long startTime=new Date().getTime();
        long endTime=0L;
        try {
            e = EJBFactory.getLocalEntityBean().getEntityTree(entityId);
            endTime=new Date().getTime();
        } catch (Exception ex) {
            logger.error("Error getting EntityBean and calling getEntityTree()");
            logger.info(ex,ex);
        }
        if (e!=null) {
            long retrievalTime=endTime-startTime;
            logger.info("Entity confirmation id="+e.getId()+", got tree in "+retrievalTime+" ms");
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

