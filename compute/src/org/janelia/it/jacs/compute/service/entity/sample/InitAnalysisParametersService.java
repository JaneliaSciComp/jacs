package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.entity.cv.AnalysisAlgorithm;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Decides what analysis pipeline to run, based on the enumerated value.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAnalysisParametersService extends AbstractDomainService {
    
    public void execute() throws Exception {
        
		String analysisAlgorithm = (String)processData.getItem("ANALYSIS_ALGORITHM");
    	if (StringUtils.isEmpty(analysisAlgorithm)) {
    		throw new IllegalArgumentException("ANALYSIS_ALGORITHM may not be null");
    	}

		AnalysisAlgorithm aa = AnalysisAlgorithm.valueOf(analysisAlgorithm);
		if (AnalysisAlgorithm.NEURON_SEPARATOR == aa) {
			String pipelineName = "FlyLightNeuronSeparation";
			contextLogger.info("Putting '"+pipelineName+"' in ANALYSIS_PIPELINE_NAME");
        	processData.putItem("ANALYSIS_PIPELINE_NAME", pipelineName);
		}

		SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        Sample sample = sampleHelper.getRequiredSample(data);
        ObjectiveSample objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
            
        Long alignmentId = data.getRequiredItemAsLong("ALIGNMENT_ID");
        List<SampleAlignmentResult> results = run.getResultsById(SampleAlignmentResult.class, alignmentId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Could not find alignment "+alignmentId+" in sample "+sample.getId());
        }
        SampleAlignmentResult alignment = results.get(0); // We can take any instance, since they're all the same

        String alignedConsolidatedLabel = DomainUtils.getFilepath(alignment, FileType.AlignedCondolidatedLabel);
        if (alignedConsolidatedLabel!=null) {
            contextLogger.info("Putting '"+alignedConsolidatedLabel+"' in ALIGNED_CONSOLIDATED_LABEL_FILEPATH");
            data.putItem("ALIGNED_CONSOLIDATED_LABEL_FILEPATH", alignedConsolidatedLabel);
        }
    }
}
