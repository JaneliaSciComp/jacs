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

    protected String vncFilename;
    
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
                    String filename = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                    if (filename!=null)  {
                        if ("VNC".equalsIgnoreCase(areaName)) {
                            vncFilename  = filename;
                        }
                        else if ("Brain".equalsIgnoreCase(areaName)) {
                            if (!filename.equals(inputFilename)) {
                                logger.warn("Aligner's default pick for input file does not match the Brain area input that was found on result entity with id="+result.getId());
                            }
                            inputFilename = filename;    
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
    protected String getAlignerCommand() {
        StringBuilder builder = new StringBuilder(super.getAlignerCommand());
        builder.append(" -v " + vncFilename);
        return builder.toString();
    }
}
