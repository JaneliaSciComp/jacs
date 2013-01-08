package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Find the latest sample processing entity in the given sample, and add it to the given pipeline run.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReuseSampleProcessingResultService extends AbstractEntityService {
	
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
        Entity latestSp = null;
        
        for(Entity pipelineRun : sampleEntity.getOrderedChildren()) {
            if (!pipelineRun.getEntityType().getName().equals(EntityConstants.TYPE_PIPELINE_RUN)) {
                continue;
            }
            if (pipelineRun.getId().toString().equals(pipelineRunId)) {
                myPipelineRun = pipelineRun;
            }
            EntityData sped = EntityUtils.findChildEntityDataWithType(pipelineRun, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT);
            if (sped!=null) {
                Entity sp = sped.getChildEntity();
                if (sp!=null) {
                    latestSp = sp;
                }
            }
        }
        
        if (myPipelineRun==null) {
            throw new Exception("Cannot find pipeline run with id="+pipelineRunId);
        }
        
        if (latestSp!=null) {
            String stitchedFilename = latestSp.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
            if (stitchedFilename!=null) {
                entityBean.addEntityToParent(ownerKey, myPipelineRun.getId(), latestSp.getId(), myPipelineRun.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);        
                entityBean.saveOrUpdateEntity(myPipelineRun);
                logger.info("Reusing sample processing result "+latestSp.getId()+" for new pipeline run "+pipelineRunId);
                processData.putItem("STITCHED_FILENAME", stitchedFilename);    
                logger.info("Putting '"+stitchedFilename+"' in STITCHED_FILENAME");
                processData.putItem("RUN_PROCESSING", Boolean.FALSE);    
                logger.info("Putting '"+Boolean.FALSE+"' in RUN_PROCESSING");
            }
            else {
                logger.warn("Sample processing result has no default 3d image path: "+latestSp.getId());
            }
        }
        else {
            logger.info("No existing sample processing available for reuse for sample: "+sampleEntityId);
        }
    }
}
