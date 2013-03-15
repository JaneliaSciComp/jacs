package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Sets the sample processing result on the SampleArea and vice versa.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetSampleProcessingAreaService extends AbstractEntityService {
	
    public void execute() throws Exception {

        AnatomicalArea sampleArea = (AnatomicalArea)processData.getItem("SAMPLE_AREA");
        if (sampleArea==null) {
            throw new IllegalArgumentException("SAMPLE_AREA may not be null");
        }
        
        Entity resultEntity = (Entity)processData.getItem("RESULT_ENTITY");
        if (resultEntity==null) {
            throw new IllegalArgumentException("RESULT_ENTITY may not be null");
        }
        
        sampleArea.setSampleProcessingResultId(resultEntity.getId());

        String filename = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if (filename!=null)  {
            sampleArea.setSampleProcessingResultFilename(filename);
        }
        
        String currValue = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        if (currValue==null) {
            resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, sampleArea.getName());
            entityBean.saveOrUpdateEntity(resultEntity);
        }
        else if (!currValue.equals(sampleArea.getName())) {
            throw new IllegalStateException("Inconsistent sample area: "+currValue+"!="+sampleArea.getName());
        }
    }
}
