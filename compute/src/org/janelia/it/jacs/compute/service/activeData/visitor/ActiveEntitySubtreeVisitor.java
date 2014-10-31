package org.janelia.it.jacs.compute.service.activeData.visitor;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by murphys on 9/26/14.
 */
public class ActiveEntitySubtreeVisitor extends ActiveVisitor {
    Logger logger = Logger.getLogger(ActiveEntitySubtreeVisitor.class);

    public void logAlignmentAttributes(Entity v) {
        Set<EntityData> eds=v.getEntityData();
        for (EntityData ed : eds) {
            if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE)) {
                logger.info("attr ALIGNMENT_SPACE=" + ed.getValue());
            } else if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)) {
                logger.info("attr OPTICAL_RESOLUTION=" + ed.getValue());
            } else if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)) {
                logger.info("attr PIXEL_RESOLUTION=" + ed.getValue());
            }
        }
    }

    @Override
    public Boolean call() throws Exception {
        logger.info("ActiveEntitySubtreeVisitor call() - entityId="+entityId);
        Entity e=null;
        long startTime=new Date().getTime();
        long endTime=0L;
        try {
            e = EJBFactory.getLocalEntityBean().getEntityTree(entityId);
            logger.info(">>>  SAMPLE_ID="+e.getId());
            final Set<Long> visitedSet=new HashSet<>();
            EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(e, new EntityVisitor() {
                public void visit(Entity v) throws Exception {
                    if (v.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                        logger.info("  >> ALIGNMENT_RESULT="+v.getId()+" NAME="+v.getName());
                        logAlignmentAttributes(v);
                        visitedSet.remove(v.getId()); // we want to start sub-search
                        EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(v, new EntityVisitor() {
                            public void visit(Entity v2) throws Exception {
                                if (v2.getEntityTypeName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                                    logger.info("   >   NEURON_SEPARATOR_PIPELINE_RESULT=" + v2.getId());
                                    logAlignmentAttributes(v2);
                                } else if (v2.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                                    if (v2.getName().endsWith(".chan") || v2.getName().endsWith(".mask")) {
                                        // skip
                                    } else {
                                        logger.info("   >   IMAGE_3D=" + v2.getId() + " NAME=" + v2.getName());
                                        logAlignmentAttributes(v2);
                                    }
                                } else if (v2.getEntityTypeName().equals(EntityConstants.TYPE_LSM_STACK)) {
                                    logger.info("   >   LSM_STACK=" + v2.getId() + " NAME="+v2.getName());
                                    logAlignmentAttributes(v2);
                                }
                            }
                        }, visitedSet);
                    }
                }
            }, visitedSet);
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

