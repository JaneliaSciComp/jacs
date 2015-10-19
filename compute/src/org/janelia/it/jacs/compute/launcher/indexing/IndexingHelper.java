package org.janelia.it.jacs.compute.launcher.indexing;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
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
	
	public static void sendReindexingMessage(Long entityId) throws Exception {
		AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
		messageInterface.startMessageSession(queueName);
		ObjectMessage message = messageInterface.createObjectMessage();
		message.setLongProperty("ENTITY_ID", entityId);
		messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
	}

	public static void sendAddAncestorMessage(Long entityId, Long newAncestorId) throws Exception {
		AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
		messageInterface.startMessageSession(queueName);
		ObjectMessage message = messageInterface.createObjectMessage();
		message.setObjectProperty("ENTITY_ID", entityId);
		message.setLongProperty("NEW_ANCESTOR_ID", newAncestorId);
		messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
	}
	
	public static void updateIndex(Long entityId) {
		if (!ENABLE_INDEXING) return;
		try {
			sendReindexingMessage(entityId);
		}
		catch (Exception e) {
			logger.error("Error sending reindexing message for "+entityId,e);
		}
	}

	public static void updateIndexAddAncestor(Long entityId, Long newAncestorId) {
		if (!ENABLE_INDEXING) return;
		try {
			sendAddAncestorMessage(entityId, newAncestorId);
		}
		catch (Exception e) {
			logger.error("Error sending add ancestor message for "+entityId+" (new ancestor is: "+newAncestorId+")",e);
		}
	}
	
	public static void updateIndex(Entity entity) {
		updateIndex(entity.getId());
	}
	
}
