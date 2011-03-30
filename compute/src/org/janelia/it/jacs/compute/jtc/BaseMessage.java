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

package org.janelia.it.jacs.compute.jtc;

import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BaseMessage {
    static final String SRC_HOST_NAME = "SrcHostname";
    static final String SRC_USER_NAME = "SrcUsername";
    static final String MESSAGE_CREATION_TIME = "CreationTime";
    static final String VERSION = "Version";

    private static final Logger logger = Logger.getLogger(BaseMessage.class);
    static final String EXPECTED_VERSION = "1";
    private static final String MINOR_VERSION = "0";
    private MapMessage mapMsg;
    private ObjectMessage objectMsg;
    private HashMap objectMap;

    protected BaseMessage(ObjectMessage msg, boolean construct) throws JMSException {
        if (msg != null) {
            this.objectMsg = msg;
        }
        else {
            throw new IllegalArgumentException("msg passed to BaseMessage is null!");
        }
        if (construct) {
            objectMap = new HashMap();
            setConstructionParameters();
            // objectMsg.setObject(objectMap);
        }
        else {
            objectMap = (HashMap) objectMsg.getObject();
        }
    }

    protected BaseMessage(MapMessage msg, boolean construct) throws JMSException {
        if (msg != null) {
            this.mapMsg = msg;
        }
        else {
            throw new IllegalArgumentException("msg passed to BaseMessage is null!");
        }
        if (construct) {
            setConstructionParameters();
        }
    }

    public boolean isRedelivered() {
        try {
            return getMessage().getJMSRedelivered();
        }
        catch (JMSException ex) {
            return false;
        }
    }

    /**
     * Checks for message validity.  Override in sub-classes, but call this method
     *
     * @return validity of this message
     */
    protected boolean isValid() {
        return (getMessage() != null && getVersion() != null && isCompatibleVersion() &&
                getSrcHostName() != null && getSrcUsername() != null &&
                getMessageCreationTime() != null);
    }

    public boolean isCompatibleVersion() {
        String version = getVersion();
        return version != null && version.startsWith(EXPECTED_VERSION);
    }

    public String getSrcHostName() {
        return getStringProperty(SRC_HOST_NAME);
    }

    public String getSrcUsername() {
        return getStringProperty(SRC_USER_NAME);
    }

    public Date getMessageCreationTime() {
        Long time = getLongProperty(MESSAGE_CREATION_TIME);
        if (time == null) return null;
        return new Date(time);
    }

    /**
     * @return null if Version cannot be found or JMSException
     */
    public String getVersion() {
        return getStringProperty(VERSION);
    }

    /**
     * @return original or constructed message
     */
    public Message getMessage() {
        if (mapMsg != null) return mapMsg;
        return objectMsg;
    }

    protected String getStringProperty(String property) {
        String obj = null;
        try {
            if (mapMsg != null) obj = mapMsg.getString(property);
            else {
                if (objectMap != null) {
                    obj = (String) objectMap.get(property);
                }
            }
        }
        catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
            return null;
        }
        return obj;
    }

    protected Long getLongProperty(String property) {
        Long obj = null;
        try {
            if (mapMsg != null) {
                obj = mapMsg.getLong(property);
            }
            else {
                if (objectMap != null) {
                    obj = (Long) objectMap.get(property);
                }
            }
        }
        catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
            return null;
        }
        return obj;
    }

    protected Integer getIntProperty(String property) {
        Integer obj = null;
        try {
            if (mapMsg != null) {
                obj = mapMsg.getInt(property);
            }
            else {
                if (objectMap != null) {
                    obj = (Integer) objectMap.get(property);
                }
            }
        }
        catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
            return null;
        }
        return obj;
    }

    protected Double getDoubleProperty(String property) {
        Double obj = null;
        try {
            if (mapMsg != null) {
                obj = mapMsg.getDouble(property);
            }
            else {
                if (objectMap != null) {
                    obj = (Double) objectMap.get(property);
                }
            }
        }
        catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
            return null;
        }
        return obj;
    }

    protected Float getFloatProperty(String property) {
        Float obj = null;
        try {
            if (mapMsg != null) {
                obj = mapMsg.getFloat(property);
            }
            else {
                if (objectMap != null) {
                    obj = (Float) objectMap.get(property);
                }
            }
        }
        catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
            return null;
        }
        return obj;
    }

    protected Boolean getBooleanProperty(String property) {
        Boolean obj = null;
        try {
            if (mapMsg != null) {
                obj = mapMsg.getBoolean(property);
            }
            else {
                if (objectMap != null) {
                    obj = (Boolean) objectMap.get(property);
                }
            }
        }
        catch (Throwable ex) {
            logger.error("Exception thrown while trying to get " + property + " property from message");
            return null;
        }
        return obj;
    }

    protected Object getObjectProperty(String property) {
        Object obj = null;
        try {
            if (mapMsg != null) {
                obj = mapMsg.getObject(property);
            }
            else {
                if (objectMap != null) {
                    obj = objectMap.get(property);
                }
            }
        }
        catch (Throwable ex) {
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
        }
        catch (Throwable ex) {
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

    protected void setBooleanProperty(String property, Boolean value) {
        try {
            if (mapMsg != null) mapMsg.setBoolean(property, value);
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
        }
        catch (UnknownHostException ex) {
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

}
