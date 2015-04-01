package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.exceptions.SAGEMetadataException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.List;

/**
 * A configured aligner which takes additional parameters to align a VNC anatomical area.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredVNCAlignmentService extends ConfiguredAlignmentService {

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
            throw new SAGEMetadataException("Tile with anatomical area 'VNC' not found for alignment");
        }
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
        return 30;
    }
}
