
package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DispatcherDAO;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.status.TaskStatus;
import org.janelia.it.jacs.model.tasks.Task;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.jms.Message;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Jul 30, 2008
 * Time: 11:39:22 AM
 */
@Stateless(name = "JobControlEJB")
@TransactionManagement
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = StrictMaxPool.class, maxSize = 20, timeout = 10000)
public class JobControlBeanImpl implements JobControlBeanLocal, JobControlBeanRemote {
    private Logger logger = Logger.getLogger(this.getClass());

    public JobControlBeanImpl() {
    }

    public List<TaskStatus> getActiveTasks() {
        ComputeDAO computeDAO = new ComputeDAO(logger);
        List<TaskStatus> tasks = new LinkedList<TaskStatus>();
        try {

            List<Long> taskIDs = computeDAO.getActiveTasks();
            for (Long id : taskIDs) {
                tasks.add(getTaskStatus(id));
            }
        }
        catch (Exception e) {
            logger.error("Unable to retrieve active task data from the database", e);
            tasks = null;
        }
        return tasks;
    }

    public List<TaskStatus> getWaitingTasks() {
        ComputeDAO computeDAO = new ComputeDAO(logger);
        List<TaskStatus> tasks = new LinkedList<TaskStatus>();
        try {

            List<Long> taskIDs = computeDAO.getWaitingTasks();
            for (Long id : taskIDs) {
                tasks.add(getTaskStatus(id));
            }
        }
        catch (Exception e) {
            logger.error("Unable to retrieve data from the database", e);
            tasks = null;
        }
        return tasks;
    }

    public List getGridJobStatusesByTaskId(long taskId) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            return computeDAO.getGridJobStatusesByTaskId(taskId, null);
        }
        catch (Exception e) {
            logger.error("Unable to retrieve data from the database", e);
        }
        return null;
    }

    public List getJobIdsByTaskId(long taskId) {
        return getJobIdsByTaskId(taskId, null); // use all states
    }

    public List getJobIdsByTaskId(long taskId, String[] states) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            List<GridJobStatus> jobs = computeDAO.getGridJobStatusesByTaskId(taskId, states);
            if (jobs != null) {
                List<String> jobIDs = new LinkedList<String>();
                for (GridJobStatus job : jobs) {
                    jobIDs.add(job.getJobID());
                }
                return jobIDs;
            }
            else
                return null;
        }
        catch (Exception e) {
            logger.error("Unable to retrieve data from the database", e);
        }
        return null;
    }

    public GridJobStatus getGridJobStatus(long taskId, String jobId) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            return computeDAO.getGridJobStatus(taskId, jobId);
        }
        catch (Exception e) {
            logger.error("Unable to retrieve data from the database", e);
        }
        return null;
    }

    public TaskStatus getTaskStatus(long taskId) {
        ComputeDAO computeDAO = new ComputeDAO(logger);
        List<GridJobStatus> gridJobs = computeDAO.getGridJobStatusesByTaskId(taskId, null);
        return (new TaskStatus(taskId, gridJobs));
    }

    public void cancelTask(Long taskId) {

        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        try {
            computeBean.submitJob("CancelTask", taskId);
        }
        catch (Exception e) {
            logger.error("Error processing CancelTask process ", e);
        }

    }

    public void cancelTask(long taskId) {
        // cannot use process manager as there is no specific task here
//        ProcessManager processManager = new ProcessManager();
//        processManager.launch("CancelTask",taskId);
        // Put a message with the task ID on queue/CancelJob

        try {
            AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
            messageInterface.startMessageSession("queue/cancelTask", messageInterface.localConnectionType);
            Message msg = messageInterface.createObjectMessage();
            msg.setLongProperty("TASK_ID", taskId);
            messageInterface.sendMessageWithinTransaction(msg);
            messageInterface.commit();
            messageInterface.endMessageSession();
        }
        catch (Exception e) {
            logger.error("Unable to post cancel message to the queue ", e);
        }
    }

    /**
     * Method to pass all the info objects at once and to eliminate thousands of individual update calls
     * @param taskId - task the jobs relate to
     * @param changedJobStateMap map of the job id to GridJobStatus.JobState
     * @param changedJobResourceMap
     */
    @Override
    public void bulkUpdateGridJobInfo(long taskId, Map<String, GridJobStatus.JobState> changedJobStateMap, Map<String, Map<String, String>> changedJobResourceMap) {
        try {

            logger.debug("bulkUpdateGridJobInfo - Updating Job state and Resource usage for "+changedJobStateMap.size()+" jobs (task="+taskId+").");
            ComputeDAO computeDAO = new ComputeDAO(logger);
            for (String jobId : changedJobStateMap.keySet()) {
                computeDAO.updateJobInfo(taskId, jobId, changedJobStateMap.get(jobId), changedJobResourceMap.get(jobId));
            }
            logger.debug("bulkUpdateGridJobInfo - Done updating Job state and Resource usage (task="+taskId+")");
        }
        catch (Exception e) {
            logger.error("Error while attempting to update info gridJobStatus:", e);
        }
    }

    /////////// ------ LOCAL INTERFACES ------- /////////////
    public void updateJobStatus(long taskId, String jobId, GridJobStatus.JobState state) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            computeDAO.updateJobStatus(taskId, jobId, state);
        }
        catch (Exception e) {
            logger.error("Error while attempting to update status gridJobStatus", e);
        }

    }

    public void updateJobInfo(long taskId, String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            computeDAO.updateJobInfo(taskId, jobId, state, infoMap);
        }
        catch (Exception e) {
            logger.error("Error while attempting to update info gridJobStatus:", e);
        }
    }

    public void bulkAddGridJobStatus(long taskId, String queue, Set<String> jobIds, GridJobStatus.JobState state) {
        try {
            //logger.debug("Storing " + jobIds.size() + " statuses for task " + taskId);
            ComputeDAO computeDAO = new ComputeDAO(logger);
            computeDAO.bulkAddGridJobStatus(taskId, queue, jobIds, state);
        }
        catch (Exception e) {
            logger.error("Error while attempting to bulk create gridJobStatus", e);
        }
    }

    public void bulkUpdateGridJobStatus(long taskId, Map<String, GridJobStatus.JobState> jobStates) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            computeDAO.bulkUpdateGridJobStatus(taskId, jobStates);
        }
        catch (Exception e) {
            logger.error("Error while attempting to bulk update status of gridJobStatus", e);
        }
    }

    public void saveGridJobStatus(GridJobStatus gridJobStatus) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            computeDAO.saveOrUpdateGridJobStatus(gridJobStatus);
        }
        catch (Exception e) {
            logger.error("Error while attempting to save gridJobStatus", e);
        }
    }

    public void cleanUpJobStatus(long taskId) {
        try {
            ComputeDAO computeDAO = new ComputeDAO(logger);
            computeDAO.cleanUpGridJobStatus(taskId);
        }
        catch (Exception e) {
            logger.error("Error while attempting to save gridJobStatus", e);
        }
    }

    /**
     * Generate an ordered active tasks based on the task creatin time.
     *
     * @return A sorted map of <task creation times, task ids>
     */

    public TreeMap<Long, Long> getOrderedWaitingTasks() {

        //logger.debug("Getting ordered active tasksOrder");

        // List that holds active task IDs
        List<Long> taskIDs;

        // Sorted Map of <task creation time stamp, task id>
        TreeMap<Long, Long> taskIDTimeStampSortedMap = new TreeMap<Long, Long>();

        Date taskTime;

        try {
            // Get an instance of computeDAO
            ComputeDAO computeDAO = new ComputeDAO(logger);

            // Get active task ids
            taskIDs = computeDAO.getWaitingTasks();

            // for each task id, get it's creation time stamp and populate the taskIDTimeStampMap
            for (Long id : taskIDs) {
                // Get the approximate task creation time stamp
                taskTime = TimebasedIdentifierGenerator.getTimestamp(id);

                // Add this timestamp and the task id to the sorted map
                taskIDTimeStampSortedMap.put(taskTime.getTime(), id);
            }

        }
        catch (Exception e) {
            logger.error("Unable to retrieve data from the database", e);
            taskIDTimeStampSortedMap = null;
        }

        //logger.debug("Ordered active tasks are :" + taskIDTimeStampSortedMap);
        return taskIDTimeStampSortedMap;
    }

    public Integer getPercentCompleteForATask(long taskId) {

        Integer percentComplete;

        try {
            // Get an instance of computeDAO
            ComputeDAO computeDAO = new ComputeDAO(logger);

            // Get percent complete
            return computeDAO.getPercentCompleteForATask(taskId);

        }
        catch (Exception e) {
            logger.error("Unable to get percent complete for task " + taskId, e);
            percentComplete = null;
        }
        return percentComplete;
    }

    @Override
    public void updateDispatcherJob(DispatcherJob job) {
        try {
            DispatcherDAO dispatcherDao = new DispatcherDAO();
            dispatcherDao.save(job);
        } catch (Exception e) {
            logger.error("Error while trying to update job " + job.getDispatchId());
        }
    }

    @Override
    public List<DispatcherJob> nextPendingJobs(String hostName, int maxRetries, int prefetchSize) {
        DispatcherDAO dispatcherDao = new DispatcherDAO();
        return dispatcherDao.nextPendingJobs(hostName, maxRetries, prefetchSize);
    }
}
