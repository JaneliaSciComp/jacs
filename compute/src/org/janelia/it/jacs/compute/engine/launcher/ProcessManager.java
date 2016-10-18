package org.janelia.it.jacs.compute.engine.launcher;

import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.data.ProcessData;
import org.janelia.it.jacs.compute.engine.def.DefCache;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * This class is responsible for preparing the data for launching of a Process Launcher.
 * It is used by ComputeBeanImpl to launch a process for a task id
 *
 * @author Tareq Nabeel
 */
public class ProcessManager {

    private static Logger logger = Logger.getLogger(ProcessManager.class);

    /**
     * This is the core method in this class.  It sends a message to a process launcher queue if
     * the processorType of <code>processDef</code> is async or creates a synchronous ILauncher and uses it to
     * launch the process
     *
     * @param processDef  process definition to run the definition of the process
     * @param processData the running state of the process
     * @return the process id
     */
    private Long launch(ProcessDef processDef, IProcessData processData) {
        try {
            logger.info("\nLaunching " + processDef.getName() + " ....\n");
            initProcessData(processData, processDef);
            if (processDef.isProcessorAsync()) {
                //processData.setActionToProcess(processDef);
                AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();
                JmsUtil.sendMessageToQueue(messageInterface, processData, null);
            }
            else {
                ILauncher launcher = ProcessorFactory.createLauncherForSeries(processDef);
                launcher.launch(processDef, processData);
            }
            return getProcessId(processData);
        }
        catch (ComputeException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Launches <code>processDefName</code> for the supplied taskid
     *
     * @param processDefName name of the process definition
     * @param taskId         identifier of the task to launch the process for
     *                       todo These methods need to capture the missing process file exception and save an error event on the tasks
     */
    public void launch(String processDefName, Long taskId) {
        ProcessDef processDef;
        try {
            processDef = loadProcessDef(processDefName);
        }
        catch (RuntimeException re) {
            // record error in task
            recordProcessLaunchError(taskId, re);
            throw re;
        }
        launch(processDef, taskId);
    }

    /**
     * Launches <code>processDefName</code> for the supplied taskids
     *
     * @param processDefName name of the process definition
     * @param taskIds        identifiers of the task to launch the process fors
     */
    public void launch(String processDefName, List<Long> taskIds) {
        ProcessDef processDef;
        try {
            processDef = loadProcessDef(processDefName);
        }
        catch (RuntimeException re) {
            // record error in task
            for (long tId : taskIds)
                recordProcessLaunchError(tId, re);
            throw re;
        }
        launch(processDef, taskIds);

    }

    /**
     * Launches <code>processDef</code> for the supplied taskid
     *
     * @param processDef process definition to run
     * @param taskId     identifier of the task to launch the process for
     */
    private void launch(ProcessDef processDef, Long taskId) {
        ProcessData processData = new ProcessData();
        processData.putItem(IProcessData.PROCESS_ID, taskId);
        processData.putItem(IProcessData.TASK, loadTask(taskId));
        launch(processDef, processData);
    }

    private Task loadTask(long taskId) {
        return new ComputeDAO().getTaskById(taskId);
    }

    /**
     * Launches <code>processDef</code> for the supplied taskids
     *
     * @param processDef process definition to run
     * @param taskIds    identifiers of the task to launch the process fors
     */
    private void launch(ProcessDef processDef, List<Long> taskIds) {
        if (taskIds == null) {
            throw new EJBException("TaskId cannot be null");
        }
        if (taskIds.size() == 1) {
            launch(processDef, taskIds.get(0));
        }
        else {
            ProcessData processData = new ProcessData();
            processData.putItem(ProcessData.PROCESS_ID, taskIds);
            launch(processDef, processData);
        }
    }

    /**
     * Launches <code>processDefName</code> for the supplied process configuration
     *
     * @param processDefName       name of the process definition
     * @param processConfiguration map of the process configuration
     * @return returns the process id of the running task
     */
    public Long launch(String processDefName, Map<String, Object> processConfiguration) {
        ProcessDef processDef;
        try {
            processDef = loadProcessDef(processDefName);
        }
        catch (RuntimeException re) {
            // record error in task
            Object o = processConfiguration.get(IProcessData.PROCESS_ID);
            long taskId;
            try {
                taskId = (Long) o;
                recordProcessLaunchError(taskId, re);
            }
            catch (Exception e) {
                logger.error("Unable to launch process or rerod the failure.", re);
            }
            throw re;
        }
        return launch(processDef, processConfiguration);
    }

    /**
     * Launches <code>processDef</code> for the supplied process configuration
     *
     * @param processDef           process definition to run
     * @param processConfiguration map of the process configuration
     * @return the process id of the running task
     */
    private Long launch(ProcessDef processDef, Map<String, Object> processConfiguration) {
        ProcessData processData = new ProcessData();
        processData.copyFrom(processConfiguration);
        return launch(processDef, processData);
    }

    /**
     * Prepares Process data for launching of process
     *
     * @param processData collection of values for the process
     * @param processDef  process definition to run
     */
    private void initProcessData(IProcessData processData, ProcessDef processDef) {
        processData.setProcessDefName(processDef.getProcessName());
        if (processData.getProcessIds() != null) {
            processDef.setForEachParam(IProcessData.PROCESS_ID);
        }
    }

    private Long getProcessId(IProcessData processData) throws MissingDataException {
        if (processData.getProcessIds() != null) {
            if (processData.getProcessIds().size() > 1) {
                return null;
            }
            else {
                return (Long) processData.getProcessIds().get(0);
            }
        }
        else {
            return processData.getProcessId();
        }
    }

    /**
     * Creates and initializes a process definition using the supplied <code>defName</code>
     *
     * @param defName the name of the .process file on the classpath
     * @return initialized process definition
     */
    private ProcessDef loadProcessDef(String defName) {
        return DefCache.getProcessDef(defName);
    }

    public void launchChildProcess(/*ProcessDef parentProcessDef, ProcessDef childProcessDef, IProcessData parentProcessData*/) {
        // We need to create a new child task instance.
        // Set the child task's parentId to processData.getProcessId();
        // Invoke launch(childProcessDef, processData); where processData is reference to or copy of parentProcessData
        throw new UnsupportedOperationException("This operation requires DB schema changes");
    }

    private void recordProcessLaunchError(long taskId, Exception e) {
        try {
            logger.error("Task " + taskId + " failed to launch. Error: " + e.getMessage(), e);
            EJBFactory.getLocalComputeBean().updateTaskStatus(taskId, Event.ERROR_EVENT, e.getMessage());
        }
        catch (Throwable t) {
            logger.error("UNABLE TO RECORD process launch failure for task " + taskId);
        }

    }
}
