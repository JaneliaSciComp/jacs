package org.janelia.it.jacs.compute.launcher.archive;

import java.util.List;
import java.util.Set;

import javax.jms.ObjectMessage;
import javax.jms.Queue;

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
	
	public static void sendMoveToArchiveMessage(String filePath, Queue replyToQueue) throws Exception {
		AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
		messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
		ObjectMessage message = messageInterface.createObjectMessage();
		message.setStringProperty("REQUEST", ArchiveAccessMDB.REQUEST_MOVE_TO_ARCHIVE);
		message.setStringProperty("FILE_PATH", filePath);
		if (replyToQueue != null) {
            message.setJMSReplyTo(replyToQueue);
        }
		messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
	}

    public static void sendMoveToArchiveMessage(Set<String> filePaths, Queue replyToQueue) throws Exception {
        AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
        messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
        ObjectMessage message = messageInterface.createObjectMessage();
        message.setStringProperty("REQUEST", ArchiveAccessMDB.REQUEST_MOVE_TO_ARCHIVE);
        message.setObjectProperty("FILE_PATHS", Task.csvStringFromCollection(filePaths));
        if (replyToQueue != null) {
            message.setJMSReplyTo(replyToQueue);
        }
        messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
    }

    public static void sendCopyFromArchiveMessage(List<String> sourceFilePaths, List<String> targetFilePaths, Queue replyToQueue) throws Exception {
        AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
        messageInterface.startMessageSession(queueName, messageInterface.localConnectionType);
        ObjectMessage message = messageInterface.createObjectMessage();
        message.setStringProperty("REQUEST", ArchiveAccessMDB.REQUEST_MOVE_TO_ARCHIVE);
        message.setStringProperty("SOURCE_FILE_PATHS", Task.csvStringFromCollection(sourceFilePaths));
        message.setStringProperty("TARGET_FILE_PATHS", Task.csvStringFromCollection(targetFilePaths));
        if (replyToQueue != null) {
            message.setJMSReplyTo(replyToQueue);
        }
        messageInterface.sendMessageWithinTransaction(message);
        messageInterface.commit();
        messageInterface.endMessageSession();
    }
}
