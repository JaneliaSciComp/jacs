package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * File discovery service for neuron separation results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparatorResultsDiscoveryService extends FileDiscoveryService {

	private Entity sampleEntity;
	private String resultEntityName;

	@Override
    public void execute(IProcessData processData) throws ServiceException {
    	this.resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
        if (resultEntityName==null) {
        	throw new ServiceException("Input parameter RESULT_ENTITY_NAME may not be null");
        }
    	super.execute(processData);
    	logger.info("resultEntityName="+resultEntityName);
    }
    
    @Override
    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir) throws Exception {

        logger.info("Discovering neuron separation results in "+dir.getAbsolutePath()+" and placing under "+parentFolder.getName());
        
    	if (!parentFolder.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
    		throw new IllegalStateException("Expected Sample as top-level folder");
    	}
    	
    	sampleEntity = parentFolder;
    	
        Entity resultEntity = createResultEntity(dir.getAbsolutePath(), resultEntityName);
        addToParent(parentFolder, resultEntity, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);
    	
    	return resultEntity;
    }
    
    @Override
    protected void processFolderForData(Entity folder) throws Exception {
    	
    	if (!folder.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		throw new IllegalStateException("Expected neuron separator result");
    	}
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }
    	
        processSeparationFolder(folder);
    }
    
    protected void processSeparationFolder(Entity resultEntity) throws Exception {
    	
        File resultDir = new File(resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
    	
        Entity filesFolder = createSupportingFilesFolder();
        addToParent(resultEntity, filesFolder, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
        
        Entity fragmentsFolder = createFragmentCollection();
        addToParent(resultEntity, fragmentsFolder, 1, EntityConstants.ATTRIBUTE_NEURON_FRAGMENTS);
        
        EntityType image2D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_2D);
        EntityType tif3D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D);
        EntityType tif3DLabel = annotationBean.getEntityTypeByName(EntityConstants.TYPE_TIF_3D_LABEL_MASK);
        
        // Sort files so that the entities are added in the correct order
        List<File> files = getOrderedFilesInDir(resultDir);
        
        ArrayList<File> fragmentFiles = new ArrayList<File>();
        
        for (File resultFile : files) {
        	String filename = resultFile.getName();

        	if (resultFile.isDirectory()) continue;
        	
            if (filename.startsWith("ConsolidatedSignal.")) {
                addResultItem(filesFolder, tif3D, resultFile);
            }
            else if (filename.startsWith("ConsolidatedLabel.")) {
                addResultItem(filesFolder, tif3DLabel, resultFile);
            }
            else if (filename.startsWith("Reference.")) {
                addResultItem(filesFolder, tif3D, resultFile);
            }
            else if (filename.startsWith("neuronSeparatorPipeline.PR.neuron") && filename.endsWith(".png")) {

            	String mipNum = filename.substring("neuronSeparatorPipeline.PR.neuron".length(), filename.lastIndexOf('.'));
            	Integer index = null;
            	try {
            		index = Integer.parseInt(mipNum);
            	}
            	catch (NumberFormatException e) {
            		logger.warn("Error parsing number from MIP filename: "+mipNum);
            	}
            	
            	fragmentFiles.ensureCapacity(index+1);
            	fragmentFiles.set(index, resultFile);
            }
            else if (filename.endsWith("MIP.png")) {
                addResultItem(filesFolder, image2D, resultFile);
                if (filename.equals("ConsolidatedSignalMIP.png")) {
                	resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, resultFile.getAbsolutePath());
                	resultEntity = annotationBean.saveOrUpdateEntity(resultEntity);
                	if (sampleEntity!=null) {
                		// Update sample with newest result MIP
                		sampleEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, resultFile.getAbsolutePath());
                		sampleEntity = annotationBean.saveOrUpdateEntity(sampleEntity);
                	}
                	else {
                		logger.warn("Sample entity is unknown");
                	}
                }
            }
            else {
                // ignore other files
            }
        }

        int index = 0;
        for(File resultFile : fragmentFiles) {
        	if (resultFile != null) {
                addResultItem(filesFolder, image2D, resultFile);
            	Entity fragmentEntity = createFragmentEntity(image2D, resultFile, index);
            	addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);	
        	}
        	index++;
        }
        
        // TODO: migrate the annotations from the previous result(s)
    }
    
	private Entity createFragmentEntity(EntityType image2D, File file, Integer index) throws Exception {
		
        Entity fragmentEntity = new Entity();
        fragmentEntity.setUser(user);
        fragmentEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT));
        fragmentEntity.setCreationDate(createDate);
        fragmentEntity.setUpdatedDate(createDate);
        fragmentEntity.setName("Neuron Fragment "+index);
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, index.toString());
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, file.getAbsolutePath());
        fragmentEntity = annotationBean.saveOrUpdateEntity(fragmentEntity);
        logger.info("Saved fragment entity as "+fragmentEntity.getId());
        
        return fragmentEntity;
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
    
	private Entity createFragmentCollection() throws Exception {
        Entity fragmentsEntity = new Entity();
        fragmentsEntity.setUser(user);
        fragmentsEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION));
        fragmentsEntity.setCreationDate(createDate);
        fragmentsEntity.setUpdatedDate(createDate);
        fragmentsEntity.setName("Neuron Fragments");

        fragmentsEntity = annotationBean.saveOrUpdateEntity(fragmentsEntity);
        logger.info("Saved fragment collection as "+fragmentsEntity.getId());
        
        return fragmentsEntity;
    }
	
	private Entity createResultEntity(String path, String name) throws Exception {
        Entity resultEntity = new Entity();
        resultEntity.setUser(user);
        resultEntity.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT));
        resultEntity.setCreationDate(createDate);
        resultEntity.setUpdatedDate(createDate);
        resultEntity.setName(name);
        resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, path);
        
        resultEntity = annotationBean.saveOrUpdateEntity(resultEntity);
        logger.info("Saved pipeline result entity as "+resultEntity.getId());

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
