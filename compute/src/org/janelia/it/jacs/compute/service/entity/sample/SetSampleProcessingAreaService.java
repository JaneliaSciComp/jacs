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

        AnatomicalArea sampleArea = (AnatomicalArea)data.getRequiredItem("SAMPLE_AREA");
        
        Long resultEntityId = data.getRequiredItemAsLong("RESULT_ENTITY_ID");
        Entity resultEntity = entityBean.getEntityById(resultEntityId);
        if (resultEntity==null) {
            throw new IllegalArgumentException("Result entity does not exist: "+resultEntityId);
        }
        
        sampleArea.setSampleProcessingResultId(resultEntity.getId());
        
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
