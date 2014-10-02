package org.janelia.it.jacs.compute.service.activeData.visitor;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.Random;

/**
 * Created by murphys on 10/1/14.
 */
public class AlignmentPropertiesVisitor extends ActiveVisitor {
    Logger logger = Logger.getLogger(AlignmentPropertiesVisitor.class);
    public final String HAS_ALIGN_PROP_NAME="HAS_ALIGN_PROP_NAME";


    @Override
    public Boolean call() throws Exception {
        logger.info("AlignmentPropertiesVisitor call() - entityId="+entityId);
        Entity e=null;
        try {
            e = EJBFactory.getLocalEntityBean().getEntityById(entityId);
        } catch (Exception ex) {
            logger.error("Error getting EntityBean and calling getEntityById()");
            logger.info(ex,ex);
        }
        if (e!=null) {
            logger.info("Entity confirmation id="+e.getId());
            String textFileName=e.getName();
            logger.info("Text file name="+textFileName);
            if (textFileName.equals("AlignedFlyBrain.properties")) {
                logger.info("HAS_ALIGN=true");
                activeData.addEntityEvent(signature, entityId, HAS_ALIGN_PROP_NAME+"=true");
            } else {
                logger.info("HAS_ALIGN=false");
                activeData.addEntityEvent(signature, entityId, HAS_ALIGN_PROP_NAME+"=false");
            }
            return true;
        } else {
            throw new Exception("Retrieved Entity e is null with id="+entityId);
        }
    }
}
