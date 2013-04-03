package org.janelia.it.jacs.compute.service.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {

    private static final String OPTICAL_RES_UNIFIED = "0.62x0.62x0.62";
    private static final String PIXEL_RES_UNIFIED = "1024x512x218";
    private static final String OPTICAL_RES_UNIFIED_63x = "0.38x0.38x0.38";
    private static final String PIXEL_RES_UNIFIED_63x = "1672x1024x256";
    private static final String OPTICAL_RES_OPTIC = "0.38x0.38x0.38";
    private static final String PIXEL_RES_OPTIC = "512x512x445";
    
	private static final Logger logger = Logger.getLogger(UpgradeUserDataService.class);

	private enum AlignmentSpace {
	    UNIFIED_20X
	}
	
	private Map<AlignmentSpace,Entity> alignmentSpaces = new HashMap<AlignmentSpace,Entity>();
		
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model to latest version: "+serverVersion);

        createAlignmentSpaces();
        processSamples();
        processNeuronSeparationResults();
    }

    private void createAlignmentSpaces() throws Exception {
        
//        entityBean.createNewEntityType(EntityConstants.TYPE_ALIGNMENT_SPACE);
//        entityBean.createNewEntityAttr(EntityConstants.TYPE_ALIGNMENT_SPACE, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
//        entityBean.createNewEntityAttr(EntityConstants.TYPE_ALIGNMENT_SPACE, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
//        entityBean.createNewEntityAttr(EntityConstants.TYPE_ALIGNMENT_SPACE, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
//        entityBean.createNewEntityAttr(EntityConstants.TYPE_ALIGNMENT_SPACE, EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
//
//        entityBean.createNewEntityType(EntityConstants.TYPE_MASK_SET);
//        entityBean.createNewEntityAttr(EntityConstants.TYPE_MASK_SET, EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE);

        entityBean.createNewEntityAttr(EntityConstants.TYPE_NEURON_FRAGMENT, EntityConstants.ATTRIBUTE_MASK_IMAGE);
        entityBean.createNewEntityAttr(EntityConstants.TYPE_NEURON_FRAGMENT, EntityConstants.ATTRIBUTE_CHAN_IMAGE);
        
        List<Entity> unified20xList = entityBean.getEntitiesByNameAndTypeName("group:flylight", "Unified 20x Alignment Space", EntityConstants.TYPE_ALIGNMENT_SPACE);
        alignmentSpaces.put(AlignmentSpace.UNIFIED_20X, unified20xList.iterator().next());

    }
    
    private void processSamples() throws Exception {
        for(Entity entity : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SAMPLE)) {
            // This is intentionally not loaded into the looped entity because we would run out of memory
            Entity sample = entityBean.getEntityTree(entity.getId());
            if (sample!=null) { 
                logger.info("Processing sample "+sample.getName()+" ("+sample.getOwnerKey()+")");
                processSample(sample);
            }
        }
    }

    /**
     * For one sample:
     *    - Set alignment space for the alignment result
     *    - Set optical format for each alignment result
     *    - Set optical format for each aligned neuron separation
     */
    private void processSample(Entity sample) throws Exception {
        
        for(Entity pipelineRun : EntityUtils.getDescendantsOfType(sample, EntityConstants.TYPE_PIPELINE_RUN, true)) {
            
            for(Entity alignmentResult : EntityUtils.getDescendantsOfType(pipelineRun, EntityConstants.TYPE_ALIGNMENT_RESULT, true)) {
                
                setAlignmentSpace(sample, alignmentResult);
                
                Entity supportingFiles = EntityUtils.getSupportingData(alignmentResult);
                Entity separation = EntityUtils.findChildWithType(alignmentResult, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                
                Entity aligned20x = EntityUtils.findChildWithName(supportingFiles, "Aligned20xScale.v3dpbd");
                
                if (aligned20x != null) {
                    setOpticalResolution(aligned20x, OPTICAL_RES_UNIFIED);
                    setPixelResolution(aligned20x, PIXEL_RES_UNIFIED);
                    Entity aligned63x = EntityUtils.findChildWithName(supportingFiles, "Aligned63xScale.v3dpbd");
                    if (aligned63x!=null) {
                        setOpticalResolution(aligned63x, OPTICAL_RES_UNIFIED_63x);
                        setPixelResolution(aligned63x, PIXEL_RES_UNIFIED_63x);
                        setOpticalResolution(separation, OPTICAL_RES_UNIFIED_63x);
                        setPixelResolution(separation, PIXEL_RES_UNIFIED_63x);
                    }
                }
                else {
                    Entity aligned = EntityUtils.findChildWithName(supportingFiles, "Aligned.v3dpbd");
                    if (aligned!=null) {
                        if ("Optic Lobe Alignment".equals(alignmentResult.getName())) {
                            setOpticalResolution(aligned, OPTICAL_RES_OPTIC);
                            setPixelResolution(aligned, PIXEL_RES_OPTIC);
                            setOpticalResolution(separation, OPTICAL_RES_OPTIC);
                            setPixelResolution(separation, PIXEL_RES_OPTIC);
                        }
                        else if ("Whole Brain 63x Alignment".equals(alignmentResult.getName())) {
                            // Ignore. These need to be redone anyway. 
                        }
                        else {
                            setOpticalResolution(aligned, OPTICAL_RES_UNIFIED);
                            setPixelResolution(aligned, PIXEL_RES_UNIFIED);
                            setOpticalResolution(separation, OPTICAL_RES_UNIFIED);
                            setPixelResolution(separation, PIXEL_RES_UNIFIED);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * For each Neuron Separation:
     *     1) Change "Neuron Fragments" to "Mask Entity Collection"
     *     2) Set a Default 3d Image for each Neuron Separation
     */
    private void processNeuronSeparationResults() throws Exception {

        for(Entity entity : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
            // This is intentionally not loaded into the looped entity because we would run out of memory
            Entity separation = entityBean.getEntityTree(entity.getId());
            logger.info("Processing separation "+separation.getName()+" ("+separation.getOwnerKey()+")");
            
            Entity supportingFiles = EntityUtils.getSupportingData(separation);
            if (supportingFiles==null) {
                // Skip invalid separations
                continue;
            }
            
            if (separation.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION)==null) {
                EntityData nfcEd = EntityUtils.findChildEntityDataWithType(separation, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
                if (nfcEd!=null) {
                    nfcEd.setEntityAttribute(entityBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION));
                    entityBean.saveOrUpdateEntityData(nfcEd);    
                }
            }
            
            Entity signalVolume = EntityUtils.findChildWithName(separation, "ConsolidatedSignal.v3dpbd");
            if (signalVolume!=null) {
                entityHelper.setImageIfNecessary(separation, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,  signalVolume);
                Entity fastLoadFolder = EntityUtils.findChildWithName(separation, "Fast Load");    
                if (fastLoadFolder!=null) {
                    Entity fastSignal = EntityUtils.findChildWithName(separation, "ConsolidatedSignal2_25.mp4");        
                    entityHelper.setImageIfNecessary(signalVolume, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, fastSignal);
                }
            }
        }
    }
    
    private void setAlignmentSpace(Entity sampleEntity, Entity alignmentResult) throws Exception {

        if ("Optic Lobe Alignment".equals(alignmentResult.getName())) {
            // No longer supporting old optic lobe alignments
        }
        else if ("Whole Brain 63x Alignment".equals(alignmentResult.getName())) {
            // No longer supporting padded alignments
        }
        else {
            setAlignmentSpace(alignmentResult, AlignmentSpace.UNIFIED_20X);
        }
    }

    public void setOpticalResolution(Entity entity, String opticalRes) throws Exception {
        if (entity==null) return;
        entityHelper.setOpticalResolution(entity, opticalRes);
        logger.info("Set optical resolution to "+opticalRes+" on "+entity.getName());
    }

    public void setPixelResolution(Entity entity, String pixelRes) throws Exception {
        if (entity==null) return;
        entityHelper.setPixelResolution(entity, pixelRes);
        logger.info("Set pixel resolution to "+pixelRes+" on "+entity.getName());
    }
    
    private void setAlignmentSpace(Entity entity, AlignmentSpace alignmentSpace) throws Exception {
        if (entity==null || alignmentSpace==null) return;
        if (EntityUtils.findChildWithType(entity, EntityConstants.TYPE_ALIGNMENT_SPACE)!=null) return;
        Entity alignmentSpaceEntity = alignmentSpaces.get(alignmentSpace);
        entityHelper.setAlignmentSpace(entity, alignmentSpaceEntity.getName());
        logger.info("Set alignment space to "+alignmentSpaceEntity.getName()+" on "+entity.getName());
    }
}
