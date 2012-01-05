package org.janelia.it.jacs.compute.service.fly;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FlyScreenDiscoveryService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.User;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 1/4/12
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScreenSampleLineCoordinationService implements IService {

    protected Logger logger;
    protected User user;
    protected Date createDate;
    protected IProcessData processData;

    public static final String SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME="FlyLight Screen Pattern Annotation";

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.processData = processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();

            Entity screenPatternTopLevelFolder = getTopLevelFolder(ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME, true /* create */);
            Entity screenSampleTopLevelFolder = getTopLevelFolder(FlyScreenDiscoveryService.SCREEN_SAMPLE_TOP_LEVEL_FOLDER_NAME, false /* create */);

            List <Entity> flyLineList = getFlyLineList(screenPatternTopLevelFolder);
            logger.info("flyLineList size="+flyLineList.size());

            List<Entity> screenSampleList = getScreenSampleList(screenSampleTopLevelFolder);
            logger.info("screenSampleList size="+screenSampleList.size());

            coordinateLinesAndSamples(flyLineList, screenSampleList);
            logger.info("After coordination step, flyLineList size="+flyLineList.size());

            Long groupSize = processData.getLong("FLY_LINE_GROUP_SIZE");
            logger.info("Using FLY_LINE_GROUP_SIZE="+groupSize);

            //List<List<Entity>> groupList = createFlyLineGroupList(flyLineList);
            //logger.info("Created FLY_LINE_GROUP_LIST with "+groupList.size()+" entries");

            //processData.putItem("FLY_LINE_GROUP_LIST", groupList);

        } catch (Exception ex) {
            String msg = "Exception in execute() : " + ex.getMessage();
            logger.error(msg);
            throw new ServiceException(ex);
        }
    }

    protected Entity getTopLevelFolder(String topLevelFolderName, boolean createIfNecessary) throws Exception {
        return FileDiscoveryService.createOrVerifyRootEntity(topLevelFolderName, user, createDate, logger, createIfNecessary);
    }

    protected List<Entity> getFlyLineList(Entity topLevelFolder) throws Exception {
        List<Entity> flyLineList = new ArrayList<Entity>();
        Set<EntityData> edList = topLevelFolder.getEntityData();
        for (EntityData ed : edList) {
            if (ed.getEntityAttribute().getName().equals(EntityConstants.TYPE_FLY_LINE)) {
                Entity child = ed.getChildEntity();
                if (child != null) {
                    flyLineList.add(child);
                }
            }
        }
        if (flyLineList.size() > 1) {
            Collections.sort(flyLineList, new Comparator<Entity>() {
                @Override
                public int compare(Entity entity, Entity entity1) {
                    return entity.getName().compareToIgnoreCase(entity1.getName());
                }
            });
        }
        return flyLineList;
    }

    protected List<Entity> getScreenSampleList(Entity topLevelFolder) throws Exception {
         List<Entity> screenSampleList = new ArrayList<Entity>();
         Set<EntityData> edList = topLevelFolder.getEntityData();
         for (EntityData ed : edList) {
             if (ed.getEntityAttribute().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                 Entity child = ed.getChildEntity();
                 if (child != null) {
                     screenSampleList.add(child);
                 }
             }
         }
         if (screenSampleList.size() > 1) {
             Collections.sort(screenSampleList, new Comparator<Entity>() {
                 @Override
                 public int compare(Entity entity, Entity entity1) {
                     return entity.getName().compareToIgnoreCase(entity1.getName());
                 }
             });
         }
         return screenSampleList;
     }

    /*

        In this method, we want to make sure every sample has a FlyLine parent entity, and likewise that
        every needed FlyLine entity exists or is created.

     */
    protected void coordinateLinesAndSamples(List<Entity> lineList, List<Entity> sampleList) throws Exception {

    }


}
