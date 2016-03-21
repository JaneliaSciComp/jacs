package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
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
    }
}
