package org.janelia.it.jacs.compute.service.entity.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeException;
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
        contextLogger.info("Running sample image registration for all "+ownerKey+" samples");
        
        if (isDebug) {
        	contextLogger.info("This is a test run. No entities will be moved or deleted.");
        }
        else {
        	contextLogger.info("This is the real thing. Entities will be moved and/or deleted!");
        }
        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (StringUtils.isEmpty(sampleEntityId)) {
        	List<Entity> samples = entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE);
        	if (null==samples) {
				contextLogger.info("User "+ownerKey+" has null returned for samples");
				return;
			}
			contextLogger.info("Processing "+samples.size()+" samples");
            int counter = 0;
			for(Entity sample : samples) {
                try {
                    counter++;
					contextLogger.info("Processing images for sample "+counter+" of "+samples.size());
					processSample(sample);
                }
                catch (Exception e) {
                    logger.error("Error processing sample: "+sample.getName(),e);
                }
            }    		
    	}
    	else {
    		contextLogger.info("Processing single sample: "+sampleEntityId);
    		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
            processSample(sampleEntity);
    	}

		contextLogger.info("Processed "+numSamples+" samples.");
    }
    
    public void processSample(Entity sample) throws Exception {
    	
    	if (!sample.getOwnerKey().equals(ownerKey)) return;
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		contextLogger.info("Processing "+sample.getName()+" (id="+sample.getId()+")");
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

    			contextLogger.info("  Processing result: "+result.getName()+" (id="+result.getId()+")");

                if (!isDebug) {
                    resultImageRegService.execute(processData, pipelineRun, result, null);
                }
    		}
    	}
    }
}
