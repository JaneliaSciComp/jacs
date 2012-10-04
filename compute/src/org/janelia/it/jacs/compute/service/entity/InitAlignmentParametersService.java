package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;

/**
 * Decides what analysis pipeline to run, based on the enumerated value.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAlignmentParametersService extends AbstractEntityService {

    public void execute() throws Exception {
        	
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
		String alignmentType = (String)processData.getItem("ALIGNMENT_ALGORITHM");
		AlignmentAlgorithm aa = AlignmentAlgorithm.valueOf(alignmentType);
		
		if (alignmentType!=null && AlignmentAlgorithm.WHOLE_40X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
		}
		else if (alignmentType!=null && AlignmentAlgorithm.WHOLE_63X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain63xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 63x Alignment");
		}
		else if (alignmentType!=null && AlignmentAlgorithm.OPTIC == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.OpticLobeAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Optic Lobe Alignment");
        	// TODO: there should be a better way to get this...
        	String[] parts = sampleEntity.getName().split("-");
        	String tileName = parts[parts.length-1].replaceAll("_", " ");
        	processData.putItem("ALIGNMENT_TILE_NAME", tileName);
		}
		else if (alignmentType!=null && AlignmentAlgorithm.CENTRAL == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.BrainAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Central Brain Alignment");
		}
    }
}
