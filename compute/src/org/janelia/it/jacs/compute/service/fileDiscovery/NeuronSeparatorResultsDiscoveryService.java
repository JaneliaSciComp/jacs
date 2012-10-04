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
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for neuron separation results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparatorResultsDiscoveryService extends SupportingFilesDiscoveryService {

	protected Entity sampleEntity;
	protected Entity pipelineRunEntity;
	
	@Override
    public void execute(IProcessData processData) throws ServiceException {
    	processData.putItem("RESULT_ENTITY_TYPE", EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    	super.execute(processData);
    }
    
    @Override
    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir, Integer index) throws Exception {

    	sampleEntity = entityBean.getAncestorWithType(parentFolder, EntityConstants.TYPE_SAMPLE);
    	pipelineRunEntity = entityBean.getAncestorWithType(parentFolder, EntityConstants.TYPE_PIPELINE_RUN);

		if (sampleEntity==null) {
			throw new IllegalStateException("Root entity must have Sample ancestor: "+parentFolder.getId());		
		}
    	
		if (pipelineRunEntity==null) {
			throw new IllegalStateException("Root entity must have Pipeline Run ancestor: "+parentFolder.getId());		
		}
    	
    	return super.verifyOrCreateChildFolderFromDir(parentFolder, dir, index);
    }
    
    @Override
    protected void processFolderForData(Entity resultEntity) throws Exception {

    	if (!resultEntity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		throw new IllegalStateException("Expected Separator Result as input");
    	}
    	
    	helper.addFileExclusion("*.sh");
    	helper.addFileExclusion("fastLoad");
    	
    	super.processFolderForData(resultEntity); // This creates the Supporting Files folder if it doesn't exist
        processSeparationFolder(resultEntity);

        Entity filesFolder = EntityUtils.getSupportingData(resultEntity);
        Entity signalMIP = EntityUtils.findChildWithName(filesFolder, "ConsolidatedSignalMIP.png");
        Entity referenceMIP = EntityUtils.findChildWithName(filesFolder, "ReferenceMIP.png");

		helper.setImage(resultEntity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
		helper.setImage(resultEntity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);
		helper.setImage(sampleEntity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
		helper.setImage(sampleEntity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);
		helper.setDefault2dImage(resultEntity, signalMIP);
		helper.setDefault2dImage(sampleEntity, signalMIP);
		
		if (pipelineRunEntity!=null) {
			helper.setImage(pipelineRunEntity, EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE, signalMIP);
			helper.setImage(pipelineRunEntity, EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE, referenceMIP);	
			helper.setDefault2dImage(pipelineRunEntity, signalMIP);
		}
    }
    
    protected void processSeparationFolder(Entity resultEntity) throws Exception {
    	
        Entity fragmentsFolder = createFragmentCollection();
        helper.addToParent(resultEntity, fragmentsFolder, 1, EntityConstants.ATTRIBUTE_NEURON_FRAGMENTS);
        
        EntityType fragmentType = entityBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT);
        
        ArrayList<File> fragmentFiles = new ArrayList<File>();
        
        for (File resultFile : allFiles) {
        	String filename = resultFile.getName();

        	if (resultFile.isDirectory()) continue;
        	
            if (filename.startsWith("neuronSeparatorPipeline.PR.neuron") && filename.endsWith(".png")) {            	
            	fragmentFiles.add(resultFile);
            }
            else {
                // ignore other files
            }
        }
        
        Collections.sort(fragmentFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				Integer i1 = getIndex(o1.getName());
				Integer i2 = getIndex(o2.getName());
				if (i1 == null && i2 == null) return 0;
				if (i1 == null) return 1;
				if (i2 == null) return -1;
				return i1.compareTo(i2);
			}
        });

        Entity filesFolder = EntityUtils.getSupportingData(resultEntity);

        String filepath = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	File fastloadDir = new File(filepath, "fastLoad");
    	if (fastloadDir.exists()) {
    		Entity fastLoadFolder = helper.createOrVerifyFolderEntity(filesFolder, "Fast Load", fastloadDir, 0);
    		processFastLoadFolder(fastLoadFolder);
    	}
    	else {
    		logger.warn("No fastLoad dir found for separationId="+resultEntity.getId());
    	}
    	
        for(File resultFile : fragmentFiles) {
            Entity fragmentMIP = EntityUtils.findChildWithName(filesFolder, resultFile.getName());
            if (fragmentMIP == null) {
            	logger.warn("Could not find "+resultFile.getName()+" in supporting files for result entity id="+resultEntity.getId());
            }
    		Integer index = getIndex(resultFile.getName());
        	Entity fragmentEntity = createFragmentEntity(fragmentType, index);
        	helper.setDefault2dImage(fragmentEntity, fragmentMIP);
    		helper.addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);	
        }
    }

    protected void processFastLoadFolder(Entity folder) throws Exception {
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));

        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

		helper.addFilesInDirToFolder(folder, dir);
    }
    
    protected Integer getIndex(String filename) {
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
    
    protected Entity createFragmentEntity(EntityType fragmentType, Integer index) throws Exception {
        Entity fragmentEntity = new Entity();
        fragmentEntity.setUser(user);
        fragmentEntity.setEntityType(fragmentType);
        fragmentEntity.setCreationDate(createDate);
        fragmentEntity.setUpdatedDate(createDate);
        fragmentEntity.setName("Neuron Fragment "+index);
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, index.toString());
        fragmentEntity = entityBean.saveOrUpdateEntity(fragmentEntity);
        logger.info("Saved fragment entity as "+fragmentEntity.getId());
        return fragmentEntity;
    }
	
    protected Entity createFragmentCollection() throws Exception {
        Entity fragmentsEntity = new Entity();
        fragmentsEntity.setUser(user);
        fragmentsEntity.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION));
        fragmentsEntity.setCreationDate(createDate);
        fragmentsEntity.setUpdatedDate(createDate);
        fragmentsEntity.setName("Neuron Fragments");
        fragmentsEntity = entityBean.saveOrUpdateEntity(fragmentsEntity);
        logger.info("Saved fragment collection as "+fragmentsEntity.getId());
        return fragmentsEntity;
    }
}
