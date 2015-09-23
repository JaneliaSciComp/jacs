package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Gets all the RESULT entities in the pipeline run given by a PIPELINE_RUN_ENTITY_ID.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetPipelineRunResultsService extends AbstractEntityService {

    public void execute() throws Exception {
        	
    	String pipelineRunEntityId = (String)processData.getItem("PIPELINE_RUN_ENTITY_ID");
    	if (StringUtils.isEmpty(pipelineRunEntityId)) {
    		throw new IllegalArgumentException("PIPELINE_RUN_ENTITY_ID may not be null");
    	}

    	Entity pipelineRun = entityBean.getEntityById(pipelineRunEntityId);
    	populateChildren(pipelineRun);
    	
    	List<String> resultIds = new ArrayList<String>();
    	for(EntityData ed : EntityUtils.getOrderedEntityDataForAttribute(pipelineRun, EntityConstants.ATTRIBUTE_RESULT)) {
    		if (ed.getChildEntity()!=null) {
    			resultIds.add(ed.getChildEntity().getId().toString());
    		}
    	}

    	contextLogger.info("Putting "+resultIds.size()+" result ids in RESULT_ENTITY_ID");
    	processData.putItem("RESULT_ENTITY_ID", resultIds);
    }
}
