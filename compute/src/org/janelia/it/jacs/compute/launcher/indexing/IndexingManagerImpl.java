package org.janelia.it.jacs.compute.launcher.indexing;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.util.DedupingDelayQueue;
import org.janelia.it.jacs.model.entity.Entity;
import org.jboss.annotation.ejb.Management;
import org.jboss.annotation.ejb.Service;
import org.jboss.annotation.ejb.TransactionTimeout;


/**
 * The indexing manager is a singleton service responsible for keeping track of entities to be indexed, and then
 * running a batch of indexing. It can be injected into other beans via the @Depends annotation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Service(objectName = "jboss:custom=IndexingManager")
@Management(IndexingManagerManagement.class)
public class IndexingManagerImpl implements IndexingManagerManagement {

	private static final int MAX_BATCH_SIZE = 10000;
	
	private static Logger logger = Logger.getLogger(IndexingManagerImpl.class);

	private DedupingDelayQueue<Long> queue;
	
	private ConcurrentSkipListMap<Long, DedupingDelayQueue<Long>> ancestorQueues;
	
	private SolrDAO solrDAO;
	private boolean processing = false;
	
	public void create() throws Exception {
		this.queue = new DedupingDelayQueue<Long>() {
			@Override
			public void process(List<Long> entityIds) {
				try {
					if (entityIds.isEmpty()) return;
					List<Entity> entities = solrDAO.getEntitiesInList(null, entityIds);
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
		queue.setWorkItemDelay(3000); // Wait 3 seconds before indexing anything, to limit duplicates
		
		this.ancestorQueues = new ConcurrentSkipListMap<Long, DedupingDelayQueue<Long>>();
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
	
	public void scheduleAddNewAncestor(final Long entityId, final Long newAncestorId) {
		logger.debug("Scheduling addition of new ancestor "+newAncestorId+" for "+entityId);
		synchronized (ancestorQueues) {
			DedupingDelayQueue<Long> ancestorQueue = ancestorQueues.get(newAncestorId);
			if (ancestorQueue==null) {
				logger.info("Creating new deduping queue for "+newAncestorId);
				ancestorQueue = new DedupingDelayQueue<Long>() {
					@Override
					public void process(List<Long> entityIds) {
						try {
							logger.info("Processing "+entityIds.size()+" ancestor adds");
							if (entityIds.isEmpty()) return;
							solrDAO.addNewAncestor(entityIds, newAncestorId);
						} 
						catch (Throwable e) {
							logger.error("Error adding new ancestor", e);
						}
					}
				};
				ancestorQueue.setWorkItemDelay(3000); // Wait 3 seconds before indexing anything, to limit duplicates
				ancestorQueues.put(newAncestorId, ancestorQueue);
			}
			ancestorQueue.addWorkItem(entityId);
		}
	}
	
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	@TransactionTimeout(500000)
	public int runNextBatch() {
		synchronized (this) {
			if (processing) {
				return 0;
			}
			this.processing = true;
		}
		solrDAO = new SolrDAO(logger, false, true);
		int numQueued = queue.getQueueSize();
		int numIndexed = queue.process(MAX_BATCH_SIZE);
		if (numIndexed>0) {
			logger.info("Indexing batch complete. Num distinct ids queued: "+numQueued+", Num processed in this batch: "+numIndexed);
		}
		
		synchronized (ancestorQueues) {
			if (!ancestorQueues.isEmpty()) {
				Long ancestorId = ancestorQueues.firstKey();
				DedupingDelayQueue<Long> ancestorQueue = ancestorQueues.get(ancestorId);
				int numAncestorAddsQueued = ancestorQueue.getQueueSize();
				int numAncestorsAdded = ancestorQueue.process(MAX_BATCH_SIZE);
				if (numAncestorsAdded>0) {
					logger.info("Add ancestor batch complete for "+ancestorId+". Num distinct ids queued: "+numAncestorAddsQueued+", Num processed in this batch: "+numAncestorsAdded);
				}
				if (ancestorQueue.getQueueSize()==0) {
					logger.info("All work items complete for ancestor "+ancestorId+". Removing its queue from the work list.");
					ancestorQueues.remove(ancestorId);
				}
			}
		}
		
		synchronized (this) {
			this.processing = false;
		}
		return numIndexed;
	}
}
