package org.janelia.it.jacs.compute.service.entity;

import java.util.*;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.SolrBeanLocal;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Removes unneeded (unannotated, not final) results from Samples. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleCleaningService extends AbstractEntityService {

    public transient static final String PARAM_testRun = "is test run";
    
    protected SolrBeanLocal solrBean;
    
    private boolean isDebug = false;
    private int numSamples = 0;
    private int numRunsDeleted = 0;
    
    public void execute() throws Exception {

        solrBean = EJBFactory.getLocalSolrBean();

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
    	
    	// Group by anatomical area
    	Map<String,List<Entity>> areaRunMap = new HashMap<String,List<Entity>>();

        for(Entity pipelineRun : runs) {
            String area = pipelineRun.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
            if (area == null) area = "";
            List<Entity> areaRuns = areaRunMap.get(area);
            if (areaRuns == null) {
                areaRuns = new ArrayList<Entity>();
                areaRunMap.put(area,areaRuns);
            }
            areaRuns.add(pipelineRun);
        }
    	
        for(String area : areaRunMap.keySet()) {
            List<Entity> areaRuns = areaRunMap.get(area);
            if (areaRuns.isEmpty()) continue;
            
            logger.info("  Processing pipeline runs for area: "+area);
            
            // Remove latest run, we don't want to touch it
            Entity lastRun = areaRuns.remove(areaRuns.size()-1);
            populateChildren(lastRun);

            if (EntityUtils.getLatestChildOfType(lastRun, EntityConstants.TYPE_ERROR)!=null) {
                logger.info("    Keeping last error run: "+lastRun.getId());
                // Last run had an error, let's keep that, but still try to find a good run to keep
                Collections.reverse(areaRuns);
                Integer keeper = null;
                int curr = 0;
                for(Entity pipelineRun : areaRuns) {
                    populateChildren(pipelineRun);
                    if (EntityUtils.getLatestChildOfType(pipelineRun, EntityConstants.TYPE_ERROR)!=null) {
                        keeper = curr;
                        break;
                    }
                    curr++;   
                }
                if (keeper!=null) {
                    Entity lastGoodRun = areaRuns.remove(keeper.intValue());
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
            deleteUnannotated(areaRuns);
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

    	logger.info("    Found "+toReallyDelete.size()+" non-annotated results for deletion:");
    	if (toReallyDelete.isEmpty()) return;
	
    	if (!isDebug) {
    		int c = 0;
    		for(Entity child : toReallyDelete) {
    			entityBean.deleteEntityTree(ownerKey, child.getId());
    			c++;
    			numRunsDeleted++;
    		}
        	logger.info("    Deleted "+c+" result trees");
    	}
    }
}
