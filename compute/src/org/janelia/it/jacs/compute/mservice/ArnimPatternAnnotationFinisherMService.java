package org.janelia.it.jacs.compute.mservice;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 10/2/12
 * Time: 1:26 PM
 * To change this template use File | Settings | File Templates.
 */


/*
    This service runs to validate the contents of the Pattern Annotation results, checking that the appropriate
    entities exist for each sample.

 */

public class ArnimPatternAnnotationFinisherMService extends MService {

    Logger logger = Logger.getLogger(ArnimPatternAnnotationFinisherMService.class);

    public ArnimPatternAnnotationFinisherMService(String username) throws Exception {
        super(username);
    }

    public void run() throws Exception {
        logger.info("ArnimPatternAnnotationFinisherMService: run() start");
        Entity topLevelSampleFolder = getTopLevelFolder(ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME,
                false /* create if doesn't exist */);
        if (topLevelSampleFolder==null) {
            throw new Exception("Top level folder with name="+ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME+" is null");
        } else {
            topLevelSampleFolder=getEntityBean().getEntityAndChildren(topLevelSampleFolder.getId());
            Set<Entity> children1=topLevelSampleFolder.getChildren();
            if (children1==null) {
                throw new Exception("children1 is null");
            } else {
                // GMR Folders
                for (Entity child1 : children1) {
                    logger.info("child1 name="+child1.getName()+" type="+child1.getEntityType().getName());
                    child1=getEntityBean().getEntityAndChildren(child1.getId());
                    Set<Entity> children2=child1.getChildren();
                    if (children2==null) {
                        logger.info("children2 is null");
                    } else {
                        // GMR<plate><well> Folders
                        for (Entity child2 : children2) {
                            logger.info("...child2 name="+child2.getName()+ "type="+child2.getEntityType().getName());
                            child2=getEntityBean().getEntityAndChildren(child2.getId());
                            Set<Entity> children3=child2.getChildren();
                            if (children3==null) {
                                logger.info("children3 is null");
                            } else {
                                // Fly Line
                                for (Entity child3 : children3) {
                                    logger.info("......child3 name="+child3.getName()+" type="+child3.getEntityType().getName());
                                    child3=getEntityBean().getEntityAndChildren(child3.getId());
                                    Set<Entity> children4=child3.getChildren();
                                    if (children4==null) {
                                        logger.info("children4 is null");
                                    } else {
                                        // Screen Sample
                                        for (Entity child4 : children4) {
                                            logger.info(".........child4 name="+child4.getName()+" type="+child4.getEntityType().getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        logger.info("ArnimPatternAnnotationFinisherMService: run() end");
    }


}
