package org.janelia.it.jacs.compute.launcher.indexing;

import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.model.entity.Entity;
import org.jboss.annotation.ejb.Management;
import org.jboss.annotation.ejb.Service;
import org.jboss.annotation.ejb.TransactionTimeout;


/**
 * The indexing manager is a singleton service responsible for keeping track of entities to be indexed, and then
 * running a batch of indexing. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Service(objectName = "jboss:custom=IndexingManager")
@Management(IndexingManagerManagement.class)
public class IndexingManagerImpl implements IndexingManagerManagement {

	private static final int MAX_BATCH_SIZE = 10000;
	
	private static Logger logger = Logger.getLogger(IndexingManagerImpl.class);

	private DedupingDelayQueue<Long> queue;
	
	private SolrDAO solrDAO;
	private boolean processing = false;
	
	public void create() throws Exception {
		this.queue = new DedupingDelayQueue<Long>() {
			@Override
			public void process(List<Long> entityIds) {
				try {
					if (entityIds.isEmpty()) return;
					List<Entity> entities = solrDAO.getEntitiesInList(entityIds);
					if (entities.size()!=entityIds.size()) {
						logger.warn("Query list contained "+entityIds.size()+" ids, but "+entities.size()+" were returned.");
					}
					if (entities.isEmpty()) return;
					solrDAO.updateIndex(entities);
				} 
				catch (Throwable e) {
					logger.error("Error updating index", e);
				}
			}
		};
		queue.setWorkItemDelay(10000); // Wait 10 seconds before indexing anything, to limit duplicates
	}

	public void start() throws Exception {
	}

	public void stop() {
	}

	public void destroy() {
	}

	public void scheduleIndexing(Long entityId) {
		queue.addWorkItem(entityId);
	}
	
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	@TransactionTimeout(500000)
	public int runNextBatch() {
		if (isProcessing()) {
			return 0;
		}
		setProcessing(true);
		solrDAO = new SolrDAO(logger, true);
		int numIndexed = queue.process(MAX_BATCH_SIZE);
		setProcessing(false);
		return numIndexed;
	}

	private synchronized void setProcessing(boolean processing) {
		this.processing = processing;
	}
	
	public synchronized boolean isProcessing() {
		return processing;
	}
}
