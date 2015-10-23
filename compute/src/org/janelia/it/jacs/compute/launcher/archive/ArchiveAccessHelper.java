package org.janelia.it.jacs.compute.launcher.archive;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.model.tasks.Task;

import javax.jms.ObjectMessage;
import javax.jms.Queue;
import java.util.Set;

/**
 * Helper class for moving to and from the archive.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ArchiveAccessHelper {

    protected static Logger logger = Logger.getLogger(ArchiveAccessHelper.class);
    
	private static final String queueName = "queue/archiveAccess";
	
//	public static void sendMoveToArchiveMessage(String filePath, Queue replyToQueue) throws Exception {
//		AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
//		messageInterface.startMessageSession(queueName, AsyncMessageInterface.LOCAL_CONNECTION_FACTORY);
//		ObjectMessage message = messageInterface.createObjectMessage();
//		message.setStringProperty("REQUEST", ArchiveAccessMDB.REQUEST_MOVE_TO_ARCHIVE);
//		message.setStringProperty("FILE_PATH", filePath);
//		if (replyToQueue != null) {
//            message.setJMSReplyTo(replyToQueue);
//        }
//		messageInterface.sendMessageWithinTransaction(message);
//        messageInterface.commit();
//        messageInterface.endMessageSession();
//	}
//
    public static void sendMoveToArchiveMessage(Set<String> filePaths, Queue replyToQueue) throws Exception {
        AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
        messageInterface.startMessageSession(queueName);
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

//    public static void sendCopyFromArchiveMessage(List<String> sourceFilePaths, List<String> targetFilePaths) throws Exception {
//        AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
//        messageInterface.startMessageSession(queueName, AsyncMessageInterface.LOCAL_CONNECTION_FACTORY);
//        ObjectMessage message = messageInterface.createObjectMessage();
//        message.setStringProperty("REQUEST", ArchiveAccessMDB.REQUEST_COPY_FROM_ARCHIVE);
//        message.setStringProperty("SOURCE_FILE_PATHS", Task.csvStringFromCollection(sourceFilePaths));
//        message.setStringProperty("TARGET_FILE_PATHS", Task.csvStringFromCollection(targetFilePaths));
//        messageInterface.sendMessageWithinTransaction(message);
//        messageInterface.commit();
//        messageInterface.endMessageSession();
//    }
//
//    public static Task synchronousGridifiedArchiveCopy(Task parentTask, 
//            List<String> sourceFilePaths, List<String> targetFilePaths, boolean async) throws Exception {
//
//        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
//        taskParameters.add(new TaskParameter(ArchiveGridService.PARAM_sourceFilePaths, Task.csvStringFromCollection(sourceFilePaths), null)); 
//        taskParameters.add(new TaskParameter(ArchiveGridService.PARAM_targetFilePaths, Task.csvStringFromCollection(targetFilePaths), null)); 
//        
//        Task subtask = new GenericTask(new HashSet<Node>(), parentTask.getOwner(), new ArrayList<Event>(), 
//                taskParameters, "archiveGridCopy", "Archive Grid Copy");
//        subtask.setParentTaskId(parentTask.getObjectId());
//        subtask = EJBFactory.getLocalComputeBean().saveOrUpdateTask(subtask);
//
//        logger.info("Launching "+subtask.getJobName()+", parent task id="+parentTask.getObjectId()+", subtask id="+subtask.getObjectId());
//        EJBFactory.getLocalComputeBean().submitJob("ArchiveGridCopy", subtask.getObjectId());
//
//        if (async) return subtask;
//
//        logger.info("Waiting for completion of archive copy subtask (id="+subtask.getObjectId()+")");
//        boolean complete = false;
//        long start = System.currentTimeMillis();
//        long timeoutMs = TIMEOUT_SECONDS*1000;
//        while (!complete) {
//            String[] statusTypeAndValue = EJBFactory.getLocalComputeBean().getTaskStatus(subtask.getObjectId());
//            if (statusTypeAndValue[0]!=null && Task.isDone(statusTypeAndValue[0])) {
//                complete = true;
//            }
//            else {
//                if ((System.currentTimeMillis()-start)>timeoutMs) {
//                    throw new Exception("Timed out after waiting "+timeoutMs+
//                            " milliseconds for archive copy subtask to finish (id="+subtask.getObjectId()+")");
//                }
//                Thread.sleep(5000);
//            }
//        }
//        
//        logger.info("Archive copy subtask is complete (id="+subtask.getObjectId()+")");
//        return subtask;
//    }
}
