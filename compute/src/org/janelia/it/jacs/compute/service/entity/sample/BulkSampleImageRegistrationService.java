package org.janelia.it.jacs.compute.service.entity.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BulkSampleImageRegistrationService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(BulkSampleImageRegistrationService.class);

//    public transient static final String PARAM_testRun = "is test run";
	
    protected int numSamples;
    
    private Set<Long> visited = new HashSet<>();
    private boolean isDebug = false;
    
    public void execute() throws Exception {
        logger.info("Running sample image registration for all "+ownerKey+" samples");
        
        if (isDebug) {
        	logger.info("This is a test run. No entities will be moved or deleted.");
        }
        else {
        	logger.info("This is the real thing. Entities will be moved and/or deleted!");
        }
        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (StringUtils.isEmpty(sampleEntityId)) {
        	List<Entity> samples = entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE);
        	if (null==samples) {
				logger.info("User "+ownerKey+" has null returned for samples");
				return;
			}
			logger.info("Processing "+samples.size()+" samples");
            int counter = 0;
			for(Entity sample : samples) {
                try {
                    counter++;
					logger.info("Processing images for sample "+counter+" of "+samples.size());
					processSample(sample);
                }
                catch (Exception e) {
                    logger.error("Error processing sample: "+sample.getName(),e);
                }
            }    		
    	}
    	else {
    		logger.info("Processing single sample: "+sampleEntityId);
    		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
            processSample(sampleEntity);
    	}

		logger.info("Processed "+numSamples+" samples.");
    }
    
    public void processSample(Entity sample) throws Exception {
    	
    	if (!sample.getOwnerKey().equals(ownerKey)) return;
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		logger.info("Processing "+sample.getName()+" (id="+sample.getId()+")");
		numSamples++;
		
		if (deleteSampleIfUnreferencedByOwner(sample)) {
			return;
		}

		upgradeSample(entityBean.getEntityTree(sample.getId()));
    }
    
    private boolean deleteSampleIfUnreferencedByOwner(Entity sample) throws ComputeException {
    	
    	for(EntityData ed : entityBean.getParentEntityDatas(sample.getId())) {
    		if (ed.getOwnerKey().equals(sample.getOwnerKey())) {
    			return false;
    		}
    	}
    	
    	logger.warn("  Sample is not referenced by owner: "+sample.getName()+" (id="+sample.getId()+")");

    	long numAnnotated = annotationBean.getNumDescendantsAnnotated(sample.getId());
    	if (numAnnotated>0) {
    		logger.warn("  Cannnot delete sample because "+numAnnotated+" descendants are annotated");
    		return false;
    	}
    	
    	logger.warn("  Removing unreferenced sample entirely: "+sample.getId());
    	
    	if (!isDebug) {
    		entityBean.deleteEntityTreeById(sample.getOwnerKey(), sample.getId(), true);
    	}
        
    	return true;
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void upgradeSample(Entity sample) throws Exception {
        ResultImageRegistrationService resultImageRegService = new ResultImageRegistrationService();
        
    	for(Entity pipelineRun : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN)) {    	    
    		for(EntityData pred : pipelineRun.getOrderedEntityData()) {
    			if (!pred.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
    				continue;
    			}
    			Entity result = pred.getChildEntity();

    			logger.info("  Processing result: "+result.getName()+" (id="+result.getId()+")");

                if (!isDebug) {
                    resultImageRegService.execute(processData, pipelineRun, result, null);
                }
    		}
    	}
    	
//    	logger.info("Registered Images for Sample:");
//    	printSample(sample);
    }

//    private void printSample(Entity sample) {
//
//    	logger.info(""+sample);
//    	printImages("  ",sample);
//    	
//    	for(Entity pipelineRun : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN)) {
//    		
//    		logger.info("    "+pipelineRun.getName());
//    		printImages("      ",pipelineRun);
//    		
//    		for(EntityData pred : pipelineRun.getOrderedEntityData()) {
//    			if (!pred.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
//    				continue;
//    			}
//    			Entity result = pred.getChildEntity();
//        		logger.info("        "+result.getName());
//        		printImages("          ",result);
//    		}
//    	}
//    }
//
//    private void printImages(String indent, Entity entity) {
//
//    	EntityData default3dImage = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
//    	if (default3dImage!=null) {
//    		logger.info(indent+"Default3d: "+new File(default3dImage.getValue()).getName());
//    		
//        	EntityData fast3dImage = default3dImage.getChildEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
//        	if (fast3dImage!=null) {
//        		logger.info(indent+"Fast3d: "+new File(fast3dImage.getValue()).getName());
//        	}
//    	}
//    	
//    	EntityData default2dImage = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
//    	if (default2dImage!=null) {
//    		logger.info(indent+"Default2d: "+new File(default2dImage.getValue()).getName());
//    	}
//    	
//    	EntityData signalMip = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE);
//    	if (signalMip!=null) {
//    		logger.info(indent+"SignalMip: "+new File(signalMip.getValue()).getName());
//    	}
//    	
//    	EntityData refMip = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE);
//    	if (refMip!=null) {
//    		logger.info(indent+"ReferenceMip: "+new File(refMip.getValue()).getName());
//    	}
//    }
    
}
