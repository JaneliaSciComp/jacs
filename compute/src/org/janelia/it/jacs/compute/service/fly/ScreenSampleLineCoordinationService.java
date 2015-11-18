package org.janelia.it.jacs.compute.service.fly;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FlyScreenDiscoveryService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 1/4/12
 * Time: 2:37 PM
 */
public class ScreenSampleLineCoordinationService implements IService {

    public static final String SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME="FlyLight Screen Pattern Annotation";

    final boolean DEBUG=true;

    protected Logger logger;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;

    protected Entity screenPatternTopLevelFolder;
    protected Entity screenSampleTopLevelFolder;
    protected Map<String, Entity> flyLineFolderByPlateMap;
    protected Map<String, Entity> flyLineFolderByPWMap;
    protected List <Entity> flyLineList;
    protected List<Entity> screenSampleList;

    private ComputeBeanLocal computeBean;
    private EntityBeanLocal entityBean;
    private FileDiscoveryHelper helper;


    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.processData = processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            computeBean = EJBFactory.getLocalComputeBean();
            entityBean = EJBFactory.getLocalEntityBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            createDate = new Date();
            helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
            
            screenPatternTopLevelFolder = getTopLevelFolder(ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME, true /* create */);
            screenSampleTopLevelFolder = getTopLevelFolder(FlyScreenDiscoveryService.SCREEN_SAMPLE_TOP_LEVEL_FOLDER_NAME, false /* create */);

            flyLineList = getFlyLineList();
            logger.info("flyLineList size="+flyLineList.size());

            screenSampleList = getScreenSampleList();
            logger.info("screenSampleList size="+screenSampleList.size());

            flyLineFolderByPlateMap = new HashMap<String, Entity>();
            flyLineFolderByPWMap = new HashMap<String, Entity>();

            populateFlyLineFolderMaps();
            logger.info("Found "+flyLineFolderByPlateMap.size()+" plate-level folders and "+flyLineFolderByPWMap.size()+" line-level folders");

            coordinateLinesAndSamples();
            logger.info("After coordination step, flyLineList size="+flyLineList.size()+" plateFolder size="+flyLineFolderByPlateMap.size() +
                    " line-level folder size="+flyLineFolderByPWMap.size());

            Long groupSize = new Long(processData.getString("FLY_LINE_GROUP_SIZE").trim());
            logger.info("Using FLY_LINE_GROUP_SIZE="+groupSize);

            List<List<String>> groupList = createFlyLineGroupList(flyLineList, groupSize);
            logger.info("Created FLY_LINE_GROUP_LIST with "+groupList.size()+" entries");

            processData.putItem("FLY_LINE_GROUP_LIST", groupList);

        } catch (Exception ex) {
            String msg = "Exception in execute() : " + ex.getMessage();
            logger.error(msg);
            throw new ServiceException(ex);
        }
    }

    List<List<String>> createFlyLineGroupList(List<Entity> flyLineList, Long groupSize) {
        List<List<String>> groupList=new ArrayList<List<String>>();
        List<String> currentGroup=null;
        for (int i=0;i<flyLineList.size();i++) {
            if (currentGroup==null) {
                currentGroup=new ArrayList<String>();
            } else if (currentGroup.size()==groupSize) {
                groupList.add(currentGroup);
                currentGroup=new ArrayList<String>();
            }
            currentGroup.add(flyLineList.get(i).getId().toString());
        }
        if (currentGroup.size()>0) {
            groupList.add(currentGroup);
        }
        return groupList;
    }

    protected Entity getTopLevelFolder(String topLevelFolderName, boolean createIfNecessary) throws Exception {
        return helper.createOrVerifyRootEntity(topLevelFolderName, createIfNecessary, false /* load tree */);
    }

    protected List<Entity> getFlyLineList() throws Exception {
        List<Entity> flyLineList = getRecursiveEntitiesFromFolderByType(screenPatternTopLevelFolder, EntityConstants.TYPE_FLY_LINE);
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

    protected List<Entity> getScreenSampleList() throws Exception {
         List<Entity> screenSampleList = getRecursiveEntitiesFromFolderByType(screenSampleTopLevelFolder, EntityConstants.TYPE_SCREEN_SAMPLE);
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
        Set<Entity> childList = entityBean.getChildEntities(folder.getId());
        for (Entity child : childList) {
            if (child != null && child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                entityList.addAll(getRecursiveEntitiesFromFolderByType(child, typeName));
            } else if (child != null && child.getEntityTypeName().equals(typeName)) {
                entityList.add(child);
            }
        }
        return entityList;
    }

    /*

        In this method, we want to make sure every Screen Sample has a FlyLine parent entity, and likewise that
        every needed FlyLine entity exists or is created.

     */
    protected void coordinateLinesAndSamples() throws Exception {
        for (Entity sample : screenSampleList) {
            String[] nameArr=getSubNamesFromGMRName(sample.getName());
            String platePrefix=nameArr[0];
            String fullPrefix=nameArr[1];
            String lineName=nameArr[2];
            Entity plateFolder=flyLineFolderByPlateMap.get(platePrefix);
            if (plateFolder==null) {
                // Need to create
                plateFolder=addSubFolder(screenPatternTopLevelFolder, platePrefix);
                flyLineFolderByPlateMap.put(platePrefix, plateFolder);
            }
            Entity pwFolder=flyLineFolderByPWMap.get(fullPrefix);
            if (pwFolder==null) {
                // Need to create
                pwFolder=addSubFolder(plateFolder, fullPrefix);
                flyLineFolderByPWMap.put(fullPrefix, pwFolder);
            }
            // Now, check if the pw folder contains an appropriate FlyLine entity with which to
            // associate the screen sample
            Set<Entity> pwChildren=entityBean.getChildEntities(pwFolder.getId());
            Entity flyLineEntity=null;
            if (pwChildren!=null) {
                for (Entity pwChild : pwChildren) {
                    if (pwChild!=null) {
                        if (pwChild.getEntityTypeName().equals(EntityConstants.TYPE_FLY_LINE)) {
                            if (pwChild.getName().equals(lineName)) {
                                flyLineEntity=pwChild;
                                break;
                            }
                        }
                    }
                }
            }
            if (flyLineEntity==null) {
                // We could not find an appropriate FlyLine, so we must add one
                flyLineEntity=createFlyLineAndAddToFolder(lineName, pwFolder);
                flyLineList.add(flyLineEntity);
            }
            // Now we can check if the flyLineEntity contains the current screen sample, and if not, add it
            boolean foundSample=false;
            Set<Entity> flyLineChildren=entityBean.getChildEntities(flyLineEntity.getId());
            if (flyLineChildren!=null) {
                for (Entity flyChild : flyLineChildren) {
                    if (flyChild!=null) {
                        if (flyChild.getEntityTypeName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                            if (flyChild.getName().equals(sample.getName())) {
                                foundSample=true;
                            }
                        }
                    }
                }
            }
            if (!foundSample) {
                // We must add the sample
                addScreenSampleToFlyLine(sample, flyLineEntity);
            }
        }
    }

    protected String[] getSubNamesFromGMRName(String gmrName) throws Exception {
        // Example GMR name: GMR_57B09_AE_01_02-mA01b_C070202_20070507160307953
        try {
            String[] u1Arr = gmrName.split("_");
            if (u1Arr.length < 7) {
                throw new Exception("u1 length=" + u1Arr.length);
            }
            String pw = u1Arr[1];
            Pattern p = Pattern.compile("(\\d+)(\\D+)(\\d+)");
            Matcher m = p.matcher(pw);
            if (m.lookingAt()) {
                if (m.groupCount() < 3) {
                    throw new Exception("pw group count=" + m.groupCount());
                }
                String plate = m.group(1);
                String well = m.group(2) + "" + m.group(3);
                String[] result = new String[3];
                result[0] = u1Arr[0] + "_" + plate;       // GMR_57
                result[1] = u1Arr[0] + "_" + u1Arr[1];    // GMR_57B09
                result[2] = u1Arr[0] + "_" + u1Arr[1] + "_" + u1Arr[2] + "_" + u1Arr[3];
                return result;
            } else {
                throw new Exception("lookingAt returned false - could not match " + pw);
            }
        } catch (Exception ex) {
            throw new Exception(ex.getMessage() + " : could not parse GMR name=" + gmrName);
        }
    }

    protected void populateFlyLineFolderMaps() throws ComputeException {
        logger.info("Populating Folder Maps");
        Set<Entity> topChildren = entityBean.getChildEntities(screenPatternTopLevelFolder.getId());
        for (Entity child : topChildren) {
            if (child!=null && child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                logger.info("Found plate-level folder name="+child.getName());
                flyLineFolderByPlateMap.put(child.getName(), child);
                Set<Entity> plateChildren = entityBean.getChildEntities(child.getId());
                for (Entity plateChild : plateChildren) {
                    if (plateChild!=null && plateChild.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                        logger.info("Found line-level folder name="+plateChild.getName());
                        flyLineFolderByPWMap.put(plateChild.getName(), plateChild);
                    } else {
                        if (plateChild!=null) {
                            logger.info("Unknown child is of type="+plateChild.getEntityTypeName());
                        }
                    }
                }
            }
        }
    }

    protected Entity addSubFolder(Entity parentFolder, String childFolderName) throws Exception {
        if (!parentFolder.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER))
            throw new Exception("A folder entity is required rather than type=" + parentFolder.getEntityTypeName());
        Entity folder = new Entity();
        folder.setCreationDate(createDate);
        folder.setUpdatedDate(createDate);
        folder.setOwnerKey(ownerKey);
        folder.setName(childFolderName);
        folder.setEntityTypeName(EntityConstants.TYPE_FOLDER);
        folder = entityBean.saveOrUpdateEntity(folder);
        entityBean.addEntityToParent(parentFolder, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

    protected Entity createFlyLineAndAddToFolder(String lineName, Entity folder) throws Exception {
        if (!folder.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER))
            throw new Exception("A folder entity is required rather than type=" + folder.getEntityTypeName());
        Entity flyLine = new Entity();
        flyLine.setCreationDate(createDate);
        flyLine.setUpdatedDate(createDate);
        flyLine.setOwnerKey(ownerKey);
        flyLine.setName(lineName);
        flyLine.setEntityTypeName(EntityConstants.TYPE_FLY_LINE);
        flyLine = entityBean.saveOrUpdateEntity(flyLine);
        entityBean.addEntityToParent(folder, flyLine, null, EntityConstants.ATTRIBUTE_ENTITY);
        return flyLine;
    }

    protected void addScreenSampleToFlyLine(Entity sample, Entity flyLine) throws Exception {
        entityBean.addEntityToParent(flyLine, sample, null, EntityConstants.ATTRIBUTE_ENTITY);
    }

}
