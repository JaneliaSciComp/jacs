package org.janelia.it.jacs.compute.service.align;

import java.util.List;

import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;

/**
 * A configured aligner which aligns a VNC on its own.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredVNCAlignmentService extends ConfiguredAlignmentService {

    @Override
    protected void populateInputs(List<AnatomicalArea> sampleAreas) throws Exception {
        for(AnatomicalArea anatomicalArea : sampleAreas) {
            String areaName = anatomicalArea.getName();
            SampleProcessingResult result = getLatestResultOfType(objectiveSample, areaName);
            if (result!=null) {
                if (VNC_AREA.equalsIgnoreCase(areaName)) {
                    alignedAreas.add(anatomicalArea);
                    input1 = new AlignmentInputFile();
                    input1.setPropertiesFromEntity(result);
                    input1.setSampleId(sample.getId());
                    input1.setObjective(objectiveSample.getObjective());
                    if (warpNeurons) input1.setInputSeparationFilename(getConsolidatedLabel(result));
                }
            }
        }

        if (input1==null) {
        	runAligner = false;
        }
    }

    @Override
    protected int getRequiredSlots() {
        return 16;
    }

    @Override
    protected String getAdditionalNativeSpecification() {
        return "-l sandy=true";
    }
}
