
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.status.TaskStatus;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Aug 4, 2008
 * Time: 12:36:58 PM
 */
@Singleton
@Startup
public class GridJobsManager extends AbstractComponentMBean implements GridJobsManagerMBean {
    private static final Logger logger = Logger.getLogger(GridJobsManager.class);

    public GridJobsManager() {
        super("jacs");
    }

    public void cancelTask(long taskId) {
        EJBFactory.getLocalJobControlBean().cancelTask(taskId);
    }

    // Prints a task report for a specific task
    public String printTaskReport(long taskId) {
        String result = null;
        try {
            TaskStatus status = EJBFactory.getRemoteJobControlBean().getTaskStatus(taskId);
            if (status.isCompleted())
                return TaskStatus.getCompleteHeader() + status.toString();
            else
                return TaskStatus.getNotCompleteHeader() + status.toString();
        }
        catch (RemoteException e) {
            e.printStackTrace();
            logger.error("Error while trying to print task report");
        }
        return result;
    }

    // Prints a report of tasks that are/were not finished
    public String printCurrentReport() {
        String output;
        List<TaskStatus> currentTasks = EJBFactory.getLocalJobControlBean().getActiveTasks();
        output = "Current tasks running:\n" + TaskStatus.getNotCompleteHeader();
        for (TaskStatus taskStat : currentTasks) {
            output += taskStat.toString();
        }
        return output;
    }

    // Prints a report of tasks that are/were not finished
    public String printStatus(long taskId) {
        TaskStatus status = null;

        String state = "DEFAULT";

        logger.debug("Getting status for taskId " + taskId);

        try {
            status = EJBFactory.getRemoteJobControlBean().getTaskStatus(taskId);
        }
        catch (RemoteException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("Error while trying to print task status ");
        }
        if (null!=status) {
            logger.debug(" Grid Jobs Map is " + status.getGridJobStatusMap().toString());
            logger.debug("Task Details  : " + status.getStatusDetails());

            if (status.isRunning())
                state = "RUNNING";

            if (status.isCompleted())
                state = "COMPLETE";

            if (status.isWaiting())
                state = "WAITING";

            if (status.isFailed())
                state = "FAILED";
        }
        else {
            state = "UNKNOWN";
        }

        logger.debug("State for taskId " + state);
        return state;
    }

    public String printTaskOrder(long taskID) {
        String taskOrder = null;

        logger.debug("Getting status for taskId " + taskID);

        try {
            Map orderedTaskMap = EJBFactory.getRemoteJobControlBean().getOrderedWaitingTasks();
            logger.debug("The map " + orderedTaskMap);
            logger.debug("The size of the map " + orderedTaskMap.size());

            Object[] keyArray = orderedTaskMap.keySet().toArray();
            logger.debug("The key array " + keyArray.toString());
            logger.debug("The Key array length " + keyArray.length);

            for (int order = 0; order < keyArray.length; order++) {
                logger.debug("In for loop " + order);
                Long key = (Long) keyArray[order];
                logger.debug("Key " + key);
                Long tid = (Long) orderedTaskMap.get(key);
                logger.debug("Order " + order + " Key : " + key + " Value : " + tid + " taskID :" + taskID);
                // If this key equals to the task id, the index of this would be the order of the task.
                if (tid.equals(taskID)) {
                    // Return this order of task among the jobs that are waiting in the grid queue.
                    logger.debug("Order of the task id " + taskID + " is " + order);
                    return (order + 1) + "";
                }
                else {
                    logger.debug(tid + " and " + taskID + " are not equal");
                }
            } // end of for loop 

        }
        catch (RemoteException e) {
            logger.error("Error while trying to print task Order");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return taskOrder;
    }


    public String printPercentComplete(long taskID) {
        Integer percentComplete = null;

        logger.debug("Getting percent complete for taskId " + taskID);

        try {
            percentComplete = EJBFactory.getRemoteJobControlBean().getPercentCompleteForATask(taskID);
        }
        catch (RemoteException e) {
            logger.error("Error while getting the percent complete for task " + taskID);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return (null!=percentComplete)?percentComplete.toString():"UNKNOWN";
    }
}
