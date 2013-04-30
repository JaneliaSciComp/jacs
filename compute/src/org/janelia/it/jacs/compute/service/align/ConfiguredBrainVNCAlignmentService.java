package org.janelia.it.jacs.compute.service.align;

import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
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
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        
        try {
            List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>)processData.getItem("SAMPLE_AREAS");
            if (sampleAreas==null) {
                throw new IllegalArgumentException("Input parameter SAMPLE_AREAS may not be null");
            }
            
            for(AnatomicalArea anatomicalArea : sampleAreas) {
                String areaName = anatomicalArea.getName();
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
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 24;
    }
}
