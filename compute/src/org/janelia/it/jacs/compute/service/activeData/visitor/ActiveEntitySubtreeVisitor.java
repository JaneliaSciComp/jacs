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

    private class IdentityEntityLoader implements AbstractEntityLoader {

        public Set<EntityData> getParents(Entity entity) throws Exception {
            // do nothing because we assume already loaded
            return null;
        }

        public Entity populateChildren(Entity entity) throws Exception {
            // do nothing because we assume already loaded
            return entity;
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


            EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(e, new EntityVisitor() {
                        public void visit(Entity result) throws Exception {
                            Set<EntityData> eds = result.getEntityData();
                            for (EntityData ed : eds) {
                                if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE)) {
                                    logger.info("Entity id=" + result.getId() + " of type=" + result.getEntityTypeName() + " has ALIGNMENT_SPACE=" + ed.getValue());
                                } else if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)) {
                                    logger.info("Entity id=" + result.getId() + " of type=" + result.getEntityTypeName() + " has OPTICAL_RESOLUTION=" + ed.getValue());
                                } else if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)) {
                                    logger.info("Entity id=" + result.getId() + " of type=" + result.getEntityTypeName() + " has PIXEL_RESOLUTION=" + ed.getValue());
                                }
                            }
                        }
                    }, new HashSet<Long>());

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

