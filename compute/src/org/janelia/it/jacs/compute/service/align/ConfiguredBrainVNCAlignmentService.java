package org.janelia.it.jacs.compute.service.align;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.exceptions.SAGEMetadataException;
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
            logger.info("Sample area "+areaName+" has processing result "+anatomicalArea.getSampleProcessingResultId());
            Entity result = entityBean.getEntityById(anatomicalArea.getSampleProcessingResultId());
            if (result!=null) {
                entityLoader.populateChildren(result);
                Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                if (image!=null)  {
                    if ("VNC".equalsIgnoreCase(areaName)) {
                        input2 = new AlignmentInputFile();
                        input2.setPropertiesFromEntity(image);
                        if (warpNeurons) input2.setInputSeparationFilename(getConsolidatedLabel(result));
                    }
                    else if ("Brain".equalsIgnoreCase(areaName)) {
                        input1 = new AlignmentInputFile();
                        input1.setPropertiesFromEntity(image);
                        if (warpNeurons) input1.setInputSeparationFilename(getConsolidatedLabel(result));
                    }
                    else {
                        logger.warn("Unrecognized sample area: "+areaName);
                    }
                }
            }
        }
        
        if (input1==null) {
            throw new SAGEMetadataException("Tile with anatomical area 'Brain' not found for alignment");
        }
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
        return 30;
    }
}
