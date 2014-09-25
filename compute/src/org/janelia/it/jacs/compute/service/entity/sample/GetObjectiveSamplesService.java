package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Extracts sub-samples based on their objective.
 * 
 * If RERUN_PIPELINES is provided as false then some additional variable are processed:
 * PIPELINES_TO_RUN_20X, PIPELINES_TO_RUN_40X. and PIPELINES_TO_RUN_63X
 * 
 * If a sub-sample already has results for all the pipelines given by its respective variable then its id is not output, 
 * so that it is not run by subsequent processing.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetObjectiveSamplesService extends AbstractEntityService {

    public void execute() throws Exception {

    	List<String> pipelines20x = null; 
    	List<String> pipelines40x = null; 
    	List<String> pipelines63x = null;
    	boolean rerun = data.getItemAsBoolean("RERUN_PIPELINES");
    	
    	if (!rerun) {
    		String pipelineStr20x = data.getItemAsString("PIPELINES_TO_RUN_20X");
    		if (pipelineStr20x!=null) {
    			pipelines20x = Task.listOfStringsFromCsvString(pipelineStr20x);
    		}
    		String pipelineStr40x = data.getItemAsString("PIPELINES_TO_RUN_40X");
    		if (pipelineStr40x!=null) {
    			pipelines40x = Task.listOfStringsFromCsvString(pipelineStr40x);
    		}
    		String pipelineStr63x = data.getItemAsString("PIPELINES_TO_RUN_63X");
    		if (pipelineStr63x!=null) {
    			pipelines63x = Task.listOfStringsFromCsvString(pipelineStr63x);
    		}
    	}
    	
        final Entity sampleEntity = entityHelper.getRequiredSampleEntity(data);
        populateChildren(sampleEntity);
        final List<Entity> subSamples = EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_SAMPLE);
        
        if (! subSamples.isEmpty()) {

            data.putItem("PARENT_SAMPLE_ID", sampleEntity.getId().toString());

            String objective;
            String subSampleId;
            for (Entity subSample : subSamples) {
                objective = subSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                subSampleId = subSample.getId().toString();
                if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                	if (rerun || !sampleHasAllPipelines(subSample, pipelines20x)) {
                        data.putItem("SAMPLE_20X_ID", subSampleId);
                	}
                } 
                else if (Objective.OBJECTIVE_40X.getName().equals(objective)) {
                    if (rerun || !sampleHasAllPipelines(subSample, pipelines40x)) {
                        data.putItem("SAMPLE_63X_ID", subSampleId);
                	}
                } 
                else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                	if (rerun || !sampleHasAllPipelines(subSample, pipelines63x)) {
                        data.putItem("SAMPLE_63X_ID", subSampleId);
                	}
                }
            }

        } 
        else {
            contextLogger.info("No sub-samples found");
            String objective = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (objective != null) {
                data.putItem("SAMPLE_" + objective.toUpperCase() + "_ID", sampleEntity.getId().toString());
            }

        }
    }

	private boolean sampleHasAllPipelines(Entity subSample, List<String> pipelines) throws Exception {

		if (pipelines==null) return false;
    	populateChildren(subSample);
		
    	Set<String> pipelineSet = new HashSet<String>(pipelines);
    	
        for(Entity pipelineRun : EntityUtils.getChildrenOfType(subSample, EntityConstants.TYPE_PIPELINE_RUN)) {
        	populateChildren(subSample);
        	if (EntityUtils.findChildWithType(subSample, EntityConstants.TYPE_ERROR)!=null) {
        		continue;
        	}
        	String pipelineName = pipelineRun.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
        	if (pipelineName!=null) {
        		pipelineSet.remove(pipelineName);
        	}
        }
    	
        if (pipelineSet.isEmpty()) {
        	logger.info("Sample "+subSample.getName()+" has no unfulfilled pipelines");
        }
        else {
        	logger.info("Sample "+subSample.getName()+" has unfulfilled pipelines: "+pipelineSet);	
        }
        
		return pipelineSet.isEmpty();
	}
}
