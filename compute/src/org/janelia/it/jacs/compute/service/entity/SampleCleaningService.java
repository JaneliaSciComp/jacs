package org.janelia.it.jacs.compute.service.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.api.SolrBeanLocal;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Removes unneeded (unannotated, not final) results from Samples. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleCleaningService implements IService {

    public transient static final String PARAM_testRun = "is test run";
    
    protected Logger logger;
    protected Task task;
    protected String username;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected SolrBeanLocal solrBean;
    
    private boolean isDebug = false;
    private int numSamples = 0;
    private int numResults = 0;
    private int numResultsDeleted = 0;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            solrBean = EJBFactory.getLocalSolrBean();
            username = task.getOwner();

            String testRun = task.getParameter(PARAM_testRun);
            if (testRun!=null) {
            	isDebug = Boolean.parseBoolean(testRun);	
            }            
            
            logger.info("Cleaning old results from samples for user: "+username);
            
            List<Entity> samples = entityBean.getUserEntitiesByTypeName(username, EntityConstants.TYPE_SAMPLE);

            logger.info("Will process "+samples.size()+" samples...");
            
            for(Entity sample : samples) {
            	logger.info("Processing "+sample.getName());
            	
            	entityBean.loadLazyEntity(sample, false);
            	
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
                	Set<Entity> toDelete = new HashSet<Entity>();
                	for(Entity child : candidates) {
                		SolrQuery query = new SolrQuery("(id:"+child.getId()+" OR ancestor_ids:"+child.getId()+") AND all_annotations:*");
                		SolrResults results = solrBean.search(query, false);
                		long numFound = results.getResponse().getResults().getNumFound();
                		if (numFound>0) {
                        	logger.info("Rejecting candidate "+child.getId()+" because it and its ancestors have "+numFound+" annotations");
                        	continue;
                		}
                		toDelete.add(child);
                	}

                	logger.info("  Found "+toDelete.size()+" non-annotated results for deletion:");

                	if (!toDelete.isEmpty()) {
                    	for(EntityData ed : sample.getOrderedEntityData()) {
                    		if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_RESULT)) {
                    			Entity child = ed.getChildEntity();
                    			if (child!=null) {
        		            		boolean candidate = candidates.contains(child);
        		            		boolean noAnnots = toDelete.contains(child);
        		            		logger.info("    "+child.getName()+" ("+child.getEntityType().getName()+") "+(candidate?"-> Candidate":"")+" "+(noAnnots?"-> DELETE":""));
                    			}
                    		}
                    	}
                    	
                    	if (!isDebug) {
                    		int c = 0;
                    		for(Entity child : toDelete) {
                    			entityBean.deleteSmallEntityTree(username, child.getId());
                    			c++;
                    			numResultsDeleted++;
                    		}
                        	logger.info("  Deleted "+c+" result trees");
                    	}
                	}
            	}
            	
            	numSamples++;
            }
            
            logger.info("Considered "+numSamples+" samples with "+numResults+" results. Deleted "+numResultsDeleted+" results.");
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running SampleCleaningService:" + e.getMessage(), e);
        }
    }
    
    
}
