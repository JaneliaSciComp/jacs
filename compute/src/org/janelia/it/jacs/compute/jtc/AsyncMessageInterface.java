
package org.janelia.it.jacs.compute.jtc;

import org.apache.log4j.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.IllegalStateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AsyncMessageInterface {
    private Logger logger = Logger.getLogger(AsyncMessageInterface.class);

    private String LOCAL_CONNECTION_FACTORY = "java:/XAConnectionFactory";
    //Default to the cluster multicast default address
    private String providerUrl = "230.0.0.4:1102";

    private QueueSender sender;
    private QueueSession session;
    private QueueConnection connection;
    private boolean openTransaction;

    private Map<Object, QueueSessionInfo> receiverMap = new HashMap<>();

    public AsyncMessageInterface() {
        PropertyHelper helper = PropertyHelper.getInstance();
        LOCAL_CONNECTION_FACTORY = helper.getProperty("AsyncMessageInterface.LocalConnectionFactory", LOCAL_CONNECTION_FACTORY);
        providerUrl = helper.getProperty("AsyncMessageInterface.ProviderURL", providerUrl);
    }

    /**
     * Used to start the session with JMS.  Always remember to End the Session!!!
     *
     * @param queueName target queue
     * @throws javax.naming.NamingException
     * @throws javax.jms.JMSException
     */
    public void startMessageSession(String queueName) throws NamingException, JMSException {
        setupConnection(queueName);
    }

    /**
     * Used to end the session with JMS.  Always remember to End the Session!!!
     *
     * @throws JMSException
     */
    public void endMessageSession() throws JMSException {
        tearDownConnection();
    }

    /**
     * Used to create a Map Message
     */
    public MapMessage createMapMessage() throws JMSException, IllegalStateException {
        if (session == null) throw new IllegalStateException("You must first start a session!");
        return session.createMapMessage();
    }

    /**
     * Used to create a Map Message
     */
    public ObjectMessage createObjectMessage() throws JMSException, IllegalStateException {
        if (session == null) throw new IllegalStateException("You must first start a session!");
        return session.createObjectMessage();
    }

    /**
     * Use to send a message after calling startMessageSession, but before commit.
     *
     * @param message message to send
     * @throws JMSException
     * @throws IllegalStateException if called before startMessageSession
     */
    public void sendMessageWithinTransaction(Message message) throws JMSException, IllegalStateException {
        openTransaction = true;
        if (sender == null) throw new IllegalStateException("You MUST call startMessageSession first!!");
        sender.send(message);
    }

    /**
     * Commit the transaction.  Must be called to send the message!!
     *
     * @throws IllegalStateException - if called before startMessageSession, or no messages have been
     *                               sent in the current transaction
     * @throws JMSException
     */
    public void commit() throws IllegalStateException, JMSException {
        if (session != null && openTransaction) {
            session.commit();
            openTransaction = false;
        }
        else throw new IllegalStateException("Cannot call commit without a transaction");
    }

    /**
     * Rollback a transaction
     *
     * @throws IllegalStateException - - if called before startMessageSession, or no messages have been
     * @throws JMSException
     */
    public void rollback() throws IllegalStateException, JMSException {
        if (session != null && openTransaction) {
            session.rollback();
            openTransaction = false;
        }
        else throw new IllegalStateException("Cannot call rollback without a transaction");
    }

    /**
     * Simplistic interface if you already have a Message and do not need to send more than one
     * message in a transaction.
     * Will establish and teardown a connection for each call. Will try a second time on failure.
     *
     * @param message message to send
     * @throws IllegalStateException if called during use of the transactional API
     *                               MUST use sendMessageWithinTransaction instead
     */
    public void sendMessage(Message message, Queue queue) throws IllegalStateException,
            JMSException, NamingException {
        if (session != null) throw new IllegalStateException("Cannot use simple API while using transactional API");
        try {
            setupConnection(queue);
            sender.send(message);
            session.commit();
            tearDownConnection();
        }
        catch (JMSException jmsEx) {
            try {
                tearDownConnection();
            }
            catch (JMSException jmsEx2) {
            //do nothing
            }
            logger.warn("Cannot contact JMS server - reestablishing connection");
            try {
                setupConnection(queue);
                sender.send(message);
                session.commit();
                tearDownConnection();
            }
            catch (JMSException ex) {
                logger.error("JMS Connection reestablish failed");
                throw ex;
            }
        }
    }

    public Queue getQueueForReceivingMessages() throws JMSException, NamingException {
        InitialContext initialContext = (InitialContext) getInitialContext();
        QueueConnectionFactory connectionFactory =
                (QueueConnectionFactory) initialContext.lookup(LOCAL_CONNECTION_FACTORY);
        QueueConnection receiveConnection = connectionFactory.createQueueConnection();
        QueueSession receiveSession = receiveConnection.createQueueSession(false, QueueSession.CLIENT_ACKNOWLEDGE);
        receiveConnection.start();
        Queue receiveQueue = receiveSession.createTemporaryQueue();
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // Do nothing.  Trying to prevent a race condition between the creation of temporary queues and code referencing them
        }
        //Queue receiveQueue=(Queue)initialContext.lookup("queue/A");
        QueueReceiver receiver = receiveSession.createReceiver(receiveQueue);
        QueueSessionInfo queueSessionInfo = new QueueSessionInfo(
                receiveQueue, receiveConnection, receiveSession, receiver);
        receiverMap.put(receiveQueue, queueSessionInfo);
        return receiveQueue;
    }

    public Message waitForMessageOnQueue(long millisecondsToWait, Queue queue)
            throws IllegalArgumentException, JMSException {
        QueueSessionInfo queueSessionInfo = receiverMap.get(queue);
        if (queueSessionInfo == null)
            throw new IllegalArgumentException("Passed Queue must come from getQueueForReceivingMessages()");
        QueueReceiver receiver = queueSessionInfo.getReceiver();
        Message msg = receiver.receive(millisecondsToWait);
        if (msg != null) msg.acknowledge();
        return msg;
    }

    public void returnQueueForReceivingMessages(Queue queue)
            throws IllegalArgumentException, JMSException {
        QueueSessionInfo queueSessionInfo = receiverMap.get(queue);
        if (queueSessionInfo == null)
            throw new IllegalArgumentException("Passed Queue must come from getQueueForReceivingMessages()");
        receiverMap.remove(queue);
        tearDownQueueSession(queueSessionInfo);
    }

    private void setupConnection(String queueName) throws NamingException, JMSException {
        setupConnection(queueName, true);
    }

    private void setupConnection(String queueName, boolean startTransaction)
            throws NamingException, JMSException {
        Context ctx = getInitialContext();
        QueueConnectionFactory conFactory =
                (QueueConnectionFactory) ctx.lookup(LOCAL_CONNECTION_FACTORY);
        connection = conFactory.createQueueConnection();
        session = connection.createQueueSession(startTransaction, QueueSession.AUTO_ACKNOWLEDGE);
        Queue queue = (Queue) ctx.lookup(queueName);
        sender = session.createSender(queue);
        connection.start();
    }

    private void setupConnection(Queue queue) throws NamingException, JMSException {
        Context ctx = getInitialContext();
        QueueConnectionFactory conFactory =
                (QueueConnectionFactory) ctx.lookup(LOCAL_CONNECTION_FACTORY);
        connection = conFactory.createQueueConnection();
        session = connection.createQueueSession(true, QueueSession.AUTO_ACKNOWLEDGE);
        sender = session.createSender(queue);
        connection.start();
    }

    private Context getInitialContext() throws NamingException {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        return new InitialContext(env);
    }

    private void tearDownQueueSession(QueueSessionInfo queueSessionInfo) throws JMSException {
        if (queueSessionInfo.getReceiver() != null) {
            queueSessionInfo.getReceiver().close();
        }
        if (queueSessionInfo.getQueueConnection() != null) {
            queueSessionInfo.getQueueConnection().stop();
        }
        if (queueSessionInfo.getSession() != null) {
            queueSessionInfo.getSession().close();
        }
    }

    private void tearDownConnection() throws JMSException {
        try {
            if (openTransaction) commit();
        }
        catch (Throwable th) {
            // NOTE: as of SuSE Linux migration, the commit call is no longer needed in DataDelivery.
            //   However, other systems using this interface may require it.
            logger.warn("Got throwable after commit: " + th.getClass().getName() + "/" + th.getMessage());
        }
        try {
            if (sender != null) {
                sender.close();
            }
        }
        finally {
            sender = null;
        }
        try {
            if (connection != null) {
                connection.stop();
            }
        }
        catch (Throwable th) {
            logger.warn("Got throwable after connection.stop: " + th.getClass().getName() + "/" + th.getMessage());
        }
        try {
            if (session != null) {
                session.close();
            }
        }
        finally {
            session = null;
        }
        try {
            if (connection != null) {
                connection.close();
            }
        }
        finally {
            connection = null;
        }
    }

    private class QueueSessionInfo {
        private Queue queue;
        private QueueConnection connection;
        private QueueSession session;
        private QueueReceiver receiver;

        public QueueSessionInfo(Queue queue, QueueConnection connection,
                                QueueSession session, QueueReceiver receiver) {
            this.queue = queue;
            this.connection = connection;
            this.session = session;
            this.receiver = receiver;
        }

        public Queue getQueue() {
            return queue;
        }

        public QueueConnection getQueueConnection() {
            return connection;
        }

        public QueueSession getSession() {
            return session;
        }

        public QueueReceiver getReceiver() {
            return receiver;
        }

    }

}
