package org.janelia.it.jacs.compute.service.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.service.domain.EntityHelperNG;
import org.janelia.it.jacs.compute.util.FileUtils;

/**
 * Helper methods for creating virtual representations of the file system in the Entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileDiscoveryHelperNG extends EntityHelperNG {

    public final Long FILE_3D_SIZE_THRESHOLD = new Long(5000000L);

    private Set<Pattern> exclusions = new HashSet<Pattern>();
    private boolean excludeSymLinks = true;
    
    public FileDiscoveryHelperNG(ComputeBeanLocal computeBean, String ownerKey, Logger logger) {
        super(computeBean, ownerKey, logger);
        addFileExclusion("*.log");
        addFileExclusion("*.oos");
        addFileExclusion("sge_*");
        addFileExclusion("temp");
        addFileExclusion("tmp.*");
        addFileExclusion("core.*");
        addFileExclusion("screenshot_*");
    }
    
    public void addFileExclusion(String filePattern) {
    	Pattern p = Pattern.compile(filePattern.replaceAll("\\*", "(.*?)"));
    	exclusions.add(p);
    }
    
	public void setExcludeSymLinks(boolean excludeSymLinks) {
		this.excludeSymLinks = excludeSymLinks;
	}

	private boolean isExcluded(String filename) {		
		for(Pattern p : exclusions) {
			Matcher m = p.matcher(filename);
			if (m.matches()) {
				//logger.debug("Excluding "+filename+" based on pattern "+p.pattern());
				return true;
			}
		}
		return false;
    }
    
    public List<File> collectFiles(File dir) throws Exception {
    	return collectFiles(dir, false);
    }
    
    public List<File> collectFiles(File dir, boolean recurse) throws Exception {
    	
    	List<File> allFiles = new ArrayList<File>();
        List<File> files = FileUtils.getOrderedFilesInDir(dir);
        logger.info("Found "+files.size()+" files in "+dir.getAbsolutePath());
        
        for (File resultFile : files) {
        	String filename = resultFile.getName();
        	
			if (isExcluded(filename)) {
				continue; 
			}
			
			if (FileUtils.isSymlink(resultFile) && excludeSymLinks) {
				continue; 
			}
        	
        	if (resultFile.isDirectory()) {
        		if (recurse) {
            		allFiles.addAll(collectFiles(resultFile, true));
            	}
        	}
        	else {
        		allFiles.add(resultFile);	
        	}
        }
        
        return allFiles;
    }

    public List<String> getFilepaths(String rootPath) throws Exception {

        List<String> filepaths = new ArrayList<>();
        File dir = new File(rootPath);
        logger.info("Processing "+dir.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
            logger.info("Cannot read from folder "+dir.getAbsolutePath());
            return filepaths;
        }
        
        for(File file : collectFiles(dir, true)) {
            filepaths.add(file.getAbsolutePath());
        }
        return filepaths;
    }

//    public Entity createResultItemForFile(File resultFile) throws Exception {
//        String type = getEntityTypeForFile(resultFile);
//        if (type==null) {
//            logger.warn("Could not determine type of file: "+resultFile.getAbsolutePath());
//            return null;
//        }
//        return createResultItem(type, resultFile);
//    }
//    
//    public Entity createResultItem(String entityTypeName, File file) throws Exception {
//        
//        Entity entity = new Entity();
//        entity.setOwnerKey(ownerKey);
//        Date createDate = new Date();
//        entity.setCreationDate(createDate);
//        entity.setUpdatedDate(createDate);
//        entity.setEntityTypeName(entityTypeName);
//        entity.setName(file.getName());
//        entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
//        
//        if (entityTypeName.equals(EntityConstants.TYPE_IMAGE_2D)) {
//            String filename = file.getName();
//            String fileFormat = filename.substring(filename.lastIndexOf('.')+1);
//            entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_IMAGE_FORMAT, fileFormat);
//        }
//        
//        entity = entityBean.saveOrUpdateEntity(entity);
//        logger.info("Saved '"+entity.getName()+"' as "+entity.getId());
//        return entity;
//    }
//    
//    public void addFilesInDirToFolder(Entity folder, File dir) throws Exception {
//    	addFilesInDirToFolder(folder, dir, false);
//    }
//    
//    public List<File> addFilesInDirToFolder(Entity folder, File dir, boolean recurse) throws Exception {
//        List<File> files = collectFiles(dir, recurse);
//        logger.info("Collected "+files.size()+" files for addition to "+folder.getName());
//        if (!files.isEmpty()) {
//        	FileUtils.sortFilesByName(files);
//    		addFilesToFolder(folder, files);
//        }
//        return files;
//    }
//
//    public void addFilesToFolder(Entity filesFolder, List<File> files) throws Exception {
//        for (File resultFile : files) {
//            String type = getEntityTypeForFile(resultFile);
//            if (type==null) {
//                logger.warn("Could not determine type of file: "+resultFile.getAbsolutePath());
//                return;
//            }
//            addResultItem(filesFolder, type, resultFile);
//        }
//    }
//    
//    public Entity addResultItem(Entity resultEntity, String entityTypeName, File file) throws Exception {
//        Entity entity = createResultItem(entityTypeName, file);
//        addToParent(resultEntity, entity, resultEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
//        return entity;
//    }
//
//    public Entity addChildFolderToEntity(Entity parent, String name, String directoryPath) throws Exception {
//    	Entity folder = createFolderForFile(name, false, directoryPath); 
//        addToParent(parent, folder, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
//        return folder;
//    }
//
//    public Entity addChildFolderToEntity(Entity parent, String name) throws Exception {
//        return addChildFolderToEntity(parent, name, null);
//    }
//    
//	/**
//	 * Tries to get the supporting data for an entity. If none exists, adds it and then returns it. 
//	 * @param entity
//	 * @return
//	 * @throws ComputeException
//	 */
//	public Entity getOrCreateSupportingFilesFolder(Entity entity) throws ComputeException {
//        Entity supportingFiles = EntityUtils.getSupportingData(entity);
//    	if (supportingFiles == null) {
//    		supportingFiles = createSupportingFilesFolder();
//            entityBean.addEntityToParent(entity, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
//    	}
//    	return supportingFiles;
//	}
//	
//	/**
//	 * Create and save a new supporting files folder with the given owner. 
//	 * @return supporting files folder entity
//	 * @throws ComputeException
//	 */
//	public Entity createSupportingFilesFolder() throws ComputeException {
//        Entity filesFolder = new Entity();
//        filesFolder.setOwnerKey(ownerKey);
//        filesFolder.setEntityTypeName(EntityConstants.TYPE_SUPPORTING_DATA);
//        Date createDate = new Date();
//        filesFolder.setCreationDate(createDate);
//        filesFolder.setUpdatedDate(createDate);
//        filesFolder.setName("Supporting Files");
//        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
//        logger.info("Saved supporting files folder as "+filesFolder.getId());
//        return filesFolder;
//    }
//    
//    protected Entity createFileEntity(String path, String name, String resultEntityType) throws Exception {
//        Entity resultEntity = new Entity();
//        resultEntity.setOwnerKey(ownerKey);
//        resultEntity.setEntityTypeName(resultEntityType);
//        Date createDate = new Date();
//        resultEntity.setCreationDate(createDate);
//        resultEntity.setUpdatedDate(createDate);
//        resultEntity.setName(name);
//        if (path!=null) {
//        	resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, path);	
//        }
//        resultEntity = entityBean.saveOrUpdateEntity(resultEntity);
//        logger.info("Saved file entity as "+resultEntity.getId());
//        return resultEntity;
//    }
//
//    public Entity createFolderForFile(String name, boolean isCommonRoot, String dir) throws Exception {
//        logger.info("Creating new folder with name=" + name+" (isCommonRoot="+isCommonRoot+")");
//        Entity folder = null;
//        if (isCommonRoot) {
//            EntityData folderEd = entityBean.createFolderInDefaultWorkspace(ownerKey, name);
//            folder = folderEd.getChildEntity();
//        }
//        else {
//            
//        }
//    	folder.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, dir);
//        folder = entityBean.saveOrUpdateEntity(folder);
//        logger.info("Saved folder " + name+" as " + folder.getId());
//        return folder;
//    }
//    
//    public Entity getRootEntity(String topLevelFolderName, boolean loadTree) throws ComputeException {
//        Set<Entity> topLevelFolders = entityBean.getEntitiesByName(topLevelFolderName);
//        Entity topLevelFolder = null;
//        if (topLevelFolders != null) {
//            // Only accept the current user's top level folder
//            for (Entity entity : topLevelFolders) {
//                if (entity.getOwnerKey().equals(ownerKey)
//                        && entity.getEntityTypeName().equals(EntityConstants.TYPE_FOLDER)
//                        && entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT) != null) {
//                    // This is the folder we want, now load the entire folder hierarchy
//                    if (loadTree) {
//                        topLevelFolder = entityBean.getEntityTree(entity.getId());
//                    } else {
//                        topLevelFolder = entity;
//                    }
//                    logger.info("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
//                    break;
//                }
//            }
//        }
//        return topLevelFolder;
//    }
//
//
//
//    public Entity createOrVerifyRootEntity(String topLevelFolderName, boolean createIfNecessary, boolean loadTree) throws Exception {
//    	Entity topLevelFolder = getRootEntity(topLevelFolderName, loadTree);
//        if (topLevelFolder == null) {
//            if (createIfNecessary) {
//            	topLevelFolder = createFolderForFile(topLevelFolderName, true, null);
//                logger.info("Saved top level folder as " + topLevelFolder.getId());
//            } else {
//                throw new Exception("Could not find top-level folder by name=" + topLevelFolderName);
//            }
//        }
//
//        logger.info("Using topLevelFolder with id=" + topLevelFolder.getId());
//        return topLevelFolder;
//    }
//    
//    public Entity createOrVerifyFolderEntity(Entity parentFolder, File dir) throws Exception {
//    	return createOrVerifyFolderEntity(parentFolder, dir.getName(), dir);
//    }
//    
//    public Entity createOrVerifyFolderEntity(Entity parentFolder, String name) throws Exception {
//    	return createOrVerifyFolderEntity(parentFolder, name, null);
//    }
//    
//    public Entity createOrVerifyFolderEntity(Entity parentFolder, String name, File dir) throws Exception {
//        return createOrVerifyFolderEntity(parentFolder, name, dir, parentFolder.getMaxOrderIndex()+1);
//    }
//
//    public Entity createOrVerifyFolderEntity(Entity parentFolder, String name, File dir, Integer orderIndex) throws Exception {
//
//        EntityData folderEd = EntityUtils.findChildEntityDataWithName(parentFolder, name);
//        if (folderEd!=null) {
//        	return folderEd.getChildEntity();
//        }
//        
//        Entity folder = createFolderForFile(name, false, dir==null?null:dir.getAbsolutePath());
//        addToParent(parentFolder, folder, orderIndex, EntityConstants.ATTRIBUTE_ENTITY);
//        return folder;
//    }
//
//    public String getEntityTypeForFile(File file) throws Exception {
//        String filenameLowerCase = file.getName().toLowerCase();
//        if (filenameLowerCase.endsWith(".lsm")) {
//            return EntityConstants.TYPE_LSM_STACK;
//        } 
//        else if (filenameLowerCase.endsWith(".tif")||filenameLowerCase.endsWith(".tiff")) {
//            if (file.length()>=FILE_3D_SIZE_THRESHOLD) {
//                return EntityConstants.TYPE_IMAGE_3D;
//            } 
//            else {
//                return EntityConstants.TYPE_IMAGE_2D;
//            }
//        } 
//        else if (filenameLowerCase.endsWith(".swc")) {
//            return EntityConstants.TYPE_SWC_FILE;
//        } 
//        else if (filenameLowerCase.endsWith(".ano")) {
//            return EntityConstants.TYPE_V3D_ANO_FILE;
//        } 
//        else if (filenameLowerCase.endsWith(".png")||filenameLowerCase.endsWith(".jpg")
//                ||filenameLowerCase.endsWith(".gif")||filenameLowerCase.endsWith(".jpeg")
//                ||filenameLowerCase.endsWith(".bmp")) {
//            return EntityConstants.TYPE_IMAGE_2D;
//        } 
//        else if (filenameLowerCase.endsWith(".raw")) {
//            if (filenameLowerCase.contains(".local.")) {
//                return EntityConstants.TYPE_ALIGNED_BRAIN_STACK;
//            } 
//            else {
//                return EntityConstants.TYPE_IMAGE_3D;
//            }
//        } 
//        else if (filenameLowerCase.endsWith(".v3draw")
//        		||filenameLowerCase.endsWith(".vaa3draw")
//        		||filenameLowerCase.endsWith(".pbd")
//        		||filenameLowerCase.endsWith(".v3dpbd")) {
//            return EntityConstants.TYPE_IMAGE_3D;
//        }
//        else if (filenameLowerCase.endsWith(".nsp")) {
//            return EntityConstants.TYPE_MYERS_NEURON_SEPARATION_FILE;
//        }
//        else if (filenameLowerCase.endsWith(".mp4")
//        		||filenameLowerCase.endsWith(".avi")
//                ||filenameLowerCase.endsWith(".mov")) {
//            return EntityConstants.TYPE_MOVIE;
//        }
//        else if (filenameLowerCase.endsWith(".metadata")
//                ||filenameLowerCase.endsWith(".properties")
//        		||filenameLowerCase.endsWith(".csv")
//        		||filenameLowerCase.endsWith(".json")
//        		||filenameLowerCase.endsWith(".txt")) {
//            return EntityConstants.TYPE_TEXT_FILE;
//        }
//        else if (filenameLowerCase.endsWith(".mask")) {
//            return EntityConstants.TYPE_IMAGE_3D;
//        }
//        else if (filenameLowerCase.endsWith(".chan")) {
//            return EntityConstants.TYPE_IMAGE_3D;
//        }
//        else {
//            return EntityConstants.TYPE_FILE;
//        }
//    }
	
}
