package org.janelia.it.jacs.compute.launcher.indexing;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.util.DedupingDelayQueue;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
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
	private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
	private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
	private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");
	private static Logger logger = Logger.getLogger(IndexingManagerImpl.class);

	private DedupingDelayQueue<WorkItem> queue;
	private DedupingDelayQueue<WorkItem> removalQueue;
	private ConcurrentSkipListMap<Long, DedupingDelayQueue<Long>> ancestorQueues;
	
	private SolrConnector solr;
	private DomainDAO dao;
	private boolean processing = false;
	
	public void create() throws Exception {
		this.queue = new DedupingDelayQueue<WorkItem>() {
			@Override
			public void process(List<WorkItem> domainObjs) {
				try {
					if (domainObjs.isEmpty()) return;
					List<DomainObject> domainObjList = new ArrayList<>();
					for (WorkItem item: domainObjs) {
						domainObjList.add(dao.getDomainObject(null, DomainUtils.getObjectClassByName(item.clazz), item.domainObjectId));
					}
					solr.updateIndices(domainObjList);
				} 
				catch (Throwable e) {
					logger.error("Error updating documents in index", e);
				}
			}
		};
		queue.setWorkItemDelay(3000); // Wait 3 seconds before indexing anything, to limit duplicates

		this.removalQueue = new DedupingDelayQueue<WorkItem>() {
			@Override
			public void process(List<WorkItem> domainObjs) {
				try {
					if (domainObjs.isEmpty()) return;
					List<DomainObject> domainObjList = new ArrayList<>();
					for (WorkItem item: domainObjs) {
						domainObjList.add(dao.getDomainObject(null, DomainUtils.getObjectClassByName(item.clazz), item.domainObjectId));
					}
					solr.removeDocuments(domainObjList);
				}
				catch (Throwable e) {
					logger.error("Error removing documents from index", e);
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

	public void scheduleIndexing(Long domainObjId, String clazz) {
		WorkItem wi = new WorkItem();
		wi.clazz = clazz;
		wi.domainObjectId = domainObjId;
		queue.addWorkItem(wi);
	}

	public void scheduleRemoval(Long domainObjId, String clazz) {
		WorkItem wi = new WorkItem();
		wi.clazz = clazz;
		wi.domainObjectId = domainObjId;
		removalQueue.addWorkItem(wi);
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
							solr.addNewAncestor(objectIds, newAncestorId);
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
		try {
			dao =  new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
			solr = new SolrConnector(dao);
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
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

	public class WorkItem {
		public Long domainObjectId;
		public String clazz;

	}
}
