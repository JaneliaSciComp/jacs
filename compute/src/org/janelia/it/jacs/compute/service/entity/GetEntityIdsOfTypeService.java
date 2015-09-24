package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Gets all the ids of entities of a given type.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetEntityIdsOfTypeService extends AbstractEntityService {

    public void execute() throws Exception {
        	
    	String entityTypeName = (String)processData.getItem("ENTITY_TYPE");
    	if (StringUtils.isEmpty(entityTypeName)) {
    		throw new IllegalArgumentException("ENTITY_TYPE may not be null");
    	}

    	List<Entity> entities = entityBean.getEntitiesByTypeName(ownerKey, entityTypeName);
    	List<String> entityIds = new ArrayList<String>();
    	for(Entity entity : entities) {
    	    entityIds.add(entity.getId().toString());
    	}
    	
    	contextLogger.info("Putting "+entityIds.size()+" result ids in ENTITY_ID");
    	processData.putItem("ENTITY_ID", entityIds);
    }
}
