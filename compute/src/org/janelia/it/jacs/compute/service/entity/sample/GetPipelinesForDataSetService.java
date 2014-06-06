package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
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
        
        String dataSetIdentifier = data.getRequiredItemAsString("DATA_SET_IDENTIFIER");

        Entity dataSet = annotationBean.getUserDataSetByIdentifier(dataSetIdentifier);

        if (dataSet==null) {
            throw new IllegalArgumentException("Data does not exist with identifier: "+dataSetIdentifier);
        }
        else {
            List<String> processNames = new ArrayList<>();
            String pipelineProcess = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
            contextLogger.info("data set pipeline process list is: " + pipelineProcess);
            for(String process : Task.listOfStringsFromCsvString(pipelineProcess)) {
                if (! StringUtils.isEmpty(process)) {
                    processNames.add("PipelineConfig_" + process);
                }
            }
            data.putItem("PIPELINE_PROCESS_NAME", processNames);
        }
    }
}
