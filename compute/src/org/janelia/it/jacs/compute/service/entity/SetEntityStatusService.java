package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Sets the status attribute of the sample. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetEntityStatusService extends AbstractEntityService {

    public void execute() throws Exception {
        	
    	Long entityId = data.getRequiredItemAsLong("ENTITY_ID");
    	Entity entity = entityBean.getEntityById(entityId);
    	
    	String status = data.getRequiredItemAsString("STATUS");
    	
    	logger.info("Setting status to "+status+" on entity "+entity.getName()+" (id="+entityId+")");
    	entityBean.setOrUpdateValue(entityId, EntityConstants.ATTRIBUTE_STATUS, status);
    	
    }
}
