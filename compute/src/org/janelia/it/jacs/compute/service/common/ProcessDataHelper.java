
package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 18, 2007
 * Time: 9:00:45 AM
 */
public class ProcessDataHelper {

    /**
     * This method allows grid services to get the "result" directory which is used as the storehouse
     * for all grid processing.
     *
     * @param processData - data object which is getting manipulated for the result file node
     * @return FileNode - object which represents a directory on the filestore
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *          - data could not be found
     */
    public static FileNode getResultFileNode(IProcessData processData) throws MissingDataException {
        FileNode resultFileNode = (FileNode) processData.getItem(ProcessDataConstants.RESULT_FILE_NODE);
        if (resultFileNode == null) {
            Long resultNodeId = (Long) processData.getMandatoryItem(ProcessDataConstants.RESULT_FILE_NODE_ID);
            try {
                resultFileNode = (FileNode) new ComputeDAO().genericLoad(FileNode.class, resultNodeId);
                processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return resultFileNode;
    }

    public static Task getTask(IProcessData processData) throws MissingDataException {
        Long taskId;
        Task task = (Task) processData.getItem(IProcessData.TASK);
        // If the task is not in the data "session", ask for the id
        if (task == null) {
            taskId = (Long) processData.getMandatoryItem(IProcessData.PROCESS_ID);
        }
        /**
         * If the task object is in the data "session", grab its id as we want a fresh one anyway
         * this may be redundant but we probably shouldn't expect objects from the db to exist in processData as
         * some services could take extremely long times
         */
        else {
            taskId = task.getObjectId();
        }
        try {
            task = new ComputeDAO().getTaskWithMessagesAndParameters(taskId);
            processData.putItem(IProcessData.TASK, task);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return task;
    }

    public static FileNode getRootFileNode(IProcessData processData) throws MissingDataException, IOException {
        Logger logger = getLoggerForTask(processData, ProcessDataHelper.class);
        if (logger!=null) {
            logger.debug("getRootFileNode() found non-null logger");
        }
        Task tmpTask=getTask(processData);
        if (logger!=null) {
            if (tmpTask!=null) {
                logger.debug("getRootFileNode() found non-null task id="+tmpTask.getObjectId());
            } else {
                logger.debug("getRootFileNode() found null task");
            }
        }
        Task task = getRootTask(tmpTask, logger);
        // If no session exists, return null
        if (null == task /*|| !(task instanceof SessionTask)*/) {
            logger.debug("getRootFileNode() returning null because getRootTask() returned null");
            return null;
        }
        // else return the session node
        logger.debug("task from getRootTask has id="+task.getObjectId());
        Node tmpSessionNode = new ComputeDAO().getResultNodeByTaskId(task.getObjectId());
        if (null == tmpSessionNode/* || !(tmpSessionNode instanceof SessionFileNode)*/) {
            logger.debug("getRootFileNode() returning null because tmpSessionNode is null");
            return null;
        }
        return (FileNode) tmpSessionNode;
    }


    public static String getSessionRelativePath(IProcessData processData) throws MissingDataException, IOException {
        FileNode tmpNode = getRootFileNode(processData);
        if (null != tmpNode && !"".equals(new File(tmpNode.getDirectoryPath()).getName())) {
            String tmpRootSubdir = tmpNode.getSubDirectory();
            String tmpRootDir = FileNode.getTreePathForId(tmpNode.getObjectId());
            return tmpRootSubdir + File.separator + tmpRootDir;
        }
        return null;
    }

    /**
     * Recursive method to get the root sesison task, if one exists
     *
     * @param tmpTask - task we're looking for a parent of
     * @param logger  handle to the logger used for this action
     * @return the parent task if one exists
     */
    private static Task getRootTask(Task tmpTask, Logger logger) {
        Task tmpRootTask;
        if (null != tmpTask.getParentTaskId()) {
            tmpRootTask = getRootTask(new ComputeDAO().getTaskById(tmpTask.getParentTaskId()), logger);
        }
        else {
            tmpRootTask = tmpTask;
        }
        if (null != tmpRootTask /*&& tmpRootTask instanceof SessionTask*/) {
            return /*(SessionTask)*/tmpRootTask;
        }
        return null;
    }

    /**
     * This method provides a handle to the task-specific log file
     *
     * @param processData the collection of parameters relating to the task at-hand
     * @param loggerClass class to log for if the local logging flag is not present
     * @return returns the logger for the
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             error that it can't find the data requested
     * @throws java.io.IOException error trying to get reference to the desired file
     */
    public static Logger getLoggerForTask(IProcessData processData, Class loggerClass) throws MissingDataException, IOException {
        String uniqueTaskIdentifier = (processData.getMandatoryItem(IProcessData.PROCESS_ID)).toString();
        return getLoggerForTask(uniqueTaskIdentifier, loggerClass);
    }

    /**
     * This method provides a handle to the task-specific log file
     *
     * @param uniqueTaskIdentifier the specific task identifier we're processing for
     * @param loggerClass          class to log for if the local logging flag is not present
     * @return returns the logger for the
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             error that it can't find the data requested
     * @throws java.io.IOException error trying to get reference to the desired file
     */
    public static Logger getLoggerForTask(String uniqueTaskIdentifier, Class loggerClass) throws MissingDataException, IOException {
        //        Logger tmpLogger = Logger.getLogger("task"+uniqueTaskIdentifier);
//        if (null==tmpLogger.getAppender("task"+uniqueTaskIdentifier)) {
//            FileAppender tmpAppender = new FileAppender(new PatternLayout("%d %-5p [%c:%C{1}] - %m%n"),
//                getTmpLogFilePathForTask(uniqueTaskIdentifier), true);
//            tmpAppender.setName("task"+uniqueTaskIdentifier);
//            System.out.println("Added appender "+tmpAppender.getName());
//            tmpLogger.addAppender(tmpAppender);
//        }
//        if (null==tmpLogger.getAppender(loggerClass.toString())) {
//            Appender tmpAppender = returnLogger.getAppender(loggerClass.toString());
//            if (null!=tmpAppender) {
//                System.out.println("Added appender "+tmpAppender.getName());
//                tmpLogger.addAppender(tmpAppender);}
//        }
//        returnLogger = tmpLogger;
        return Logger.getLogger(loggerClass);
    }

    public static String getTmpLogFilePathForTask(String uniqueTaskIdentifier) {
        String finalPath;
        String tmpPath = SystemConfigurationProperties.getString("Logs.Dir");
        finalPath = tmpPath + File.separator + "task" + uniqueTaskIdentifier + ".log";
        return finalPath;
    }

    /**
     * This method should copy the process log over to the result directory, should one exist.
     *
     * @param processData the map of the objects used to process
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             could not find required data
     * @throws java.io.IOException problem trying to access the log file
     */
    protected void copyLogs(IProcessData processData) throws MissingDataException, IOException {
        // Grab the Task
        Task task = ProcessDataHelper.getTask(processData);
        FileNode resultNode = ProcessDataHelper.getRootFileNode(processData);
        if (null != resultNode) {
            File sourceFile = new File(ProcessDataHelper.getTmpLogFilePathForTask(task.getObjectId().toString()));
            File destFile = new File(resultNode.getDirectoryPath() + File.separator + sourceFile.getName());
            FileUtil.copyFile(sourceFile, destFile);
        }
    }

    public static String getProcessDataMapAsString(IProcessData processData) {
        StringBuilder sb=new StringBuilder();
        Set<Map.Entry<String,Object>> pdm = processData.entrySet();
        if (pdm==null) {
            sb.append("ProcessData map is null\n");
        } else {
            sb.append("ProcessData map contains "+pdm.size()+" entries\n");
            for (Map.Entry<String,Object> m : pdm) {
                if (m==null) {
                    sb.append("map entry is null\n");
                } else {
                    String key=m.getKey();
                    if (key==null) {
                        key="null";
                    }
                    Object o=m.getValue();
                    String value=o.toString();
                    if (value==null) {
                        value="null";
                    }
                    sb.append("ProcessData key=" + key + " value=" + value + "\n");
                }
            }
        }
        return sb.toString();
    }


}
