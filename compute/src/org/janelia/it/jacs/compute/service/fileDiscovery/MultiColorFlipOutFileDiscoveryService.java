package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MultiColorFlipOutFileDiscoveryTask;
import org.janelia.it.jacs.model.user_data.User;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiColorFlipOutFileDiscoveryService implements IService {

    private MultiColorFlipOutFileDiscoveryTask task;
    private String sessionName;
    private static String TOP_LEVEL_FOLDER_NAME_PARAM = "TOP_LEVEL_FOLDER_NAME";
    private static String DIRECTORY_PARAM_PREFIX = "DIRECTORY_";
    private org.apache.log4j.Logger logger;
    List<String> directoryPathList = new ArrayList<String>();
    AnnotationBeanRemote annotationBean;
    ComputeBeanRemote computeBean;
    String topLevelFolderName;
    Entity topLevelFolder;
    User user;

    public void execute(IProcessData processData) {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (MultiColorFlipOutFileDiscoveryTask) ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            annotationBean = EJBFactory.getRemoteAnnotationBean();
            computeBean = EJBFactory.getRemoteComputeBean();
            user = computeBean.getUserByName(task.getOwner());
            Set<Map.Entry<String, Object>> entrySet = processData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String paramName = entry.getKey();
                if (paramName.startsWith(DIRECTORY_PARAM_PREFIX)) {
                    directoryPathList.add((String) entry.getValue());
                } else if (paramName.equals(TOP_LEVEL_FOLDER_NAME_PARAM)) {
                    topLevelFolderName=(String)entry.getValue();
                }
            }
            String taskInputDirectoryList = task.getParameter(MultiColorFlipOutFileDiscoveryTask.PARAM_inputDirectoryList);
            if (taskInputDirectoryList != null) {
                String[] directoryArray = taskInputDirectoryList.split(",");
                for (String d : directoryArray) {
                    String trimmedPath=d.trim();
                    if (trimmedPath.length()>0) {
                        directoryPathList.add(trimmedPath);
                    }
                }
            }
            for (String directoryPath : directoryPathList) {
                logger.info(" MultiColorFlipOutFileDiscoveryService including directory = "+directoryPath);
            }
            createOrVerifyRootEntity();
            processDirectories();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    protected void createOrVerifyRootEntity() throws Exception {
        // We expect there to be a single folder in the system with this name
        Set<Entity> topLevelFolders = annotationBean.getEntitiesByName(topLevelFolderName);
        if (topLevelFolders!=null && topLevelFolders.size()>1) {
            throw new Exception("Unexpectedly found more than one existing folder for MultiColorFlipOutFileDiscoveryService with name="+topLevelFolderName);
        }
        if (topLevelFolders!=null && topLevelFolders.size()==1) {
            topLevelFolder=topLevelFolders.iterator().next();
        } else if (topLevelFolder==null || topLevelFolders.size()==0) {
            topLevelFolder=new Entity();
            Date createDate = new Date();
            topLevelFolder.setCreationDate(createDate);
            topLevelFolder.setUpdatedDate(createDate);
            topLevelFolder.setUser(user);
            topLevelFolder.setName(topLevelFolderName);
            EntityType folderType=annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
            topLevelFolder.setEntityType(folderType);
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            annotationBean.saveOrUpdateEntity(topLevelFolder);
        }
    }

    protected void processDirectories() throws Exception {
        for (String directoryPath : directoryPathList) {
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
            }
            else if (!dir.isDirectory()) {
                logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
            } else {

                processDirectory(dir);
            }
        }
    }

    protected void processDirectory(File dir) throws Exception {
        File[] dirContents = dir.listFiles();
        for (File file : dirContents) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else {
                logger.info("Found file = " + file.getAbsolutePath());
                if (file.getName().toUpperCase().endsWith(".LSM")) {
                    considerNewLsmEntity(file);
                }
            }
        }
    }

    protected void considerNewLsmEntity(File file) throws Exception {
        logger.info("Considering LSM file = " + file.getAbsolutePath());
        List<Entity> possibleLsmFiles = annotationBean.getEntitiesWithFilePath(file.getAbsolutePath());
        List<Entity> lsmStacks = new ArrayList<Entity>();
        for (Entity entity : possibleLsmFiles) {
            if (entity.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                lsmStacks.add(entity);
            }
        }
        if (lsmStacks.size()>0) {
            logger.info("File is already represented as LSM_STACK_TYPE = " + file.getAbsolutePath());
        } else {
            createLsmStackFromFile(file);
        }
    }

    protected void createLsmStackFromFile(File file) throws Exception {
        EntityType lsmEntityType = annotationBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK);
        if (lsmEntityType==null) {
            throw new Exception("Could not find EntityType = " + EntityConstants.TYPE_LSM_STACK);
        }
        Entity lsmStack = new Entity();
        lsmStack.setUser(user);
        lsmStack.setEntityType(lsmEntityType);
        Date createDate = new Date();
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack = annotationBean.saveOrUpdateEntity(lsmStack);
        Set<EntityData> eds = lsmStack.getEntityData();
        EntityData ed = new EntityData();
        EntityAttribute filePathAttribute = lsmStack.getAttributeByName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (filePathAttribute==null) {
            throw new Exception("Expected to find ATTRIBUTE_FILE_PATH in lsmStack");
        }
        ed.setEntityAttribute(filePathAttribute);
        ed.setValue(file.getAbsolutePath());
        ed.setParentEntity(lsmStack);
        ed.setUser(user);
        ed.setCreationDate(createDate);
        eds.add(ed);
        annotationBean.saveOrUpdateEntity(lsmStack);
    }

}

