package org.janelia.it.jacs.compute.service.domain.sample;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Creates a new pipeline run entity under the given sample.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreatePipelineRunEntityService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        
    	String pipelineName = (String)processData.getItem("PIPELINE_NAME");
    	if (StringUtils.isEmpty(pipelineName)) {
    		pipelineName = "Pipeline";
    	}
    	
    	String pipelineRunName = pipelineName+" Results";
        
    	String pipelineProcess = (String)processData.getItem("PIPELINE_PROCESS");
    	if (StringUtils.isEmpty(pipelineProcess)) {
    		throw new IllegalArgumentException("PIPELINE_PROCESS may not be null");
    	}
    	
    	int pipelineVersion = 1; // TODO: pipeline versions
    	
    	SamplePipelineRun pipelineRun = sampleHelper.addNewPipelineRun(objectiveSample, pipelineRunName, pipelineProcess, pipelineVersion);
    	sampleHelper.saveSample(sample);

    	contextLogger.info("Added new pipeline run to sample "+sample.getName());
    	
    	processData.putItem("PIPELINE_RUN_ENTITY_ID", pipelineRun.getId().toString());
    	contextLogger.info("Putting '"+pipelineRun.getId()+"' in PIPELINE_RUN_ENTITY_ID");
    }
}
