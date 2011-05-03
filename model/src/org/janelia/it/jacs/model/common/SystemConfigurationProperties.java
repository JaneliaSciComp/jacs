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

package org.janelia.it.jacs.model.common;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * This class encapsulates the properties in /WEB-INF/classes/jacs.properties.  Property
 * values can be obtained by calling SystemConfigurationProperties.getXXX(propName) where XXX can be String,
 * Boolean, Int, Long, Double, and Float currently.  We could add Date as well but then we'd
 * require a format as well.
 * <p/>
 * If a default value is supplied to getXXX methods, then the defaultValue is returned if the
 * property name is not found in property file.  The getXXX methods that don't take a default
 * value will throw an exception if the property does not exist.  These overloaded methods would
 * be called normally as one wouldn't ask for a property that doesn't exist.
 * <p/>
 * Note: For some odd reason, GWT requires a transient instance variable to be pointed
 * to SystemConfigurationProperties.getInstance() even when we declare <code>me</code> below as transient
 * Please refer to org.janelia.it.jacs.model.user_data.FileNode if you're having trouble calling
 * SystemConfigurationProperties.getXXX from GWT aware modules.
 *
 * @author smurphy
 * @author tnabeel
 */
public class SystemConfigurationProperties extends Properties implements Serializable,IsSerializable {
    private static Set<String> fileNames = new HashSet<String>();

    static {
        fileNames.add("jacs.properties");
        fileNames.add("dma.properties");
    }

    private SystemConfigurationProperties() {
    }

    private static SystemConfigurationProperties me;

    private static SystemConfigurationProperties load(boolean reload) {
        if (me == null || reload) {
            me = new SystemConfigurationProperties();
            // Needed to prevent getters in other threads from accessing properties while
            // properties are being loaded.  This is especially an issue during
            // property file reloads
            synchronized (me) {
                for (String fileName : fileNames) {
                    load(fileName, me);
                }
            }
        }
        return me;
    }

    public static SystemConfigurationProperties getInstance() {
        load(false);
        return me;
    }

    /**
     * Used by Spring to inject the property file name at startup
     *
     * @param filename - property file name to inject into Spring
     */
    public void setFileName(String filename) {
        fileNames.clear();
        fileNames.add(filename);
    }

    /**
     * Can be used to reload a property file at runtime
     */
    public static void reload() {
        load(true);
    }

    /**
     * Loads properties if they've haven't been loaded already
     */
    public static void load() {
        load(false);
    }

    /**
     * Used by the getXXX methods to retrieve the property value
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the value of the property if it exists or defaultValue if it doesn't
     */
    private static String getPropertyValue(String name, String defaultValue) {
        return load(false).getProperty(name, defaultValue);
    }

    /**
     * This method returns a property value if it exists and throws an exception
     * if it does not
     *
     * @param name - property key desired
     * @return the property value
     */
    private static String getNotNullPropValue(String name) {
        return ensureNotNull(name, getPropertyValue(name, null));
    }

    /**
     * Same as getNotNullPropValue(name) method
     *
     * @param name - property key desired
     * @return the property value
     */
    public static String getString(String name) {
        return getNotNullPropValue(name);
    }

    /**
     * Same as getPropertyValue(name) method
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value
     */
    public static String getString(String name, String defaultValue) {
        return getPropertyValue(name, defaultValue);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property value is not "true" or "false" then false is returned
     *
     * @param name - property key desired
     * @return the property value as a boolean.
     */
    public static boolean getBoolean(String name) {
        return getBooleanValue(getNotNullPropValue(name), false);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property value is not "true" or "false" then defaultValue is
     * returned
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as a boolean.
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        return getBooleanValue(getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property does not exist or if it's not an int, then an exception
     * is thrown.
     *
     * @param name - property key desired
     * @return the property value as an int.
     */
    public static int getInt(String name) {
        return getIntValue(name, getNotNullPropValue(name), -1);
    }

    /**
     * This method returns the property value as a primitive int value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an int, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an int.
     */
    public static int getInt(String name, int defaultValue) {
        return getIntValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive long value
     * If the property does not exist or if it's not an long,
     * then an exception is thrown.
     *
     * @param name - property key desired
     * @return the property value as an long.
     */
    public static long getLong(String name) {
        return getLongValue(name, getNotNullPropValue(name), -1);
    }

    /**
     * This method returns the property value as a primitive long value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a long, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an long.
     */
    public static long getLong(String name, long defaultValue) {
        return getLongValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive float value
     * If the property does not exist or if it's not an float,
     * then an exception is thrown.
     *
     * @param name - property key desired
     * @return the property value as an float.
     */
    public static float getFloat(String name) {
        return getFloatValue(name, getNotNullPropValue(name), -1.0f);
    }

    /**
     * This method returns the property value as a primitive float value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a float, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an float.
     */
    public static float getFloat(String name, float defaultValue) {
        return getFloatValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method returns the property value as a primitive double value
     * If the property does not exist or if it's not an double,
     * then an exception is thrown.
     *
     * @param name - property key desired
     * @return the property value as an double.
     */
    public static double getDouble(String name) {
        return getDoubleValue(name, getNotNullPropValue(name), -1.0);
    }

    /**
     * This method returns the property value as a primitive double value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an double, then an exception is thrown.
     *
     * @param name         - property key desired
     * @param defaultValue - value to use if not defined
     * @return the property value as an double.
     */
    public static double getDouble(String name, float defaultValue) {
        return getDoubleValue(name, getPropertyValue(name, null), defaultValue);
    }

    /**
     * This method is called by subclasses of to return file system path constructed
     * of 2 properties -
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an double.
     *
     * @param pathProperty     - name of path property
     * @param filenameProperty - name of filename property
     * @return constructed property value as String
     */
    public static String getFilePath(String pathProperty, String filenameProperty) {
        String path = getString(pathProperty);
        String filename = getString(filenameProperty);
        // concatinate
        if (path.endsWith(File.separator))
            return path + filename;
        else
            return path + File.separator + filename;
    }


    /**
     * This method is responsible for loading the property file represented by propertiesFileName
     * into properties passed in.
     *
     * @param propertiesFileName - property file to load
     * @param properties         - SystemConfigurationProperties object to load props into
     * @return the BaseProperties instance loaded with properties
     */
    protected static SystemConfigurationProperties load(String propertiesFileName, SystemConfigurationProperties properties) {
        InputStream in;
        ClassLoader cl = SystemConfigurationProperties.class.getClassLoader();
        if (cl == null) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(new File(propertiesFileName));
            }
            catch (Exception ex) {
                throw new RuntimeException("ClassLoader returned null and could not find file=" + propertiesFileName);
            }
            in = fis;
        }
        else {
            in = cl.getResourceAsStream(propertiesFileName);
        }
        if (in == null) {
            Logger.getLogger(SystemConfigurationProperties.class).warn("Could not read properties file: " + propertiesFileName);
        }
        try {
            properties.load(in);
            if (null != in) {
                in.close();
            }
        }
        catch (Exception e) {
            Logger.getLogger(SystemConfigurationProperties.class).warn("Could not read properties file: " + propertiesFileName);
        }
        return properties;
    }


    /**
     * This method is called by subclasses of BaseProperties to return defaultValue
     * if property value is null or the boolean representation of value
     *
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return true if value is "true", false otherwise
     */
    protected static boolean getBooleanValue(String value, boolean defaultValue) {
        return (value == null ? defaultValue : Boolean.valueOf(value));
    }

    /**
     * This method is called by subclasses of BaseProperties to return the int
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an int.
     *
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as int
     */
    protected static int getIntValue(String name, String value, int defaultValue) {
        try {
            return (value == null ? defaultValue : Integer.parseInt(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an int");
        }
    }

    /**
     * This method is called by subclasses of BaseProperties to return the long
     * representation of value.  It returns defaultValue if value is null.  It
     * throws an exception if value cannot be parsed to an long.
     *
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as long
     */
    protected static long getLongValue(String name, String value, long defaultValue) {
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
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as float
     */
    protected static float getFloatValue(String name, String value, float defaultValue) {
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
     * @param name         - property key desired
     * @param value        - value string to convert
     * @param defaultValue - value to use if not defined
     * @return property value as double
     */
    protected static double getDoubleValue(String name, String value, double defaultValue) {
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
     * @param name  - property key desired
     * @param value - value string to test null for
     * @return value - returns the value if not null or throws an exception
     */
    protected static String ensureNotNull(String name, String value) {
        if (value != null) {
            return value;
        }
        else {
            throw new IllegalArgumentException(name + " property not found");
        }
    }

}
