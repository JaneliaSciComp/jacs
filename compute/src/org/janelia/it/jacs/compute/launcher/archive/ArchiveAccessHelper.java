package org.janelia.it.jacs.compute.launcher.archive;

import java.util.Set;

import javax.jms.ObjectMessage;

import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Helper class for moving to and from the archive.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ArchiveAccessHelper {
	
	private static final String queueName = "queue/archiveAccess";
	
	public static void sendMoveToArchiveMessage(String filePath) throws Exception {
		AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
		messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
		ObjectMessage message = messageInterface.createObjectMessage();
		message.setStringProperty("REQUEST", ArchiveAccessMDB.REQUEST_MOVE_TO_ARCHIVE);
		message.setStringProperty("FILE_PATH", filePath);
		messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
	}

    public static void sendMoveToArchiveMessage(Set<String> filePaths) throws Exception {
        AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
        messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
        ObjectMessage message = messageInterface.createObjectMessage();
        message.setStringProperty("REQUEST", ArchiveAccessMDB.REQUEST_MOVE_TO_ARCHIVE);
        message.setObjectProperty("FILE_PATHS", Task.csvStringFromCollection(filePaths));
        messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
    }
}
