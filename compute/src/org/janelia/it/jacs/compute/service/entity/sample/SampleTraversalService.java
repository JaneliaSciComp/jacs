package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

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

    protected boolean includeParentSamples = false;
    protected boolean includeChildSamples = true;
    protected boolean includeAllSamples;
    protected boolean includeNewSamples;
    protected boolean includeErrorSamples;
   

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

        logger.info("Traversing samples owned by "+ownerKey+", with rules:");
        
        String parentOrChildren = (String) processData.getItem("PARENT_OR_CHILDREN");
        logger.info("    parentOrChildren="+parentOrChildren);
        
        if (parentOrChildren!=null) {
            if (parentOrChildren.equals("parent")) {
                includeChildSamples = false;
                includeParentSamples = true;
            }
            else if (parentOrChildren.equals("children")) {
                includeChildSamples = true;
                includeParentSamples = false;
            }
            else if (parentOrChildren.equals("both")) {
                includeChildSamples = true;
                includeParentSamples = true;
            }
            else {
                throw new IllegalArgumentException("Unrecognized value for PARENT_OR_CHILDREN:"+parentOrChildren);
            }
        }
    	
        String dataSetName = (String) processData.getItem("DATA_SET_NAME");
        logger.info("    dataSetName="+dataSetName);
        
        String runMode = (String)processData.getItem("RUN_MODE");
        logger.info("    runMode="+runMode);
        
        if (RUN_MODE_NEW.equals(runMode)) {
            includeNewSamples = true;
        } 
        else if (RUN_MODE_INCOMPLETE.equals(runMode)) {
            includeErrorSamples = true;
        }
        else if (RUN_MODE_ALL.equals(runMode)) {
            includeAllSamples = true;
        }
        else if (RUN_MODE_NONE.equals(runMode)) {
            // No samples will be selected
        }
        else {
            throw new IllegalStateException("Illegal mode: "+runMode);    
        }
        
        List<Object> outObjects = new ArrayList<Object>();
        
        if (!RUN_MODE_NONE.equals(runMode)) {

            List<Entity> entities;
            if (dataSetName == null) {
                entities = entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE);
            } 
            else {
                List<Entity> dataSets = entityBean.getUserEntitiesByNameAndTypeName(ownerKey, dataSetName, EntityConstants.TYPE_DATA_SET);
                if (dataSets.size() == 1) {
                    Entity dataSetEntity = dataSets.get(0);
                    String dataSetIdentifier = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
                    entities = entityBean.getUserEntitiesWithAttributeValueAndTypeName(ownerKey,
                            EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier,
                            EntityConstants.TYPE_SAMPLE);
                } 
                else {
                    throw new IllegalArgumentException("found " + dataSets.size() + " entities for " + ownerKey
                            + " data set '" + dataSetName + "' when only one is expected");
                }

            }

			logger.info("Found " + entities.size() + " Samples. Filtering with rules:");
			logger.info("    includeNewSamples="+includeNewSamples);
			logger.info("    includeErrorSamples="+includeErrorSamples);
			logger.info("    includeAllSamples="+includeAllSamples);
			logger.info("    includeChildSamples="+includeChildSamples);
			logger.info("    includeParentSamples="+includeParentSamples);
			
	    	for(Entity entity : entities) {
	    	    List<Entity> included = getIncludedSamples(entity);
	    		for(Entity sample : included) {
	    			outObjects.add(outputObjects ? sample : sample.getId());	
	    		}
	    	}
        }

		logger.info("Putting "+outObjects.size()+" ids in "+outvar);
    	processData.putItem(outvar, outObjects);
    }
    
    private List<Entity> getIncludedSamples(Entity sample) throws Exception {

        List<Entity> included = new ArrayList<Entity>();

        // Ignore blocked samples
        if (sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_PROCESSING_BLOCK)!=null) {
            logger.info("Excluded "+sample+" (blocked)");
            return included;
        }
        
        populateChildren(sample);
        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
        
        if (childSamples.isEmpty()) {
            // Childless samples are always included
            if (includeSample(sample)) {
                included.add(sample);
                logger.info("Included "+sample+" (childless sample)");
            }
            else {
                logger.info("Excluded "+sample+" (childless sample)");
            }
        }
        else {
            // This is a parent sample because it has child samples. Check if any of the children are to be included.
            Set<Entity> childrenIncluded = new HashSet<Entity>();
            Set<Entity> childrenExcluded = new HashSet<Entity>();
            for(Entity childSample : childSamples) {
                populateChildren(childSample);
                if (includeSample(childSample)) {
                    childrenIncluded.add(childSample);
                }
                else {
                    childrenExcluded.add(childSample);
                }
            }
            
            if (includeParentSamples && !childrenIncluded.isEmpty()) {
                // The parent sample should be included if any of the children samples are included
                included.add(sample);
                logger.info("Included "+sample+" (parent sample)");
            }
            else {
                logger.info("Excluded "+sample+" (parent sample)");
            }
            
            for(Entity childSample : childrenIncluded) {
                if (includeChildSamples) {
                    logger.info("  Included "+childSample+" (child sample)");
                    included.add(childSample);
                }
                else {
                    logger.info("  Excluded "+childSample+" (child sample) because child samples are not wanted");    
                }
            }

            for(Entity childSample : childrenExcluded) {
                logger.info("  Excluded "+childSample+" (child sample) because it does not fit the run mode");
            }
        }
        
        return included;
    }
    
    private boolean includeSample(Entity sample) throws Exception {

        if (sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_PROCESSING_BLOCK)!=null) return false;
        
        if (includeAllSamples) {
            return true;
        }
        
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
        
        return false;
    }
}
