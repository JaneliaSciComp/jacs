package org.janelia.it.jacs.compute.service.entity.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.TaskParameter;

/**
 * Print the sample being processed.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleDebuggerService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(SampleDebuggerService.class);

	public void execute() throws Exception {
		
		contextLogger.info("----------------------------------------------------------");
		contextLogger.info("Task Id: "+task.getObjectId());
		contextLogger.info("Task Name: "+task.getDisplayName());
		contextLogger.info("Task Parameters: ");
		for (TaskParameter parameter : task.getTaskParameterSet()) {
			contextLogger.info("    "+parameter.getName()+": "+parameter.getValue());
		}
        
    	String sampleEntityId = data.getRequiredItemAsString("SAMPLE_ENTITY_ID");
		contextLogger.info("Sample Id: "+sampleEntityId);
		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		logger.warn("Sample entity not found with id="+sampleEntityId);
    	}
    	else {
    		String dataSetIdentifier = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    		String objective = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
    		
    		contextLogger.info("    Name: "+sampleEntity.getName());
    		if (dataSetIdentifier!=null) {
    			contextLogger.info("    Data Set: "+dataSetIdentifier);
    		}
    		if (objective!=null) {
    			contextLogger.info("    Objective: "+objective);
    		}
    	}
        
    }
}
