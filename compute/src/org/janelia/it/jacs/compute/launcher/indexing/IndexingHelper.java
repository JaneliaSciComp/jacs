package org.janelia.it.jacs.compute.launcher.indexing;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.entity.Entity;

import javax.jms.ObjectMessage;

/**
 * Helper class for asynchronous indexing. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IndexingHelper {

	private static final Boolean ENABLE_INDEXING = SystemConfigurationProperties.getBoolean("Solr.EnableIndexing");

	private static final String queueName = "queue/indexing";

	private static Logger logger = Logger.getLogger(IndexingHelper.class);

	public static void updateIndex(DomainObject domainObj) {
		if (!ENABLE_INDEXING) return;
		try {
			AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
			messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
			ObjectMessage message = messageInterface.createObjectMessage();
			message.setLongProperty("OBJECT_ID", domainObj.getId());
			message.setStringProperty("OBJECT_CLASS", domainObj.getClass().getName());
			message.setStringProperty("OPERATION", "UPDATE");
			messageInterface.sendMessageWithinTransaction(message);
			messageInterface.commit();
			messageInterface.endMessageSession();
		}
		catch (Exception e) {
			logger.error("Error sending reindexing message for "+domainObj.getId(),e);
		}
	}

	public static void removeFromIndex(DomainObject domainObj) {
		if (!ENABLE_INDEXING) return;
		try {
			AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
			messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
			ObjectMessage message = messageInterface.createObjectMessage();
			message.setLongProperty("OBJECT_ID", domainObj.getId());
			message.setStringProperty("OBJECT_CLASS", domainObj.getClass().getName());
			message.setStringProperty("OPERATION", "REMOVE");
			messageInterface.sendMessageWithinTransaction(message);
			messageInterface.commit();
			messageInterface.endMessageSession();
		}
		catch (Exception e) {
			logger.error("Error sending reindexing message for "+domainObj.getId(),e);
		}
	}

	public static void updateIndex(Long entityId) {
		//updateIndex(entity.getId());
	}

	public static void updateIndexAddAncestor(Long entityId, Long parentId) {
		//updateIndex(entity.getId());
	}

}
