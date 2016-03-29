package org.janelia.it.jacs.compute.service.domain.alignment;

import org.janelia.it.jacs.compute.service.align.ParameterizedAlignmentAlgorithm;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

/**
 * Find the latest alignment entity in the given sample, and add it to the given pipeline run.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ReuseAlignmentResultService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        ParameterizedAlignmentAlgorithm paa = (ParameterizedAlignmentAlgorithm)processData.getItem("PARAMETERIZED_ALIGNMENT_ALGORITHM");

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);

        SampleAlignmentResult latestAr = objectiveSample.getLatestResultOfType(SampleAlignmentResult.class, paa.getResultName());
        if (latestAr!=null) {
                sampleHelper.addResult(run, latestAr);
                contextLogger.info("Reusing alignment result "+latestAr.getId()+" for "+latestAr.getName()+" in new pipeline run "+run.getId());
                String alignedFilename = DomainUtils.getDefault3dImageFilePath(latestAr);
                processData.putItem("ALIGNED_FILENAME", alignedFilename);    
                contextLogger.info("Putting '"+alignedFilename+"' in ALIGNED_FILENAME");
                processData.putItem("RUN_ALIGNMENT", Boolean.FALSE);    
                contextLogger.info("Putting '"+Boolean.FALSE+"' in RUN_ALIGNMENT");
        }
        else {
            contextLogger.info("No existing alignment with name '"+paa.getResultName()+"' available for reuse for sample: "+sample.getId());
        }
        
        sampleHelper.saveSample(sample);
    }
}
