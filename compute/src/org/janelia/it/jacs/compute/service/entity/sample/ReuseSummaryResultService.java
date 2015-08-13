package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Find the latest summary entity in the given sample, and add it to the given pipeline run.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReuseSummaryResultService extends AbstractEntityService {
	
    public void execute() throws Exception {

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (StringUtils.isEmpty(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }

        String pipelineRunId = (String)processData.getItem("PIPELINE_RUN_ENTITY_ID");
        if (StringUtils.isEmpty(pipelineRunId)) {
            throw new IllegalArgumentException("PIPELINE_RUN_ENTITY_ID may not be null");
        }
        
        Entity sampleEntity = entityBean.getEntityTree(new Long(sampleEntityId));
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }

        Entity myPipelineRun = null;
        Entity latestLsr = null;
        
        for(Entity pipelineRun : sampleEntity.getOrderedChildren()) {
            if (pipelineRun.getEntityTypeName().equals(EntityConstants.TYPE_PIPELINE_RUN)) {
                if (pipelineRun.getId().toString().equals(pipelineRunId)) {
                    myPipelineRun = pipelineRun;
                }
                List<Entity> results = pipelineRun.getOrderedChildren();
                Collections.reverse(results);
                for(Entity result : results) {
                    if (result.getEntityTypeName().equals(EntityConstants.TYPE_LSM_SUMMARY_RESULT)) {
                        latestLsr = result;
                    }
                }
            }
        }
        
        if (myPipelineRun==null) {
            throw new Exception("Cannot find pipeline run with id="+pipelineRunId);
        }
        
        if (latestLsr!=null) {
                entityBean.addEntityToParent(ownerKey, myPipelineRun.getId(), latestLsr.getId(), myPipelineRun.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);        
                entityBean.saveOrUpdateEntity(myPipelineRun);
                logger.info("Reusing alignment result "+latestLsr.getId()+" for "+latestLsr.getName()+" in new pipeline run "+pipelineRunId);
                processData.putItem("RESULT_ENTITY", latestLsr);
                logger.info("Putting '"+latestLsr+"' in RESULT_ENTITY");
                processData.putItem("RESULT_ENTITY_ID", latestLsr.getId().toString());
                logger.info("Putting '"+latestLsr.getId()+"' in RESULT_ENTITY_ID");
                processData.putItem("RUN_SUMMARY", Boolean.FALSE);    
                logger.info("Putting '"+Boolean.FALSE+"' in RUN_SUMMARY");
        }
        else {
            logger.info("No existing LSM summary available for reuse for sample: "+sampleEntityId);
        }
    }
}
