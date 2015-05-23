package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Get the ancestor of an entity.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FindAncestorService extends AbstractEntityService {

    public void execute() throws Exception {
        	
    	Long entityId = data.getRequiredItemAsLong("ENTITY_ID");
    	Entity entity = entityBean.getEntityById(entityId);
    	
    	if (entity==null) {
            processData.putItem("ANCESTOR_ID", null);
    	}
    	else {
        	String ancestorType = data.getRequiredItemAsString("ANCESTOR_TYPE");
        	boolean useSelf = data.getItemAsBoolean("USE_SELF");
        	
        	Entity ancestor = entityBean.getAncestorWithType(entity, ancestorType);
        	if (ancestor==null && useSelf) {
        	    ancestor = entity;
        	}
        	
        	processData.putItem("ANCESTOR_ID", ancestor==null?null:ancestor.getId()+"");
    	}
    }
}
