/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.compute.engine.data;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.def.ActionDef;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.jtc.BaseMessage;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class encapsulates messages sent to and received from queues.  It's based on
 * earlier work done by Sean Murphy on TaskMessage
 *
 * @author Tareq Nabeel
 */
public class QueueMessage extends BaseMessage implements IProcessData {

    private static final Logger logger = Logger.getLogger(QueueMessage.class);
    
    public QueueMessage(ObjectMessage msg, boolean construct) throws JMSException {
        super(msg, construct);
        try {
            // ObjectMessage will not have an id if it's a newly created JMS Message
            // However, if it's a JMS Message received in an onMessage method of an MDB
            // then it will be captured and that's important
            setMessageId(msg.getJMSMessageID());
        }
        catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void putItem(String key, Object obj) {
        setObjectProperty(key, obj);
    }

    public Object getItem(String key) {
        Object value = getObjectProperty(key);
        if (value instanceof String) {
            String valueStr = (String) value;
            // If the value is wrapped in $V{} then we're interested in the value's value in processData
            if (valueStr.startsWith("$V{")) {
                return getItem(valueStr.substring(valueStr.indexOf("$V{") + 3, valueStr.length() - 1));
            }
            else {
                return valueStr;
            }
        }
        else {
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

    public List<IProcessData> createForEachPDs(String forEachPDParam) {
        // not implemented
        return null;
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

    public ProcessDef getProcessDef() throws MissingDataException {
        return (ProcessDef) getMandatoryItem(IProcessData.PROCESS_DEFINITION);
    }

    public void setProcessDef(ProcessDef processDef) {
        putItem(IProcessData.PROCESS_DEFINITION, processDef);
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
    	try {
    		return (Long)getItem(key);	
    	}
    	catch (Throwable t) {
            logger.error("Exception thrown while trying to get " + key + " property from message");
            return null;
    	}
    }

    public Double getDouble(String key) {
    	try {
    		return (Double)getItem(key);	
    	}
    	catch (Throwable t) {
            logger.error("Exception thrown while trying to get " + key + " property from message");
            return null;
    	}
    }

    public Float getFloat(String key) {
    	try {
    		return (Float)getItem(key);	
    	}
    	catch (Throwable t) {
            logger.error("Exception thrown while trying to get " + key + " property from message");
            return null;
    	}
    }

    public Integer getInt(String key) {
    	try {
    		return (Integer)getItem(key);	
    	}
    	catch (Throwable t) {
            logger.error("Exception thrown while trying to get " + key + " property from message");
            return null;
    	}
    }

    public Boolean getBoolean(String key) {
    	try {
    		return (Boolean)getItem(key);	
    	}
    	catch (Throwable t) {
            logger.error("Exception thrown while trying to get " + key + " property from message");
            return null;
    	}
    }

    public String getString(String key) {
    	try {
    		return (String)getItem(key);	
    	}
    	catch (Throwable t) {
            logger.error("Exception thrown while trying to get " + key + " property from message");
            return null;
    	}
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
