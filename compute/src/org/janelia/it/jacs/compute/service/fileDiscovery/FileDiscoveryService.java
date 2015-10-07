package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * Discover files in a set of input directories and create corresponding entities in the database. This class
 * may be extended to add additional entities based on files found. Parameters:
 *   ROOT_ENTITY_NAME or ROOT_ENTITY_ID (name of the root entity to populate)
 *   INPUT_DIR_LIST or ROOT_FILE_NODE_ID (input directory to scan)
 *   OUTVAR_ENTITY_ID (name of the ProcessData variable where we should put the entity id of the root entity)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileDiscoveryService implements IService {

    protected Logger logger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected Date createDate;
    protected IProcessData processData;
    protected FileDiscoveryHelper helper;
    protected EntityBeanEntityLoader entityLoader;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	this.processData = processData;
        	this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.entityBean = EJBFactory.getLocalEntityBean();
            this.computeBean = EJBFactory.getLocalComputeBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.createDate = new Date();
            this.entityLoader = new EntityBeanEntityLoader(entityBean);
            helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
            helper.addFileExclusion("*.log");
            helper.addFileExclusion("*.oos");
            helper.addFileExclusion("sge_*");
            helper.addFileExclusion("temp");
            helper.addFileExclusion("tmp.*");
            helper.addFileExclusion("core.*");
            helper.addFileExclusion("screenshot_*");
            helper.addFileExclusion("sampleEntityId.txt");
            
            // What database entity do we load into?
            
            String topLevelFolderName;
            Entity topLevelFolder;
            if (processData.getItem("ROOT_ENTITY_NAME") != null) {
            	topLevelFolderName = (String)processData.getItem("ROOT_ENTITY_NAME");
            	topLevelFolder = createOrVerifyRootEntity(topLevelFolderName);
            }
            else {
            	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
            	if (rootEntityId==null) {
            		throw new IllegalArgumentException("Both ROOT_ENTITY_NAME and ROOT_ENTITY_ID may not be null");
            	}
            	topLevelFolder = entityBean.getEntityById(rootEntityId);
            }
            
        	if (topLevelFolder==null) {
        		throw new IllegalArgumentException("Both ROOT_ENTITY_NAME and ROOT_ENTITY_ID may not be null");
        	}
        	
            logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());
            
            // What directory should we discover files within?

            String taskInputDirectoryList = null;
            Object inputDirList = processData.getItem("INPUT_DIR_LIST");
            if (inputDirList != null && !"".equals(inputDirList)) {
            	taskInputDirectoryList = (String)inputDirList;
            }
            else {
            	FileNode resultFileNode = (FileNode)processData.getItem("ROOT_FILE_NODE");
            	if (resultFileNode==null) {
            		logger.info("Both ROOT_FILE_NODE and INPUT_DIR_LIST are null, no file discovery will be done.");
            	}
            	else {
            		taskInputDirectoryList = resultFileNode.getDirectoryPath();	
            	}
            }
            
            if (taskInputDirectoryList != null) {
            	List<String> directoryPathList = new ArrayList<String>();
                String[] directoryArray = taskInputDirectoryList.split(",");
                for (String d : directoryArray) {
                    String trimmedPath=d.trim();
                    if (trimmedPath.length()>0) {
                        directoryPathList.add(trimmedPath);
                    }
                }
                processPathList(directoryPathList, topLevelFolder);
            }
            
            
        	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
        	if (outvar != null) {
        		logger.info("Putting "+topLevelFolder.getId()+" in "+outvar);
        		processData.putItem(outvar, topLevelFolder.getId());
        	}
            
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    protected void processPathList(List<String> directoryPathList, Entity topLevelFolder) throws Exception {

        if (directoryPathList.isEmpty()) throw new Exception("No input directories provided");
        
        for (String directoryPath : directoryPathList) {
            logger.info("Discover files in "+directoryPath);
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
            }
            else if (!dir.isDirectory()) {
                logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
            } 
            else {
                Entity folder = verifyOrCreateChildFolderFromDir(topLevelFolder, dir, null /*index*/);
                processFolderForData(folder);
            }
        } 
    }

    protected Entity createOrVerifyRootEntityButDontLoadTree(String topLevelFolderName) throws Exception {
        return helper.createOrVerifyRootEntity(topLevelFolderName, true /* create if necessary */, false /* load tree */);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
        return helper.createOrVerifyRootEntity(topLevelFolderName,true /* create if necessary */, true);
    }

    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir, Integer index) throws Exception {

        logger.info("Looking for folder entity with path "+dir.getAbsolutePath()+" in parent folder "+parentFolder.getId());
        Entity folder = null;
        
        for (EntityData ed : parentFolder.getEntityData()) {
            Entity child = ed.getChildEntity();
        	
            if (child != null && child.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)) {
                String folderPath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (folderPath == null) {
                    logger.warn("Unexpectedly could not find attribute '"+EntityConstants.ATTRIBUTE_FILE_PATH+"' for entity id="+child.getId());
                }
                else if (folderPath.equals(dir.getAbsolutePath())) {
                    if (folder != null) {
                    	logger.warn("Unexpectedly found multiple child folders with path=" + dir.getAbsolutePath()+" for parent folder id="+parentFolder.getId());
                    }
                    else {
                    	folder = ed.getChildEntity();	
                    }
                }
            }
        }
        
        if (folder == null) {
            // We need to create a new folder
            folder = new Entity();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setOwnerKey(ownerKey);
            folder.setName(dir.getName());
            folder.setEntityTypeName(EntityConstants.TYPE_FOLDER);
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath());
            folder = entityBean.saveOrUpdateEntity(folder);
            logger.info("Saved folder as "+folder.getId());
            helper.addToParent(parentFolder, folder, index, EntityConstants.ATTRIBUTE_ENTITY);
        }
        else {
            logger.info("Found folder with id="+folder.getId());
        }
        
        return folder;
    }
    
    /**
     * Override this method to create additional entities within the given folder. If you want the child folders
     * to be processed recursively, make sure to call processChildFolders.
     * @param folder
     * @throws Exception
     */
    protected void processFolderForData(Entity folder) throws Exception {
    	processChildFolders(folder);
    }
    
	protected void processChildFolders(Entity folder) throws Exception {
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
	    for (File file : FileUtils.getOrderedFilesInDir(dir)) {
	        if (file.isDirectory()) {
                Entity subfolder = verifyOrCreateChildFolderFromDir(folder, file, null /*index*/);
                processFolderForData(subfolder);
	        } 
	    }
	}

}
