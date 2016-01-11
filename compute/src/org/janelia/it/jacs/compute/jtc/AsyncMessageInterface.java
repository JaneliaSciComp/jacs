package org.janelia.it.jacs.compute.jtc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;

public class AsyncMessageInterface {
    private String localConnectionFactoryProperty = "AsyncMessageInterface.LocalConnectionFactory";
    private String remoteConnectionFactoryProperty = "AsyncMessageInterface.RemoteConnectionFactory";
    public String providerUrlProperty = "AsyncMessageInterface.ProviderURL";
    private String dlqProperty = "AsyncMessageInterface.DLQ";
    private Logger logger = Logger.getLogger(AsyncMessageInterface.class);

    public ConnectionType remoteConnectionType;
    public ConnectionType localConnectionType;
    private static String REMOTE_CONNECTION_FACTORY = "UIL2XAConnectionFactory";
    private static String LOCAL_CONNECTION_FACTORY = "java:/XAConnectionFactory";
    //Default to the cluster multicast default address
    private String providerUrl = "230.0.0.4:1102";
    private String dlq = "queue/DLQ";

    private QueueSender sender;
    private QueueSession session;
    private QueueConnection connection;
    private boolean openTransaction;

    private Map<Object, QueueSessionInfo> receiverMap = new HashMap<Object, QueueSessionInfo>();

    public String getLocalConnectionFactoryProperty() {
        return localConnectionFactoryProperty;
    }

    public String getRemoteConnectionFactoryProperty() {
        return remoteConnectionFactoryProperty;
    }

    public String getProviderUrlProperty() {
        return providerUrlProperty;
    }

    public String getDlqProperty() {
        return dlqProperty;
    }

    public ConnectionType getRemoteConnectionType() {
        return remoteConnectionType;
    }

    public ConnectionType getLocalConnectionType() {
        return localConnectionType;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    public AsyncMessageInterface(String localConnectionFactoryProperty
            , String remoteConnectionFactoryProperty
            , String providerUrlProperty
            , String dlqProperty) {
        this.localConnectionFactoryProperty = localConnectionFactoryProperty;
        this.remoteConnectionFactoryProperty = remoteConnectionFactoryProperty;
        this.providerUrlProperty = providerUrlProperty;
        this.dlqProperty = dlqProperty;

        PropertyHelper helper = PropertyHelper.getInstance();
        remoteConnectionType = new ConnectionType("RemoteConnection", REMOTE_CONNECTION_FACTORY);
        localConnectionType = new ConnectionType("LocalConnection", LOCAL_CONNECTION_FACTORY);
        providerUrl = helper.getProperty(providerUrlProperty, providerUrl);
        dlq = helper.getProperty(dlqProperty, dlq);
    }

    public AsyncMessageInterface() {

        PropertyHelper helper = PropertyHelper.getInstance();
        REMOTE_CONNECTION_FACTORY = helper.getProperty(remoteConnectionFactoryProperty, REMOTE_CONNECTION_FACTORY);
        LOCAL_CONNECTION_FACTORY = helper.getProperty(localConnectionFactoryProperty, LOCAL_CONNECTION_FACTORY);

        remoteConnectionType = new ConnectionType("RemoteConnection", REMOTE_CONNECTION_FACTORY);
        localConnectionType = new ConnectionType("LocalConnection", LOCAL_CONNECTION_FACTORY);
        providerUrl = helper.getProperty(providerUrlProperty, providerUrl);
        dlq = helper.getProperty(dlqProperty, dlq);
    }

    /**
     * Used to start the session with JMS.  Always remember to End the Session!!!
     *
     * @param queueName
     * @throws javax.naming.NamingException
     * @throws javax.jms.JMSException
     */
    public void startMessageSession(String queueName, ConnectionType connectionType) throws NamingException, JMSException {
        setupConnection(queueName, connectionType);
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
     * @param message
     * @throws JMSException
     * @throws IllegalStateException if called before startMessageSession
     */
    public void sendMessageWithinTransaction(Message message) throws JMSException, IllegalStateException {
        openTransaction = true;
        if (sender == null) throw new IllegalStateException("You MUST call startMessageSession first!!");
        if (logger.isDebugEnabled()) {
        	logger.debug("Sending message within transaction with size: "+getMessageSizeInBytes(message));
        }
        sender.send(message);
    }

    /**
     * Use to send a message within a container, in which transactions are managed for you.
     *
     * @param message
     * @throws JMSException
     * @throws IllegalStateException if called before startMessageSession
     */
    public void sendMessageWithinContainer(Message message) throws JMSException, IllegalStateException {
        if (sender == null) throw new IllegalStateException("You MUST call startMessageSession first!!");
        logger.info("Transaction in effect is " + this.session.getTransacted());
        if (logger.isDebugEnabled()) {
        	logger.debug("Sending message within container with size: "+getMessageSizeInBytes(message));
        }
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
     * @param message
     * @throws IllegalStateException if called during use of the transactional API
     *                               MUST use sendMessageWithinTransaction instead
     */
    public void sendMessage(Message message, String queueName, ConnectionType connectionType) throws IllegalStateException,
            JMSException, NamingException {
        if (session != null) throw new IllegalStateException("Cannot use simple API while using transactional API");
        try {
            setupConnection(queueName, connectionType);
            if (logger.isDebugEnabled()) {
            	logger.debug("Sending message with size: "+getMessageSizeInBytes(message));
            }
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
                setupConnection(queueName, connectionType);
                if (logger.isDebugEnabled()) {
                	logger.debug("Resending message with size: "+getMessageSizeInBytes(message));
                }
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

    /**
     * Simplistic interface if you already have a Message and do not need to send more than one
     * message in a transaction.
     * Will establish and teardown a connection for each call. Will try a second time on failure.
     *
     * @param message
     * @throws IllegalStateException if called during use of the transactional API
     *                               MUST use sendMessageWithinTransaction instead
     */
    public void sendMessage(Message message, Queue queue, ConnectionType connectionType) throws IllegalStateException,
            JMSException, NamingException {
        if (session != null) throw new IllegalStateException("Cannot use simple API while using transactional API");
        try {
            setupConnection(queue, connectionType);
            if (logger.isDebugEnabled()) {
            	logger.debug("Sending message with size: "+getMessageSizeInBytes(message));
            }
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
                setupConnection(queue, connectionType);
                if (logger.isDebugEnabled()) {
                	logger.debug("Resending message with size: "+getMessageSizeInBytes(message));
                }
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

    /**
     * Simplistic interface if you already have a Message and do not need to send more than one
     * message in a transaction.
     * Will establish and teardown a connection for each call. Will try a second time on failure.
     *
     * @param message
     * @throws IllegalStateException if called during use of the transactional API
     *                               MUST use sendMessageWithinTransaction instead
     */
    public void sendMessageToDLQ(Message message, ConnectionType connectionType) throws IllegalStateException,
            JMSException, NamingException {
        sendMessage(message, dlq, connectionType);
    }

    /**
     * Non-blocking method of waiting for a message on a queue
     *
     * @param listener
     * @return
     * @throws JMSException
     * @throws NamingException
     */
    public Queue registerTemporaryQueueListener(MessageListener listener, ConnectionType connectionType) throws JMSException, NamingException {
        InitialContext initialContext = (InitialContext) getInitialContext();
        QueueConnectionFactory connectionFactory =
                (QueueConnectionFactory) initialContext.lookup(connectionType.connectionFactory);
        QueueConnection receiveConnection = connectionFactory.createQueueConnection();
        QueueSession receiveSession = receiveConnection.createQueueSession(false, QueueSession.CLIENT_ACKNOWLEDGE);
        receiveConnection.start();
        Queue receiveQueue = receiveSession.createTemporaryQueue();
        QueueReceiver receiver = receiveSession.createReceiver(receiveQueue);
        QueueSessionInfo queueSessionInfo = new QueueSessionInfo(
                receiveQueue, receiveConnection, receiveSession, receiver);
        receiverMap.put(listener, queueSessionInfo);
        receiver.setMessageListener(new MessageListenerWrapper(listener));
        return receiveQueue;
    }

    /**
     * deregister from the non-blocking msg waiting mechanism
     *
     * @param listener
     * @throws JMSException
     */
    public void deregisterTemporaryQueueListener(MessageListener listener) throws JMSException {
        QueueSessionInfo queueSessionInfo = receiverMap.get(listener);
        tearDownQueueSession(queueSessionInfo);
        receiverMap.remove(listener);
    }


    public Queue getQueueForReceivingMessages(ConnectionType connectionType) throws JMSException, NamingException {
        InitialContext initialContext = (InitialContext) getInitialContext();
        QueueConnectionFactory connectionFactory =
                (QueueConnectionFactory) initialContext.lookup(connectionType.connectionFactory);
        QueueConnection receiveConnection = connectionFactory.createQueueConnection();
        QueueSession receiveSession = receiveConnection.createQueueSession(false, QueueSession.CLIENT_ACKNOWLEDGE);
        receiveConnection.start();
        Queue receiveQueue = receiveSession.createTemporaryQueue();
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

    private void setupConnection(String queueName, ConnectionType connectionType) throws NamingException, JMSException {
        setupConnection(queueName, connectionType, true);
    }

    private void setupConnection(String queueName, ConnectionType connectionType, boolean startTransaction)
            throws NamingException, JMSException {
        Context ctx = getInitialContext();
        QueueConnectionFactory conFactory =
                (QueueConnectionFactory) ctx.lookup(connectionType.connectionFactory);
        connection = conFactory.createQueueConnection();
        session = connection.createQueueSession(startTransaction, QueueSession.AUTO_ACKNOWLEDGE);
        Queue queue = (Queue) ctx.lookup(queueName);
        sender = session.createSender(queue);
        connection.start();
    }

    private void setupConnection(Queue queue, ConnectionType connectionType) throws NamingException, JMSException {
        Context ctx = getInitialContext();
        QueueConnectionFactory conFactory =
                (QueueConnectionFactory) ctx.lookup(connectionType.connectionFactory);
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
        if (queueSessionInfo.getQueueConnection() == null) {
            queueSessionInfo.getQueueConnection().stop();
        }
        if (queueSessionInfo.getSession() != null) {
            queueSessionInfo.getSession().close();
        }
        if (queueSessionInfo.getQueueConnection() == null) {
            queueSessionInfo.getQueueConnection().close();
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

	private XStream stream = new XStream();
	
    private Integer getMessageSizeInBytes(Message message) {
    	try {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        ObjectOutputStream oos = new ObjectOutputStream(baos);
	        oos.writeObject(message);
	        oos.close();
	        
	        int len = baos.size();
	        
	        if (logger.isTraceEnabled()) {
        		File outfile = new File("/tmp","msg_"+System.currentTimeMillis()+"-"+len+".xml");
        		FileWriter writer = new FileWriter(outfile);
        		if (message instanceof ObjectMessage) {
        			// This is very wasteful, because it deserializes the object just to stream it to XML. But this method is only called for trace debugging, so we put up with it for now.
        			stream.toXML(((ObjectMessage)message).getObject(), writer);
        		}
        		else {
        			stream.toXML(message, writer);
        		}
        		logger.trace("Serialized message to "+outfile);
	        }
	        
	        return len;
    	}
    	catch (Exception e) {
    		logger.error("Error getting message size",e);
    		return null;
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

    /**
     * forced acknowldge
     */
    private class MessageListenerWrapper implements MessageListener {
        private MessageListener theMessageListener;

        public MessageListenerWrapper(MessageListener messageListner) {
            this.theMessageListener = messageListner;
        }

        public void onMessage(Message msg) {
            try {
                msg.acknowledge();
            }
            catch (Exception ex) {
                logger.info("Could not acknowledge message");
            }
            theMessageListener.onMessage(msg);
        }
    }

    private static class ConnectionType {
        private String connectionType;
        private String connectionFactory;

        private ConnectionType(String connectionType, String connectionFactory) {
            this.connectionType = connectionType;
            this.connectionFactory = connectionFactory;
        }

        public String getConnectionType() {
            return connectionType;
        }

    }
}
