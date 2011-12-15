package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * File discovery service for sample processing results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleProcessingResultsDiscoveryService extends FileDiscoveryService {
	
	private String resultEntityName;

	@Override
    public void execute(IProcessData processData) throws ServiceException {
    	this.resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
        if (resultEntityName==null) {
        	throw new ServiceException("Input parameter RESULT_ENTITY_NAME may not be null");
        }
    	super.execute(processData);
    }
    
    @Override
    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir) throws Exception {

        logger.info("Discovering sample processing results in "+dir.getAbsolutePath()+" and placing under "+parentFolder.getName());
        
    	if (!parentFolder.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
    		throw new IllegalStateException("Expected Sample as top-level folder");
    	}
    	
        Entity resultEntity = createResultEntity(dir.getAbsolutePath(), resultEntityName);
        addToParent(parentFolder, resultEntity, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);
    	
    	return resultEntity;
    }
    
    @Override
    protected void processFolderForData(Entity folder) throws Exception {
    	
    	if (!folder.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
    		throw new IllegalStateException("Expected sample processing result");
    	}
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }
        
        processProcessingFolder(folder);
    }
    
    protected void processProcessingFolder(Entity resultEntity) throws Exception {
    	
        File resultDir = new File(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        
        Entity filesFolder = createSupportingFilesFolder();
        addToParent(resultEntity, filesFolder, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);

		processSubDirectory(filesFolder, new File(resultDir, "metadata"));	
		processSubDirectory(filesFolder, new File(resultDir, "merge"));	
		processSubDirectory(filesFolder, new File(resultDir, "stitch"));	
		processSubDirectory(filesFolder, new File(resultDir, "align"));	
    }
    
    protected void processSubDirectory(Entity entity, File dir) throws Exception {
    	
        EntityType textFile = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TEXT_FILE);
        EntityType tif3D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D);
        
        // Sort files so that the entities are added in the correct order
        List<File> files = getOrderedFilesInDir(dir);
        
        logger.info("Found "+files.size()+" files in "+dir.getAbsolutePath());
        
        for (File resultFile : files) {
        	if (resultFile.isDirectory()) continue;
        	String filename = resultFile.getName();
            if (filename.endsWith(".metadata")) {
                addResultItem(entity, textFile, resultFile);
            }
            else if (filename.endsWith(".csv")) {
                addResultItem(entity, textFile, resultFile);
            }
            else if (filename.endsWith(".v3draw")) {
                addResultItem(entity, tif3D, resultFile);
            }
            else {
                // ignore other files
            }
        }
    }
	
    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setUser(user);
        filesFolder.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = annotationBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }
	
	private Entity createResultEntity(String path, String name) throws Exception {
        Entity resultEntity = new Entity();
        resultEntity.setUser(user);
        resultEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT));
        resultEntity.setCreationDate(createDate);
        resultEntity.setUpdatedDate(createDate);
        resultEntity.setName(name);
        resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, path);
        
        resultEntity = annotationBean.saveOrUpdateEntity(resultEntity);
        logger.info("Saved sample processing result entity as "+resultEntity.getId());

        return resultEntity;
    }

    private Entity addResultItem(Entity resultEntity, EntityType type, File file) throws Exception {
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
        	entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, file.getAbsolutePath());
        }
        
        entity = annotationBean.saveOrUpdateEntity(entity);
        logger.info("Saved "+type.getName()+" as "+entity.getId());
        addToParent(resultEntity, entity, resultEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        return entity;
    }

}
