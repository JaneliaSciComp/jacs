package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
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
	protected EntityHelper entityHelper;

	@Override
    public void execute(IProcessData processData) throws ServiceException {
        entityHelper = new EntityHelper(false);
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
    protected void processFolderForData(Entity resultEntity) throws Exception {

    	if (!resultEntity.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		throw new IllegalStateException("Expected Separator Result as input");
    	}
    	
    	super.processFolderForData(resultEntity);
        processSeparationFolder(resultEntity);
        
        // Remove current default images
        
		entityHelper.removeMIPs(resultEntity);
		entityHelper.removeMIPs(sampleEntity);
		entityHelper.removeDefaultImage(resultEntity);
		entityHelper.removeDefaultImage(sampleEntity);

		// Add new default images
        Entity filesFolder = EntityUtils.getSupportingData(resultEntity);
        Entity signalMIP = EntityUtils.findChildWithName(filesFolder, "ConsolidatedSignalMIP.png");
        Entity referenceMIP = EntityUtils.findChildWithName(filesFolder, "ReferenceMIP.png");
        
        if (signalMIP != null) {
			entityHelper.addMIPs(resultEntity, signalMIP, referenceMIP);
			entityHelper.addDefaultImage(resultEntity, signalMIP);
        }
        else {
        	logger.warn("Could not find ConsolidatedSignalMIP.png in supporting files for result entity id="+resultEntity.getId());
        }
        
        if (referenceMIP != null) {
			entityHelper.addMIPs(sampleEntity, signalMIP, referenceMIP);
			entityHelper.addDefaultImage(sampleEntity, signalMIP);
        }
        else {
        	logger.warn("Could not find ReferenceMIP.png in supporting files for result entity id="+resultEntity.getId());
        }
    }
    
    protected void addFilesToSupportingFiles(Entity filesFolder, List<File> files) throws Exception {

		EntityType image3D = annotationBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_3D);

        for (File resultFile : files) {
        	
        	String filename = resultFile.getName();
            if (filename.contains(".chk")) {
                addResultItem(filesFolder, image3D, resultFile);
            }
            else {
                // ignore other files
            }
        }

    	super.addFilesToSupportingFiles(filesFolder, files);
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
        
        for(File resultFile : fragmentFiles) {
            Entity fragmentMIP = EntityUtils.findChildWithName(filesFolder, resultFile.getName());
            if (fragmentMIP == null) {
            	logger.warn("Could not find "+resultFile.getName()+" in supporting files for result entity id="+resultEntity.getId());
            }
    		Integer index = getIndex(resultFile.getName());
        	Entity fragmentEntity = createFragmentEntity(fragmentType, fragmentMIP, index);
        	addToParent(fragmentsFolder, fragmentEntity, index, EntityConstants.ATTRIBUTE_ENTITY);	
        }
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
    
    protected Entity createFragmentEntity(EntityType fragmentType, Entity fragmentMIP, Integer index) throws Exception {
        Entity fragmentEntity = new Entity();
        fragmentEntity.setUser(user);
        fragmentEntity.setEntityType(fragmentType);
        fragmentEntity.setCreationDate(createDate);
        fragmentEntity.setUpdatedDate(createDate);
        fragmentEntity.setName("Neuron Fragment "+index);
        fragmentEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER, index.toString());
        if (fragmentMIP != null) {
        	fragmentEntity.addChildEntity(fragmentMIP, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        }
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
