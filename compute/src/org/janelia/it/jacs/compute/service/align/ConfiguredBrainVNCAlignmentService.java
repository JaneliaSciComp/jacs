package org.janelia.it.jacs.compute.service.align;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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
            Entity result = getLatestResultOfType(sampleEntity, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, areaName);
            if (result!=null) {
                entityLoader.populateChildren(result);
                Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                if (image!=null)  {
                    if (BRAIN_AREA.equalsIgnoreCase(areaName)) {
                    	alignedAreas.add(anatomicalArea);
                        input1 = new AlignmentInputFile();
                        input1.setPropertiesFromEntity(image);
                        input1.setSampleId(sampleEntity.getId());
                        input1.setObjective(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
                        if (warpNeurons) input1.setInputSeparationFilename(getConsolidatedLabel(result));
                    }
                    else if (VNC_AREA.equalsIgnoreCase(areaName)) {
                    	alignedAreas.add(anatomicalArea);
                        input2 = new AlignmentInputFile();
                        input2.setPropertiesFromEntity(image);
                        input2.setSampleId(sampleEntity.getId());
                        input2.setObjective(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE));
                        if (warpNeurons) input2.setInputSeparationFilename(getConsolidatedLabel(result));
                    }
                    else {
                        logger.warn("Unrecognized sample area: "+areaName);
                    }
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
