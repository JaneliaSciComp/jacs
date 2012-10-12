package org.janelia.it.jacs.compute.service.entity;

import java.util.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.SolrBeanLocal;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

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
    private int numResults = 0;
    private int numResultsDeleted = 0;
    
    public void execute() throws Exception {

        solrBean = EJBFactory.getLocalSolrBean();

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }            
        
        logger.info("Cleaning old results from samples for user: "+user.getUserLogin());
        
        List<Entity> samples = entityBean.getUserEntitiesByTypeName(user.getUserLogin(), EntityConstants.TYPE_SAMPLE);

        logger.info("Will process "+samples.size()+" samples...");
        
        for(Entity sample : samples) {
        	logger.info("Processing "+sample.getName());
        	
        	populateChildren(sample);
        	processSample(sample);
        	
        	
        	Set<String> seenNames = new HashSet<String>();
        	Set<Entity> candidates = new HashSet<Entity>();
        	
        	List<EntityData> orderedEds = sample.getOrderedEntityData();
        	Collections.reverse(orderedEds);
        	
        	for(EntityData ed : orderedEds) {
        		if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
        			Entity child = ed.getChildEntity();
        			if (child!=null) {
        				numResults++;
                		if (seenNames.contains(child.getName())) {
                			candidates.add(child);
                		}
                		seenNames.add(child.getName());	
        			}
        		}
        	}

        	logger.info("  Found "+candidates.size()+" candidates for deletion");
        	
        	if (!candidates.isEmpty()) {

        	}
        	
        	numSamples++;
        }
        
        logger.info("Considered "+numSamples+" samples with "+numResults+" results. Deleted "+numResultsDeleted+" results.");
    }
    
    private void processSample(Entity sample) {
    	
    	List<Entity> toDelete = new ArrayList<Entity>();
    	
    	List<Entity> pipelineRuns = new ArrayList<Entity>();
    	
    	for(Entity pipelineRun : sample.getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
    		
    		populateChildren(pipelineRun);
    		Entity error = pipelineRun.getLatestChildOfType(EntityConstants.TYPE_ERROR);
    		
    		if (error!=null) {
    			toDelete.add(pipelineRun);
    		}
    		else {
    			pipelineRuns.add(pipelineRun);
    		}
    	}
    	
    }
    
    private void deleteUnannotated(List<Entity> toDelete) throws ComputeException {
    	
    	Set<Entity> toReallyDelete = new HashSet<Entity>();
    	for(Entity child : toDelete) {
    		SolrQuery query = new SolrQuery("(id:"+child.getId()+" OR ancestor_ids:"+child.getId()+") AND all_annotations:*");
    		SolrResults results = solrBean.search(query, false);
    		long numFound = results.getResponse().getResults().getNumFound();
    		if (numFound>0) {
            	logger.info("Rejecting candidate "+child.getId()+" because it and its ancestors have "+numFound+" annotations");
            	continue;
    		}
    		toReallyDelete.add(child);
    	}

    	logger.info("  Found "+toReallyDelete.size()+" non-annotated results for deletion:");

    	if (!toReallyDelete.isEmpty()) {
//        	for(EntityData ed : sample.getOrderedEntityData()) {
//        		if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
//        			Entity child = ed.getChildEntity();
//        			if (child!=null) {
//	            		boolean candidate = candidates.contains(child);
//	            		boolean noAnnots = toReallyDelete.contains(child);
//	            		logger.info("    "+child.getName()+" ("+child.getEntityType().getName()+") "+(candidate?"-> Candidate":"")+" "+(noAnnots?"-> DELETE":""));
//        			}
//        		}
//        	}
        	
        	if (!isDebug) {
        		int c = 0;
        		for(Entity child : toReallyDelete) {
        			entityBean.deleteSmallEntityTree(user.getUserLogin(), child.getId());
        			c++;
        			numResultsDeleted++;
        		}
            	logger.info("  Deleted "+c+" result trees");
        	}
    	}
    }
}
