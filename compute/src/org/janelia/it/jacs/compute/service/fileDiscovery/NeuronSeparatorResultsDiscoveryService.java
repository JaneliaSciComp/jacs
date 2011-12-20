package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
public class NeuronSeparatorResultsDiscoveryService extends SupportingFilesDiscoveryService {

	protected Entity sampleEntity;

	@Override
    public void execute(IProcessData processData) throws ServiceException {
    	processData.putItem("RESULT_ENTITY_TYPE", EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    	super.execute(processData);
    }
    
    @Override
    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir) throws Exception {

    	if (!parentFolder.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE)) {
    		throw new IllegalStateException("Expected Sample as top-level folder");
    	}
    	
    	sampleEntity = parentFolder;
    	
    	return super.verifyOrCreateChildFolderFromDir(parentFolder, dir);
    }
    
    @Override
    protected void processFolderForData(Entity folder) throws Exception {
    	
    	super.processFolderForData(folder);
    	
        processSeparationFolder(folder);
    }
    
    protected void processSeparationFolder(Entity resultEntity) throws Exception {
    	
        Entity fragmentsFolder = createFragmentCollection();
        addToParent(resultEntity, fragmentsFolder, 1, EntityConstants.ATTRIBUTE_NEURON_FRAGMENTS);
        
        EntityType fragmentType = annotationBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT);
        
        ArrayList<File> fragmentFiles = new ArrayList<File>();
        
        for (File resultFile : allFiles) {
        	String filename = resultFile.getName();

        	if (resultFile.isDirectory()) continue;
        	
            if (filename.startsWith("neuronSeparatorPipeline.PR.neuron") && filename.endsWith(".png")) {            	
            	fragmentFiles.add(resultFile);
            }
            else if (filename.endsWith(".png")) {
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
        
        Collections.sort(fragmentFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				Integer i1 = getIndex(o1);
				Integer i2 = getIndex(o2);
				if (i1 == null && i2 == null) return 0;
				if (i1 == null) return 1;
				if (i2 == null) return -1;
				return i1.compareTo(i2);
			}
        });
        
        for(File resultFile : fragmentFiles) {
    		Integer index = getIndex(resultFile);
        	Entity fragmentEntity = createFragmentEntity(fragmentType, resultFile, index);
        	addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);	
        }
    }
    
    protected Integer getIndex(File neuronMIPFile) {
    	String filename = neuronMIPFile.getName();
    	String mipNum = filename.substring("neuronSeparatorPipeline.PR.neuron".length(), filename.lastIndexOf('.'));
    	try {
        	// New 2-stage neuron separator creates files with an extra dot in the filename, so we need to account for that
        	if (mipNum.startsWith(".")) mipNum = mipNum.substring(1); 
    		return Integer.parseInt(mipNum);
    	}
    	catch (NumberFormatException e) {
    		logger.warn("Error parsing number from MIP filename: "+mipNum);
    	}
    	return null;
    }
    
    protected Entity createFragmentEntity(EntityType fragmentType, File file, Integer index) throws Exception {
        Entity fragmentEntity = new Entity();
        fragmentEntity.setUser(user);
        fragmentEntity.setEntityType(fragmentType);
        fragmentEntity.setCreationDate(createDate);
        fragmentEntity.setUpdatedDate(createDate);
        fragmentEntity.setName("Neuron Fragment "+index);
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, index.toString());
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, file.getAbsolutePath());
        fragmentEntity = annotationBean.saveOrUpdateEntity(fragmentEntity);
        logger.info("Saved fragment entity as "+fragmentEntity.getId());
        return fragmentEntity;
    }
	
    protected Entity createFragmentCollection() throws Exception {
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
}
