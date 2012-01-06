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
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.user_data.User;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            Long groupSize = new Long(processData.getString("FLY_LINE_GROUP_SIZE").trim());
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
        List<Entity> flyLineList = getRecursiveEntitiesFromFolderByType(topLevelFolder, EntityConstants.TYPE_FLY_LINE);
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
         List<Entity> screenSampleList = getRecursiveEntitiesFromFolderByType(topLevelFolder, EntityConstants.TYPE_SCREEN_SAMPLE);
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

    protected List<Entity> getRecursiveEntitiesFromFolderByType(Entity folder, String typeName) throws Exception {
        List<Entity> entityList = new ArrayList<Entity>();
        Set<EntityData> edList = folder.getEntityData();
        for (EntityData ed : edList) {
            Entity child = ed.getChildEntity();
            if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                entityList.addAll(getRecursiveEntitiesFromFolderByType(child, typeName));
            } else if (child != null && child.getEntityType().getName().equals(typeName)) {
                entityList.add(child);
            }
        }
        return entityList;
    }

    /*

        In this method, we want to make sure every sample has a FlyLine parent entity, and likewise that
        every needed FlyLine entity exists or is created.

     */
    protected void coordinateLinesAndSamples(List<Entity> lineList, List<Entity> sampleList) throws Exception {
        for (Entity sample : sampleList) {
            String[] pwArr=getPlateAndWellPrefixesFromGMRName(sample.getName());
            //logger.info("sampleName = "+sample.getName()+" platePrefix = "+pwArr[0]+" fullPrefix = "+pwArr[1]);
        }
    }

    protected String[] getPlateAndWellPrefixesFromGMRName(String gmrName) throws Exception {
        // Example GMR name: GMR_57B09_AE_01_02-mA01b_C070202_20070507160307953
        try {
            String[] u1Arr = gmrName.split("_");
            if (u1Arr.length < 7) {
                throw new Exception("u1 length=" + u1Arr.length);
            }
            String pw = u1Arr[1];
            Pattern p = Pattern.compile("(\\d+)(\\D+)(\\d+)");
            logger.info("Applying matcher to string=" + pw);
            Matcher m = p.matcher(pw);
            if (m.lookingAt()) {
                if (m.groupCount() < 3) {
                    throw new Exception("pw group count=" + m.groupCount());
                }
                String plate = m.group(1);
                String well = m.group(2) + "" + m.group(3);
                String[] result = new String[2];
                result[0] = u1Arr[0] + "_" + plate;       // GMR_57
                result[1] = u1Arr[0] + "_" + u1Arr[1];    // GMR_57B09
                return result;
            } else {
                throw new Exception("lookingAt returned false - could not match " + pw);
            }
        } catch (Exception ex) {
            throw new Exception(ex.getMessage() + " : could not parse GMR name=" + gmrName);
        }
    }


}
