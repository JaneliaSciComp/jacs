
package org.janelia.it.jacs.compute.engine.launcher;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.*;
import org.janelia.it.jacs.compute.engine.def.*;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.List;

/**
 * This class is responsible for launching a sequence action
 *
 * @author Tareq Nabeel
 */
public class SequenceLauncher extends SeriesLauncher {
    private static Logger logger = Logger.getLogger(SequenceLauncher.class);

    /**
     * Launches the sequence definition
     *
     * @param actionDef   the sequence definition
     * @param processData the running state of the process
     * @throws LauncherException
     * @throws ServiceException
     */
    protected void launchAction(ActionDef actionDef, IProcessData processData) throws ComputeException {
        if (actionDef.isSeriesDef()) {
            launchSeries((SeriesDef) actionDef, processData);
        }
        else {
            if (actionDef.getForEachParam() != null) {
                List<IProcessData> pds = DataExtractor.createForEachPDs(processData, actionDef.getForEachParam());
                if (pds != null) {
                    for (IProcessData pd : pds) {
                    	try {
                    		launchOperation((OperationDef) actionDef, pd);	
                    	}
                        finally {
	                        // If it's asynchronous, then pd would not have the operations output parameters at this point
	                        // The operation mdb should send its output parameters in its reply message
	                        if (actionDef.getProcessorType() != ProcessorType.LOCAL_MDB) {
	                            // Copy the operation's output parameters into the parent process data
	                            DataExtractor.copyData(pd, processData, actionDef.getOutputParameters());
	                        }
                        }
                    }
                }
            }
            else {
            	IProcessData pd = new ProcessData();
            	DataExtractor.copyData(processData, pd, actionDef.getInputParameters());
            	try {
            		launchOperation((OperationDef) actionDef, pd);	
            	}
                finally {
                    DataExtractor.copyData(pd, processData, actionDef.getOutputParameters());
                }
            }
        }
    }

    /**
     * Launches the immediate children of the supplied sequence definition
     *
     * @param seriesDef   the sequence definition
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
                case SEQUENCE:
                    launchSequence((SequenceDef) actionDef, processData);
                    break;
                case OPERATION:
                    launchActions(actionDef, processData);
                    break;
                default:
                    throw new LauncherException("Unknown action: " + actionDef.getActionType());
            }
        }
    }


    /**
     * Executes an operation
     *
     * @param operationDef the operation to execute
     * @param processData  running state of the process
     * @throws LauncherException
     * @throws ServiceException
     */
    private void launchOperation(OperationDef operationDef, IProcessData processData) throws LauncherException, ServiceException {
    	copyHardCodedValuesToPd(operationDef, processData);
        switch (operationDef.getProcessorType()) {
            case POJO:
            case LOCAL_SLSB:
                try {
                    IService service = ProcessorFactory.createServiceForOperation(operationDef);
                    service.execute(processData);
                    recordProcessSuccess(processData, operationDef);
                }
                catch (ServiceException e) {
                    recordProcessError(processData, operationDef, e);
                    if (operationDef.haltProcessOnError()) {
                        throw e;
                    }
                }
                catch (Throwable e) {
                    ServiceException ee = new ServiceException("Operation " + operationDef.getName() + " encountered unexpected exception: ", e);
                    recordProcessError(processData, operationDef, ee);
                    throw ee;
                }
                break;
            case LOCAL_MDB:
                // LOCAL_MDB has to be in place listening on operDef processor queue
                sendMessageToQueue(processData);
                break;
            default:
                throw new LauncherException("Unknown processor type:" + operationDef.getProcessorType());
        }
    }

    /**
     * This method copies values hardcoded in process definition over to process data before
     * operation is launched
     *
     * @param operationDef the operation whose values are to be copied from the descriptior
     * @param processData  the running state of the process
     */
    private void copyHardCodedValuesToPd(OperationDef operationDef, IProcessData processData) {
        for (Parameter parameter : operationDef.getInputParameters()) {
            if (parameter.getValue() != null) {
                processData.putItem(parameter.getName(), parameter.getValue());
            }
        }
    }

    /**
     * Captures the processing status of the asynchronous sequence or operation launched by this SequenceLauncher
     * Copies over output parameters to process data if operation or series was successful.  It throws an exception
     * if the operation was in error
     *
     * @param messageFromQueue the message received from queue
     * @param processData      used by for copying over operation output parameters
     * @throws LauncherException
     * @throws ServiceException
     */
    protected void captureMessageFromQueue(QueueMessage messageFromQueue, IProcessData processData) throws ComputeException {
        try {
            ActionDef processedAction = messageFromQueue.getProcessedAction();
            if (messageFromQueue.isProcessedSuccessfully()) {
                // All MDB operations have to record success themselves, otherwise task could be completed
                // but hits would not have been persisted for example.
                //recordProcessSuccess(processData, processedOperAction);
                DataExtractor.copyData(messageFromQueue, processData, processedAction.getOutputParameters());
            }
            else {
                handleFailure(messageFromQueue, processedAction);
            }
        }
        catch (MissingDataException ee) {
            throw new LauncherException(ee);
        }
    }

    private void handleFailure(QueueMessage messageFromQueue, ActionDef processedAction) throws ComputeException {
        ComputeException ce;
        if (processedAction.isOperation()) {
            ce = createOrExtractServiceException(messageFromQueue, processedAction);
        }
        else {
            ce = new LauncherException("Processing of " + processedAction.getName() + " " + processedAction.getActionType() + " messageId:" + messageFromQueue.getMessageId() + " failed", messageFromQueue.getProcessingException());
        }
        if (processedAction.haltProcessOnError() && ce != null) {
            throw ce;
        }
    }

    private ServiceException createOrExtractServiceException(QueueMessage messageFromQueue, ActionDef processedAction) {
        ServiceException se;
        if (messageFromQueue.getProcessingException() instanceof ServiceException) {
            se = (ServiceException) messageFromQueue.getProcessingException();
        }
        else {
            se = new ServiceException("Operation " + processedAction.getName() + " encountered exception:", messageFromQueue.getProcessingException());
        }
        recordProcessError(messageFromQueue, processedAction, se);
        return se;
    }

    /**
     * This method marks the process in error if an operation/sequence was unsuccessful and the process wasn't
     * responsible for managing it's status
     *
     * @param processData the running state of the process
     * @param e
     */
    private void recordProcessError(IProcessData processData, ActionDef actionDef, Throwable e) {
        try {
            if (actionDef.updateProcessStatusOnFailure() && processData.getProcessDef().containsUpdateStatusOnSuccessAction()) {
                // SequenceLauncher always rethrows the exception, so it will get logged by someone else
                //logger.error("Process: " + processData.getProcessDef() +  " failed", e);
                EJBFactory.getLocalComputeBean().recordProcessError(processData.getProcessDef(), processData.getProcessId(), e);

                // Notify user
                Task task = ProcessDataHelper.getTask(processData);

                // If the current task id is the top level task, send notification
                if (task.getParentTaskId() == null) {
                    notifyUser(processData);
                }
            }
        }
        catch (MissingDataException ee) {
            logger.error("Failed to update process status", ee);
        }
    }

}
