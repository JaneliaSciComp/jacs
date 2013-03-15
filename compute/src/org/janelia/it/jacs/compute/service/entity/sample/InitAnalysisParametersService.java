package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.AnalysisAlgorithm;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Decides what analysis pipeline to run, based on the enumerated value.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAnalysisParametersService extends AbstractEntityService {
	
    public void execute() throws Exception {
    	
		String analysisAlgorithm = (String)processData.getItem("ANALYSIS_ALGORITHM");
    	if (StringUtils.isEmpty(analysisAlgorithm)) {
    		throw new IllegalArgumentException("ANALYSIS_ALGORITHM may not be null");
    	}

		AnalysisAlgorithm aa = AnalysisAlgorithm.valueOf(analysisAlgorithm);
		if (AnalysisAlgorithm.NEURON_SEPARATOR == aa) {
			String pipelineName = "FlyLightNeuronSeparation";
			logger.info("Putting '"+pipelineName+"' in ANALYSIS_PIPELINE_NAME");
        	processData.putItem("ANALYSIS_PIPELINE_NAME", pipelineName);
		}
    	
    	String resultEntityId = (String)processData.getItem("RESULT_ENTITY_ID");
    	if (StringUtils.isEmpty(resultEntityId)) {
    		throw new IllegalArgumentException("RESULT_ID may not be null");
    	}
    	
    	Entity result = entityBean.getEntityById(resultEntityId);
    	populateChildren(result);
    	
    	Entity default3dImage = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    	if (default3dImage==null) {
    		throw new IllegalArgumentException("Result with id="+result.getId()+" has no Default 3d Image.");
    	}
    	
    	String filename = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		
    	logger.info("Putting '"+filename+"' in OUTPUT_FILENAME");
    	processData.putItem("OUTPUT_FILENAME", filename);
    }
}
