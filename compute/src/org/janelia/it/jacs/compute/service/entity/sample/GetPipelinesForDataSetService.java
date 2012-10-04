package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Gets all the pipeline names for a given DATA_SET_IDENTIFIER, and returns them as a list called PIPELINE_PROCESS_NAME.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetPipelinesForDataSetService extends AbstractEntityService {

    public void execute() throws Exception {
        
    	String dataSetIdentifier = (String)processData.getItem("DATA_SET_IDENTIFIER");
    	if (StringUtils.isEmpty(dataSetIdentifier)) {
    		throw new IllegalArgumentException("DATA_SET_IDENTIFIER may not be null");
    	}
    	
    	List<Entity> dataSets = entityBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier);
    	
    	if (dataSets.isEmpty()) {
    		throw new IllegalArgumentException("Could not find a data set with identifier="+dataSetIdentifier);
    	}
    	
    	if (dataSets.size()>1) {
    		logger.warn("Found more than one data set with identifier="+dataSetIdentifier);
    	}
    	
    	Entity dataSet = dataSets.get(0);
    	String pipelineProcess = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
    	logger.info("Putting '"+pipelineProcess+"' in PIPELINE_PROCESS_NAME");
    	processData.putItem("PIPELINE_PROCESS_NAME", Task.listOfStringsFromCsvString(pipelineProcess));
    }
}
