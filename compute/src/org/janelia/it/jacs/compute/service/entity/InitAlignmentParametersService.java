package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.compute.service.align.ParameterizedAlignmentAlgorithm;
import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;

/**
 * Decides what analysis pipeline to run, based on the enumerated value.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAlignmentParametersService extends AbstractEntityService {

	@Override 
    public void execute() throws Exception {
        
		ParameterizedAlignmentAlgorithm paa = (ParameterizedAlignmentAlgorithm)processData.getItem("PARAMETERIZED_ALIGNMENT_ALGORITHM");
		AlignmentAlgorithm aa = paa.getAlgorithm();
		
		if (AlignmentAlgorithm.WHOLE_40X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
		}
		else if (AlignmentAlgorithm.WHOLE_40X_IMPROVED == aa) {
            processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xImprovedAlignmentService");
            processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
        }
		else if (AlignmentAlgorithm.WHOLE_63X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain63xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 63x Alignment");
		}
		else if (AlignmentAlgorithm.OPTIC == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.OpticLobeAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Optic Lobe Alignment");
		}
		else if (AlignmentAlgorithm.CENTRAL == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.CentralBrainAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Central Brain Alignment");
		}
		else if (AlignmentAlgorithm.CONFIGURED == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.ConfiguredAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Brain Alignment");
        	processData.putItem("ALIGNMENT_SCRIPT_NAME", paa.getParameter());
		}
        else if (AlignmentAlgorithm.CONFIGURED_BRAIN_VNC == aa) {
            processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.ConfiguredBrainVNCAlignmentService");
            processData.putItem("ALIGNMENT_RESULT_NAME", "Brain/VNC Alignment");
            processData.putItem("ALIGNMENT_SCRIPT_NAME", paa.getParameter());
        }
		else if (AlignmentAlgorithm.CONFIGURED_PAIR == aa) {
            processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.ConfiguredPairAlignmentService");
            processData.putItem("ALIGNMENT_RESULT_NAME", "Brain 20X/63X Alignment");
            processData.putItem("ALIGNMENT_SCRIPT_NAME", paa.getParameter());
        }
		else {
			throw new IllegalArgumentException("No such alignment algorithm: "+aa);
		}

        logger.info("Putting '"+processData.getItem("ALIGNMENT_SERVICE_CLASS")+"' in ALIGNMENT_SERVICE_CLASS");
        logger.info("Putting '"+processData.getItem("ALIGNMENT_RESULT_NAME")+"' in ALIGNMENT_RESULT_NAME");
        logger.info("Putting '"+processData.getItem("ALIGNMENT_SCRIPT_NAME")+"' in ALIGNMENT_SCRIPT_NAME");
    }
}
