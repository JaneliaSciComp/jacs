package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Walk the user's neuron separation entities and pick out the ones that need some sort of repair. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetIncompleteSeparationsService extends AbstractEntityService {

    private static final String centralDir = SystemConfigurationProperties.getString("FileStore.CentralDir");
    private static final String centralDirArchived = SystemConfigurationProperties.getString("FileStore.CentralDir.Archived");
    
    public static final String MODE_FASTLOAD = "fastLoad";
    public static final String MODE_ALL_MASK_CHAN = "maskChan"; 
    public static final String MODE_REF_MASK_CHAN = "refMaskChan";
    
    public void execute() throws Exception {

        logger.info("Finding neuron separations in need of repair, which are owned by "+ownerKey+" and located in "+centralDir);
        
        List<String> missingFastload = new ArrayList<String>();
        List<String> missingAllMaskChan = new ArrayList<String>();
        List<String> missingRefMaskChan = new ArrayList<String>();
        
        for(Entity separation : entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {

        	String inputPath = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (!inputPath.startsWith(centralDir) && !inputPath.startsWith(centralDirArchived)) {
                logger.debug("  Cannot repair artifacts in dir which is not in the FileStore.CentralDir: "+inputPath);
                continue;
            }
            
        	logger.info("Processing neuron separation, id="+separation.getId());
        		
        	boolean hasFastLoad = false; 
        	boolean hasRefMask = false;
        	boolean hasRefChan = false;
        	boolean hasNeuronMaskChans = false;
        	
        	// Load children
        	populateChildren(separation);
        	Entity neuronFragments = EntityUtils.findChildWithType(separation, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        	populateChildren(neuronFragments);
        	Entity supportingData = EntityUtils.findChildWithType(separation, EntityConstants.TYPE_SUPPORTING_DATA);
        	populateChildren(supportingData);

        	// Load reference stack
        	Entity reference = null;
        	if (supportingData!=null) {
	        	for(Entity image3d : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_3D)) {
	        		if (image3d.getName().startsWith("Reference.")) {
	        			reference = image3d;
	        		};
	        	}
	        	populateChildren(reference);
        	}
        	
        	if (reference!=null) {
            	// Check for missing fastload
        		hasFastLoad = reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)!=null;
            	// Check for missing ref mask/chan
            	hasRefMask = reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE)!=null;
            	hasRefChan = reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE)!=null;
        	}
        	
        	// Check for missing mask or chan files
        	if (neuronFragments!=null) {
        		hasNeuronMaskChans = true;
	            for(Entity fragment : EntityUtils.getChildrenOfType(neuronFragments, EntityConstants.TYPE_NEURON_FRAGMENT)) {
	                populateChildren(fragment);    
	                if (fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE) == null || fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE) == null) {
	                	hasNeuronMaskChans = false;
	                    break;
	                }
	            }
        	}
        	
        	logger.info("  hasFastLoad="+hasFastLoad+", hasRefMask="+hasRefMask+", hasRefChan="+hasRefChan+", hasNeuronMaskChans="+hasNeuronMaskChans);
            
            if (!hasFastLoad) {
            	missingFastload.add(separation.getId().toString());
            }
            
            if (!hasNeuronMaskChans) {
            	missingAllMaskChan.add(separation.getId().toString());
            }
            else if (!hasRefMask || !hasRefChan) {
            	missingRefMaskChan.add(separation.getId().toString());
            }
            
        	// Free memory
        	for(EntityData childEd : separation.getEntityData()) {
        	    childEd.setChildEntity(null);
        	}
        }
        
        List<RunModeItemGroups<String>> allModeGroups = new ArrayList<RunModeItemGroups<String>>();
        allModeGroups.add(new RunModeItemGroups<String>(MODE_FASTLOAD, missingFastload));
        allModeGroups.add(new RunModeItemGroups<String>(MODE_ALL_MASK_CHAN, missingAllMaskChan));
        allModeGroups.add(new RunModeItemGroups<String>(MODE_REF_MASK_CHAN, missingRefMaskChan));
        				
        processData.putItem("RUN_MODE_ITEM_GROUPS", allModeGroups);
    }
}
