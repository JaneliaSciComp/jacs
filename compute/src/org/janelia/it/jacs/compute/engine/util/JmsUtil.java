package org.janelia.it.jacs.compute.engine.util;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.DataExtractor;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.data.QueueMessage;
import org.janelia.it.jacs.compute.engine.def.ActionDef;
import org.janelia.it.jacs.compute.engine.def.OperationDef;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.engine.launcher.LauncherException;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;

import javax.ejb.EJBException;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import java.util.Map;

/**
 * This class contains utility methods for sending JMS messages to action processors
 *
 * @author Tareq Nabeel
 */
public class JmsUtil {
    private static Logger logger = Logger.getLogger(JmsUtil.class);

    public static boolean replyToReceivedFromQueue(QueueMessage originalQueueMessage, ActionDef actionToProcess) {
        return replyToReceivedFromQueue(originalQueueMessage, actionToProcess, null);
    }

    public static boolean replyToReceivedFromQueue(QueueMessage originalQueueMessage, ActionDef actionToProcess, Throwable e) {
        try {
            Queue replyToQueue = (Queue) originalQueueMessage.getMessage().getJMSReplyTo();
            if (replyToQueue == null) {
                return false;
            }
            AsyncMessageInterface messageInterface = createAsyncMessageInterface();
            // Cannot start a session with temporary queue: javax.naming.NameNotFoundException: JMS_TQ2 not bound
            // Have to use original message and have to clear the body of the original message before calling replyMessage.setObjectMessage
            // messageInterface.startMessageSession(replyToQueue.getQueueName(),AsyncMessageInterface.LOCAL_CONNECTION_FACTORY);
            QueueMessage replyMessage = new QueueMessage((ObjectMessage) originalQueueMessage.getMessage(), true);
            prepareReplyMessage(originalQueueMessage, actionToProcess, replyMessage, e);
            originalQueueMessage.clearBody();
            replyMessage.setObjectMessage();
            messageInterface.sendMessage(replyMessage.getMessage(), replyToQueue);
            return true;
        }
        catch (Throwable ee) {
            throw new EJBException(new ComputeException("Error encountered during replyToReceivedFromQueue for message id:" + originalQueueMessage.getMessageId() + " for action: " + actionToProcess, ee));
        }
    }

    public static void forwardToLinkedToQueue(QueueMessage originalQueueMessage, ActionDef actionToProcess) {
        try {
            logActionStatus(originalQueueMessage, null);
            AsyncMessageInterface messageInterface = createAsyncMessageInterface();
            messageInterface.startMessageSession(actionToProcess.getQueueToLinkTo());
            ObjectMessage jmsMessage = messageInterface.createObjectMessage();
            jmsMessage.setJMSReplyTo(originalQueueMessage.getMessage().getJMSReplyTo());
            QueueMessage replyMessage = new QueueMessage(jmsMessage, true);
            prepareReplyMessage(originalQueueMessage, actionToProcess, replyMessage, null);
            replyMessage.setObjectMessage();
            messageInterface.sendMessageWithinTransaction(jmsMessage);
            messageInterface.commit();
            messageInterface.endMessageSession();
        }
        catch (Throwable ee) {
            throw new EJBException(new ComputeException("Error encountered during forwardToLinkedToQueue of message id:" + originalQueueMessage.getMessageId() + " for action: " + actionToProcess, ee));
        }
    }


    private static void logActionStatus(QueueMessage originalQueueMessage, Throwable e) throws MissingDataException {
        if (e == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Successfully processed message id:" + originalQueueMessage.getMessageId() + " for action: " + originalQueueMessage.getActionToProcess());
            }
        }
        else {
            logger.error("Error encountered during processing of message id:" + originalQueueMessage.getMessageId() + " for action: " + originalQueueMessage.getActionToProcess() + " for process:" + originalQueueMessage.getProcessDef(), e);
        }
    }

    public static AsyncMessageInterface createAsyncMessageInterface() {
        return new AsyncMessageInterface();
    }

    /**
     * This method prepares the JMS reply message.  It puts the output data of the service into the message
     * It also sets service status data (processedSuccessfully, processing exception, and processed action) so
     * caller can determine what happened.  If the action has a queueToLinkTo parameter, it also sets the data
     * that the service it's linking to needs.
     *
     * @param originalQueueMessage original message
     * @param processedAction processed Action
     * @param replyMessage the reply message
     * @param e error
     * @throws LauncherException server error
     * @throws JMSException JMS error
     */
    private static void prepareReplyMessage(QueueMessage originalQueueMessage, ActionDef processedAction, QueueMessage replyMessage, Throwable e) throws LauncherException, JMSException {
        try {
            replyMessage.setProcessedSuccessfully(e == null);
            replyMessage.setProcessingException(e);
            replyMessage.setProcessedAction(processedAction);
            if (originalQueueMessage.getOriginalMessageId() != null) {
                replyMessage.setOriginalMessageId(originalQueueMessage.getOriginalMessageId());
            }
            else {
                replyMessage.setOriginalMessageId(originalQueueMessage.getMessage().getJMSMessageID());
            }
            // Copy output parameters of executed action
            DataExtractor.copyData(originalQueueMessage, replyMessage, processedAction.getOutputParameters());
            copyLinkedChainInputParameters(originalQueueMessage, replyMessage, processedAction, processedAction.getQueueToLinkTo(), true);
        }
        catch (MissingDataException ee) {
            throw new LauncherException("Failed to reply for " + processedAction, ee);
        }

    }

    private static void copyLinkedChainInputParameters(IProcessData processData, QueueMessage replyMessage, ActionDef action, String queueToLinkTo, boolean setActionToProcess) throws LauncherException {
        try {
            if (queueToLinkTo == null) {
                return;
            }
            ActionDef firstLinkedAction = getLinkedAction(processData, action.getQueueToLinkTo());
            ActionDef linkedAction = firstLinkedAction;
            ProcessDef processDef = processData.getProcessDef();
            do {
                if (linkedAction.isOperation()) {
                    DataExtractor.copyData(processData, replyMessage, linkedAction.getParentDef().getInputParameters());
                }
                else {
                    DataExtractor.copyData(processData, replyMessage, linkedAction.getInputParameters());
                }
                queueToLinkTo = linkedAction.getQueueToLinkTo();
            }
            while (queueToLinkTo != null &&
                    (linkedAction = processDef.getLinkedAction(queueToLinkTo)) != null);

            if (setActionToProcess)
                replyMessage.setActionToProcess(firstLinkedAction);
        }
        catch (MissingDataException ee) {
            throw new LauncherException("Failed to reply for " + action, ee);
        }
    }

    private static ActionDef getLinkedAction(IProcessData processData, String queueToReplyTo) throws MissingDataException {
        ProcessDef processDef = processData.getProcessDef();
        ActionDef linkedAction = processDef.getLinkedAction(queueToReplyTo);
        if (linkedAction == null) {
            throw new RuntimeException("Linked queue for " + queueToReplyTo + " has to exist");
        }
        return linkedAction;
    }

    public static QueueMessage sendMessageToQueue(AsyncMessageInterface messageInterface, IProcessData processData, Queue replyToQueue) throws LauncherException {
        try {
            ActionDef actionToProcess = processData.getActionToProcess();
            messageInterface.startMessageSession(getQueueName(actionToProcess));
            ObjectMessage jmsMessage = messageInterface.createObjectMessage();
            if (replyToQueue != null) {
                jmsMessage.setJMSReplyTo(replyToQueue);
            }
            QueueMessage queueMessage = new QueueMessage(jmsMessage, true);
            DataExtractor.copyAllData(processData, queueMessage);
            copyLinkedChainInputParameters(processData, queueMessage, actionToProcess, actionToProcess.getQueueToLinkTo(), false);
            queueMessage.setObjectMessage();
            messageInterface.sendMessageWithinTransaction(queueMessage.getMessage());
            messageInterface.commit();
            messageInterface.endMessageSession();
            queueMessage.setMessageId(jmsMessage.getJMSMessageID());
            logger.debug("Sent message with messageId:" + jmsMessage.getJMSMessageID() + " for taskId:" + processData.getItem(IProcessData.TASK));
            return queueMessage;
        }
        catch (Throwable e) {
            // Cannot proceed further
            throw new LauncherException(e);
        }
    }

    public static void sendMessageToQueue(AsyncMessageInterface messageInterface, Map<String,String> parameters, String queueName) throws LauncherException {
        try {
            messageInterface.startMessageSession(queueName);
            MapMessage mapMessage = messageInterface.createMapMessage();
            for ( String paramName: parameters.keySet() ) {
                mapMessage.setString( paramName, parameters.get( paramName ) );
            }
            messageInterface.sendMessageWithinTransaction(mapMessage);
            messageInterface.commit();
            messageInterface.endMessageSession();
        }
        catch (Throwable e) {
            // Cannot proceed further
            throw new LauncherException(e);
        }
    }

    private static String getQueueName(ActionDef actionDef) {
        if (actionDef instanceof OperationDef) {
            if (actionDef.isProcessorAsync() && !actionDef.getProcessorName().startsWith("queue")) {
                return ((OperationDef) actionDef).getDefaultMdbProcessor();
            }
        }
        return actionDef.getProcessorName();
    }

}
