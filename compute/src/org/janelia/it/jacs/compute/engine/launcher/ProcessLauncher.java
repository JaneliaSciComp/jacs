
package org.janelia.it.jacs.compute.engine.launcher;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.def.ActionDef;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.engine.def.SequenceDef;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;

/**
 * This class is responsible for launching a process action
 *
 * @author Tareq Nabeel
 */
public class ProcessLauncher extends SeriesLauncher {
    private static Logger _logger = Logger.getLogger(ProcessLauncher.class);

    /**
     * ProcessLauncher needs to catch exceptions thrown from sub launchers and services and update the process status
     * accordingly.  It will rethrow the exceptions to let the containing EJB handle it.
     *
     * @param actionDef   the Process definition
     * @param processData The running state of the process
     * @throws LauncherException
     * @throws ServiceException
     */
    protected void launchAction(ActionDef actionDef, IProcessData processData) throws LauncherException, ServiceException {
        try {
            Long processId = (Long) processData.getMandatoryItem(IProcessData.PROCESS_ID);
            ProcessDef processDef = (ProcessDef) actionDef;
            _logger.info("Process " + processDef.getName() + " (id="+processId+") started");
            enactActionPreProcessing(processData);
            launchSeries(processDef, processData);
            enactActionPostProcessing(processData/*, processDef*/);
            _logger.info("Process " + processDef.getName() + " (id="+processId+") finished\n");
        }
        // Note: this method used to throw exceptions but in my experience that just results in duplicate stack traces 
        // and the logging of secondary errors such as "Unexpected error delivering message org.jboss.mq.SpyObjectMessage" 
        // which are distracting from the original problem. If there is any value in having these exceptions propagated, 
        // then we should reinstate these lines below, but for now I'm going to see if we can live without them. -KR
        catch (ServiceException e) {
            recordProcessError(processData, e);
//            throw e;
        }
        catch (LauncherException e) {
            recordProcessError(processData, e);
//            throw e;
        }
        catch (Throwable e) {
            LauncherException ee = new LauncherException("Action " + actionDef.getActionType() + " " + actionDef.getName() + " encountered unexpected exception: ", e);
            recordProcessError(processData, ee);
//            throw ee;
        }
    }

    private void enactActionPreProcessing(IProcessData processData) throws MissingDataException,
            ServiceException {
        // Grab the task
        Task task = ProcessDataHelper.getTask(processData);
        // If the current task id is the top level task, run preprocessing checks
        if (task.getParentTaskId() == null && (null != task.getParameter(Task.PARAM_finalOutputDirectory) &&
                !"".equals(task.getParameter(Task.PARAM_finalOutputDirectory)))) {
            try {
                FileUtil.ensureDirExists(task.getParameter(Task.PARAM_finalOutputDirectory));
            }
            catch (IOException e) {
                throw new ServiceException("The final output destination does not exist and could not be created: " +
                        task.getParameter(Task.PARAM_finalOutputDirectory));
            }
        }
    }


    private void enactActionPostProcessing(IProcessData processData/*, ProcessDef processDef*/) throws IOException, MissingDataException {
        // todo Check processDef for complete action.  If no error, postprocessing should mark as complete
        // Send email if the user wants to be notified
        // If the current task id is the top level task, send notification
        Task task = ProcessDataHelper.getTask(processData);
        if (task.getParentTaskId() == null) {
            notifyUser(processData);
        }

        /**
         * Remove all the appenders.  Unfortunately, Log4j does not allow loggers to be removed (as read from their
         * documentation).  I guess it is hard.  I think we should still grab the loggers we made and geld them
         * by removing all appenders, to force a formal closing of the streams.
         */
        // todo Remove this or come up with a better way to track task actions
//        Logger taskLogger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
//        if (null!=taskLogger && taskLogger.getName().startsWith("task")) {
//            _logger.debug("Removing appenders for logger "+taskLogger.getName());
//            taskLogger.removeAllAppenders();
//        }
    }

    /**
     * Launches the immediate children of the supplied process definition
     *
     * @param seriesDef   the process definition
     * @param processData the running state of the process
     * @throws LauncherException
     * @throws ServiceException
     */
    protected void launchSeriesChildren(SeriesDef seriesDef, IProcessData processData) throws ComputeException {
        for (ActionDef actionDef : seriesDef.getChildActionDefs()) {
            if (!actionDef.isExecutable(processData)) {
                continue;
            }
            processData.setActionToProcess(actionDef);
            switch (actionDef.getActionType()) {
                case PROCESS:
                    // ProcessDef contained within another ProcessDef
                    ProcessManager processManager = new ProcessManager();
                    processManager.launchChildProcess(/*(ProcessDef) seriesDef,
                            (ProcessDef) actionDef, processData*/);
                    break;
                case SEQUENCE:
                    // Sequence contained within another sequence or within a process
                    launchSequence((SequenceDef) actionDef, processData);
                    break;
                default:
                    throw new LauncherException("Unknown action: " + actionDef.getActionType());
            }
        }
    }

    /**
     * Captures the processing status of the asyncrhonous process or sequence launched by this ProcessLauncher
     *
     * @param processData      used by base class for capturing operation output parameters
     * @param messageFromQueue the message received from queue
     * @throws LauncherException
     * @throws ServiceException
     */
    protected void captureMessageFromQueue(QueueMessage messageFromQueue, IProcessData processData) throws ComputeException {
        try {
            // Since a Process launcher can only wait for asynchronous processes and sequences, we'll cast to
            // processed action to SeriesDef and throw LauncherException if it failed
            SeriesDef processedAction = (SeriesDef) messageFromQueue.getProcessedAction();
            if (!messageFromQueue.isProcessedSuccessfully()) {
                throw new LauncherException("Processing of " + processedAction.getName() + " " + processedAction.getActionType() +
                        " messageId:" + messageFromQueue.getMessageId() + " failed", messageFromQueue.getProcessingException());
            }
        }
        catch (MissingDataException ee) {
            throw new LauncherException(ee);
        }
    }

    /**
     * Marks the process in error
     *
     * @param processData process data
     * @param e           throwable which occurred
     */
    private void recordProcessError(IProcessData processData, Throwable e) {
        try {
            // Log it from here instead of compute bean so we know where it came from
            _logger.error("Process: " + processData.getProcessDef() + " failed", e);
            EJBFactory.getLocalComputeBean().recordProcessError(processData.getProcessDef(), processData.getProcessId(), e);

            // Notify user
            Task task = ProcessDataHelper.getTask(processData);

            // If the current task id is the top level task, send notification
            if (task.getParentTaskId() == null) {
                notifyUser(processData);
            }

        }
        catch (MissingDataException ee) {
            _logger.error("Failed to update process status", ee);
        }
    }

}
