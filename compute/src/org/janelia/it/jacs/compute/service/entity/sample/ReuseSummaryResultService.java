package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;

/**
 * Find the latest summary entity in the given sample, and add it to the given pipeline run.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReuseSummaryResultService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        
        LSMSummaryResult latestLsr = objectiveSample.getLatestResultOfType(LSMSummaryResult.class);
        if (latestLsr!=null) {
                sampleHelper.addResult(run, latestLsr);
                contextLogger.info("Reusing summary result "+latestLsr.getId()+" for "+latestLsr.getName()+" in new pipeline run "+run.getId());
                processData.putItem("RESULT_ENTITY", latestLsr);
                contextLogger.info("Putting '"+latestLsr+"' in RESULT_ENTITY");
                processData.putItem("RESULT_ENTITY_ID", latestLsr.getId().toString());
                contextLogger.info("Putting '"+latestLsr.getId()+"' in RESULT_ENTITY_ID");
                processData.putItem("RUN_SUMMARY", Boolean.FALSE);    
                contextLogger.info("Putting '"+Boolean.FALSE+"' in RUN_SUMMARY");
        }
        else {
            contextLogger.info("No existing LSM summary available for reuse for sample: "+sample.getId());
        }
        
        sampleHelper.saveSample(sample);
    }
}
