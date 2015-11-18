
package org.janelia.it.jacs.compute.engine.launcher;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SubjectDAO;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.*;
import org.janelia.it.jacs.compute.engine.def.*;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.MailHelper;

/**
 * Base class for launching of Series actions i.e. a processes and sequences
 *
 * @author Tareq Nabeel
 */
public abstract class SeriesLauncher implements ILauncher {

    private static Logger logger = Logger.getLogger(SeriesLauncher.class);

    /**
     * Used to create temporary queues, send messages to queues, and wait for messages from queues.
     */
    protected AsyncMessageInterface messageInterface;

    /**
     * Whether or not this launcher is waiting for asynchronous actions to be completed i.e. whether
     * this launcher is waiting for messages from a temporary queue
     */
    protected boolean waitingOnAsync;

    /**
     * The temporary queue that was created to wait for asynchrounous actions to send their completed status to
     */
    protected Queue tempWaitQueue;

    /**
     * The name of the temporary queue that was created to wait for asynchrounous actions to send their completed status to
     */
    protected String tempQueueName;

    /**
     * The ids of messages to sent to the temporary queue that was created to wait for asynchrounous actions to send their completed status to
     */
    private Set<String> messageIds = new HashSet<String>();

    /**
     * This method launches the supplied series definition
     *
     * @param seriesDef   a process or sequence definition
     * @param processData the running state of the process
     * @throws LauncherException
     * @throws ServiceException
     */
    public void launch(SeriesDef seriesDef, IProcessData processData) throws ComputeException {
        launchActions(seriesDef, processData);
    }

    /**
     * This method breaks up processData into multiple process data instances, once for each iteration
     * if a forEach parameter is found or a loop condition is met.  Otherwise, no break up is performed
     * and the series is launched using the supplied process data
     *
     * @param actionDef   a process or sequence definition
     * @param processData the running state of the process
     * @throws ComputeException server error
     *
     */
    protected void launchActions(ActionDef actionDef, IProcessData processData) throws ComputeException {
        createStartEvent(actionDef, processData);
        if (actionDef.getLoopUntilCondition() != null) {
            while (!actionDef.getLoopUntilCondition().isSatisfiedBy(processData)) {
                launchAction(actionDef, processData);
            }
        }
        else {
            launchAction(actionDef, processData);
        }
    }

    protected abstract void launchAction(ActionDef actionDef, IProcessData processData) throws ComputeException;

    protected abstract void launchSeriesChildren(SeriesDef seriesDef, IProcessData processData) throws ComputeException;

    protected abstract void captureMessageFromQueue(QueueMessage messageFromQueue, IProcessData processData) throws ComputeException;

    /**
     * A sequence or process can each contain asynchronous actions within it.  The method contains the method calls
     * needed for preparation and execution of such asynchronous actions.
     *
     * @param seriesDef   a process or sequence definition
     * @param processData the running state of the process
     * @throws ComputeException server error
     *
     */
    protected void launchSeries(SeriesDef seriesDef, IProcessData processData) throws ComputeException {
        try {
        	copyHardCodedLocalValuesToPd(seriesDef, processData);
            
            setupAsyncActionsLaunch(seriesDef);
            if (seriesDef.getForEachParam() != null) {
                List<IProcessData> pds = DataExtractor.createForEachPDs(processData, seriesDef.getForEachParam());
                if (pds != null) {
                    for (IProcessData pd : pds) {
                    	try {
                    		launchSeriesChildren(seriesDef, pd);
                    	}
                    	finally {
	                        // If it's asynchronous, then pd would not have the series output parameters at this point
	                        // The series mdb should send its output parameters in its reply message
	                        if (seriesDef.getProcessorType() != ProcessorType.LOCAL_MDB) {
	                            // Copy the series' output parameters into the parent process data
	                            DataExtractor.copyData(pd, processData, seriesDef.getOutputParameters());
	                        }
                    	}
                    }
                }
            }
            else {
            	IProcessData pd = new ProcessData();
            	DataExtractor.copyData(processData, pd, seriesDef.getInputParameters());
            	try {
            		launchSeriesChildren(seriesDef, pd);	
            	}
                finally {
                    DataExtractor.copyData(pd, processData, seriesDef.getOutputParameters());
                }
            }
            waitForAsyncActions(processData, seriesDef);
            recordProcessSuccess(processData, seriesDef);
        }
    	catch (ComputeException e) {
    		handleException(e, seriesDef, processData);
    		throw e;
    	}
        finally {
            cleanupAsyncActionsLaunch();
        }
    }

    /**
     * The method launches a sequence using the supplied sequence definition
     *
     * @param sequenceDef a sequence definition
     * @param processData the running state of the process
     * @throws ComputeException server error
     *
     */
    protected void launchSequence(SequenceDef sequenceDef, IProcessData processData) throws ComputeException {
        switch (sequenceDef.getProcessorType()) {
            case LOCAL_MDB:
                sendMessageToQueue(processData);
                break;
            case LOCAL_SLSB:
            case POJO:
                ILauncher launcher = ProcessorFactory.createLauncherForSeries(sequenceDef);
                launcher.launch(sequenceDef, processData);
                break;
            default:
                throw new LauncherException("Unknown processor type:" + sequenceDef.getProcessorType());
        }
    }

    /**
     * Creates the AsyncMessageInterface instance used to send messages to queues for asynchronous actions.
     * Also creates a temporary queue if waitForAsync is set to true for the supplied series definition
     *
     * @param seriesDef the series definition
     * @throws LauncherException launcher error
     */
    protected void setupAsyncActionsLaunch(SeriesDef seriesDef) throws LauncherException {
        if (seriesDef.containsAysncActions()) {
            messageInterface = JmsUtil.createAsyncMessageInterface();
            if (seriesDef.joinOnAsyncActions()) {
                try {
                    tempWaitQueue = messageInterface.getQueueForReceivingMessages();
                    tempQueueName = tempWaitQueue.getQueueName();
                }
                catch (NamingException e) {
                    throw new LauncherException(e);
                }
                catch (JMSException e) {
                    throw new LauncherException(e);
                }
            }
        }
    }

    /**
     * Returns the temporary queue that might have been created
     */
    protected void cleanupAsyncActionsLaunch() {
        if (tempWaitQueue != null && waitingOnAsync) {
            try {
                messageInterface.returnQueueForReceivingMessages(tempWaitQueue);
                tempWaitQueue = null;
                waitingOnAsync = false;
            }
            catch (JMSException e) {
                logger.error("Error deregistering", e);        // leak
            }
        }
    }

    /**
     * Sends a message to queue for an asynchronous action.
     *
     * @param processData the running state of the process.  Needed to copy any input parameters for the asynchronous
     *                    action.  Currently, only input parameters for the first such action is copied.  There are workarounds
     *                    for the case where a subsequent async operation needs input parameters.  We need to refine this method
     *                    to service this need seemlessly.
     * @throws LauncherException launcher error
     */
    protected void sendMessageToQueue(IProcessData processData) throws LauncherException {
        QueueMessage queueMessage = JmsUtil.sendMessageToQueue(messageInterface, processData, tempWaitQueue);
        if (tempWaitQueue != null) {
            if (queueMessage.getMessageId() == null) {
                throw new LauncherException("Cannot wait for message with null message id");
            }
            waitingOnAsync = true;
            messageIds.add(queueMessage.getMessageId());
        }
    }

    /**
     * Waits for asynchronous actions in <code>seriesDef</code>to complete.
     *
     * @param processData the running state of the process
     * @param seriesDef   the process or sequence definition this launcher has launched
     * @throws ComputeException server error
     *
     */
    protected void waitForAsyncActions(IProcessData processData, SeriesDef seriesDef) throws ComputeException {
        if (!waitingOnAsync || tempWaitQueue == null)
            return;

        // Wait for all asynchronous actions to complete
        while (messageIds.size() > 0) {
            QueueMessage responseQueueMessage;
            ObjectMessage responseMessage;
            try {
                // Wait for a response to an message sent to a queue for processing of an asynchronous action
                responseMessage = (ObjectMessage) messageInterface.waitForMessageOnQueue(seriesDef.getMaxJMSWaitTime(), tempWaitQueue);

                // This would happen if we timed out waiting for the message
                if (responseMessage == null)
                    throw new ServiceException("Failed to receive message from temporary queue:" + tempQueueName);


                responseQueueMessage = new QueueMessage(responseMessage, false);
            }
            catch (Throwable e) {
                throw new LauncherException("Encountered exception waiting for message from temporary queue: " + tempQueueName, e);
            }
            // count down
            messageIds.remove(responseQueueMessage.getOriginalMessageId());
            // Capture the state and output of the processed action
            captureMessageFromQueue(responseQueueMessage, processData);
        }
    }

    /**
     * Optional event that would get persisted before an operation or sequence is launched
     * It is needed because if an operation were to be launched asynchronously and that operation
     * was INTERNALLY creating an event, we would get duplicate-event-constraint-violation exceptions
     * even if we checked for pre-existence of event in DAO (because they're running in separate tx)
     * The solution is to move status update functionality to the framework
     *
     * @param actionDef   the sequence definition
     * @param processData the running state of the process
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException data missing error
     * @throws org.janelia.it.jacs.compute.access.DaoException data access error
     *
     */
    private void createStartEvent(ActionDef actionDef, IProcessData processData) throws DaoException, MissingDataException {
        if (actionDef.getStartEvent() != null) {
            String description;
            if (actionDef.isProcessorAsync()) {
                description = "Sending message to " + actionDef.getProcessorName() + " queue";
            }
            else {
                description = actionDef.getName();
            }
            EJBFactory.getLocalComputeBean().updateTaskStatus(processData.getProcessId(), actionDef.getStartEvent(), description);
        }
    }

    /**
     * This method marks the process in success if the operation or sequence was successful and the process wasn't
     * responsibile for managing it's status and this particular operation or sequence was responisble for updating
     * the process status
     *
     * @param processData the running state of the process
     * @param actionDef   the operation
     */
    protected void recordProcessSuccess(IProcessData processData, ActionDef actionDef) {
        try {
            if (actionDef.updateProcessStatusOnSuccess()) {
                // If successful, copy the output information. - Does this catch everything?!?
                // Post-Processing could be an issue (success happens then other operations continue to delete or create files)
                copyDataToFinalDestination(processData);

                // Updating to success should happen as part of the overall transaction, otherwise
                // users could see the status as complete while hits haven't been peristed yet, for example
                EJBFactory.getLocalComputeBean().recordProcessSuccess(processData.getProcessDef(), processData.getProcessId());
            }
        }
        catch (Exception e) {
            logger.error("Failed to update process status", e);
        }
    }

    private void copyDataToFinalDestination(IProcessData processData) throws MissingDataException,
            ServiceException {
        // Grab the task
        Task task = ProcessDataHelper.getTask(processData);

        // If the current task id is the top level task, enact postprocessing
        if (task.getParentTaskId() == null && (null != task.getParameter(Task.PARAM_finalOutputDirectory) &&
                !"".equals(task.getParameter(Task.PARAM_finalOutputDirectory)))) {
            // Already checked that the final directory exists...
            // Get the node for this guy and copy everything over to the final destination.
            // Clean up the original system dir or let it expire?
            FileNode tmpResultNode = ProcessDataHelper.getResultFileNode(processData);
            try {
                FileUtil.copyDirectory(tmpResultNode.getDirectoryPath(), task.getParameter(Task.PARAM_finalOutputDirectory));
            }
            catch (IOException e) {
                throw new ServiceException("Could not copy the output files to the final destination (from " +
                        tmpResultNode.getDirectoryPath() + " to " + task.getParameter(Task.PARAM_finalOutputDirectory));
            }
        }
    }

    protected void notifyUser(IProcessData processData) throws MissingDataException {

        // Get the username of the owner of the task and send an email
        Task task = ProcessDataHelper.getTask(processData);
        String ownerName = task.getOwner();
        // Check to see if the user wants email
        Subject tmpUser = new SubjectDAO(logger).getSubjectByNameOrKey(ownerName);
        if (null == tmpUser) {
            logger.warn("Cannot notify user: " + ownerName + ". User does not exist in the db.");
            return;
        }
        SubjectPreference emailPref = tmpUser.getPreference(SubjectPreference.CAT_NOTIFICATION, SubjectPreference.PREF_EMAIL_ON_JOB_COMPLETION);
        if (null == emailPref || !Boolean.valueOf(emailPref.getValue())) {
            return;
        }
        String emailAddress = tmpUser.getEmail();
        if (null == emailAddress || "".equals(emailAddress)) {
            logger.debug("No email exists for the user. Skipping mail step.");
            return;
        }

        MailHelper helper = new MailHelper();
        helper.sendEmail("saffordt@janelia.hhmi.org", emailAddress, "Job '" + task.getJobName() + "' finished.", "Job '" + task.getJobName() +
                "' with id " + task.getObjectId() + " finished with state '" + task.getLastEvent().getEventType() + "'");
    }
    
    private void handleException(Exception e, SeriesDef seriesDef, IProcessData processData) {

    	SequenceDef exceptionHandlerDef = seriesDef.getExceptionHandlerDef();
    	if (exceptionHandlerDef==null) return;
    	
		processData.putItem(IProcessData.PROCESSING_EXCEPTION, e);
		try {
			launchSequence(exceptionHandlerDef, processData);
		}
		catch (Exception x) {
            logger.error("Failed to run exception handler for SeriesDef, name="+seriesDef.getName(), x);
		}
    }
    
    /**
     * This method copies values hardcoded in include definition over to process data before
     * sequence is launched
     *
     * @param operationDef the operation whose values are to be copied from the descriptior
     * @param processData  the running state of the process
     */
    private void copyHardCodedLocalValuesToPd(SeriesDef seriesDef, IProcessData processData) {
        for (Parameter parameter : seriesDef.getLocalInputParameters()) {
            if (parameter.getValue() != null) {
            	String value = (String)parameter.getValue();
            	processData.putItem(parameter.getName(), value);
            	if (value.startsWith("$V{")) {
            		// dereference any variables across include boundaries
                    processData.putItem(parameter.getName(), processData.getItem(parameter.getName()));
            	}
            }
        }
    }
}
