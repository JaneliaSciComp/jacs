package org.janelia.it.jacs.compute.service.utility;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.SubmitJobAndWaitHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;

import java.io.IOException;
import java.util.Date;

/**
 * This service is intended to provide a means for users to fire off continuously running pipelines.
 */
public class ContinuousExecutionService implements IService {

    private ContinuousExecutionTask task;
    private int loopTimerInMinutes;
    private int statusCheckDelayInSeconds;
    private Task originalSubtask;
    private String currentSubtaskProcess;
    private Logger logger;

    public ContinuousExecutionService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            while(true) {
                // Get the task from the database - things may have changed
                task = (ContinuousExecutionTask) EJBFactory.getLocalComputeBean().getTaskById(task.getObjectId());
                // Check the enabled state
                if (!task.isStillEnabled()) {
                    break;
                }

                // Format the new Task and try again.  If the original is good, use it
                Task subTask = originalSubtask;
                if (subTask.isDone()) {
                    subTask = Task.clone(subTask);
                    subTask.setObjectId(null);
                }
                subTask.setParentTaskId(task.getObjectId());
                subTask = EJBFactory.getLocalComputeBean().saveOrUpdateTask(subTask);
                // save task to the db and submit
                logger.debug("Submitting a new subtask");
                SubmitJobAndWaitHelper jobHelper = new SubmitJobAndWaitHelper(currentSubtaskProcess, subTask.getObjectId());
                jobHelper.setWaitIntervalInSeconds(statusCheckDelayInSeconds);
                EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), Event.SUBTASKRUNNING_EVENT, "Starting subexecution:" + subTask.getDisplayName(), new Date());
                jobHelper.startAndWaitTillDone();

                // If the last subtask event was an error, stop and
                subTask = EJBFactory.getLocalComputeBean().getTaskById(subTask.getObjectId());
                if (Event.ERROR_EVENT.equals(subTask.getLastEvent().getEventType())) {
                    EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), Event.ERROR_EVENT, "Subtask failed:"
                            + subTask.getObjectId(), new Date());
                    throw new ServiceException("Subtask ("+ originalSubtask.getObjectId() +") for continuous execution task="+task.getObjectId()+", user="+task.getOwner()+
                            " did not complete successfully.");
                }
                else {
                    // Your subtask was successful.  Congrats.  You get to wait an try again.
                    EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), Event.SUBTASKCOMPLETED_EVENT, "Completed subexecution:"
                            + subTask.getDisplayName(), new Date());

                    Thread.sleep(loopTimerInMinutes);
                }
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Initialize the input parameters
     *
     * @param processData params of the task
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             cannot find data required to process
     * @throws java.io.IOException problem accessing file data
     */
    private void init(IProcessData processData) throws MissingDataException, IOException {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        task = (ContinuousExecutionTask) ProcessDataHelper.getTask(processData);
        loopTimerInMinutes = Integer.valueOf(task.getParameter(ContinuousExecutionTask.PARAM_LOOP_TIMER));
        long originalSubtaskId = Long.valueOf(task.getParameter(ContinuousExecutionTask.PARAM_SUBTASK_ID));
        originalSubtask = EJBFactory.getLocalComputeBean().getTaskById(originalSubtaskId);
        currentSubtaskProcess = task.getParameter(ContinuousExecutionTask.PARAM_SUBTASK_PROCESS);
        statusCheckDelayInSeconds = Integer.valueOf(task.getParameter(ContinuousExecutionTask.PARAM_SUBTASK_STATUS_TIMER));
    }
}
