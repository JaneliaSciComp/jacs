package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Gets all the pipeline names for a given DATA_SET_IDENTIFIER, and returns them as a list called PIPELINE_PROCESS_NAME.
 * 
 * If REUSE_PIPELINE_RUNS is specified and is true, then the SAMPLE_ENTITY_ID is checked to see which pipelines have not been 
 * run on it to completion, and only those pipelines are returned as part of the PIPELINE_PROCESS_NAME list.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetPipelinesForDataSetService extends AbstractEntityService {

    public void execute() throws Exception {
        
        String dataSetIdentifier = data.getRequiredItemAsString("DATA_SET_IDENTIFIER");
        boolean reusePipelineRuns = data.getItemAsBoolean("REUSE_PIPELINE_RUNS");
        
        Entity dataSet = annotationBean.getUserDataSetByIdentifier(dataSetIdentifier);

        if (dataSet==null) {
            throw new IllegalArgumentException("Data does not exist with identifier: "+dataSetIdentifier);
        }
        else {
            List<String> processNames = new ArrayList<String>();
            String pipelineProcess = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
            contextLogger.info("data set pipeline process list is: " + pipelineProcess);
            
            List<String> pipelineNames = Task.listOfStringsFromCsvString(pipelineProcess);
            

            Set<String> skippedPipelines = new HashSet<String>();
            
            if (reusePipelineRuns) {
            	// If we don't want to rerun pipelines which already have results, then we have to check if the results are there
            	Long sampleEntityId = data.getRequiredItemAsLong("SAMPLE_ENTITY_ID");
            	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
            	skippedPipelines.addAll(getPipelinesHavingSuccessfulRuns(populateChildren(sampleEntity), pipelineNames));
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

    private Set<String> getPipelinesHavingSuccessfulRuns(Entity sample, List<String> pipelineNames) throws Exception {
    	
    	Set<String> successfulPipelines = null;

    	List<Entity> subSamples = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
    	if (!subSamples.isEmpty()) {
    		// This is a parent sample, process the sub-samples
    		for (Entity subSample : subSamples) {
    			Set<String> successfulSubPipelines = getPipelinesHavingSuccessfulRuns(populateChildren(subSample), pipelineNames);
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
    	else {
    		successfulPipelines = new HashSet<String>();
    	}
    	
    	List<Entity> runs = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
    	if (runs.isEmpty()) return successfulPipelines;
    	
        for(Entity pipelineRun : runs) {
            String processName = pipelineRun.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
            // Do we care about this pipeline?
            if (pipelineNames.contains(processName)) {
	            populateChildren(pipelineRun);
	            // Is the run a success?
	            if (EntityUtils.getLatestChildOfType(pipelineRun, EntityConstants.TYPE_ERROR)==null) {
	            	successfulPipelines.add(processName);
	            }
            }
        }
        
    	return successfulPipelines;
    }
}
