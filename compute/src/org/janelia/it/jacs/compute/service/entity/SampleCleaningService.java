package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    	
    	logger.info("  Cleaning up sample "+sample.getName());
    	
    	List<Entity> toDelete = new ArrayList<Entity>();
    	
    	List<Entity> children = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
    	if (children.isEmpty()) return;
    	
    	// Remove latest run, we don't want to touch it
    	Entity lastRun = children.remove(children.size()-1);
    	populateChildren(lastRun);

    	// Clean up everything except the last good run and the last error run
    	for(Entity pipelineRun : children) {
    		populateChildren(pipelineRun);
    		if (EntityUtils.getLatestChildOfType(lastRun, EntityConstants.TYPE_ERROR)!=null) {
    			// Last run had error, so only delete other error runs
    			if (EntityUtils.getLatestChildOfType(pipelineRun, EntityConstants.TYPE_ERROR)!=null) {
    				toDelete.add(pipelineRun);
    			}
    		}
    		else {
    	    	// Clean up everything except the last run
    			toDelete.add(pipelineRun);
    		}
    	}
    	
		deleteUnannotated(toDelete);
    }
    
    private void deleteUnannotated(List<Entity> toDelete) throws ComputeException {
    	
    	Set<Entity> toReallyDelete = new HashSet<Entity>();
    	for(Entity entity : toDelete) {
    		long numFound = annotationBean.getNumDescendantsAnnotated(entity.getId());
    		if (numFound>0) {
            	logger.info("Rejecting candidate "+entity.getId()+" because it and its ancestors have "+numFound+" annotations");
            	continue;
    		}
    		toReallyDelete.add(entity);
    	}

    	logger.info("  Found "+toReallyDelete.size()+" non-annotated results for deletion:");
    	if (toReallyDelete.isEmpty()) return;
	
    	if (!isDebug) {
    		int c = 0;
    		for(Entity child : toReallyDelete) {
    			entityBean.deleteSmallEntityTree(ownerKey, child.getId());
    			c++;
    			numRunsDeleted++;
    		}
        	logger.info("  Deleted "+c+" result trees");
    	}
    }
}
