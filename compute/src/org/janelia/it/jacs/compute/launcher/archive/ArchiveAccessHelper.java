package org.janelia.it.jacs.compute.launcher.archive;

import javax.jms.ObjectMessage;

import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;

/**
 * Helper class for moving to and from the archive.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ArchiveAccessHelper {
	
	private static final String queueName = "queue/archiveAccess";
	
	public static void sendArchiveSyncMessage(String filePath) throws Exception {
		AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
		messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
		ObjectMessage message = messageInterface.createObjectMessage();
		message.setStringProperty("REQUEST", "syncDir");
		message.setStringProperty("FILE_PATH", filePath);
		messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
	}
}
