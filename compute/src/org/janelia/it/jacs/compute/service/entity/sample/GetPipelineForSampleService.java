package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Get the pipeline process file configured for given sample or parent sample.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetPipelineForSampleService extends AbstractEntityService {
	
    public void execute() throws Exception {

        final Entity sampleEntity = entityHelper.getRequiredSampleEntity(data);

        String dataSetIdentifier = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        
        if (dataSetIdentifier==null) {
            Entity parentSampleEntity = entityBean.getAncestorWithType(sampleEntity, EntityConstants.TYPE_SAMPLE);
            dataSetIdentifier = parentSampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        }
        
        Entity dataSet = annotationBean.getUserDataSetByIdentifier(dataSetIdentifier);
        
        String pipelineProcess = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
        contextLogger.info("data set pipeline process list is: " + pipelineProcess);
        List<String> pipelineNames = Task.listOfStringsFromCsvString(pipelineProcess);
        
        String pipelineName = pipelineNames.get(0);
        String processDefName = "PipelineConfig_"+pipelineName;
        processData.putItem("PROCESS_DEF_NAME", processDefName);    
        contextLogger.info("Putting "+processDefName+" in PROCESS_DEF_NAME");
    }
}
