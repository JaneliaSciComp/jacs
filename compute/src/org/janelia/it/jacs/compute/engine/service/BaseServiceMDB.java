
package org.janelia.it.jacs.compute.engine.service;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.DataExtractor;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.def.OperationDef;
import org.janelia.it.jacs.compute.engine.launcher.ProcessorFactory;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.io.IOException;
import java.util.List;

/**
 * This is the base class for all Service MDBs.  It wraps a pojo service instance and is used for asynchronous
 * processing of an operation. Because it runs in a new transaction context, it rollbacks the transaction
 * when something goes wrong in the service.  Before it does so it sends a jms response to the initiator
 * of the process action if a response was required.
 *
 * @author Tareq Nabeel
 */
@MessageDriven(name = "BaseServiceMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/baseService"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "300"),
//        @ActivationConfigProperty(propertyName = "MaxMessages", propertyValue = "75"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 300, timeout = 10000)
public class BaseServiceMDB implements MessageListener {
    protected Logger logger;

    public void onMessage(Message message) {
        try {
            QueueMessage queueMessage = new QueueMessage((ObjectMessage) message, false);
            logger = ProcessDataHelper.getLoggerForTask(queueMessage, BaseServiceMDB.class);
            if (logger.isInfoEnabled()) {
                logger.info(getClass().getName() + " onMessage...");
            }
            // ActionToProcess could have changed after execution of service
            // It's important to grab this first
            OperationDef operationToProcess = (OperationDef) queueMessage.getActionToProcess();
            try {
                if (!isExecutable(operationToProcess, queueMessage)) {  // If we're linking directly
                    return;
                }
                initializeInputParameters(queueMessage);
                executeService(queueMessage, operationToProcess);
                forwardOrReply(queueMessage, operationToProcess);
            }
            catch (Throwable e) {
                recordProcessError(queueMessage, operationToProcess, e);
                JmsUtil.replyToReceivedFromQueue(queueMessage, operationToProcess, e);
            }
        }
        catch (Throwable e) {
            logger.error("Error in the base service." + e.getMessage());
        }
    }

    public IService getService(QueueMessage queueMessage) throws MissingDataException {
        OperationDef operationToProcess = (OperationDef) queueMessage.getActionToProcess();
        return ProcessorFactory.createServiceForOperation(operationToProcess);
    }

    public void initializeInputParameters(QueueMessage queueMessage) throws MissingDataException, ServiceException {
        try {
            ProcessDataHelper.getTask(queueMessage);
        }
        catch (Exception e) {
            throw new ServiceException(getService(queueMessage) + " caught exception:", e);
        }
    }

    protected boolean forwardOrReply(QueueMessage queueMessage, OperationDef operationToProcess) throws MissingDataException,
            ServiceException {
        boolean replyAttempted = false;
        if (operationToProcess.getQueueToLinkTo() != null) {
            JmsUtil.forwardToLinkedToQueue(queueMessage, operationToProcess);
        }
        else {
            recordProcessSuccess(queueMessage, operationToProcess);
            replyAttempted = true;
            JmsUtil.replyToReceivedFromQueue(queueMessage, operationToProcess);
        }
        return replyAttempted;
    }

    private void executeService(QueueMessage queueMessage, OperationDef operationToProcess) throws MissingDataException, ServiceException {
        IService service = getService(queueMessage);
        if (operationToProcess.getForEachParam() != null) {
            List<IProcessData> pds = DataExtractor.createForEachPDs(queueMessage, operationToProcess.getForEachParam());
            if (pds != null) {
                for (IProcessData pd : pds) {
                    service.execute(pd);
                    DataExtractor.copyData(pd, queueMessage, operationToProcess.getOutputParameters());
                }
            }
        }
        else {
            service.execute(queueMessage);
        }
    }

    protected boolean isExecutable(OperationDef operationToProcess, QueueMessage queueMessage) {
        return operationToProcess.getProcessIfCondition() == null || operationToProcess.getProcessIfCondition().isSatisfiedBy(queueMessage);
    }


    protected void recordProcessSuccess(QueueMessage queueMessage, OperationDef operationDef) throws MissingDataException,
            ServiceException {
        // Status should be set to completed when MDB transaction completes, otherwise task could be completed
        // but hits would not have been persisted for example.
        //EJBFactory.getLocalComputeBean().recordProcessSuccess(queueMessage.getProcessDef(), queueMessage.getProcessId());
        if (operationDef.updateProcessStatusOnSuccess()) {
            copyDataToFinalDestination(queueMessage.getProcessId());
            new ComputeDAO(logger).recordProcessSuccess(queueMessage.getProcessDefName(), queueMessage.getProcessId());
        }
    }

    // todo This should be a service added onto the running process not hardwired into the core.
    private void copyDataToFinalDestination(Long processId) throws MissingDataException,
            ServiceException {
        ComputeDAO computeDAO = new ComputeDAO(logger);
        // Grab the task
        Task task = computeDAO.getTaskById(processId);

        // If the current task id is the top level task, enact postprocessing
        if (task.getParentTaskId() == null && (null != task.getParameter(Task.PARAM_finalOutputDirectory) &&
                !"".equals(task.getParameter(Task.PARAM_finalOutputDirectory)))) {
            // Already checked that the final directory exists in ProcessLauncher...
            // Get the node for this guy and copy everything over to the final destination.
            // Clean up the original system dir or let it expire?
            FileNode tmpResultNode = (FileNode) computeDAO.getResultNodeByTaskId(processId);
            try {
                FileUtil.copyDirectory(tmpResultNode.getDirectoryPath(), task.getParameter(Task.PARAM_finalOutputDirectory));
            }
            catch (IOException e) {
                throw new ServiceException("Could not copy the output files to the final destination (from " +
                        tmpResultNode.getDirectoryPath() + " to " + task.getParameter(Task.PARAM_finalOutputDirectory));
            }
        }
    }

    protected void recordProcessError(QueueMessage queueMessage, OperationDef operationDef, Throwable e) {
        try {
            if (e instanceof ValidServiceException) {
                logger.error("Process: " + queueMessage.getProcessDefName() + " failed due to user cancellation");
            }
            else {
                logger.error("Process: " + queueMessage.getProcessDefName() + " failed");
            }
        }
        catch (MissingDataException ee) {
            logger.error("Failed to update process status", ee);
        }
    }
}
