
package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 8, 2010
 * Time: 12:46:08 PM
 */
public class SubmitJobAndWaitHelper {
    int waitIntervalInSeconds = 5;
    String processName;
    Long taskId;
    Logger logger;

    public SubmitJobAndWaitHelper(String processName, Long taskId) {
        this.processName = processName;
        this.taskId = taskId;
    }

    public Node startAndWaitTillDone() throws Exception {
        if (taskId == null || taskId == 0) {
            throw new Exception("taskId must be non-null and have non-zero value");
        }
        this.logger = ProcessDataHelper.getLoggerForTask(taskId.toString(), this.getClass());
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        if (computeBean == null) {
            String em = "computeBean is unexpectedly null";
            logger.error(em);
            throw new Exception(em);
        }
        logger.info("computeBean.submitJob() processName=" + processName + " taskId=" + taskId);
        computeBean.submitJob(processName, taskId);
        logger.info("starting waitForTask for taskId=" + taskId);
        return waitForTask(taskId);
    }

    protected Node waitForTask(Long taskId) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        String[] taskStatus = null;
        while (taskStatus == null || !Task.isDone(taskStatus[0])) {
            taskStatus = computeBean.getTaskStatus(taskId);
            Thread.sleep(waitIntervalInSeconds * 1000);
        }
        if (!taskStatus[0].equals(Event.COMPLETED_EVENT)) {
            throw new Exception("Task " + taskId + " finished with non-complete status=" + taskStatus[0]);
        }
        return computeBean.getResultNodeByTaskId(taskId);
    }

    public void setWaitIntervalInSeconds(int waitIntervalInSeconds) {
        this.waitIntervalInSeconds = waitIntervalInSeconds;
    }
}
