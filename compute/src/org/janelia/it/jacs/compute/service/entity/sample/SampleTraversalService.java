package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns all the samples for the task owner which match the parameters. Parameters must be provided in the ProcessData:
 *   OUTVAR_ENTITY_ID (The output variable to populate with a List of Entities)
 *   RUN_MODE (Mode to use for including entities)
 *     NONE - Don't run any samples.
 *     NEW - Include Samples which have no Pipeline Runs. 
 *     INCOMPLETE - Include Samples which have no Pipeline Runs, and Samples which have errors in their latest Pipeline Runs.
 *     ALL - Include every Sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTraversalService extends AbstractEntityService {

	public static final String RUN_MODE_NONE = "NONE";
	public static final String RUN_MODE_NEW = "NEW";
	public static final String RUN_MODE_INCOMPLETE = "INCOMPLETE";
	public static final String RUN_MODE_ALL = "ALL";

	protected boolean excludeChildSamples = true;
    protected String runMode;
    protected String dataSetName = null;

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

        dataSetName = (String) processData.getItem("DATA_SET_NAME");

        this.runMode = (String)processData.getItem("RUN_MODE");
        List<Object> outObjects = new ArrayList<Object>();
        
        if (!RUN_MODE_NONE.equals(runMode)) {

            List<Entity> entities;
            if (dataSetName == null) {

                logger.info("Searching for samples owned by " + ownerKey + "...");

                entities = entityBean.getUserEntitiesByTypeName(ownerKey,
                                                                EntityConstants.TYPE_SAMPLE);
            } else {

                List<Entity> dataSets = entityBean.getUserEntitiesByNameAndTypeName(
                        ownerKey,
                        dataSetName,
                        EntityConstants.TYPE_DATA_SET);

                if (dataSets.size() == 1) {

                    final Entity dataSetEntity = dataSets.get(0);
                    final String dataSetIdentifier = dataSetEntity.getValueByAttributeName(
                            EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);

                    logger.info("Searching for " + ownerKey + " '" + dataSetName +
                                "' data set (" + dataSetIdentifier + ") samples ...");

                    entities = entityBean.getUserEntitiesWithAttributeValueAndTypeName(
                            ownerKey,
                            EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER,
                            dataSetIdentifier,
                            EntityConstants.TYPE_SAMPLE);

                } else {
                    throw new IllegalArgumentException("found " + dataSets.size() +
                                                       " entities for " + ownerKey +
                                                       " data set '" + dataSetName +
                                                       "' when only one is expected");
                }

            }

			logger.info("Found " + entities.size() + " Samples. Filtering...");
			
	    	for(Entity entity : entities) {
	    		if (includeSample(entity)) {
	    			outObjects.add(outputObjects ? entity : entity.getId());	
	    		}
	    	}
        }

		logger.info("Putting "+outObjects.size()+" ids in "+outvar);
    	processData.putItem(outvar, outObjects);
    }
    
    private boolean includeSample(Entity sample) throws Exception {

        if (excludeChildSamples && sample.getName().contains("~")) {
            return false;
        }

		if (RUN_MODE_NEW.equals(runMode)) {
            return includeSample(sample, true, false);
		} 
		else if (RUN_MODE_INCOMPLETE.equals(runMode)) {
		    return includeSample(sample, true, true);
		}
		else if (RUN_MODE_ALL.equals(runMode)) {
			return true;
		}
		
		throw new IllegalStateException("Illegal mode: "+runMode);
    }
    
    private boolean includeSample(Entity sample, boolean includeNewSamples, boolean includeErrorSamples) throws Exception {

        populateChildren(sample);

        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
        if (childSamples.isEmpty()) {

            if (includeNewSamples) {
                Entity pipelineRun = EntityUtils.getLatestChildOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
                if (pipelineRun==null) {
                    return true;
                }
            }
            if (includeErrorSamples) {
                Entity pipelineRun = EntityUtils.getLatestChildOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
                populateChildren(pipelineRun);
                Entity error = EntityUtils.getLatestChildOfType(pipelineRun, EntityConstants.TYPE_ERROR);
                if (error!=null) {
                    return true;
                }
            }
        }
        else {
            for(Entity childSample : childSamples) {
                populateChildren(childSample);
                if (!includeSample(childSample, includeNewSamples, includeErrorSamples)) {
                    return false;
                }    
            }
            // All child samples passed
            return true;
        }
        
        return false;
    }
}
