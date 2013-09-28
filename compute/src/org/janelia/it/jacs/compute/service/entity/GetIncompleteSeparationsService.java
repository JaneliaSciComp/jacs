package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

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

    public static final String MODE_FASTLOAD = "fastLoad";
    public static final String MODE_ALL_MASK_CHAN = "maskChan"; 
    public static final String MODE_REF_MASK_CHAN = "refMaskChan";
    
    public void execute() throws Exception {

        String runMode = (String)processData.getItem("RUN_MODE");
        
        logger.info("Finding neuron separations in need of "+runMode+" repair");
        
        List<Entity> missingFastload = new ArrayList<Entity>();
        List<Entity> missingAllMaskChan = new ArrayList<Entity>();
        List<Entity> missingRefMaskChan = new ArrayList<Entity>();
        
        for(Entity result : entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
        	logger.info("Processing neuron separation, id="+result.getId());
        	
        	boolean hasFastLoad = false; 
        	boolean hasRefMask = false;
        	boolean hasRefChan = false;
        	boolean hasNeuronMaskChans = false;
        	
        	// Load children
        	populateChildren(result);
        	Entity neuronFragments = EntityUtils.findChildWithType(result, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        	populateChildren(neuronFragments);
        	Entity supportingData = EntityUtils.findChildWithType(result, EntityConstants.TYPE_SUPPORTING_DATA);
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
            	logger.info("  Adding separation to missingFastLoad list");
            	missingFastload.add(result);
            }
            
            if (!hasNeuronMaskChans) {
            	logger.info("  Adding separation to missingAllMaskChan list");
            	missingAllMaskChan.add(result);
            }
            else if (!hasRefMask || !hasRefChan) {
            	logger.info("  Adding separation to missingRefMaskChan list");
            	missingRefMaskChan.add(result);
            }
            
        	// Free memory
        	for(EntityData childEd : result.getEntityData()) {
        	    childEd.setChildEntity(null);
        	}
        }
        
        logger.info("missingFastload.size="+missingFastload.size());
        logger.info("missingAllMaskChan.size="+missingAllMaskChan.size());
        logger.info("missingRefMaskChan.size="+missingRefMaskChan.size());
        
        List<RunModeItemGroups> allModeGroups = new ArrayList<RunModeItemGroups>();
        allModeGroups.add(new RunModeItemGroups(MODE_FASTLOAD, missingFastload));
        allModeGroups.add(new RunModeItemGroups(MODE_ALL_MASK_CHAN, missingAllMaskChan));
        allModeGroups.add(new RunModeItemGroups(MODE_REF_MASK_CHAN, missingRefMaskChan));
        				
        processData.putItem("RUN_MODE_ITEM_GROUPS", allModeGroups);
    }
}
