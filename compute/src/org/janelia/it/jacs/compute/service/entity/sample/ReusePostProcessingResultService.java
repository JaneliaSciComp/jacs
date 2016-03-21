package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;

/**
 * Find the latest post-processing entity in the given sample, and add it to the given pipeline run.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReusePostProcessingResultService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        
        SamplePostProcessingResult latestPpr = objectiveSample.getLatestResultOfType(SamplePostProcessingResult.class);
        if (latestPpr!=null) {
            sampleHelper.addResult(run, latestPpr);
                contextLogger.info("Reusing post-processing result "+latestPpr.getId()+" for "+latestPpr.getName()+" in new pipeline run "+run.getId());
                processData.putItem("RESULT_ENTITY", latestPpr);
                contextLogger.info("Putting '"+latestPpr+"' in RESULT_ENTITY");
                processData.putItem("RESULT_ENTITY_ID", latestPpr.getId().toString());
                contextLogger.info("Putting '"+latestPpr.getId()+"' in RESULT_ENTITY_ID");
                processData.putItem("RUN_POST", Boolean.FALSE);    
                contextLogger.info("Putting '"+Boolean.FALSE+"' in RUN_POST");
        }
        else {
            contextLogger.info("No existing post-processing available for reuse for sample: "+sample.getId());
        }
        
        sampleHelper.saveSample(sample);
    }
}
