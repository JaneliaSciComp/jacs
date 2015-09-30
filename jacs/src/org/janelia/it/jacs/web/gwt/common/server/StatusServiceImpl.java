
package org.janelia.it.jacs.web.gwt.common.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
//import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.FileNodeDAO;
import org.janelia.it.jacs.server.access.NodeDAO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.api.JobControlAPI;
import org.janelia.it.jacs.server.api.SearchAPI;
import org.janelia.it.jacs.shared.tasks.*;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 30, 2006
 * Time: 2:39:30 PM
 */
public class StatusServiceImpl extends JcviGWTSpringController implements StatusService {
    static Logger logger = Logger.getLogger(StatusServiceImpl.class.getName());

    private TaskDAO taskDAO;
    private NodeDAO nodeDAO;
    private FileNodeDAO fileNodeDAO;
    private transient SearchAPI searchAPI;
    private JobControlAPI jobControlAPI;

    public void setSearchAPI(SearchAPI searchAPI) {
        this.searchAPI = searchAPI;
    }

    public void setJobControlAPI(JobControlAPI jobControlAPI) {
        this.jobControlAPI = jobControlAPI;
    }

    public TaskDAO getTaskDAO() {
        return taskDAO;
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public FileNodeDAO getFileNodeDAO() {
        return fileNodeDAO;
    }

    public void setFileNodeDAO(FileNodeDAO fileNodeDAO) {
        this.fileNodeDAO = fileNodeDAO;
    }

    public NodeDAO getNodeDAO() {
        return nodeDAO;
    }

    public void setNodeDAO(NodeDAO nodeDAO) {
        this.nodeDAO = nodeDAO;
    }

    public SearchAPI getSearchAPI() {
        return searchAPI;
    }

    public JobControlAPI getJobControlAPI() {
        return jobControlAPI;
    }

    public JobInfo getTaskResultForUser(String taskId) throws GWTServiceException {
        JobInfo jobInfo;
        try {
            // Commenting this out for the Prok pipeline tables. 
            //validateUserByTaskId(taskId);
            jobInfo = taskDAO.getJobStatusByJobId(new Long(taskId.trim()));

//            TaskStatus taskStatus = getTaskStatus(taskId);
//
//            jobInfo.setIsRunningOnTheGrid(taskStatus.isRunning());
//            jobInfo.setIsWaitingOnTheGrid(taskStatus.isWaiting());
//            if (taskStatus.isWaiting())
//                jobInfo.setJobOrder(getTaskOrder(taskId));
//
//            jobInfo.setPercentComplete(getPercentCompleteOfTask(taskId));
//

        }
        catch (Exception e) {
            logger.error("Exception during acquisition of the job information for task '" + taskId + "'. " +
                    "Returning empty status", e);
            throw new GWTServiceException("Exception during acquisition of the job information for task '" + taskId + "'. ",
                    e);
        }
        return jobInfo;
    }

    public void markTaskForDeletion(String taskId) throws GWTServiceException {
        try {

            Long taskID = new Long(taskId);
            Task existingTask = taskDAO.getTaskById(taskID);
            if (existingTask != null) {

                // Delete the job from the Grid
                jobControlAPI.cancelTask(taskID);

                existingTask.setTaskDeleted(true);
                Event deleteEvent = new Event("Task " + taskId + " marked for deletion",
                        new Date(),
                        Event.DELETED_EVENT);
                existingTask.addEvent(deleteEvent);
                taskDAO.saveOrUpdateTask(existingTask);
            }
        }
        catch (Throwable t) {
            logger.error("Update task '" + taskId + "' failed", t);
            throw new GWTServiceException("Update task '" + taskId + "' failed");
        }
    }

    public void purgeTask(String taskId) throws GWTServiceException {
        try {
            taskDAO.purgeTask(new Long(taskId));
        }
        catch (Throwable e) {
            throw new GWTServiceException("Delete task '" + taskId + "' failed");
        }
    }

//    public RnaSeqJobInfo[] getPagedRnaSeqPipelineTaskResultsForUser(String classname, int startIndex, int numRows, SortArgument[] sortArgs)
//            throws GWTServiceException {
//
//        if (logger.isInfoEnabled()) {
//            logger.info("getPagedRnaSeqPipelineTaskResultsForUser: task class=" + classname + ", startIndex=" + startIndex + ", numRows=" + numRows);
//            for (SortArgument s : sortArgs) {
//                logger.info("sortArgument=" + s.getSortArgumentName() + " dir=" + s.getSortDirection());
//            }
//        }
//
//        try {
//            // TODO: figure out why this is getting called w/startIndex==-1
//            if (startIndex == -1) {
//                logger.info("startIndex is -1, returning null");
//                return null;
//            }
//
//            // Get the jobs in the specified range
//            List<RnaSeqJobInfo> jobs = taskDAO.getPagedRnaSeqJobsForUserLogin(classname, getSessionUser().getUserLogin(),
//                    null, startIndex, numRows, sortArgs);
//            logger.info("taskDAO.getPagedRnaSeqJobsForUserLogin returned job count=" + jobs.size());
//            return jobs.toArray(new RnaSeqJobInfo[jobs.size()]);
//        }
//        catch (DaoException e) {
//            logger.error("Exception during acquisition of the job information, returning empty status array.", e);
//            throw new GWTServiceException("Exception during acquisition of the job information", e);
//        }
//        catch (Throwable t) {
//            logger.error("Caught Throwable during acquisition of the job information", t);
//            throw new GWTServiceException("Unexpected exception during acquisition of the job information", t);
//        }
//    }
//
    public BlastJobInfo[] getPagedBlastTaskResultsForUser(String classname, int startIndex, int numRows, SortArgument[] sortArgs)
            throws GWTServiceException {

        if (logger.isInfoEnabled()) {
            logger.info("getPagedBlastTaskResultsForUser: task class=" + classname + ", startIndex=" + startIndex + ", numRows=" + numRows);
            for (SortArgument s : sortArgs) {
                logger.info("sortArgument=" + s.getSortArgumentName() + " dir=" + s.getSortDirection());
            }
        }

        try {
            // TODO: figure out why this is getting called w/startIndex==-1
            if (startIndex == -1) {
                logger.info("startIndex is -1, returning null");
                return null;
            }

            // Get the jobs in the specified range
            List<BlastJobInfo> jobs = taskDAO.getPagedBlastJobsForUserLogin(classname, getSessionUser().getUserLogin(),
                    null, startIndex, numRows, sortArgs);
            logger.info("taskDAO.getPagedBlastJobsStatusForUserLogin returned job count=" + jobs.size());


            // Set the status of corresponding grid job and it's order in the queue to this BlastInfo object
//            for (BlastJobInfo job : jobs) {
//                String taskId = job.getJobId();
//                TaskStatus taskStatus = getTaskStatus(taskId);
//
//                if (taskStatus != null) {
////                    logger.debug("getPagedBlastTaskResultsForUser: Task id " + taskId + " : Running ? " + taskStatus.isRunning() +
////                                    " Waiting ? " + taskStatus.isWaiting() + " Done ? " + taskStatus.isCompleted());
//
//                    job.setIsWaitingOnTheGrid(taskStatus.isWaiting());
//                    job.setIsRunningOnTheGrid(taskStatus.isRunning());
//                    if (taskStatus.isWaiting()) {
//                        job.setJobOrder(getTaskOrder(taskId));
//                    }
//                    job.setPercentComplete(getPercentCompleteOfTask(taskId));
//
//                } // end if taskStatus is waiting
//            }   // end of for loop
            return jobs.toArray(new BlastJobInfo[jobs.size()]);
        }
        catch (DaoException e) {
            logger.error("Exception during acquisition of the job information, returning empty status array.", e);
            throw new GWTServiceException("Exception during acquisition of the job information", e);
        }
        catch (Throwable t) {
            logger.error("Caught Throwable during acquisition of the job information", t);
            throw new GWTServiceException("Unexpected exception during acquisition of the job information", t);
        }
    }

//    // These methods are used to retrieve Recruitment Viewer data ----------------------------------------------------
//    public Integer getNumRVUserTaskResults(String likeString) throws GWTServiceException {
//        return getNumRVTaskResultsBase(getSessionUser().getUserLogin(), likeString);
//    }
//
//    public Integer getNumRVSystemTaskResults(String likeString) throws GWTServiceException {
//        return getNumRVTaskResultsBase(User.SYSTEM_USER_LOGIN, likeString);
//    }
//
//    private Integer getNumRVTaskResultsBase(String username, String likeString) throws GWTServiceException {
//        try {
//            // count only the tasks owned by system
//            return taskDAO.getNumTasksForUserLoginByClass(username,
//                    likeString,
//                    RecruitmentViewerFilterDataTask.class.getName(), false);
//        }
//        catch (DaoException e) {
//            logger.error("Error getting the number of recruitment tasks.\n" + e.getMessage());
//            throw new GWTServiceException("Get number of system task results failed");
//        }
//    }
//
//    public RecruitableJobInfo[] getPagedRVUserTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
//        return getPagedRVTaskResultsBase(getSessionUser().getUserLogin(), likeString, startIndex, numRows, sortArgs);
//    }
//
//    public RecruitableJobInfo[] getPagedRVSystemTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
//        return getPagedRVTaskResultsBase(User.SYSTEM_USER_LOGIN, likeString, startIndex, numRows, sortArgs);
//    }
//
//    private RecruitableJobInfo[] getPagedRVTaskResultsBase(String username, String likeString, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
//        RecruitableJobInfo[] jobs;
//        try {
//            // select only the tasks that have results and are owned by system
//            List<Task> tmpTasks = taskDAO.getPagedTasksForUserLoginByClass(username,
//                    likeString,
//                    RecruitmentViewerFilterDataTask.class.getName(),
//                    startIndex,
//                    numRows,
//                    sortArgs,
//                    false);
//            jobs = new RecruitableJobInfo[tmpTasks.size()];
//            for (int i = 0; i < tmpTasks.size(); i++) {
//                jobs[i] = RecruitmentTaskToInfoTranslator.getInfoForRecruitmentResultTask(
//                        (RecruitmentViewerFilterDataTask) tmpTasks.get(i));
//            }
//            return jobs;
//        }
//        catch (Exception e) {
//            logger.error("\n\n\nCaught error in SSI \n\n\n", e);
//            throw new GWTServiceException("Get System Task Results failed.");
//        }
//    }
//
    public List<String> getUserTaskQueryNames() throws GWTServiceException {
        return getTaskQueryNamesBase(getSessionUser().getUserLogin());
    }

    public List<String> getSystemTaskQueryNames() throws GWTServiceException {
        return getTaskQueryNamesBase(User.SYSTEM_USER_LOGIN);
    }

    private List<String> getTaskQueryNamesBase(String username) throws GWTServiceException {
        try {
            return taskDAO.getTaskQueryNamesForUser(username);
        }
        catch (Exception e) {
            throw new GWTServiceException("Update Task Failed");
        }
    }

    public List<SearchJobInfo> getPagedSearchInfoForUser(int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
        List<SearchJobInfo> searches;
        try {
            // Get the search info
            searches = taskDAO.getPagedSearchInfoForUserLogin(getSessionUser().getUserLogin(), startIndex, numRows, sortArgs);

            // Retrieve and set the number of hits for each category in each search
            for (SearchJobInfo search : searches) {
                Map<String, Integer> hits = searchAPI.getCategorySearchResultsByTaskId(getSessionUser().getUserLogin(),
                        new Long(search.getJobId()), search.getCategories().keySet());
                for (String category : hits.keySet())
                    search.getCategories().get(category).setNumHits(hits.get(category));
            }
        }
        catch (Exception e) {
            logger.error("Error retrieving searchInfo", e);
            throw new GWTServiceException("Retrieve search info failed");
        }
        return searches;
    }

//    public RecruitableJobInfo getRecruitmentTaskById(String taskId) throws GWTServiceException {
//        try {
//            Task task = taskDAO.getTaskById(new Long(taskId));
//            if (task instanceof RecruitmentViewerFilterDataTask)
//                return RecruitmentTaskToInfoTranslator.getInfoForRecruitmentResultTask(
//                        (RecruitmentViewerFilterDataTask) task);
//            else
//                return null;
//        }
//        catch (Exception e) {
//            throw new GWTServiceException("getSystemTaskById(" + taskId + ")" + " failed: " + e.getMessage());
//        }
//    }
//
//    public RecruitableJobInfo getRecruitmentFilterTaskByUserPipelineId(String parentTaskId) throws GWTServiceException {
//        try {
//            Task task = taskDAO.getRecruitmentFilterTaskByUserPipelineId(new Long(parentTaskId));
//            if (task instanceof RecruitmentViewerFilterDataTask) {
//                return RecruitmentTaskToInfoTranslator.getInfoForRecruitmentResultTask(
//                        (RecruitmentViewerFilterDataTask) task);
//            }
//            else {
//                return null;
//            }
//        }
//        catch (Exception e) {
//            throw new GWTServiceException("getRecruitmentFilterTaskByUserPipelineId(" + parentTaskId + ")" + " failed: " + e.getMessage());
//        }
//    }
//
    // GenericService Task Monitoring Methods
    public Integer getNumTaskResultsForUser(String taskClassName) throws GWTServiceException {
        try {
            // count only the tasks owned by system
            return taskDAO.getNumTasksForUserLoginByClass(getSessionUser().getUserLogin(),
                    null,
                    taskClassName, true);
        }
        catch (DaoException e) {
            logger.error("Error getting the number of " + taskClassName + " tasks.\n" + e.getMessage());
            throw new GWTServiceException("Get number of system task results failed");
        }
    }

    public JobInfo[] getPagedTaskResultsForUser(String taskClassName, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
        JobInfo[] jobs;
        try {
            // select only the tasks that have results and are owned by system
            List<Task> tmpTasks = taskDAO.getPagedTasksForUserLoginByClass(getSessionUser().getUserLogin(),
                    null,
                    taskClassName,
                    startIndex,
                    numRows,
                    sortArgs, true);
            jobs = new JobInfo[tmpTasks.size()];
            for (int i = 0; i < tmpTasks.size(); i++) {
                Task tmpTask = tmpTasks.get(i);
                jobs[i] = getJobInfoFromTask(tmpTask);
            }
            return jobs;
        }
        catch (Exception e) {
            logger.error("\n\n\nCaught error in SSI \n\n\n", e);
            throw new GWTServiceException("Get System Task Results failed.");
        }
    }

    public Integer getNumTaskResults(String taskClassName) throws GWTServiceException {
        try {
            // count only the tasks owned by system
            return taskDAO.getNumTasksForUserLoginByClass(null,
                    null,
                    taskClassName, true);
        }
        catch (DaoException e) {
            logger.error("Error getting the number of " + taskClassName + " tasks.\n" + e.getMessage());
            throw new GWTServiceException("Get number of system task results failed");
        }
    }

    public JobInfo[] getPagedTaskResults(String taskClassName, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
        JobInfo[] jobs;
        try {
            // select only the tasks that have results and are owned by system
            List<Task> tmpTasks = taskDAO.getPagedTasksForUserLoginByClass(null,
                    null,
                    taskClassName,
                    startIndex,
                    numRows,
                    sortArgs, true);
            jobs = new JobInfo[tmpTasks.size()];
            for (int i = 0; i < tmpTasks.size(); i++) {
                Task tmpTask = tmpTasks.get(i);
                jobs[i] = getJobInfoFromTask(tmpTask);
            }
            return jobs;
        }
        catch (Exception e) {
            logger.error("\n\n\nCaught error in SSI \n\n\n", e);
            throw new GWTServiceException("Get System Task Results failed.");
        }
    }

    private JobInfo getJobInfoFromTask(Task tmpTask) throws GWTServiceException {
        JobInfo tmpInfo = new JobInfo();
//        TaskStatus taskStatus;
        String taskId = tmpTask.getObjectId().toString();
        tmpInfo.setUsername(tmpTask.getOwner());
        tmpInfo.setJobId(taskId);
        tmpInfo.setJobname(tmpTask.getJobName());
        tmpInfo.setJobNote(tmpTask.getTaskNote());
        // Force parameter loading
        Set<String> tmpKeySet = tmpTask.getParameterKeySet();
        Map<String, String> tmpParamMap = new HashMap<String, String>();
        for (String tmpKey : tmpKeySet) {
            tmpParamMap.put(tmpKey, tmpTask.getParameter(tmpKey));
        }
        tmpInfo.setParamMap(tmpParamMap);
//        taskStatus = getTaskStatus(taskId);
//
//        tmpInfo.setIsRunningOnTheGrid(taskStatus.isRunning());
//        tmpInfo.setIsWaitingOnTheGrid(taskStatus.isWaiting());
//        if (taskStatus.isWaiting())
//            tmpInfo.setJobOrder(getTaskOrder(taskId));
//        tmpInfo.setPercentComplete(getPercentCompleteOfTask(taskId));

        // Copy the events
        // Event-based attributes
        Event lastEvent = tmpTask.getLastEvent();
        if (lastEvent != null) {
            tmpInfo.setStatusDescription(lastEvent.getDescription());
            tmpInfo.setStatus(lastEvent.getEventType());
        }
        Event firstEvent = tmpTask.getFirstEvent();
        if (firstEvent != null) {
            tmpInfo.setSubmitted(new Date(firstEvent.getTimestamp().getTime()));
        }
        return tmpInfo;
    }

    public String replaceTaskJobName(String taskId, String jobName)
            throws GWTServiceException {
        String newJobName;
        try {
            Task existingTask = taskDAO.getTaskById(new Long(taskId));
            existingTask.setJobName(jobName);
            taskDAO.saveOrUpdateTask(existingTask);
            newJobName = existingTask.getJobName();
        }
        catch (Exception e) {
            throw new GWTServiceException("Update Task Failed");
        }
        return newJobName;
    }

//    /**
//     * This method returns the order a task amon the waiting jobs on SGE.
//     * @param taskId
//     * @return an integer that represents the order
//     * @throws GWTServiceException
//     */
//    public Integer getTaskOrder (String taskId) throws GWTServiceException {
//
//        int taskOrder;
//
//       //logger.debug("Getting order task id : " + taskId);
//
//        try {
//               taskOrder = jobControlAPI.getTaskOrder(new Long(taskId));
//        }
//        catch (Exception e) {
//            throw new GWTServiceException(e.getMessage());
//            /*return taskOrder;*/
//        }
//
//        //logger.debug("Order of task id : " + taskOrder);
//        return taskOrder;
//    }
//
//     /**
//     * This method returns the order a task amon the waiting jobs on SGE.
//     * @param taskId
//     * @return an integer that represents the order
//     * @throws GWTServiceException
//     */
//    public TaskStatus getTaskStatus (String taskId) throws GWTServiceException {
//
//       //logger.debug("Getting TaskStatus object for task id : " + taskId);
//
//       TaskStatus taskStatus;
//
//        try {
//               taskStatus = jobControlAPI.getTaskStatus(new Long(taskId));
//        }
//        catch (Exception e) {
//            throw new GWTServiceException(e.getMessage());
//            //return taskStatus;
//        }
//        //logger.debug(new StringBuilder().append(" Got TaskStatus : Details are :").append(taskStatus.getStatusDetails()));
//        return taskStatus;
//    }
//
//    public Integer getPercentCompleteOfTask (String taskId) throws GWTServiceException {
//
//       //logger.debug("Getting percent complete for Task : " + taskId);
//
//        Integer percentComplete;
//
//        try {
//               percentComplete = jobControlAPI.getPercentCompleteForATask(new Long(taskId));
//        }
//        catch (Exception e) {
//            throw new GWTServiceException(e.getMessage());
//            //return taskStatus;
//        }
//
//        //logger.debug("Percent complete for Task : " + taskId + " is " + percentComplete);
//        return percentComplete;
//    }

    //
    public Integer getFilteredNumTaskResultsByUserAndClass(String userLogin, String className, String likeString) throws GWTServiceException {
        try {
            // count only the tasks owned by system
            return taskDAO.getNumTasksForUserLoginByClass(userLogin, likeString, className, false);
        }
        catch (DaoException e) {
            logger.error("Error getting the number of recruitment tasks.\n" + e.getMessage());
            throw new GWTServiceException("Get number of system task results failed");
        }
    }

    public JobInfo[] getFilteredPagedTaskResultsByUserAndClass(String username, String className, String likeString, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
        JobInfo[] jobs;
        try {
            // select only the tasks that have results and are owned by system
            List<Task> tmpTasks = taskDAO.getPagedTasksForUserLoginByClass(username,
                    likeString,
                    className,
                    startIndex,
                    numRows,
                    sortArgs,
                    false);
            jobs = new JobInfo[tmpTasks.size()];
            for (int i = 0; i < tmpTasks.size(); i++) {
                jobs[i] = getJobInfoFromTask(tmpTasks.get(i));
            }
            return jobs;
        }
        catch (Exception e) {
            logger.error("\n\n\nCaught error in SSI \n\n\n", e);
            throw new GWTServiceException("Get System Task Results failed.");
        }
    }

    public List<String> getSystemTaskNamesByClass(String className) throws GWTServiceException {
        try {
            // count only the tasks owned by system
            return taskDAO.getTaskQueryNamesByClassAndUser(User.SYSTEM_USER_LOGIN, className);
        }
        catch (DaoException e) {
            logger.error("Error getting the number of recruitment tasks.\n" + e.getMessage());
            throw new GWTServiceException("Get number of system task results failed");
        }
    }

}