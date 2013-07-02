package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.align.ParameterizedAlignmentAlgorithm;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Find the latest alignment entity in the given sample, and add it to the given pipeline run.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReuseAlignmentResultService extends AbstractEntityService {
	
    public void execute() throws Exception {

        ParameterizedAlignmentAlgorithm paa = (ParameterizedAlignmentAlgorithm)processData.getItem("PARAMETERIZED_ALIGNMENT_ALGORITHM");
        
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
        Entity latestAr = null;
        
        for(Entity pipelineRun : sampleEntity.getOrderedChildren()) {
            if (pipelineRun.getEntityType().getName().equals(EntityConstants.TYPE_PIPELINE_RUN)) {
                if (pipelineRun.getId().toString().equals(pipelineRunId)) {
                    myPipelineRun = pipelineRun;
                }
                for(Entity ar : pipelineRun.getChildren()) {
                    if (ar.getEntityType().getName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                        if (paa.getResultName().equals(ar.getName())) {
                            latestAr = ar;
                        }
                    }
                }
            }
        }
        
        if (myPipelineRun==null) {
            throw new Exception("Cannot find pipeline run with id="+pipelineRunId);
        }
        
        if (latestAr!=null) {
                entityBean.addEntityToParent(ownerKey, myPipelineRun.getId(), latestAr.getId(), myPipelineRun.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);        
                entityBean.saveOrUpdateEntity(myPipelineRun);
                logger.info("Reusing alignment result "+latestAr.getId()+" for "+latestAr.getName()+" in new pipeline run "+pipelineRunId);
                processData.putItem("RESULT_ENTITY", latestAr);
                logger.info("Putting '"+latestAr+"' in RESULT_ENTITY");
                processData.putItem("RESULT_ENTITY_ID", latestAr.getId().toString());
                logger.info("Putting '"+latestAr.getId()+"' in RESULT_ENTITY_ID");
                String alignedFilename = latestAr.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                processData.putItem("ALIGNED_FILENAME", alignedFilename);    
                logger.info("Putting '"+alignedFilename+"' in ALIGNED_FILENAME");
                processData.putItem("RUN_ALIGNMENT", Boolean.FALSE);    
                logger.info("Putting '"+Boolean.FALSE+"' in RUN_ALIGNMENT");
        }
        else {
            logger.info("No existing alignment with name '"+paa.getResultName()+"' available for reuse for sample: "+sampleEntityId);
        }
    }
}
