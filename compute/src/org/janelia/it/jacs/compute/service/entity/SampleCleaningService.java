package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Removes redundant (unannotated, not final) results from Samples or Sub-Samples. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleCleaningService extends AbstractEntityService {

    public transient static final String PARAM_testRun = "is test run";
    
    private boolean isDebug = false;
    private int numSamples = 0;
    private int numRunsDeleted = 0;
    
    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }            
        
        logger.info("Cleaning old results from samples for user: "+ownerKey);
        
        List<Entity> samples = entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE);

        logger.info("Will process "+samples.size()+" samples...");
        
        for(Entity sample : samples) {
        	processSample(populateChildren(sample));
        	numSamples++;
        }
        
        logger.info("Considered "+numSamples+" samples. Deleted "+numRunsDeleted+" results.");
    }
    
    private void processSample(Entity sample) throws Exception {
    	
    	logger.info("Cleaning up sample "+sample.getName());
    	
    	List<Entity> runs = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
    	if (runs.isEmpty()) return;
    	
    	// Group by pipeline process
    	Map<String,List<Entity>> processRunMap = new HashMap<String,List<Entity>>();

        for(Entity pipelineRun : runs) {
            String process = pipelineRun.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
            if (process == null) process = "";
            List<Entity> areaRuns = processRunMap.get(process);
            if (areaRuns == null) {
                areaRuns = new ArrayList<Entity>();
                processRunMap.put(process,areaRuns);
            }
            areaRuns.add(pipelineRun);
        }
    	
        for(String process : processRunMap.keySet()) {
            List<Entity> processRuns = processRunMap.get(process);
            if (processRuns.isEmpty()) continue;
            
            logger.info("  Processing pipeline runs for process: "+process);
            
            // Remove latest run, we don't want to touch it
            Entity lastRun = processRuns.remove(processRuns.size()-1);
            populateChildren(lastRun);

            if (EntityUtils.getLatestChildOfType(lastRun, EntityConstants.TYPE_ERROR)!=null) {
                logger.info("    Keeping last error run: "+lastRun.getId());
                // Last run had an error, let's keep that, but still try to find a good run to keep
                Collections.reverse(processRuns);
                Integer keeper = null;
                int curr = 0;
                for(Entity pipelineRun : processRuns) {
                    populateChildren(pipelineRun);
                    if (EntityUtils.getLatestChildOfType(pipelineRun, EntityConstants.TYPE_ERROR)==null) {
                        keeper = curr;
                        break;
                    }
                    curr++;   
                }
                if (keeper!=null) {
                    Entity lastGoodRun = processRuns.remove(keeper.intValue());
                    logger.info("    Keeping last good run: "+lastGoodRun.getId());
                    
                }
                else {
                    logger.info("    Could not find a good run to keep.");
                }
            }
            else {
                logger.info("    Keeping last good run: "+lastRun.getId());
            }
            
            // Clean up everything else
            deleteUnannotated(processRuns);
        }
    }
    
    private void deleteUnannotated(List<Entity> toDelete) throws ComputeException {
    	
    	Set<Entity> toReallyDelete = new HashSet<Entity>();
    	for(Entity entity : toDelete) {
    		long numFound = annotationBean.getNumDescendantsAnnotated(entity.getId());
    		if (numFound>0) {
            	logger.info("    Rejecting candidate "+entity.getId()+" because it and its descendants have "+numFound+" annotations");
            	continue;
    		}
    		toReallyDelete.add(entity);
    	}

    	if (toReallyDelete.isEmpty()) return;
    	logger.info("    Found "+toReallyDelete.size()+" non-annotated results for deletion:");
		int c = 0;
		for(Entity child : toReallyDelete) {
	    	if (!isDebug) {
	    		entityBean.deleteEntityTreeById(ownerKey, child.getId());
	    	}
    		else {
    			logger.info("      Would delete tree "+child.getId());
    		}
			c++;
			numRunsDeleted++;
		}
    	logger.info("    Deleted "+c+" result trees");
    }
}
