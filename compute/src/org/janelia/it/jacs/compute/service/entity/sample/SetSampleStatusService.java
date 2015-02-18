package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Sets the status attribute of the sample. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetSampleStatusService extends AbstractEntityService {

    public void execute() throws Exception {
        
        String status = data.getRequiredItemAsString("STATUS");
    	Long entityId = data.getRequiredItemAsLong("ENTITY_ID");
    	Entity sample = entityBean.getEntityById(entityId);
    	Entity parentSample = entityBean.getAncestorWithType(sample, EntityConstants.TYPE_SAMPLE);
    	
    	if (parentSample==null) {
            logger.info("Setting status to "+status+" on parent-less sample "+sample.getName()+" (id="+entityId+")");
            entityBean.setOrUpdateValue(entityId, EntityConstants.ATTRIBUTE_STATUS, status);
    	}
    	else {
            logger.info("Setting status to "+status+" on sample "+sample.getName()+" (id="+entityId+")");
            entityBean.setOrUpdateValue(entityId, EntityConstants.ATTRIBUTE_STATUS, status);
            
            if (status.equals(EntityConstants.VALUE_COMPLETE)) {
                entityLoader.populateChildren(parentSample);
                for(Entity childSample : EntityUtils.getChildrenOfType(parentSample, EntityConstants.TYPE_SAMPLE)) {
                    String childStatus = childSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
                    if (childStatus!=null && !EntityConstants.VALUE_COMPLETE.equals(childStatus)) {
                        // All child samples are not finished, so the parent cannot be finished
                        return;
                    }
                }
            }
            
            // Also update parent status
            logger.info("Setting status to "+status+" on parent sample "+parentSample.getName()+" (id="+parentSample.getId()+")");
            entityBean.setOrUpdateValue(parentSample.getId(), EntityConstants.ATTRIBUTE_STATUS, status);
    	}
    }
}
