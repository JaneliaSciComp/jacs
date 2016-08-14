package org.janelia.it.jacs.compute.service.align;

import java.util.List;

import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;

/**
 * A configured aligner which takes additional parameters to align a VNC along with a Brain area.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredBrainVNCAlignmentService extends ConfiguredAlignmentService {

    @Override
    protected void populateInputs(List<AnatomicalArea> sampleAreas) throws Exception {
        for(AnatomicalArea anatomicalArea : sampleAreas) {
            String areaName = anatomicalArea.getName();
            SampleProcessingResult result = getLatestResultOfType(objectiveSample, areaName);
            if (result!=null) {
                if (BRAIN_AREA.equalsIgnoreCase(areaName)) {
                	alignedAreas.add(anatomicalArea);
                    input1 = new AlignmentInputFile(areaName);
                    input1.setPropertiesFromEntity(result);
                    input1.setSampleId(sample.getId());
                    input1.setObjective(objectiveSample.getObjective());
                    if (warpNeurons) input1.setInputSeparationFilename(getConsolidatedLabel(result));
                }
                else if (VNC_AREA.equalsIgnoreCase(areaName)) {
                	alignedAreas.add(anatomicalArea);
                    input2 = new AlignmentInputFile(areaName);
                    input2.setPropertiesFromEntity(result);
                    input2.setSampleId(sample.getId());
                    input2.setObjective(objectiveSample.getObjective());
                    if (warpNeurons) input2.setInputSeparationFilename(getConsolidatedLabel(result));
                }
                else {
                    logger.warn("Unrecognized sample area: "+areaName);
                }
            }
        }
        
        if (input1==null) {
        	runAligner = false;
        }
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
        return 30;
    }
}
