package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Traverses the entity tree starting from a given root entity and builds a flattened list of ancestor
 * entities. Parameters must be provided in the ProcessData:
 *   ENTITY_TYPE_NAME (The entityType to traverse. If null, all entities will be returned)
 *   ROOT_ENTITY_ID (The entity to start at)
 *   OUTVAR_ENTITY_ID (The output variable to populate with a List of Entities)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTreeTraversalService implements IService {

    protected Logger logger;
    protected Task task;
    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            
            String entityTypeName = (String)processData.getItem("ENTITY_TYPE_NAME");
            
        	Long rootEntityId = (Long)processData.getItem("ROOT_ENTITY_ID");
        	if (rootEntityId == null) {
        		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
        	}

        	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
        	if (outvar == null) {
        		throw new IllegalArgumentException("OUTVAR_ENTITY_ID may not be null");
        	}
        	
        	Entity rootEntity = annotationBean.getEntityTree(rootEntityId);
        	
        	if (rootEntity == null) {
        		throw new IllegalArgumentException("Root entity not found with id="+rootEntityId);
        	}
        	
        	logger.info("Traversing entity tree rooted at "+rootEntity.getName()+" and searching for "+entityTypeName+"...");
        	
        	List<Entity> entities = rootEntity.getDescendantsOfType(entityTypeName);

    		logger.info("Found "+entities.size()+" entities.");
    		
    		List<Long> ids = new ArrayList<Long>();
        	for(Entity entity : entities) {
        		// TODO: remove this.. it was used for debugging
//        		if (entity.getName().equals("GMR_57C10_AD_01-20110606_2_B6"))
        			ids.add(entity.getId());
        	}

    		logger.info("Putting "+ids.size()+" ids in "+outvar);
        	processData.putItem(outvar, ids);
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
