
package org.janelia.it.jacs.compute.engine.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.def.ActionDef;

/**
 * This class encapsulates messages sent to and received from queues.  It's based on
 * earlier work done by Sean Murphy on TaskMessage
 *
 * @author Tareq Nabeel
 */
public class QueueMessage implements IProcessData {
    static final String SRC_HOST_NAME = "SrcHostname";
    static final String SRC_USER_NAME = "SrcUsername";
    static final String MESSAGE_CREATION_TIME = "CreationTime";
    static final String VERSION = "Version";
    static final String EXPECTED_VERSION = "1";
    private static final String MINOR_VERSION = "0";
    private MapMessage mapMsg;
    private ObjectMessage objectMsg;
    private HashMap objectMap;


    private static final Logger logger = Logger.getLogger(QueueMessage.class);
    
    public QueueMessage(ObjectMessage msg, boolean construct) throws JMSException {
        if (msg != null) {
            this.objectMsg = msg;
        } else {
            throw new IllegalArgumentException("msg passed to QueueMessage is null!");
        }
        if (construct) {
            objectMap = new HashMap();
            setConstructionParameters();
            // objectMsg.setObject(objectMap);
        } else {
            objectMap = (HashMap) objectMsg.getObject();
        }
        try {
            // ObjectMessage will not have an id if it's a newly created JMS Message
            // However, if it's a JMS Message received in an onMessage method of an MDB
            // then it will be captured and that's important
            setMessageId(msg.getJMSMessageID());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return original or constructed message
     */
    public Message getMessage() {
        if (mapMsg != null) return mapMsg;
        return objectMsg;
    }

    protected Object getObjectProperty(String property) {
        Object obj = null;
        try {
            if (mapMsg != null) {
                obj = mapMsg.getObject(property);
            } else {
                if (objectMap != null) {
                    obj = objectMap.get(property);
                }
            }
        } catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
            return null;
        }
        return obj;
    }

    protected void setStringProperty(String property, String value) {
        try {
            if (mapMsg != null) mapMsg.setString(property, value);
            else {
                if (objectMap != null) {
                    objectMap.put(property, value);
                }
            }
        } catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
        }
    }

    protected void setLongProperty(String property, Long value) {
        try {
            if (mapMsg != null) mapMsg.setLong(property, value);
            else {
                if (objectMap != null) {
                    objectMap.put(property, value);
                }
            }
        }
        catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
        }
    }

    protected void setObjectProperty(String property, Object value) {
        if (value != null && !(value instanceof Serializable))
            throw new IllegalStateException("Cannot set Object properties that are not serializable");
        if (mapMsg != null)
            throw new IllegalStateException("Cannot set Object properties on a MapMessage");
        else {
            if (objectMap != null) {
                objectMap.put(property, value);
            }
        }
    }

    /**
     * This MUST be called when all properties are populated on the message.  Otherwise
     * the properties will never be copied into the message.
     *
     * @throws JMSException error with the message
     */
    protected void finishedObjectMsgConstruction() throws JMSException {
        objectMsg.setObject(objectMap);
    }

    private void setConstructionParameters() {
        setLongProperty(MESSAGE_CREATION_TIME, System.currentTimeMillis());
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            setStringProperty(SRC_HOST_NAME, hostName);
        } catch (UnknownHostException ex) {
            setStringProperty(SRC_HOST_NAME, "Unknown");
        }
        String userName = System.getProperty("user.name");
        setStringProperty(SRC_USER_NAME, userName);
        setStringProperty(VERSION, EXPECTED_VERSION + "." + MINOR_VERSION);
    }

    public void clearBody() throws JMSException {
        getMessage().clearBody();
        if (objectMap != null) {
            objectMap.clear();
        }
    }

    public Map getObjectMap() {
        if (this.objectMsg != null) {
            return this.objectMap;
        }
        return null;
    }

    public void putItem(String key, Object obj) {
        setObjectProperty(key, obj);
    }

    public Object getLiteralItem(String key) {
        return getObjectProperty(key);
    }
    
    public Object getItem(String key) {
        Object value = getLiteralItem(key);
        if (value instanceof String) {
            String valueStr = (String) value;
            // If the value is wrapped in $V{} then we're interested in the value's value in processData
            if (valueStr.startsWith("$V{")) {
                String key2 = valueStr.substring(valueStr.indexOf("$V{") + 3, valueStr.length() - 1);
                if (key.equals(key2)) {
                    logger.error("QueueMessage variable refers to itself: "+key);
                    return null;
                }
                return getItem(key2);
            } else {
                return valueStr;
            }
        } else {
            return value;
        }
    }

    public Object getMandatoryItem(String key) throws MissingDataException {
        Object item = getItem(key);
        if (item == null) {
            throw new MissingDataException("Missing mandatory messsage data item: " + key);
        }
        return item;
    }


    public void removeItem(String key) {
        //
    }

    public List<Long> getProcessIds() {
        return null;
    }

    public void setProcessIds(List<Long> ids) {
        //
    }

    public Long getProcessId() throws MissingDataException {
        return (Long) getMandatoryItem(IProcessData.PROCESS_ID);
    }

    public void setProcessId(Long id) {
        putItem(IProcessData.PROCESS_ID, id);
    }
    
    public String getProcessDefName() throws MissingDataException {
        return (String) getMandatoryItem(IProcessData.PROCESS_DEFINITION);
    }

    public void setProcessDefName(String processDefName) {
        putItem(IProcessData.PROCESS_DEFINITION, processDefName);
    }

    public ActionDef getProcessedAction() throws MissingDataException {
        return (ActionDef) getMandatoryItem(IProcessData.PROCESSED_ACTION);
    }

    public void setProcessedAction(ActionDef actionDef) {
        putItem(IProcessData.PROCESSED_ACTION, actionDef);
    }

    public ActionDef getActionToProcess() throws MissingDataException {
        return (ActionDef) getMandatoryItem(IProcessData.ACTION_TO_PROCESS);
    }

    public void setActionToProcess(ActionDef actionDef) {
        putItem(IProcessData.ACTION_TO_PROCESS, actionDef);
    }

    public String getMessageId() {
        return (String) getItem(IProcessData.MESSAGE_ID);
    }

    public void setMessageId(String messageId) {
        putItem(IProcessData.MESSAGE_ID, messageId);
    }

    public String getOriginalMessageId() {
        return (String) getItem(IProcessData.ORIGINAL_MESSAGE_ID);
    }

    public void setOriginalMessageId(String messageId) {
        putItem(IProcessData.ORIGINAL_MESSAGE_ID, messageId);
    }

    public boolean isProcessedSuccessfully() {
        return getBoolean(IProcessData.PROCESSED_SUCCESSFULLY);
    }

    public void setProcessedSuccessfully(boolean processedSuccessfully) {
        putItem(IProcessData.PROCESSED_SUCCESSFULLY, processedSuccessfully);
    }

    public Throwable getProcessingException() {
        return (Throwable) getItem(IProcessData.PROCESSING_EXCEPTION);
    }

    public void setProcessingException(Throwable processingException) {
        putItem(IProcessData.PROCESSING_EXCEPTION, processingException);
    }

    public Long getLong(String key) {
        return ItemDataConverter.getItemAsLong(key, getItem(key));
    }

    public Integer getInt(String key) {
        return ItemDataConverter.getItemAsInt(key, getItem(key));
    }

    public Float getFloat(String key) {
        return ItemDataConverter.getItemAsFloat(key, getItem(key));
    }

    public Double getDouble(String key) {
        return ItemDataConverter.getItemAsDouble(key, getItem(key));
    }

    public Boolean getBoolean(String key) {
        return ItemDataConverter.getItemAsBoolean(key, getItem(key));
    }

    public String getString(String key) {
        return ItemDataConverter.getItemAsString(key, getItem(key));
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        if (this.getObjectMap() != null) {
            return this.getObjectMap().entrySet();
        }
        return null;
    }

    public void setObjectMessage() {
        try {
            finishedObjectMsgConstruction();
        }
        catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        return "QueueMessage: " + getObjectMap() == null ? "null" : getObjectMap().toString();
    }
}
