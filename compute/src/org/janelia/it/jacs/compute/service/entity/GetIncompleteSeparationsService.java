package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        contextLogger.info("Finding neuron separations in need of repair, which are owned by "+ownerKey+" and located in "+centralDir+" or "+centralDirArchived);
        
        List<String> sepsMissingFastLoad = new ArrayList<String>();
        List<String> sepsMissingAllMaskChan = new ArrayList<String>();
        List<String> sepsMissingRefMaskChan = new ArrayList<String>();
        Set<String> incompleteItems = new HashSet<String>();
        
        for(Entity separation : entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {

        	String inputPath = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (!inputPath.startsWith(centralDir) && !inputPath.startsWith(centralDirArchived)) {
                contextLogger.debug("  Cannot repair artifacts in dir which is not in the FileStore.CentralDir: "+inputPath);
                continue;
            }
            
        	boolean isAligned = false;
        	for(Entity parent : entityBean.getParentEntities(separation.getId())) {
        	    if (parent.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
        	        isAligned = true;
        	        break;
        	    }
        	}
            
        	contextLogger.info("Processing "+separation.getName()+", id="+separation.getId()+", isAligned="+isAligned);
        	
        	if (!isAligned) {
        	    contextLogger.info("  Skipping unaligned separation");
        	    continue;
        	}
        	
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
            
            boolean missingNeuronMaskChan = false;
            if (isAligned) {
                // Check for missing mask or chan files
                if (neuronFragments!=null) {
                    for(Entity fragment : EntityUtils.getChildrenOfType(neuronFragments, EntityConstants.TYPE_NEURON_FRAGMENT)) {
                        populateChildren(fragment);    
                        if (fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE) == null || fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE) == null) {
                            missingNeuronMaskChan = true;
                            break;
                        }
                    }
                }
            }

            boolean missingFastLoad = false;
            boolean missingRefChan = false;
            if (!missingNeuronMaskChan && isAligned && reference!=null) {
            	// Check for missing fastload
        	    if (reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)==null) {
        	        missingFastLoad = true;
        	    }
            	// Check for missing ref mask/chan
        		if (isAligned) {
                	if (reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE)==null) {
                	    missingRefChan = true;
                	}
                	if (reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE)==null) {
                	    missingRefChan = true;
                	}
        		}
            }
        	
        	contextLogger.info("  missingFastLoad="+missingFastLoad+", missingRefChan="+missingRefChan+", missingNeuronMaskChan="+missingNeuronMaskChan);
            
            if (missingFastLoad) {
            	sepsMissingFastLoad.add(separation.getId().toString());
            }
            
            if (missingNeuronMaskChan) {
            	sepsMissingAllMaskChan.add(separation.getId().toString());
            }
            else if (missingRefChan) {
            	sepsMissingRefMaskChan.add(separation.getId().toString());
            }
            
        	// Free memory
        	for(EntityData childEd : separation.getEntityData()) {
        	    childEd.setChildEntity(null);
        	}
        }
        
        incompleteItems.addAll(sepsMissingFastLoad);
        incompleteItems.addAll(sepsMissingAllMaskChan);
        incompleteItems.addAll(sepsMissingRefMaskChan);
        
        List<RunModeItemGroups<String>> allModeGroups = new ArrayList<RunModeItemGroups<String>>();
        allModeGroups.add(new RunModeItemGroups<String>(MODE_FASTLOAD, sepsMissingFastLoad));
        allModeGroups.add(new RunModeItemGroups<String>(MODE_ALL_MASK_CHAN, sepsMissingAllMaskChan));
        allModeGroups.add(new RunModeItemGroups<String>(MODE_REF_MASK_CHAN, sepsMissingRefMaskChan));
        
        processData.putItem("RUN_MODE_ITEM_GROUPS", allModeGroups);
        processData.putItem("INCOMPLETE_ITEMS", new ArrayList<String>(incompleteItems));
    }
}
