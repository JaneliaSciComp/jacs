package org.janelia.it.jacs.compute.engine.service;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.def.OperationDef;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.GridProcessResult;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobService;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SimpleJobStatusLogger;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: sreenath
 * Date: Jan 15, 2009
 * This MDB will process 2 types of messages - request for submission, and response from asynchronous submitter
 */
@MessageDriven(name = "GridSubmitAndWaitMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/gridSubmitAndWait"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "200"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="5"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "reconnectInterval", propertyValue = "30"),
//        @ActivationConfigProperty(propertyName="RedeliveryLimit", propertyValue="100"),
//        @ActivationConfigProperty(propertyName="RedeliveryDelay", propertyValue="30"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 200, timeout = 10000)
public class GridSubmitAndWaitMDB extends BaseServiceMDB {

    public void onMessage(Message message) {
        try {
        	logger = Logger.getLogger(GridSubmitAndWaitMDB.class);
        	logger.trace("Received message: "+message);
            if (message instanceof ObjectMessage) {
                Object obj;
                try {
                    obj = ((ObjectMessage) message).getObject();
                }
                catch (JMSException e) {
                    logger.error("OnMessage : Unable to get message object", e);
                    return;
                }
                if (obj instanceof GridProcessResult) {
                	GridProcessResult gpr = (GridProcessResult)obj;
                	logger.debug("Got grid process result for "+ gpr.getGridSubmissionKey());
                    completeProcessing(gpr);
                }
                else {
                	logger.debug("Got grid submission request");
                    submitToGrid((ObjectMessage) message);
                }
            }
            else {
                logger.error("OnMessage : Invalid message type in GridSubmitAndWait MDB.");
            }
        }
        catch (Throwable e) {
            logger.error("Error processing message",e);
        }
    }

    private void completeProcessing(GridProcessResult gpr) {

        // Get the unique key from the queue message
        String uniqueKey = gpr.getGridSubmissionKey();

        // Retriev the original Message and the service objects from the GridSubmitHelperMap
        Map dataMap = GridSubmitHelperMap.getInstance().getFromDataMap(uniqueKey);

        if (dataMap == null) {
            throw new IllegalStateException("Unique key not found in data map: "+uniqueKey+" GridProcessResult("+gpr+")");
        }

        QueueMessage queueMessage = ((QueueMessage) dataMap.get(GridSubmitHelperMap.ORIGINAL_QUEUE_MESSAGE_KEY));
        OperationDef operationToProcess = null;
        try {
            logger = ProcessDataHelper.getLoggerForTask(queueMessage, this.getClass());
            // Get the original queueMessage object
            operationToProcess = (OperationDef) queueMessage.getActionToProcess();

            // Get the original service object
            SubmitJobService originalservice = ((SubmitJobService) dataMap.get(GridSubmitHelperMap.ORIGINAL_SERVICE_KEY));

            // Call postprocess method of the service so that the necessary information is set to the processData
            try {
            	logger.debug("Post processing grid submission "+uniqueKey+" with "+originalservice.getClass().getSimpleName());
                originalservice.cleanup();
                originalservice.handleErrors();
                originalservice.postProcess();
            }
            finally {
            	logger.debug("Removing grid submission from data map: "+uniqueKey);
                GridSubmitHelperMap.getInstance().removeFromDataMap(uniqueKey);
            }

            //try to handle grid errors here
            if (!gpr.isCompleted()) {
                //Task task = (Task)queueMessage.getObjectMap().get("TASK");
                //Task task = EJBFactory.getRemoteComputeBean().getTaskById(gpr.getTaskId());
                ComputeDAO computeDAO = new ComputeDAO(logger);
                Task task = computeDAO.getTaskById(gpr.getTaskId());

                // Throw a ValidService Exception in case of a cancelled event
                if (task != null && task.getEvents() != null) {
                    for (Event event : task.getEvents()) {
                        if (event.getEventType().equals(Event.CANCELED_EVENT)) {

                            logger.info("Task " + task.getObjectId() + " has been cancelled by the user.");
                            throw new ValidServiceException(task.getObjectId() + " cancelled by the user");
                        }
                    }
                }
                throw new ServiceException("GRID JOB FAILURES: operation " + operationToProcess.getName() +
                        " for task " + uniqueKey + " resulted in error '" + gpr.getError() + "'");
            }
            
            logger.debug("Forwarding message with operation: "+operationToProcess.getName());
            forwardOrReply(queueMessage, operationToProcess);
        }
        catch (Throwable e) {
            // update all non-done records in accounting to ERROR
            if (null == queueMessage || null == queueMessage.getObjectMap()
                    || null == queueMessage.getObjectMap().get("TASK")) {
            	logger.error("Error processing grid result "+uniqueKey, e);
                return;
            }
        	logger.error("Error processing grid result "+uniqueKey);
            cleanUpAccounting((Task) queueMessage.getObjectMap().get("TASK"));
            recordProcessError(queueMessage, operationToProcess, e);
            JmsUtil.replyToReceivedFromQueue(queueMessage, operationToProcess, e);
        }

    }

    private void submitToGrid(ObjectMessage message) {
        QueueMessage queueMessage = null;
        OperationDef operationToProcess = null;
        SubmitJobService service;

        // generate a unique key per grid submission
        String submissionKey = String.valueOf(TimebasedIdentifierGenerator.generateIdList(1).get(0));
        
        Process proc;
        try {
            queueMessage = new QueueMessage(message, false);
            Long taskId = (Long) queueMessage.getMandatoryItem(IProcessData.PROCESS_ID);
            operationToProcess = (OperationDef) queueMessage.getActionToProcess();

            logger = ProcessDataHelper.getLoggerForTask(queueMessage, this.getClass());
            logger.info("Task "+taskId+" created submission key "+submissionKey+" for grid job '"+operationToProcess.getName()+"'");

            if (!isExecutable(operationToProcess, queueMessage)) {  // If we're linking directly
                return;
            }

            initializeInputParameters(queueMessage);
            // I think this is calling for the data from Hibernate
            Task tempT = (Task) queueMessage.getObjectMap().get("TASK");
            tempT.getInputNodes();
            tempT.getOutputNodes();
            tempT.getEvents();

            service = getJobService(queueMessage);
            logger.debug("Executing service "+service.getClass().getName()+" for grid submission "+submissionKey);
            proc = executeService(queueMessage, operationToProcess, service, submissionKey);
            // add process to a set of monitored processes.
        }
        catch (Throwable e) {
        	logger.error("Error processing grid result "+submissionKey);
            recordProcessError(queueMessage, operationToProcess, e);
            JmsUtil.replyToReceivedFromQueue(queueMessage, operationToProcess, e);
            return;
        }

        if (proc!=null) {
            // store original message objects for use when job is completed. Otherwise
            // reply-to/forward-to info is getting lost
            HashMap<String, Object> originalObjectMap = new HashMap<String, Object>();
            originalObjectMap.put(GridSubmitHelperMap.ORIGINAL_SERVICE_KEY, service);
            originalObjectMap.put(GridSubmitHelperMap.ORIGINAL_QUEUE_MESSAGE_KEY, queueMessage);
            originalObjectMap.put(GridSubmitHelperMap.PROCESS_OBJECT, proc);
        	logger.debug("Adding grid submission to data map: "+submissionKey);
            GridSubmitHelperMap.getInstance().addToDataMap(submissionKey, originalObjectMap);
        }
        else {
            // Process is null, which means the job was cancelled and we should continue processing
        	logger.debug("Job was cancelled:"+submissionKey);
            JmsUtil.replyToReceivedFromQueue(queueMessage, operationToProcess);
        }

    }

    private void cleanUpAccounting(Task t) {
        JobStatusLogger jsl = new SimpleJobStatusLogger(t.getObjectId());
        // store statuses
        jsl.cleanUpData();
    }

    private Process executeService(QueueMessage queueMessage, OperationDef operationToProcess,
                                   SubmitJobService service, String submissionKey)
            throws MissingDataException, ServiceException {
        if (operationToProcess.getForEachParam() != null) {
            // with a single submission key association to submission,
            // we cannot do iterative submissions.
            // I am not sure that this feature was actually useful.
            Task task = (Task) queueMessage.getObjectMap().get("TASK");
            throw new ServiceException("Grid job definition error for operation " + operationToProcess.getName() +
                    " for task " + task.getObjectId() + ": Invalid usage of 'foreach' attribute in process file");
        }
        else {
            return service.submitAsynchJob(queueMessage, String.valueOf(submissionKey));
        }
    }

    private SubmitJobService getJobService(QueueMessage message) {

        SubmitJobService service = null;
        Constructor constructor;

        String iServiceName = message.getString("iservice");
        logger.trace("Instantiating service class: " + iServiceName);

        try {
            Class[] paramTypes = {};
            constructor = Class.forName(iServiceName).getConstructor(paramTypes);

            // Instantiate default constructor
            service = (SubmitJobService) constructor.newInstance();
        }
        catch (Throwable e) {
            logger.error("Error instantiating "+ getClass().getName(), e);
        }

        return service;
    }
}
