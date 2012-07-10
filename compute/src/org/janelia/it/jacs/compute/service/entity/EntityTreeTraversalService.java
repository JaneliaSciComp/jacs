package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.*;

/**
 * Traverses the entity tree starting from a given root entity and builds a flattened list of ancestor
 * entities. Parameters must be provided in the ProcessData:
 *   ENTITY_TYPE_NAME (The entityType to traverse. If null, all entities will be returned)
 *   ENTITY_FILTER_CLASSNAME (The EntityFilter to use to narrow the resultset)
 *   ROOT_ENTITY_ID (The entity to start at)
 *   OUTVAR_ENTITY_ID (The output variable to populate with a List of Entities)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTreeTraversalService implements IService {

    protected Logger logger;
    protected Task task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            
            String entityTypeName = (String)processData.getItem("ENTITY_TYPE_NAME");
            Set<String> entityTypeSet = new HashSet<String>(Arrays.asList(entityTypeName.split(",")));
            String entityFilterClassName = (String)processData.getItem("ENTITY_FILTER_CLASS");
            
            EntityFilter entityFilter = null;            
            if (entityFilterClassName!=null) {
                try {
                	entityFilter = (EntityFilter)Class.forName(entityFilterClassName).newInstance();
                }
                catch (RuntimeException e) {
            		throw new IllegalArgumentException("Error instantiating ENTITY_FILTER_CLASS, "+entityFilterClassName,e);
                }
            }

            Object rootEntityIdObj = processData.getItem("ROOT_ENTITY_ID");
        	Long rootEntityId = (rootEntityIdObj instanceof Long) ? (Long)rootEntityIdObj : Long.parseLong(rootEntityIdObj.toString());
        	if (rootEntityId == null) {
        		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
        	}

        	boolean outputObjects = false;
        	
        	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
        	if (outvar == null) {
            	outvar = (String)processData.getItem("OUTVAR_ENTITY");
            	outputObjects = true;
            	if (outvar == null) {
            		throw new IllegalArgumentException("Both OUTVAR_ENTITY_ID and OUTVAR_ENTITY may not be null");
            	}
        	}
        	
        	Entity rootEntity = EJBFactory.getLocalEntityBean().getEntityTree(rootEntityId);
        	
        	if (rootEntity == null) {
        		throw new IllegalArgumentException("Root entity not found with id="+rootEntityId);
        	}
        	
        	logger.info("Traversing entity tree rooted at "+rootEntity.getName()+" and searching for "+entityTypeName+"...");
        	
        	List<Entity> entities = getDescendantsOfType(entityTypeSet, true, rootEntity);

    		logger.info("Found "+entities.size()+" entities. Filtering...");
    		
    		List outObjects = new ArrayList();
        	for(Entity entity : entities) {
        		if (entityFilter==null || entityFilter.includeEntity(processData, entity)) {
        			outObjects.add(outputObjects ? entity : entity.getId());	
        		}
        	}

    		logger.info("Putting "+outObjects.size()+" ids in "+outvar);
        	processData.putItem(outvar, outObjects);
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    public List<Entity> getDescendantsOfType(Set types, boolean ignoreNested, Entity calledEntity) {

        boolean found = false;
        List<Entity> items = new ArrayList<Entity>();
        if (types==null || types.contains(calledEntity.getEntityType().getName())) {
            items.add(calledEntity);
            found = true;
        }

        if (!found || !ignoreNested) {
            for (EntityData entityData : calledEntity.getOrderedEntityData()) {
                Entity child = entityData.getChildEntity();
                if (child != null) {
                    items.addAll(getDescendantsOfType(types,ignoreNested,child));
                }
            }
        }

        return items;
    }
}
