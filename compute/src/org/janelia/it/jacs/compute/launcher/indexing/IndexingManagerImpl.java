package org.janelia.it.jacs.compute.launcher.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.compute.util.DedupingDelayQueue;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
//import org.jboss.ejb3.annotation.Management;
//import org.jboss.ejb3.annotation.Service;
import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * The indexing manager is a singleton service responsible for keeping track of entities to be indexed, and then
 * running a batch of indexing. It can be injected into other beans via the @Depends annotation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
//@Service(objectName = "jboss:custom=IndexingManager")
//@Management(IndexingManagerManagement.class)
@Singleton( name = "IndexingManager" )
@Startup
public class IndexingManagerImpl implements IndexingManagerManagement {

	private static final int MAX_BATCH_SIZE = 10000;
	private static Logger logger = Logger.getLogger(IndexingManagerImpl.class);

	private DedupingDelayQueue<WorkItem> queue;
	private DedupingDelayQueue<Long> removalQueue;
	private ConcurrentSkipListMap<Long, DedupingDelayQueue<Long>> ancestorQueues;
	
	private SolrConnector solr;
	private boolean processing = false;
	
	public void create() throws Exception {
		this.queue = new DedupingDelayQueue<WorkItem>() {
			@Override
			public void process(List<WorkItem> domainObjs) {
				try {
					if (domainObjs.isEmpty()) return;
					List<DomainObject> domainObjList = new ArrayList<>();
					for (WorkItem item: domainObjs) {
						DomainObject domainObj = DomainDAL.getInstance().getDomainObject(null, item.className, item.domainObjectId);
						if (domainObj!=null) {
							domainObjList.add(domainObj);
						}
					}
					solr.updateIndices(domainObjList);
				} 
				catch (Throwable e) {
					logger.error("Error updating documents in index", e);
				}
			}
		};
		queue.setWorkItemDelay(3000); // Wait 3 seconds before indexing anything, to limit duplicates

		this.removalQueue = new DedupingDelayQueue<Long>() {
			@Override
			public void process(List<Long> domainObjs) {
				try {
					if (domainObjs.isEmpty()) return;
					solr.removeDocuments(domainObjs);
				}
				catch (Throwable e) {
					logger.error("Error removing documents from index", e);
				}
			}
		};
		removalQueue.setWorkItemDelay(3000); // Wait 3 seconds before indexing anything, to limit duplicates
		
		this.ancestorQueues = new ConcurrentSkipListMap<Long, DedupingDelayQueue<Long>>();
	}

	public void start() throws Exception {
	}

	public void stop() {
	}

	public void destroy() {
	}

	public void scheduleIndexing(Long domainObjId, String clazz) {
		WorkItem wi = new WorkItem();
		wi.className = clazz;
		wi.domainObjectId = domainObjId;
		queue.addWorkItem(wi);
	}

	public void scheduleRemoval(Long domainObjId) {
		logger.debug("Scheduling removal of item " + domainObjId);
		removalQueue.addWorkItem(domainObjId);
	}

	public void scheduleAddNewAncestor(final Long domainObjId, final Long newAncestorId) {
		logger.debug("Scheduling addition of new ancestor " + newAncestorId + " for " + domainObjId);
		synchronized (ancestorQueues) {
			DedupingDelayQueue<Long> ancestorQueue = ancestorQueues.get(newAncestorId);
			if (ancestorQueue==null) {
				logger.info("Creating new deduping queue for "+newAncestorId);
				ancestorQueue = new DedupingDelayQueue<Long>() {
					@Override
					public void process(List<Long> objectIds) {
						try {
							logger.info("Processing "+objectIds.size()+" ancestor adds");
							if (objectIds.isEmpty()) return;
							//solr.addNewAncestor(objectIds, newAncestorId);
						}
						catch (Throwable e) {
							logger.error("Error adding new ancestor", e);
						}
					}
				};
				ancestorQueue.setWorkItemDelay(3000); // Wait 3 seconds before indexing anything, to limit duplicates
				ancestorQueues.put(newAncestorId, ancestorQueue);
			}
			ancestorQueue.addWorkItem(domainObjId);
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

		DomainDAO dao = DomainDAOManager.getInstance().getDao();
	    solr = new SolrConnector(dao, false, false);

		int numQueued = queue.getQueueSize();
		int numIndexed = queue.process(MAX_BATCH_SIZE);
		if (numIndexed>0) {
			logger.info("Indexing batch complete. Num distinct ids queued: "+numQueued+", Num processed in this batch: "+numIndexed);
		}

		numIndexed = removalQueue.process(MAX_BATCH_SIZE);
		if (numIndexed>0) {
			logger.info("Removal batch complete. Num processed in this batch: "+numIndexed);
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

	public class WorkItem {
		public Long domainObjectId;
		public String className;

	}
}
