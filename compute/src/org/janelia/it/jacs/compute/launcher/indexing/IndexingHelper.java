package org.janelia.it.jacs.compute.launcher.indexing;

import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Helper class for asynchronous indexing. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IndexingHelper {
	
	private static final String queueName = "queue/indexing";
	
	private static Logger logger = Logger.getLogger(IndexingHelper.class);
	
	public static void sendReindexingMessage(Long entityId) throws Exception {
		AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
		messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
		ObjectMessage message = messageInterface.createObjectMessage();
		message.setLongProperty("ENTITY_ID", entityId);
		messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
	}
	
	public static void updateIndex(Long entityId) {
		try {
			sendReindexingMessage(entityId);
		}
		catch (Exception e) {
			logger.error("Error sending reindexing message for "+entityId,e);
		}
	}
	
	public static void updateIndex(Entity entity) {
		updateIndex(entity.getId());
	}
	
}
