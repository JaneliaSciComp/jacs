package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Extracts FILE_PATH attributes from a list of entities and makes them available as a string array called FILE_PATHS. 
 * The string array will have the same number of elements as the original entity list, and nulls in places where an
 * entity does not have a FILE_PATH attribute.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetFilePathsService implements IService {

    protected Logger logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        	
            List<Entity> entityList = (List<Entity>)processData.getItem("ENTITY_LIST");
        	if (entityList == null || entityList.isEmpty()) {
        		Entity entity = (Entity)processData.getItem("ENTITY");
        		if (entity==null) {
        			String entityId = (String)processData.getItem("ENTITY_ID");	
        			if (entityId == null) {
        				throw new IllegalArgumentException("Both ENTITY/ENTITY_ID and ENTITY_LIST may not be null");	
        			}
        			entity = EJBFactory.getLocalEntityBean().getEntityById(entityId);
        		}
        		entityList = new ArrayList<Entity>();
        		entityList.add(entity);
        	}
        	
        	List<String> filePaths = new ArrayList<String>();
        	for(Entity entity : entityList) {
        		filePaths.add(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        	}
        	
        	processData.putItem("FILE_PATHS", filePaths);
        	
        	if (filePaths.size()==1) {
        		processData.putItem("FILE_PATH", filePaths.get(0));
        	}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
