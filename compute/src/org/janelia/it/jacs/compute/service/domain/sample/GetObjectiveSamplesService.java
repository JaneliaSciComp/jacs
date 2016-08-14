package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Extracts sub-samples based on their objective.
 * 
 * If RUN_OBJECTIVES is provided as a CSV list of objectives (e.g. "20x,63x") then anything that is not in the list is not run. Otherwise all objectives are run.
 * 
 * The pipelines to run for each objective are given by:
 * PIPELINES_TO_RUN_20X, PIPELINES_TO_RUN_40X. and PIPELINES_TO_RUN_63X
 * 
 * If a sub-sample already has results for all the pipelines given by its respective variable then its id is not output, 
 * so that it is not run by subsequent processing.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetObjectiveSamplesService extends AbstractDomainService {

	private Long sampleId;
	private List<String> pipelines20x = null; 
	private List<String> pipelines40x = null; 
	private List<String> pipelines63x = null;
	private boolean run20x = true;
	private boolean run40x = true;
	private boolean run63x = true;
	
    public void execute() throws Exception {

        boolean reusePipelineRuns = data.getItemAsBoolean("REUSE_PIPELINE_RUNS");
		sampleId = data.getItemAsLong("SAMPLE_ENTITY_ID");
    	
    	String objectiveList = data.getItemAsString("RUN_OBJECTIVES");
    	if (objectiveList!=null) {
    	    contextLogger.info("Will only run the objectives provided by RUN_OBJECTIVES: "+objectiveList);
    		Set<String> objectiveSet = new HashSet<String>(Task.listOfStringsFromCsvString(objectiveList));
			if (!objectiveSet.contains(Objective.OBJECTIVE_20X.getName())) {
				run20x = false;
			}
			if (!objectiveSet.contains(Objective.OBJECTIVE_40X.getName())) {
				run40x = false;
			}
			if (!objectiveSet.contains(Objective.OBJECTIVE_63X.getName())) {
				run63x = false;
			}
    	}
    	
    	if (run20x) {
			String pipelineStr20x = data.getItemAsString("PIPELINES_TO_RUN_20X");
			if (pipelineStr20x!=null) {
			    contextLogger.info("Will run these 20x pipelines: "+pipelineStr20x);
				pipelines20x = Task.listOfStringsFromCsvString(pipelineStr20x);
			}
    	}
    	
    	if (run40x) {
			String pipelineStr40x = data.getItemAsString("PIPELINES_TO_RUN_40X");
			if (pipelineStr40x!=null) {
			    contextLogger.info("Will run these 40x pipelines: "+pipelineStr40x);
				pipelines40x = Task.listOfStringsFromCsvString(pipelineStr40x);
			}
    	}
    	
    	if (run63x) {
			String pipelineStr63x = data.getItemAsString("PIPELINES_TO_RUN_63X");
			if (pipelineStr63x!=null) {
			    contextLogger.info("Will run these 63x pipelines: "+pipelineStr63x);
				pipelines63x = Task.listOfStringsFromCsvString(pipelineStr63x);
			}
    	}
	
        final Sample sampleEntity = domainHelper.getRequiredSample(data);
    
        String sampleIdStr = sampleEntity.getId().toString();
        data.putItem("PARENT_SAMPLE_ID", sampleIdStr);

        String objective;
        for (ObjectiveSample objectiveSample : sampleEntity.getObjectiveSamples()) {
            objective = objectiveSample.getObjective();
            if (run20x && Objective.OBJECTIVE_20X.getName().equals(objective)) {
            	if (!reusePipelineRuns || !sampleHasAllPipelines(objectiveSample, pipelines20x)) {
                    data.putItem("SAMPLE_20X_ID", sampleIdStr);
            	}
            } 
            else if (run40x && Objective.OBJECTIVE_40X.getName().equals(objective)) {
                if (!reusePipelineRuns || !sampleHasAllPipelines(objectiveSample, pipelines40x)) {
                    data.putItem("SAMPLE_40X_ID", sampleIdStr);
            	}
            } 
            else if (run63x && Objective.OBJECTIVE_63X.getName().equals(objective)) {
            	if (!reusePipelineRuns || !sampleHasAllPipelines(objectiveSample, pipelines63x)) {
                    data.putItem("SAMPLE_63X_ID", sampleIdStr);
            	}
            }
        }

    }

	private boolean sampleHasAllPipelines(ObjectiveSample objectiveSample, List<String> pipelines) throws Exception {

		if (pipelines==null) return false;
		
    	Set<String> pipelineSet = new HashSet<String>(pipelines);
    	
    	if (objectiveSample.hasPipelineRuns()) {
            for(SamplePipelineRun pipelineRun : objectiveSample.getPipelineRuns()) {
            	if (pipelineRun.hasError()) {
            		continue;
            	}
            	String pipelineName = pipelineRun.getPipelineProcess();
            	if (pipelineName!=null) {
            		pipelineSet.remove(pipelineName);
            	}
            }
    	}
    	
        if (pipelineSet.isEmpty()) {
            contextLogger.info("Sample "+sampleId+" "+objectiveSample.getObjective()+" has no unfulfilled pipelines");
        }
        else {
            contextLogger.info("Sample "+sampleId+" "+objectiveSample.getObjective()+" has unfulfilled pipelines: "+pipelineSet);
        }
        
		return pipelineSet.isEmpty();
	}
}
