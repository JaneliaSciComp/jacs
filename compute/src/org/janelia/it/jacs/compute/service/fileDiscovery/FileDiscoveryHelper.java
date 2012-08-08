package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Helper methods for creating virtual representations of the file system in the Entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileDiscoveryHelper extends EntityHelper {

    protected Logger logger = Logger.getLogger(FileDiscoveryHelper.class);

    public final Long FILE_3D_SIZE_THRESHOLD = new Long(5000000L);

    private Set<String> exclusions = new HashSet<String>();
    
	public FileDiscoveryHelper(String username) {
		super(username);
	}
	
	public FileDiscoveryHelper(User user) {
		super(user);
	}
	
    public FileDiscoveryHelper(EntityBeanRemote entityBean, ComputeBeanRemote computeBean, String username) {
    	super(entityBean, computeBean, username);
    }
	
    public FileDiscoveryHelper(EntityBeanRemote entityBean, ComputeBeanRemote computeBean, User user) {
        super(entityBean, computeBean, user);
    }
    
    public void addFileExclusion(String filePattern) {
    	exclusions.add(filePattern);
    }
    
    private boolean isExcluded(String filename) {
    	int dot = filename.lastIndexOf('.');
    	if (dot>0) {
    		String extension = filename.substring(dot+1);	
    		if (exclusions.contains(extension)) {
    			return true;
    		}
    	}
    	return exclusions.contains(filename);
    }
    
    public List<File> collectFiles(File dir) throws Exception {
    	
    	List<File> allFiles = new ArrayList<File>();
        List<File> files = FileUtils.getOrderedFilesInDir(dir);
        logger.info("Found "+files.size()+" files in "+dir.getAbsolutePath());
        
        for (File resultFile : files) {
        	if (resultFile.isDirectory()) continue;
        	String filename = resultFile.getName();
        	if (FileUtils.isSymlink(resultFile)) continue;
			if (isExcluded(filename)) {
				logger.info("Excluding "+filename);
				continue; 
			}
        	allFiles.add(resultFile);
        }
        
        return allFiles;
    }

    public void addFilesToFolder(Entity filesFolder, List<File> files) throws Exception {
        for (File resultFile : files) {
        	EntityType type = getEntityTypeForFile(resultFile);
        	if (type==null) {
        		logger.warn("Could not determine type of file: "+resultFile.getAbsolutePath());
        		return;
        	}
        	addResultItem(filesFolder, type, resultFile);
        }
    }
    
    public void addFilesInDirToFolder(Entity folder, File dir) throws Exception {
        List<File> files = collectFiles(dir);
        FileUtils.sortFilesByName(files);        
		addFilesToFolder(folder, files);
    }
    
    public Entity addResultItem(Entity resultEntity, EntityType type, File file) throws Exception {
    	
        Entity entity = new Entity();
        entity.setUser(user);
        Date createDate = new Date();
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityType(type);
        entity.setName(file.getName());
        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        
        if (type.getName().equals(EntityConstants.TYPE_IMAGE_2D)) {
        	String filename = file.getName();
        	String fileFormat = filename.substring(filename.lastIndexOf('.')+1);
        	entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, fileFormat);
        }
        
        entity = entityBean.saveOrUpdateEntity(entity);
        logger.info("Saved "+type.getName()+" as "+entity.getId());
        addToParent(resultEntity, entity, resultEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        return entity;
    }
    
    public void addToParent(Entity parent, Entity entity, Integer index, String attrName) throws Exception {
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        entityBean.saveOrUpdateEntityData(ed);
        logger.info("Added "+entity.getEntityType().getName()+"#"+entity.getId()+
        		" as child of "+parent.getEntityType().getName()+"#"+parent.getId());
    }

    public Entity addChildFolderToEntity(Entity parent, String name, String directoryPath) throws Exception {
    	Entity folder = createFolderForFile(name, false, directoryPath); 
        addToParent(parent, folder, null, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

	
	/**
	 * Tries to get the supporting data for an entity. If none exists, adds it and then returns it. 
	 * @param entity
	 * @return
	 * @throws ComputeException
	 */
	public Entity getOrCreateSupportingFilesFolder(Entity entity) throws ComputeException {
        Entity supportingFiles = EntityUtils.getSupportingData(entity);
    	if (supportingFiles == null) {
        	// Update in-memory model
    		supportingFiles = createSupportingFilesFolder();
			EntityData ed = entity.addChildEntity(supportingFiles, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
			ed.setOrderIndex(0);
        	// Update database
            entityBean.saveOrUpdateEntityData(ed);
    	}
    	return supportingFiles;
	}
	
	/**
	 * Create and save a new supporting files folder with the given owner. 
	 * @param username
	 * @return
	 * @throws ComputeException
	 */
	public Entity createSupportingFilesFolder() throws ComputeException {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        Date createDate = new Date();
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
    	// Update database
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
    	// Return the new object so that we can update in-memory model
        return filesFolder;
    }

    protected Entity createFileEntity(String path, String name, String resultEntityType) throws Exception {
    	return createFileEntity(path, name, entityBean.getEntityTypeByName(resultEntityType));
    }
    
    protected Entity createFileEntity(String path, String name, EntityType resultEntityType) throws Exception {
        Entity resultEntity = new Entity();
        resultEntity.setUser(user);
        resultEntity.setEntityType(resultEntityType);
        Date createDate = new Date();
        resultEntity.setCreationDate(createDate);
        resultEntity.setUpdatedDate(createDate);
        resultEntity.setName(name);
        if (path!=null) {
        	resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, path);	
        }
        resultEntity = entityBean.saveOrUpdateEntity(resultEntity);
        logger.info("Saved file entity as "+resultEntity.getId());
        return resultEntity;
    }

    public Entity createFolderForFile(String name, boolean isCommonRoot, String dir) throws Exception {
        logger.info("Creating new topLevelFolder with name=" + name);
        Entity folder = new Entity();
        Date createDate = new Date();
        folder.setCreationDate(createDate);
        folder.setUpdatedDate(createDate);
        folder.setUser(user);
        folder.setName(name);
        folder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_FOLDER));
        if (isCommonRoot) {
        	folder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
        }
        if (dir!=null) {
        	folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir);
        }
        folder = entityBean.saveOrUpdateEntity(folder);
        logger.info("Saved folder " + name+" as " + folder.getId());
        return folder;
    }

    public Entity createOrVerifyRootEntity(String topLevelFolderName, boolean createIfNecessary, boolean loadTree) throws Exception {
    	
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
            	topLevelFolder = createFolderForFile(topLevelFolderName, true, null);
                logger.info("Saved top level folder as " + topLevelFolder.getId());
            } else {
                throw new Exception("Could not find top-level folder by name=" + topLevelFolderName);
            }
        }

        logger.info("Using topLevelFolder with id=" + topLevelFolder.getId());
        return topLevelFolder;
    }
    
    public Entity createOrVerifyFolderEntity(Entity parentFolder, File dir) throws Exception {
    	return createOrVerifyFolderEntity(parentFolder, dir.getName(), dir);
    }
    
    public Entity createOrVerifyFolderEntity(Entity parentFolder, String name, File dir) throws Exception {
        return createOrVerifyFolderEntity(parentFolder, name, dir, parentFolder.getMaxOrderIndex()+1);
    }

    public Entity createOrVerifyFolderEntity(Entity parentFolder, String name, File dir, Integer orderIndex) throws Exception {

        logger.info("Looking for fast load entity in parent folder "+parentFolder.getId());
        EntityData folderEd = EntityUtils.findChildEntityDataWithName(parentFolder, name);
        if (folderEd!=null) {
        	return folderEd.getChildEntity();
        }
        
        Entity folder = createFolderForFile(name, false, dir.getAbsolutePath());
        addToParent(parentFolder, folder, orderIndex, EntityConstants.ATTRIBUTE_ENTITY);
        return folder;
    }

    public EntityType getEntityTypeForFile(File file) throws Exception {
        String filenameLowerCase=file.getName().toLowerCase();
        if (filenameLowerCase.endsWith(".lsm")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK);
        } 
        else if (filenameLowerCase.endsWith(".tif")||filenameLowerCase.endsWith(".tiff")) {
            if (file.length()>=FILE_3D_SIZE_THRESHOLD) {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
            } 
            else {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D);
            }
        } 
        else if (filenameLowerCase.endsWith(".swc")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_SWC_FILE);
        } 
        else if (filenameLowerCase.endsWith(".ano")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_V3D_ANO_FILE);
        } 
        else if (filenameLowerCase.endsWith(".png")||filenameLowerCase.endsWith(".jpg")
                ||filenameLowerCase.endsWith(".gif")||filenameLowerCase.endsWith(".jpeg")
                ||filenameLowerCase.endsWith(".bmp")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D);
        } 
        else if (filenameLowerCase.endsWith(".raw")) {
            if (filenameLowerCase.contains(".local.")) {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
            } 
            else {
                return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
            }
        } 
        else if (filenameLowerCase.endsWith(".v3draw")
        		||filenameLowerCase.endsWith(".vaa3draw")
        		||filenameLowerCase.endsWith(".pbd")
        		||filenameLowerCase.endsWith(".v3dpbd")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
        } 
        else if (filenameLowerCase.endsWith(".nsp")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_MYERS_NEURON_SEPARATION_FILE);
        }
        else if (filenameLowerCase.endsWith(".mp4")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_MOVIE);
        }
        else if (filenameLowerCase.endsWith(".metadata")
        		||filenameLowerCase.endsWith(".csv")
        		||filenameLowerCase.endsWith(".txt")) {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_TEXT_FILE);
        } 
        else {
            return entityBean.getEntityTypeByName(EntityConstants.TYPE_FILE);
        }
    }
	
}
