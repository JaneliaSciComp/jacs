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

package org.janelia.it.jacs.compute.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 26, 2007
 * Time: 10:36:00 AM
 */
public class TaskServiceProperties extends Properties {

    public TaskServiceProperties(InputStream propertiesFileStream) {
        if (propertiesFileStream == null) {
            throw new RuntimeException("InputStream to blast properties cannot be null");
        }
        try {
            load(propertiesFileStream);
        }
        catch (IOException e) {
            throw new RuntimeException("Exception reading input stream to blast properties", e);
        }
    }

    /**
     * Used by the getXXX methods to retrieve the property value
     *
     * @param name
     * @param defaultValue
     * @return the value of the property if it exists or defaultValue if it doesn't
     */
    private String getPropertyValue(String name, String defaultValue) {
        return getProperty(name, defaultValue);
    }

    /**
     * This method returns a property value if it exists and throws an exception
     * if it does not
     *
     * @param name
     * @return the property value
     */
    private String getNotNullPropValue(String name) {
        return ensureNotNull(name, getPropertyValue(name, null));
    }

    /**
     * Same as getNotNullPropValue(name) method
     *
     * @param name
     * @return the property value
     */
    public String getString(String name) {
        return getNotNullPropValue(name);
    }

    /**
     * Same as getPropertyValue(name) method
     *
     * @param name
     * @param defaultValue
     * @return the property value
     */
    public String getString(String name, String defaultValue) {
        return getPropertyValue(name, defaultValue);
    }

    /**
     * This method is called by subclasses of BaseProperties to return defaultValue
     * if property value is null or the boolean representation of value
     *
     * @param value
     * @param defaultValue
     * @return true if value is "true", false otherwise
     */
    protected boolean getBooleanValue(String value, boolean defaultValue) {
        return (value == null ? defaultValue : Boolean.valueOf(value));
    }

    /**
     * This method is called by subclasses of BaseProperties to return the int
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an int.
     *
     * @param name
     * @param value
     * @param defaultValue
     * @return property value as int
     */
    protected int getIntValue(String name, String value, int defaultValue) {
        try {
            return (value == null ? defaultValue : Integer.parseInt(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an int");
        }
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property value is not "true" or "false" then false is returned
     *
     * @param name
     * @return the property value as a boolean.
     */
    public boolean getBoolean(String name) {
        return getBooleanValue(getNotNullPropValue(name), false);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property value is not "true" or "false" then defaultValue is
     * returned
     *
     * @param name
     * @return the property value as a boolean.
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        return getBooleanValue(getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property does not exist or if it's not an int, then an exception
     * is thrown.
     *
     * @param name
     * @return the property value as an int.
     */
    public int getInt(String name) {
        return getIntValue(name, getNotNullPropValue(name), -1);
    }

    /**
     * This method returns the property value as a primitive int value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an int, then an exception is thrown.
     *
     * @param name
     * @return the property value as an int.
     */
    public int getInt(String name, int defaultValue) {
        return getIntValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive long value
     * If the property does not exist or if it's not an long,
     * then an exception is thrown.
     *
     * @param name
     * @return the property value as an long.
     */
    public long getLong(String name) {
        return getLongValue(name, getNotNullPropValue(name), -1);
    }

    /**
     * This method returns the property value as a primitive long value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a long, then an exception is thrown.
     *
     * @param name
     * @return the property value as an long.
     */
    public long getLong(String name, long defaultValue) {
        return getLongValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive float value
     * If the property does not exist or if it's not an float,
     * then an exception is thrown.
     *
     * @param name
     * @return the property value as an float.
     */
    public float getFloat(String name) {
        return getFloatValue(name, getNotNullPropValue(name), -1.0f);
    }

    /**
     * This method returns the property value as a primitive float value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a float, then an exception is thrown.
     *
     * @param name
     * @return the property value as an float.
     */
    public float getFloat(String name, float defaultValue) {
        return getFloatValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive double value
     * If the property does not exist or if it's not an double,
     * then an exception is thrown.
     *
     * @param name
     * @return the property value as an double.
     */
    public double getDouble(String name) {
        return getDoubleValue(name, getNotNullPropValue(name), -1.0);
    }

    /**
     * This method returns the property value as a primitive double value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an double, then an exception is thrown.
     *
     * @param name
     * @return the property value as an double.
     */
    public double getDouble(String name, double defaultValue) {
        return getDoubleValue(name, getPropertyValue(name, null), defaultValue);
    }


    /**
     * This method is called by subclasses of BaseProperties to return the long
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an long.
     *
     * @param name
     * @param value
     * @param defaultValue
     * @return property value as long
     */
    protected long getLongValue(String name, String value, long defaultValue) {
        try {
            return (value == null ? defaultValue : Long.parseLong(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an long");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to return the float
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an float.
     *
     * @param name
     * @param value
     * @param defaultValue
     * @return property value as float
     */
    protected float getFloatValue(String name, String value, float defaultValue) {
        try {
            return (value == null ? defaultValue : Float.parseFloat(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an float");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to return the double
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an double.
     *
     * @param name
     * @param value
     * @param defaultValue
     * @return property value as double
     */
    protected double getDoubleValue(String name, String value, double defaultValue) {
        try {
            return (value == null ? defaultValue : Double.parseDouble(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an float");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to ensure that a property
     * exists in the property file.  If it doesn't, an exception is thrown.
     *
     * @param name
     * @param value
     * @return value
     */
    protected String ensureNotNull(String name, String value) {
        if (value != null) {
            return value;
        }
        else {
            throw new IllegalArgumentException(name + " property not found");
        }
    }


}
