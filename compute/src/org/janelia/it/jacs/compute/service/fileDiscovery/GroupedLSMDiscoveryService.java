package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for single (unpaired) LSMs grouped for stitching by directory. Any directory which contains
 * LSM files is treated and saved as a Sample entity.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupedLSMDiscoveryService extends FileDiscoveryService {
    
    protected void processFolderForData(Entity folder) throws Exception {
    	
        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());
        
        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

        processSampleFolder(folder);
    }
    
    protected void processSampleFolder(Entity folder) throws Exception {

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder as unstitched data="+dir.getAbsolutePath());
        for (File file : FileUtils.getOrderedFilesInDir(dir)) {
            if (file.isDirectory()) {
            	if (dirContainsLsms(file)) {
            		processSampleDir(folder, file);
            	}
            	else {
                    Entity subfolder = verifyOrCreateChildFolderFromDir(folder, file, null /*index*/);
                    processFolderForData(subfolder);
            	}
            } 
            else if (file.getName().toUpperCase().endsWith(".LSM")) {
        		processSampleDir(folder, dir);
            }
            else {
            	// Ignore other files
            }
        }
    }
    
    private boolean dirContainsLsms(File dir) {
        for (File file : FileUtils.getOrderedFilesInDir(dir)) {
        	if (file.getName().toUpperCase().endsWith(".LSM")) {
        		return true;
        	}
        }
        return false;
    }
    
    private void processSampleDir(Entity parentFolder, File sampleDir) throws Exception {

        List<File> lsmFileList = new ArrayList<File>();
        for (File file : FileUtils.getOrderedFilesInDir(sampleDir)) {
        	if (file.getName().toUpperCase().endsWith(".LSM")) {
        		lsmFileList.add(file);
            }
        }
        
    	Entity sample = findExistingSample(parentFolder, sampleDir.getName());

    	try {
    		if (sample == null) {
	        	// Create the sample
	            sample = createSample(sampleDir.getName(), lsmFileList);
	            helper.addToParent(parentFolder, sample, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
    		}
    	}
    	catch (ComputeException e) {
    		logger.warn("Could not delete existing sample for regeneration, id="+sample.getId(),e);
    	}
    }
    
    /**
     * Find and return the child Sample entity which contains the given folder as a Sample. 
     */
    private Entity findExistingSample(Entity folder, String name) {

    	for(EntityData ed : folder.getEntityData()) {
			Entity sampleFolder = ed.getChildEntity();
    		if (sampleFolder == null) continue;
    		if (!EntityConstants.TYPE_SAMPLE.equals(sampleFolder.getEntityTypeName())) continue;
    		if (sampleFolder.getName().equals(name)) {
    			return sampleFolder;
    		}
    	}
    	return null;
    }
    
    protected Entity createSample(String name, List<File> lsmFileList) throws Exception {

        Entity sample = new Entity();
        sample.setOwnerKey(ownerKey);
        sample.setEntityTypeName(EntityConstants.TYPE_SAMPLE);
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        sample = entityBean.saveOrUpdateEntity(sample);
        logger.info("Saved sample as "+sample.getId());

        Entity supportingFiles = EntityUtils.getSupportingData(sample);
    	if (supportingFiles == null) {
    		supportingFiles = createSupportingFilesFolder();
    		helper.addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
    	}
    	
    	int i = 0;
    	for(File file : lsmFileList) {
    		Entity lsmStack = createLsmStackFromFile(file);
    		helper.addToParent(supportingFiles, lsmStack, i++, EntityConstants.ATTRIBUTE_ENTITY);
    	}
    	
        return sample;
    }

    protected Entity createSupportingFilesFolder() throws Exception {
        Entity filesFolder = new Entity();
        filesFolder.setOwnerKey(ownerKey);
        filesFolder.setEntityTypeName(EntityConstants.TYPE_SUPPORTING_DATA);
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }
    
    private Entity createLsmStackFromFile(File file) throws Exception {
        Entity lsmStack = new Entity();
        lsmStack.setOwnerKey(ownerKey);
        lsmStack.setEntityTypeName(EntityConstants.TYPE_LSM_STACK);
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(file.getName());
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, file.getAbsolutePath());
        lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        logger.info("Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }
}
