package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.User;

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
    protected User user;
    protected Date createDate;
    protected IProcessData processData;

    public final Long FILE_3D_SIZE_THRESHOLD = new Long(5000000L);

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	this.processData=processData;
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();

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
        return createOrVerifyRootEntity(topLevelFolderName, user, createDate, logger, true /* create if necessary */, false /* load tree */);
    }

    protected Entity createOrVerifyRootEntity(String topLevelFolderName) throws Exception {
        return createOrVerifyRootEntity(topLevelFolderName, user, createDate, logger, true /* create if necessary */, true);
    }

    public static Entity createOrVerifyRootEntity(String topLevelFolderName, User user, Date createDate, org.apache.log4j.Logger logger, boolean createIfNecessary, boolean loadTree) throws Exception {
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
        Entity topLevelFolder = null;
        if (topLevelFolders != null) {
            // Only accept the current user's top level folder
            for (Entity entity : topLevelFolders) {
                if (entity.getUser().getUserLogin().equals(user.getUserLogin())
                        && entity.getEntityType().getName().equals(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER).getName())
                        && entity.getAttributeByName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
                    // This is the folder we want, now load the entire folder hierarchy
                    if (loadTree) {
                        topLevelFolder = entityBean.getEntityTree(entity.getId());
                    } else {
                        topLevelFolder = entity;
                    }
                    logger.info("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
                    break;
                }
            }
        }

        if (topLevelFolder == null) {
            if (createIfNecessary) {
                logger.info("Creating new topLevelFolder with name=" + topLevelFolderName);
                topLevelFolder = new Entity();
                topLevelFolder.setCreationDate(createDate);
                topLevelFolder.setUpdatedDate(createDate);
                topLevelFolder.setUser(user);
                topLevelFolder.setName(topLevelFolderName);
                topLevelFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
                topLevelFolder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
                topLevelFolder = entityBean.saveOrUpdateEntity(topLevelFolder);
                logger.info("Saved top level folder as " + topLevelFolder.getId());
            } else {
                throw new Exception("Could not find top-level folder by name=" + topLevelFolderName);
            }
        }

        logger.info("Using topLevelFolder with id=" + topLevelFolder.getId());
        return topLevelFolder;
    }

    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir, Integer index) throws Exception {

        logger.info("Looking for folder entity with path "+dir.getAbsolutePath()+" in parent folder "+parentFolder.getId());
        Entity folder = null;
        
        for (EntityData ed : parentFolder.getEntityData()) {
            Entity child = ed.getChildEntity();
        	
            if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
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
            folder.setUser(user);
            folder.setName(dir.getName());
            folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath());
            folder = entityBean.saveOrUpdateEntity(folder);
            logger.info("Saved folder as "+folder.getId());
            addToParent(parentFolder, folder, index, EntityConstants.ATTRIBUTE_ENTITY);
        }
        else {
            logger.info("Found folder with id="+folder.getId());
        }
        
        return folder;
    }

    protected void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
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
	    for (File file : getOrderedFilesInDir(dir)) {
	        if (file.isDirectory()) {
                Entity subfolder = verifyOrCreateChildFolderFromDir(folder, file, null /*index*/);
                processFolderForData(subfolder);
	        } 
	    }
	}
    
    /**
     * Returns the child files of the given directory, sorted by name.
     * @param dir
     * @return
     */
    protected List<File> getOrderedFilesInDir(File dir) {
        List<File> files = Arrays.asList(dir.listFiles());
        Collections.sort(files, new Comparator<File>() {
        	@Override
        	public int compare(File file1, File file2) {
        		return file1.getName().compareTo(file2.getName());
        	}
		});
        return files;
    }

    EntityType getEntityTypeForFile(File file) throws Exception {
        String filenameLowerCase=file.getName().toLowerCase();
        if (filenameLowerCase.endsWith(".lsm")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK);
        } else if (filenameLowerCase.endsWith(".tif")) {
            if (file.length()>=FILE_3D_SIZE_THRESHOLD) {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
            } else {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D);
            }
        } else if (filenameLowerCase.endsWith(".txt")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_TEXT_FILE);
        } else if (filenameLowerCase.endsWith(".swc")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_SWC_FILE);
        } else if (filenameLowerCase.endsWith(".ano")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_V3D_ANO_FILE);
        } else if (filenameLowerCase.endsWith(".png")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D);
        } else if (filenameLowerCase.endsWith(".raw")) {
            if (filenameLowerCase.contains(".local.")) {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
            } else {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
            }
        } else if (filenameLowerCase.endsWith(".v3draw")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
        } else if (filenameLowerCase.endsWith(".vaa3draw")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
        } else if (filenameLowerCase.endsWith(".pbd")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
        } else if (filenameLowerCase.endsWith(".v3dpbd")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
        } else {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA);
        }
    }

    protected Entity verifyOrCreateVirtualSubFolder(Entity parentFolder, String subFolderName) throws Exception {
        for (Entity child : parentFolder.getChildren()) {
            if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) &&
                    child.getName().equals(subFolderName)) {
                return child;
            }
        }
        // Need to create
        Entity subFolder=addChildFolderToEntity(parentFolder, subFolderName, null);
        return subFolder;
    }

    protected Entity addChildFolderToEntity(Entity parent, String name, String directoryPath) throws Exception {
        Entity folder = new Entity();
        folder.setCreationDate(createDate);
        folder.setUpdatedDate(createDate);
        folder.setUser(user);
        folder.setName(name);
        folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
        if (directoryPath!=null) {
            folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, directoryPath);
        }
        folder = entityBean.saveOrUpdateEntity(folder);
        logger.info("Saved folder " + name+" as " + folder.getId()+" , will now add as child to parent entity name="+parent.getName()+" parentId="+parent.getId());
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

}
