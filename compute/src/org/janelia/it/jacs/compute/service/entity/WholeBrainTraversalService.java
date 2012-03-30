package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.TilingPattern;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns all the samples in the Whole Brain folder, and finds all the whole brains.
 * Parameters must be provided in the ProcessData:
 *   OUTVAR_ENTITY_ID (The output variable to populate with a List of Entities)
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WholeBrainTraversalService implements IService {

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
            
            boolean outputObjects = false;
        	
        	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
        	if (outvar == null) {
            	outvar = (String)processData.getItem("OUTVAR_ENTITY");
            	outputObjects = true;
            	if (outvar == null) {
            		throw new IllegalArgumentException("Both OUTVAR_ENTITY_ID and OUTVAR_ENTITY may not be null");
            	}
        	}
        	
        	List<Entity> wholeBrains = annotationBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_TILING_PATTERN, TilingPattern.WHOLE_BRAIN.toString());
        	
    		logger.info("Found "+wholeBrains.size()+" whole brains. Filtering by owner...");
    		
    		List outObjects = new ArrayList();
        	for(Entity entity : wholeBrains) {
        		if (entity.getUser().getUserLogin().equals(task.getOwner())) {
        			outObjects.add(outputObjects ? entity : entity.getId());	
        		}
        	}

    		logger.info("Putting "+outObjects.size()+" ids in "+outvar);
        	processData.putItem(outvar, outObjects);
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
