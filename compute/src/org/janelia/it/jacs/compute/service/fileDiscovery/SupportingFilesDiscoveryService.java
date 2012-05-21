package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * File discovery service for supporting files.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SupportingFilesDiscoveryService extends FileDiscoveryService {
	
	protected String resultEntityName;
	protected String resultEntityType;
	protected List<File> allFiles = new ArrayList<File>();

	@Override
    public void execute(IProcessData processData) throws ServiceException {
    	
	    this.resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
        if (resultEntityName==null) {
        	throw new ServiceException("Input parameter RESULT_ENTITY_NAME may not be null");
        }
    	
        this.resultEntityType = (String)processData.getItem("RESULT_ENTITY_TYPE");
        if (resultEntityType==null) {
        	throw new ServiceException("Input parameter RESULT_ENTITY_TYPE may not be null");
        }
	    
        super.execute(processData);
    }
    
    @Override
    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir, Integer index) throws Exception {

        logger.info("Discovering supporting files in "+dir.getAbsolutePath());
    	
        Entity resultEntity = createResultEntity(dir.getAbsolutePath(), resultEntityName, resultEntityType);
        addToParent(parentFolder, resultEntity, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);
    	
    	return resultEntity;
    }
    
    @Override
    protected void processFolderForData(Entity folder) throws Exception {
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

        File resultDir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        
        Entity filesFolder = EntityUtils.getSupportingData(folder);
        if (filesFolder==null) {
        	filesFolder = createSupportingFilesFolder();
        	addToParent(folder, filesFolder, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
        }

		collectFiles(resultDir);

		// Sort files so that the entities are added in the correct order
        Collections.sort(allFiles, new Comparator<File>() {
        	@Override
        	public int compare(File file1, File file2) {
        		return file1.getName().compareTo(file2.getName());
        	}
		});
        
		addFilesToSupportingFiles(filesFolder, allFiles);
    }
    
    protected void collectFiles(File dir) throws Exception {
        
        List<File> files = getOrderedFilesInDir(dir);
        logger.info("Found "+files.size()+" files in "+dir.getAbsolutePath());
        
        for (File resultFile : files) {

        	// Skip symbolic links
        	if (FileUtils.isSymlink(resultFile)) continue;
        	
        	if (resultFile.isDirectory() && !resultFile.getName().startsWith("sge_")) {
        		collectFiles(resultFile);
        	}
        	
        	allFiles.add(resultFile);
        }
    }

    protected void addFilesToSupportingFiles(Entity filesFolder, List<File> files) throws Exception {

        EntityType textFile = entityBean.getEntityTypeByName(EntityConstants.TYPE_TEXT_FILE);
		EntityType image3D = entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);
		EntityType image2D = entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D);

        for (File resultFile : files) {
        	
        	String filename = resultFile.getName();
            if (filename.endsWith(".metadata")) {
                addResultItem(filesFolder, textFile, resultFile);
            }
            else if (filename.endsWith(".csv")) {
                addResultItem(filesFolder, textFile, resultFile);
            }
            else if (filename.endsWith("groups.txt")) {
                addResultItem(filesFolder, textFile, resultFile);
            }
            else if (filename.endsWith(".v3draw")) {
                addResultItem(filesFolder, image3D, resultFile);
            }
            else if (filename.endsWith(".v3dpbd")) {
                addResultItem(filesFolder, image3D, resultFile);
            }
            else if (filename.endsWith(".png")) {
                addResultItem(filesFolder, image2D, resultFile);
            }
            else {
                // ignore other files
            }
        }
    }
    
    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }
	
    protected Entity createResultEntity(String path, String name, String resultEntityType) throws Exception {
        Entity resultEntity = new Entity();
        resultEntity.setUser(user);
        resultEntity.setEntityType(entityBean.getEntityTypeByName(resultEntityType));
        resultEntity.setCreationDate(createDate);
        resultEntity.setUpdatedDate(createDate);
        resultEntity.setName(name);
        resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, path);
        
        resultEntity = entityBean.saveOrUpdateEntity(resultEntity);
        logger.info("Saved result entity as "+resultEntity.getId());

        return resultEntity;
    }

    protected Entity addResultItem(Entity resultEntity, EntityType type, File file) throws Exception {
    	
        Entity entity = new Entity();
        entity.setUser(user);
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

}
