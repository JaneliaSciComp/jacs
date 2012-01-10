
package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.JobControlBeanRemote;
import org.janelia.it.jacs.model.status.TaskStatus;
import org.janelia.it.jacs.server.utils.SystemException;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 22, 2006
 * Time: 5:06:29 PM
 */
public class JobControlAPI {

    static Logger logger = Logger.getLogger(JobControlAPI.class.getName());

    JobControlBeanRemote jobControlBean;

    public void setJobControlBean(JobControlBeanRemote jobControlBean) {
        this.jobControlBean = jobControlBean;
    }

    public JobControlAPI() {

    }

    public TaskStatus getTaskStatus(long taskId) throws SystemException {

        TaskStatus taskStatus = null;

        try {
            taskStatus = jobControlBean.getTaskStatus(taskId);
        }
        catch (Throwable t) {
            logger.error("Error in getting the TaskStatus for " + taskId + " from jobControlBean." + t.getMessage(), t);
            throw new SystemException(new StringBuilder().append("Error in getting the TaskStatus for ").append(taskId).append(" from jobControlBean.").append(t.getMessage()).toString());
        }
        return taskStatus;
    }

    public int getTaskOrder(Long taskID) throws SystemException {

        // task order
        int taskOrder = -1;
        try {
            Map orderedTaskMap = jobControlBean.getOrderedWaitingTasks();
            //logger.debug("The map " + orderedTaskMap);
            //logger.debug("The size of the map " + orderedTaskMap.size());

            Set keySet = orderedTaskMap.keySet();
            Object[] keyArray = orderedTaskMap.keySet().toArray();
            //logger.debug("The key array " + keyArray);
            //logger.debug("The Key array length " + keyArray.length);

            for (int order = 0; order < keyArray.length; order++) {
                //logger.debug("In for loop "  + order);
                Long key = (Long) keyArray[order];
                //logger.debug("Key " + key);
                Long tid = (Long) orderedTaskMap.get(key);
                //logger.debug("Order " + order + " Key : " + key + " Value : " + tid + " taskID :" + taskID);
                // If this key equals to the task id, the index of this would be the order of the task.
                if (tid.equals(taskID)) {
                    // Return this order of task among the jobs that are waiting in the grid queue.
                    //logger.debug("Order of the task id " + taskID + " is " + (order+1));
                    return (order + 1);
                }
                else {
                    //logger.debug(tid + " and " + taskID + " are not equal");
                }
            } // end of for loop
        }
        catch (Throwable e) {
            logger.error("Error returned in finding the order of the task from jobControlBean :" + e.getMessage(), e);
            throw new SystemException("Error returned in finding the order of the task from jobControlBean :" + e.getMessage());
        }

        return taskOrder;
    }

    public Integer getPercentCompleteForATask(long taskID) throws SystemException {

        try {
            return jobControlBean.getPercentCompleteForATask(taskID);
        }
        catch (RemoteException e) {
            logger.error("Error encountered in finding the percent complete for task " + taskID + " from jobControlBean :" + e.getMessage(), e);
            //throw new SystemException("Error returned in finding the order of the task from jobControlBean :"+e.getMessage());
            return null;
        }
    }

    public void cancelTask(Long taskId) {
        try {

            jobControlBean.cancelTask(taskId);
        }
        catch (RemoteException re) {
            logger.error("Error deleting the task " + taskId + re.getMessage(), re);
        }

    }
}