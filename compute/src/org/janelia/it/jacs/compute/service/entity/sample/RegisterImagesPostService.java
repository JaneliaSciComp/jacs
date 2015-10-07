package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Re-run all images registrations for all results in a given sample.
 *   
 * Meant to be run with the GSPS_ApplyProcessToSamples script, but using the "parent" setting. If "children" is used, 
 * then the objectives could be processed out of order, and the wrong image may end up assigned at the top-level sample.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RegisterImagesPostService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(RegisterImagesPostService.class);
    
    public void execute() throws Exception {

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (sampleEntityId == null || "".equals(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }
        
        Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityTypeName())) {
            throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
        }
        
        contextLogger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");

        rerunRegistrationsForSample(sampleEntity);
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void rerunRegistrationsForSample(Entity sample) throws Exception {
          
        ResultImageRegistrationService resultImageRegService = new ResultImageRegistrationService();
        
        populateChildren(sample);
        
        List<Entity> subsamples = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
        
        List<Entity> runs = null;
        if (subsamples.isEmpty()) {
            runs = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
        }
        else {
            runs = new ArrayList<Entity>();
            for(Entity subsample : subsamples) {
                populateChildren(subsample);
                runs.addAll(EntityUtils.getChildrenOfType(subsample, EntityConstants.TYPE_PIPELINE_RUN));
            }
        }
        
    	for(Entity pipelineRun : runs) {
    	    populateChildren(pipelineRun);
    		for(EntityData pred : pipelineRun.getOrderedEntityData()) {
    			if (!pred.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
    				continue;
    			}
    			Entity result = pred.getChildEntity();

    			contextLogger.info("  Processing result: "+result.getName()+" (id="+result.getId()+")");
                resultImageRegService.execute(processData, pipelineRun, result, null);
    		}
    	}
    }
}
