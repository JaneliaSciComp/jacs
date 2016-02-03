package org.janelia.it.jacs.compute.launcher.indexing;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.compute.util.DedupingDelayQueue;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import javax.inject.Named;


/**
 * The indexing manager is a singleton service responsible for keeping track of entities to be indexed, and then
 * running a batch of indexing. It can be injected into other beans via the @Depends annotation.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Named
public class IndexingManager {

	private static final int MAX_BATCH_SIZE = 10000;
	private static final String OPERATION_REMOVE = "REMOVE";
	private static final String OPERATION_UPDATE = "UPDATE";
	private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
	private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
	private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
	private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");

	private static Logger logger = Logger.getLogger(IndexingManager.class);

	private static DedupingDelayQueue<DomainObject> updateQueue;
	private static DedupingDelayQueue<DomainObject> removalQueue;
	private static DomainDAO dao;
	private static SolrConnector solr;
	
	public static void init() throws Exception {
		dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
		solr = new SolrConnector(dao);
		updateQueue = new DedupingDelayQueue<DomainObject>() {
			@Override
			public void process(List<DomainObject> domainObjects) {
				try {
					if (domainObjects.isEmpty()) return;
					solr.updateIndices(domainObjects);
				} 
				catch (Throwable e) {
					logger.error("Error updating index", e);
				}
			}
		};
		updateQueue.setWorkItemDelay(3000); // Wait 3 seconds before indexing anything, to limit duplicates
		removalQueue = new DedupingDelayQueue<DomainObject>() {
			@Override
			public void process(List<DomainObject> domainObjects) {
				try {
					if (domainObjects.isEmpty()) return;
					solr.removeDocuments(domainObjects);
				}
				catch (Throwable e) {
					logger.error("Error removing document from index", e);
				}
			}
		};
	}

	public void start() throws Exception {
	}

	public void stop() {
	}

	public void destroy() {
	}

	public static void scheduleIndexing(Long domainObjId, String objectClass, String operation) {
		try {
			init();

			// for now, since old-style injection doesn't work in wildfly, directly call solr

			List<Long> objIds = new ArrayList<Long>();
			objIds.add(domainObjId);
			List<DomainObject> objList = dao.getDomainObjects(null, objectClass, objIds);
			if (!objList.isEmpty()) {
				if (operation.equals(OPERATION_REMOVE)) {
					solr.removeDocuments(objList);
				} else {
					solr.updateIndices(objList);
				}
			}
		} catch (Exception e) {
			logger.error("Error removing document from index", e);
		}

	}
}
