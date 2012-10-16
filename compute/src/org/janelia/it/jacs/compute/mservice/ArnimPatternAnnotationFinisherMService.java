package org.janelia.it.jacs.compute.mservice;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.*;

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
    Set<Entity> sampleUpdateSet = Collections.synchronizedSet(new HashSet<Entity>());

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
            searchEntityContents(topLevelSampleFolder);

        }
        logger.info("ArnimPatternAnnotationFinisherMService: run() end");
    }

    protected void searchEntityContents(Entity parent) {
        searchEntityContents(parent, null, null);
    }

    private void searchEntityContents(Entity parent, Map<Entity, Integer> levelMap, String[] spaceArray) {
        if (levelMap==null) {
            levelMap=new HashMap<Entity, Integer>();
        }
        if (spaceArray==null) {
            spaceArray=new String[1000];
            StringBuilder builder=new StringBuilder();
            for (int i=0;i<1000;i++) {
                spaceArray[i]=builder.toString();
                builder.append(" ");
            }
        }
        Integer parentLevel=levelMap.get(parent);
        if (parentLevel==null) {
            parentLevel=0;
            logger.info("0: name="+parent.getName()+" type="+parent.getEntityType().getName());
        }
        parent=getEntityBean().getEntityAndChildren(parent.getId());
        Set<Entity> children=parent.getChildren();
        for (Entity child : children) {
            Integer childLevel=parentLevel+1;
            levelMap.put(child, childLevel);
            logger.info(spaceArray[childLevel]+childLevel+": name="+child.getName()+" type="+child.getEntityType().getName());
            searchEntityContents(child, levelMap, spaceArray);
        }
    }
}
