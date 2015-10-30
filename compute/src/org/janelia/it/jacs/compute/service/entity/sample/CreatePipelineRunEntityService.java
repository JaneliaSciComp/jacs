package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Creates a new pipeline run entity under the given sample.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreatePipelineRunEntityService extends AbstractEntityService {
	
    public void execute() throws Exception {
        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (StringUtils.isEmpty(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}

    	String pipelineName = (String)processData.getItem("PIPELINE_NAME");
    	if (StringUtils.isEmpty(pipelineName)) {
    		pipelineName = "Pipeline";
    	}
    	
    	String pipelineRunName = pipelineName+" Results";
        
    	String pipelineProcess = (String)processData.getItem("PIPELINE_PROCESS");
    	if (StringUtils.isEmpty(pipelineProcess)) {
    		throw new IllegalArgumentException("PIPELINE_PROCESS may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(new Long(sampleEntityId));
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	Entity pipelineRun = entityBean.createEntity(ownerKey, EntityConstants.TYPE_PIPELINE_RUN, pipelineRunName);
    	pipelineRun.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS, pipelineProcess);
    	
    	entityBean.saveOrUpdateEntity(pipelineRun);
    	entityBean.addEntityToParent(ownerKey, sampleEntity.getId(), pipelineRun.getId(), sampleEntity.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);

    	contextLogger.info("Added new pipeline run to sample "+sampleEntity);
    	
    	processData.putItem("PIPELINE_RUN_ENTITY_ID", pipelineRun.getId().toString());
    	contextLogger.info("Putting '"+pipelineRun.getId()+"' in PIPELINE_RUN_ENTITY_ID");
    }
}
