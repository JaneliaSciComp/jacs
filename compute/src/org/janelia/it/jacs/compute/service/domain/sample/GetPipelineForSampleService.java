package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.Sample;

/**
 * Get the pipeline process file configured for given sample or parent sample.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetPipelineForSampleService extends AbstractDomainService {
	
    public void execute() throws Exception {

        final Sample sampleEntity = domainHelper.getRequiredSample(data);

        String dataSetIdentifier = sampleEntity.getDataSet();
        DataSet dataSet = domainDao.getDataSetByIdentifier(ownerKey, dataSetIdentifier);
        
        List<String> pipelineNames = dataSet.getPipelineProcesses();
        contextLogger.info("data set pipeline process list is: " + pipelineNames);
        
        String pipelineName = pipelineNames.get(0);
        String processDefName = "PipelineConfig_"+pipelineName;
        processData.putItem("PROCESS_DEF_NAME", processDefName);    
        contextLogger.info("Putting "+processDefName+" in PROCESS_DEF_NAME");
    }
}
