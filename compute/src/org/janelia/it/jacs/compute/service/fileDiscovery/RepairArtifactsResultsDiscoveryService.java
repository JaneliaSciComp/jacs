package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.GetIncompleteSeparationsService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for artifacts that repair neuron separation results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RepairArtifactsResultsDiscoveryService extends AbstractEntityService {

	private FileDiscoveryHelper helper;
    private String runMode;
    private List<Entity> separations;
    
	@Override
    public void execute() throws Exception {

        helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
    	runMode = (String)processData.getItem("RUN_MODE");
        if (runMode==null) {
        	throw new ServiceException("Input parameter RUN_MODE may not be null");
        }

        separations = (List<Entity>)processData.getItem("ENTITY_LIST");
        if (separations==null) {
        	throw new ServiceException("Input parameter ENTITY_LIST may not be empty");
        }
        
        for(Entity separation : separations) {
        	try {
        		processSeparation(separation);
        	}
        	catch (Exception e) {
    			logger.error("Results discovery failed for separation with id="+separation.getId(), e);	
        	}
        }
    }
	
	protected void processSeparation(Entity separation) throws Exception {

    	if (!separation.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		logger.info("Not a neuron separation result: "+separation.getId());
    		return;
    	}
	
    	// Load the entire neuron separation entity tree
		entityBean.loadLazyEntity(separation, true);
		
		String filepath = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

        if (runMode.equals(GetIncompleteSeparationsService.MODE_REF_MASK_CHAN)) {
        	discoverRefMaskChan(separation, filepath);
        }
        else if (runMode.equals(GetIncompleteSeparationsService.MODE_ALL_MASK_CHAN)) {
        	discoverAllMaskChan(separation, filepath);
        }
        else if (runMode.equals(GetIncompleteSeparationsService.MODE_FASTLOAD)) {
        	discoverFastLoad(separation, filepath);        	
        }
	}

	private void discoverFastLoad(Entity separation, String filepath) throws Exception {
		// TODO Auto-generated method stub
		
	}

	private void discoverAllMaskChan(Entity separation, String filepath) throws Exception {
		
        logger.info("Processing mask/chan results for separation (id="+separation.getId()+") with path "+filepath);

        // Find mask/chan files
        File maskChanDir = new File(filepath.replaceFirst("groups", "archive")+"/archive/maskChan");
        List<File> maskChanFiles = helper.collectFiles(maskChanDir, true);
        logger.info("Collected "+maskChanFiles.size()+" files in "+maskChanDir);
        
        Map<Integer,String> maskFiles = new HashMap<Integer,String>();
        Map<Integer,String> chanFiles = new HashMap<Integer,String>();
        
        for(File file : maskChanFiles) {
            String name = file.getName();
            if (!name.endsWith("mask") && !name.endsWith("chan")) continue;
            Integer index = null;
            try {
                index = NeuronSeparatorResultsDiscoveryService.getNeuronIndexFromMaskChanFile(name);
            }
            catch (Exception e) {
                logger.warn("Could not parse mask/chan file name: "+name+", "+e.getMessage());
            }
            if (index==null) continue;
            if (name.endsWith("mask")) {
                maskFiles.put(index, file.getAbsolutePath());
            }
            else if (name.endsWith("chan")) {
                chanFiles.put(index, file.getAbsolutePath());
            }
        }
        
        Entity fragmentFolder = separation.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        
        if (fragmentFolder==null) {
            logger.warn("No fragment folder found for separation "+separation.getId());
            return;
        }
        
        for(Entity fragmentEntity : EntityUtils.getChildrenOfType(fragmentFolder, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            
            // Delete existing images, if any. They are being replaced.
            
            Entity maskImage = fragmentEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE);
            if (maskImage!=null) {
                entityBean.deleteEntityTree(maskImage.getOwnerKey(), maskImage.getId());
            }
            
            Entity chanImage = fragmentEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE);
            if (chanImage!=null) {
                entityBean.deleteEntityTree(maskImage.getOwnerKey(), maskImage.getId());
            }
            
            // Get mask/chan files for this neuron index
            
            String indexStr = fragmentEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
            int index = Integer.parseInt(indexStr);

            String maskFilepath = maskFiles.get(index);
            if (maskFilepath==null) {
                throw new MissingDataException("No mask file for neuron "+index);
            }
            maskImage = helper.create3dImage(maskFilepath);
            helper.setImage(fragmentEntity, EntityConstants.ATTRIBUTE_MASK_IMAGE, maskImage);
            
            String chanFilepath = chanFiles.get(index);
            if (chanFilepath==null) {
                throw new MissingDataException("No chan file for neuron "+index);
            }
            chanImage = helper.create3dImage(chanFilepath);
            helper.setImage(fragmentEntity, EntityConstants.ATTRIBUTE_CHAN_IMAGE, chanImage); 
        }
		
	}

	private void discoverRefMaskChan(Entity separation, String filepath) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
