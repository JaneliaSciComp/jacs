package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.apache.xerces.impl.dv.dtd.ENTITYDatatypeValidator;
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
            annotationBean.saveOrUpdateEntity(topLevelFolder);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    protected void createOrVerifyRootEntity() throws Exception {
        // We expect there to be a single folder in the system with this name
        Set<Entity> topLevelFolders = annotationBean.getEntitiesByName(topLevelFolderName);
        if (topLevelFolders!=null && topLevelFolders.size()>1) {
            for (Entity e : topLevelFolders) {
                logger.info("Found topLevelFolder entityId="+e.getId());
            }
            throw new Exception("Unexpectedly found " + topLevelFolders.size()+" folders for MultiColorFlipOutFileDiscoveryService with name="+topLevelFolderName);
        }
        if (topLevelFolders!=null && topLevelFolders.size()==1) {
            topLevelFolder = topLevelFolders.iterator().next();
            logger.info("Found existing topLevelFolder, name=" + topLevelFolder.getName());
        } else if (topLevelFolders==null || topLevelFolders.size()==0) {
            logger.info("Creating new topLevelFolder with name="+topLevelFolderName);
            topLevelFolder=new Entity();
            Date createDate = new Date();
            topLevelFolder.setCreationDate(createDate);
            topLevelFolder.setUpdatedDate(createDate);
            topLevelFolder.setUser(user);
            topLevelFolder.setName(topLevelFolderName);
            EntityType folderType=annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
            topLevelFolder.setEntityType(folderType);
            topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            logger.info("Calling saveOrUpdateEntity for topLevelFolder Entity");
            topLevelFolder = annotationBean.saveOrUpdateEntity(topLevelFolder);
            logger.info("Done calling saveOrUpdateEntity for topLevelFolder Entity");
        }
        logger.info("Using topLevelFolder id="+topLevelFolder.getId());
    }

    protected void processDirectories() throws Exception {
        for (String directoryPath : directoryPathList) {
            logger.info("Processing dir="+directoryPath);
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
            }
            else if (!dir.isDirectory()) {
                logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
            } else {
                Entity folder = verifyOrCreateChildFolderFromDir(topLevelFolder, dir);
                processFolder(folder);
                annotationBean.saveOrUpdateEntity(folder);
            }
        }
    }

    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir) throws Exception {
        logger.info("verifyOrCreateChildFolderFromDir() called with parentFolder id="+parentFolder.getId());
        logger.info("Looking for Entity matching Folder path="+dir.getAbsolutePath());
        Entity folder=null;
        Set<EntityData> data=parentFolder.getEntityData();
        for (EntityData ed : data) {
            long childId=0;
            String entityTypeName="<not specified>";
            if (ed.getChildEntity()!=null) {
                childId=ed.getChildEntity().getId();
                entityTypeName=ed.getChildEntity().getEntityType().getName();
            }
            logger.info("Checking EntityData entry with value="+ed.getValue()+" childEntityId="+childId+" entityType="+entityTypeName);
            if (ed.getChildEntity()!=null &&
                    ed.getChildEntity().getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
                String folderPath = ed.getChildEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (folderPath==null) {
                    throw new Exception("Unexpectedly could not find ATTRIBUTE_FILE_PATH for entity id="+ed.getChildEntity().getId());
                }
                if (folderPath.equals(dir.getAbsolutePath())) {
                    if (folder!=null) {
                        throw new Exception("Unexpectedly found multiple child folders with path=" + dir.getAbsolutePath()+" for parent folder id="+parentFolder.getId());
                    }
                    folder = ed.getChildEntity();
                }
            } else {
                logger.info("EntityData does not qualify as Folder");
            }
        }
        if (folder!=null) {
            logger.info("Found folder="+folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            return folder;
        } else {
            logger.info("Could not find matching folder, so creating new one");
            // We need to create a new folder
            folder = new Entity();
            Date createDate = new Date();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setUser(user);
            folder.setName(dir.getAbsolutePath());
            EntityType folderType=annotationBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER);
            folder.setEntityType(folderType);
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath());
            folder=annotationBean.saveOrUpdateEntity(folder);
            logger.info("After saving new folder, id="+folder.getId());
            parentFolder.addChildEntity(folder);
            logger.info("Before updating parentFolder id="+parentFolder.getId());
            //annotationBean.saveOrUpdateEntity(parentFolder);
            logger.info("After updating parentFolder id="+parentFolder.getId());
            logger.info("Created new folder with path="+folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        }
        return folder;
    }

    protected void processFolder(Entity folder) throws Exception {
        File dir=new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());
        File[] dirContents = dir.listFiles();
        for (File file : dirContents) {
            if (file.isDirectory()) {
                Entity subfolder=verifyOrCreateChildFolderFromDir(folder, file);
                processFolder(subfolder);
                annotationBean.saveOrUpdateEntity(subfolder);
            } else {
                logger.info("Found file = " + file.getAbsolutePath());
                if (file.getName().toUpperCase().endsWith(".LSM")) {
                    Entity lsmStack = verifyOrCreateLsmStack(file);
                    folder.addChildEntity(lsmStack);
                }
            }
        }
    }

    protected Entity verifyOrCreateLsmStack(File file) throws Exception {
        logger.info("Considering LSM file = " + file.getAbsolutePath());
        List<Entity> possibleLsmFiles = annotationBean.getEntitiesWithFilePath(file.getAbsolutePath());
        List<Entity> lsmStacks = new ArrayList<Entity>();
        for (Entity entity : possibleLsmFiles) {
            if (entity.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                lsmStacks.add(entity);
            }
        }
        if (lsmStacks.size()==0) {
            return createLsmStackFromFile(file);
        } else if (lsmStacks.size()==1) {
            logger.info("Found lsm stack = " + lsmStacks.get(0).getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            return lsmStacks.get(0);
        } else {
            throw new Exception("Unexpectedly found " + lsmStacks.size() + " lsm stacks for file="+file.getAbsolutePath());
        }
    }

    protected Entity createLsmStackFromFile(File file) throws Exception {
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
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = annotationBean.saveOrUpdateEntity(lsmStack);
        logger.info("Created lsm stack = " +lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        return lsmStack;
    }

}

