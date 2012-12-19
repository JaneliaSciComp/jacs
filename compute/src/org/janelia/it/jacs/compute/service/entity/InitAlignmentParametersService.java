package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.compute.service.align.ParameterizedAlignmentAlgorithm;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Decides what analysis pipeline to run, based on the enumerated value.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAlignmentParametersService extends AbstractEntityService {

	@Override 
    public void execute() throws Exception {
        	
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	processData.putItem("OPTICAL_RESOLUTION", getConsensusOpticalResolution(sampleEntity));
    	
		ParameterizedAlignmentAlgorithm paa = (ParameterizedAlignmentAlgorithm)processData.getItem("PARAMETERIZED_ALIGNMENT_ALGORITHM");
		AlignmentAlgorithm aa = paa.getAlgorithm();
		
		if (AlignmentAlgorithm.WHOLE_40X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
		}
		else if (AlignmentAlgorithm.WHOLE_63X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain63xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 63x Alignment");
		}
		else if (AlignmentAlgorithm.OPTIC == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.OpticLobeAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Optic Lobe Alignment");
        	// TODO: there should be a better way to get this...
        	String[] parts = sampleEntity.getName().split("-");
        	String tileName = parts[parts.length-1].replaceAll("_", " ");
        	processData.putItem("ALIGNMENT_TILE_NAME", tileName);
		}
		else if (AlignmentAlgorithm.CENTRAL == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.CentralBrainAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Central Brain Alignment");
		}
		else if (AlignmentAlgorithm.CONFIGURED == aa) {
			String scriptName = paa.getParameter();
			if (scriptName==null) {
				throw new IllegalArgumentException("Tried to use Configured alignment algorithm without a parameter");
			}
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.ConfiguredAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Brain Alignment");
        	processData.putItem("ALIGNMENT_SCRIPT_NAME", scriptName);
		}
		else {
			throw new IllegalArgumentException("No such alignment algorithm: "+aa);
		}
		
		logger.info("Set OPTICAL_RESOLUTION = "+processData.getItem("OPTICAL_RESOLUTION"));
		logger.info("Set ALIGNMENT_SERVICE_CLASS = "+processData.getItem("ALIGNMENT_SERVICE_CLASS"));
		logger.info("Set ALIGNMENT_RESULT_NAME = "+processData.getItem("ALIGNMENT_RESULT_NAME"));
		logger.info("Set ALIGNMENT_TILE_NAME = "+processData.getItem("ALIGNMENT_TILE_NAME"));
		logger.info("Set ALIGNMENT_SCRIPT_NAME = "+processData.getItem("ALIGNMENT_SCRIPT_NAME"));
    }
    
    private String getConsensusOpticalResolution(Entity sampleEntity) throws Exception {
    	
    	String consensus = null;
    	populateChildren(sampleEntity);

    	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
    	if (supportingFiles==null) return null; 
        populateChildren(supportingFiles);

		for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE)) {		
            populateChildren(imageTile);

    		for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
    			String opticalRes = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
    			if (consensus==null) {
    				consensus = opticalRes;
    			}
    			else {
    				if (!consensus.equals(opticalRes)) {
    					logger.warn("At least two different optical resolutions in sample with id="+
    							sampleEntity.getId()+": (1) "+consensus+" and (2) "+opticalRes+". Using (1)");
    					return consensus;
    				}
    			}
    		}
		}
		
		return consensus;
    }
}
