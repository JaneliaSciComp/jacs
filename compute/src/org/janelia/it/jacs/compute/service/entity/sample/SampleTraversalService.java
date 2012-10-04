package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Traverses the entity tree starting from a given root entity and builds a flattened list of ancestor
 * entities. Parameters must be provided in the ProcessData:
 *   OUTVAR_ENTITY_ID (The output variable to populate with a List of Entities)
 *   RUN_MODE (Mode to use for including entities)
 *     NEW - Include Samples which have no Pipeline Runs. 
 *     INCOMPLETE - Include Samples which have no Pipeline Runs, and Samples which have errors in their latest Pipeline Runs.
 *     ALL - Include every Sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTraversalService extends AbstractEntityService {

	public static final String RUN_MODE_NEW = "NEW";
	public static final String RUN_MODE_INCOMPLETE = "INCOMPLETE";
	public static final String RUN_MODE_ALL = "ALL";
	
    protected String runMode;

    public void execute() throws Exception {
    	
    	boolean outputObjects = false;
    	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
    	if (outvar == null) {
        	outvar = (String)processData.getItem("OUTVAR_ENTITY");
        	outputObjects = true;
        	if (outvar == null) {
        		throw new IllegalArgumentException("Both OUTVAR_ENTITY_ID and OUTVAR_ENTITY may not be null");
        	}
    	}
    	
        this.runMode = (String)processData.getItem("RUN_MODE");

    	logger.info("Searching for Samples owned by "+user.getUserLogin()+"...");
        List<Entity> entities = entityBean.getUserEntitiesByTypeName(user.getUserLogin(), EntityConstants.TYPE_SAMPLE);

		logger.info("Found "+entities.size()+" Samples. Filtering...");
		List outObjects = new ArrayList();
    	for(Entity entity : entities) {
    		if (includeSample(entity)) {
    			outObjects.add(outputObjects ? entity : entity.getId());	
    		}
    	}

		logger.info("Putting "+outObjects.size()+" ids in "+outvar);
    	processData.putItem(outvar, outObjects);
    }
    
    private boolean includeSample(Entity sample) {

		if (RUN_MODE_NEW.equals(runMode)) {
	    	populateChildren(sample);
	    	Entity pipelineRun = sample.getLatestChildOfType(EntityConstants.TYPE_PIPELINE_RUN);
	    	return (pipelineRun==null);
		} 
		else if (RUN_MODE_INCOMPLETE.equals(runMode)) {
	    	populateChildren(sample);
	    	Entity pipelineRun = sample.getLatestChildOfType(EntityConstants.TYPE_PIPELINE_RUN);
	    	if (pipelineRun==null) {
	    		return true;
	    	}
	    	populateChildren(pipelineRun);
	    	Entity error = pipelineRun.getLatestChildOfType(EntityConstants.TYPE_ERROR);
	    	return (error!=null); 
		}
		else if (RUN_MODE_ALL.equals(runMode)) {
			return true;
		}
		
		throw new IllegalStateException("Illegal mode: "+runMode);
    }
}
