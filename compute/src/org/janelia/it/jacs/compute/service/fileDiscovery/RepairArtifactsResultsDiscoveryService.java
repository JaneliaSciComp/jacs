package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * File discovery service for a list of mask/chan results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RepairArtifactsResultsDiscoveryService extends AbstractEntityService {

    protected FileDiscoveryHelper helper;
    
	@Override
    public void execute() throws Exception {

        helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        
        List<Entity> entityList = (List<Entity>)processData.getItem("SEPARATION_LIST");
        if (entityList==null) {
        	Entity entity = (Entity)processData.getItem("SEPARATION");
        	if (entity==null) {
        		String entityId = (String)processData.getItem("SEPARATION_ID");
        		if (entityId==null) {
        			throw new ServiceException("Both SEPARATION/SEPARATION_ID and SEPARATION_LIST may not be null");	
        		}
        		entity = entityBean.getEntityById(entityId);
        	}
        	entityList = new ArrayList<Entity>();
        	entityList.add(entity);
        }
        
        for(Entity entity : entityList) {
        	try {
        		processSeparation(entity);
        	}
        	catch (Exception e) {
        		if (entityList.size()==1) {
        			throw e;
        		}
        		else {
        			logger.error("Results discovery failed for separation id="+entity.getId(), e);	
        		}
        	}
        }
    }
	
	protected void processSeparation(Entity separation) throws Exception {

    	if (!separation.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
    		logger.info("Not a neuron separation result: "+separation.getId());
    		return;
    	}
	
		entityBean.loadLazyEntity(separation, true);
		
		String filepath = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

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
}
