package org.janelia.it.jacs.compute.service.align;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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
            Entity result = getLatestResultOfType(sampleEntity, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, areaName);
            if (result!=null) {
                entityLoader.populateChildren(result);
                Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                if (image!=null)  {
                    if (VNC_AREA.equalsIgnoreCase(areaName)) {
                        alignedAreas.add(anatomicalArea);
                        input1 = new AlignmentInputFile(areaName);
                        input1.setPropertiesFromEntity(image);
                        input1.setSampleId(sampleEntity.getId());
                        input1.setObjective(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
                        if (warpNeurons) input1.setInputSeparationFilename(getConsolidatedLabel(result));
                    }
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
