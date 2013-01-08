package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.Date;
import java.util.HashSet;

import org.hibernate.exception.ExceptionUtils;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Creates an error entity based on the given exception, and adds it to the root entity.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateErrorEntityService extends AbstractEntityService {
	
    public void execute() throws Exception {
            
    	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
    	if (StringUtils.isEmpty(rootEntityId)) {
    		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
    	}

    	Entity rootEntity = entityBean.getEntityById(rootEntityId);
    	if (rootEntity == null) {
    		throw new IllegalArgumentException("Root entity not found with id="+rootEntityId);
    	}

    	Exception exception = (Exception)processData.getItem(IProcessData.PROCESSING_EXCEPTION);

    	Date date = new Date();
    	Entity error = new Entity(null, "Error", rootEntity.getOwnerKey(), null, 
    			entityBean.getEntityTypeByName(EntityConstants.TYPE_ERROR), date, date, new HashSet<EntityData>());	
    	error.setValueByAttributeName(EntityConstants.ATTRIBUTE_MESSAGE, ExceptionUtils.getStackTrace(exception));
    	entityBean.saveOrUpdateEntity(error);

    	logger.info("Saved error entity as id="+error.getId());
    	
    	entityBean.addEntityToParent(ownerKey, rootEntity.getId(), error.getId(), rootEntity.getMaxOrderIndex()+1, 
    			EntityConstants.ATTRIBUTE_ENTITY);
    }
}
