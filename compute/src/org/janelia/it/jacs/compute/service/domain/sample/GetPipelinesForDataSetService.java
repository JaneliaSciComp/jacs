package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Gets all the pipeline names for a given DATA_SET_IDENTIFIER, and returns them as a list called PIPELINE_PROCESS_NAME.
 * 
 * If REUSE_PIPELINE_RUNS is specified and is true, then the SAMPLE_ENTITY_ID is checked to see which pipelines have not been 
 * run on it to completion, and only those pipelines are returned as part of the PIPELINE_PROCESS_NAME list.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetPipelinesForDataSetService extends AbstractDomainService {

    public void execute() throws Exception {
        
        String dataSetIdentifier = data.getRequiredItemAsString("DATA_SET_IDENTIFIER");
        boolean reusePipelineRuns = data.getItemAsBoolean("REUSE_PIPELINE_RUNS");

        DataSet dataSet = domainDao.getDataSetByIdentifier(ownerKey, dataSetIdentifier);

        if (dataSet==null) {
            throw new IllegalArgumentException("Data does not exist with identifier: "+dataSetIdentifier);
        }
        else {
            List<String> processNames = new ArrayList<String>();
            List<String> pipelineNames = dataSet.getPipelineProcesses();
            contextLogger.info("data set pipeline process list is: " + pipelineNames);
            
            Set<String> skippedPipelines = new HashSet<String>();
            
            if (reusePipelineRuns) {
            	// If we don't want to rerun pipelines which already have results, then we have to check if the results are there
            	Long sampleEntityId = data.getRequiredItemAsLong("SAMPLE_ENTITY_ID");
            	Sample sample = domainDao.getDomainObject(ownerKey, Sample.class, new Long(sampleEntityId));
            	skippedPipelines.addAll(getPipelinesHavingSuccessfulRuns(sample, pipelineNames));
            }
            
            for(String processName : pipelineNames) {
                if (! StringUtils.isEmpty(processName)) {
                	if (skippedPipelines.contains(processName)) {
                		contextLogger.info("Skipping "+processName+" because successful run already exists");
                	}
                	else {
                		processNames.add("PipelineConfig_" + processName);
                	}
                }
            }
            data.putItem("PIPELINE_PROCESS_NAME", processNames);
        }
    }

    private Set<String> getPipelinesHavingSuccessfulRuns(Sample sample, List<String> pipelineNames) throws Exception {
    	
    	Set<String> successfulPipelines = null;
    	List<ObjectiveSample> objectiveSamples = sample.getObjectiveSamples();
		// This is a parent sample, process the sub-samples
		for (ObjectiveSample objectiveSample : objectiveSamples) {
			Set<String> successfulSubPipelines = getPipelinesHavingSuccessfulRuns(objectiveSample, pipelineNames);
			if (successfulPipelines==null) {
				successfulPipelines = new HashSet<String>(successfulSubPipelines);
			}
			else {
				// Assumption: all sub-samples must have a successful run for any given pipeline to be considered a success
				successfulPipelines.retainAll(successfulSubPipelines);
			}
		}
		return successfulPipelines;
    }
    
    private Set<String> getPipelinesHavingSuccessfulRuns(ObjectiveSample objectiveSample, List<String> pipelineNames) throws Exception {
        
        Set<String> successfulPipelines = new HashSet<String>();
        if (!objectiveSample.hasPipelineRuns()) return successfulPipelines;
        
        for(SamplePipelineRun pipelineRun : objectiveSample.getPipelineRuns()) {
            String processName = pipelineRun.getPipelineProcess();
            // Do we care about this pipeline?
            if (pipelineNames.contains(processName)) {
                // Is the run a success?
                if (!pipelineRun.hasError()) {
                    successfulPipelines.add(processName);
                }
            }
        }
        
        return successfulPipelines;
    }
}
