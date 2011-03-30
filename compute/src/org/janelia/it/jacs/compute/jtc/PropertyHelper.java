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
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class PropertyHelper {
    private static Logger logger = Logger.getLogger(PropertyHelper.class);
    private static String DEFAULT_HOSTNAME = "UNKNOWN";
    private String hostName = DEFAULT_HOSTNAME;
    private static PropertyHelper instance = new PropertyHelper();
    Properties property = new Properties();
    private static boolean initialized = false;

    private PropertyHelper() {
    }

    public static PropertyHelper getInstance() {
        if (!initialized) {
            SystemConfigurationProperties systemConfigurationProperties = SystemConfigurationProperties.getInstance();
            instance.load(systemConfigurationProperties);
            initialized = true;
        }
        return instance;
    }

    public String getProperty(String propertyName, String defaultValue) {
        String propertyValue = property.getProperty(propertyName);
        if (propertyValue == null) {
            if (logger != null) logger.warn(propertyName + " not found.  Using Default: " + defaultValue);
            return defaultValue;
        }
        else {
            return propertyValue;
        }
    }

    public int getProperty(String propertyName, int defaultValue) {
        String propertyValue = property.getProperty(propertyName);
        if (propertyValue == null) {
            if (logger != null) logger.warn(propertyName + " not found.  Using Default: " + defaultValue);
            return defaultValue;
        }
        else {
            try {
                return Integer.parseInt(propertyValue);
            }
            catch (NumberFormatException ex) {
                if (logger != null) logger.warn(propertyName + " is not an Integer.  Using Default: " + defaultValue);
                return defaultValue;
            }

        }
    }

    public long getProperty(String propertyName, long defaultValue) {
        String propertyValue = property.getProperty(propertyName);
        if (propertyValue == null) {
            if (logger != null) logger.warn(propertyName + " not found.  Using Default: " + defaultValue);
            return defaultValue;
        }
        else {
            try {
                return Long.parseLong(propertyValue);
            }
            catch (NumberFormatException ex) {
                if (logger != null) logger.warn(propertyName + " is not a Long.  Using Default: " + defaultValue);
                return defaultValue;
            }

        }
    }

    public boolean getProperty(String propertyName, boolean defaultValue) {
        String propertyValue = property.getProperty(propertyName);
        if (propertyValue == null) {
            if (logger != null) logger.warn(propertyName + " not found.  Using Default: " + defaultValue);
            return defaultValue;
        }
        else {
            try {
                return Boolean.valueOf(propertyValue);
            }
            catch (Exception ex) {
                if (logger != null) logger.warn(propertyName + " is not a Booleanr.  Using Default: " + defaultValue);
                return defaultValue;
            }

        }
    }

    public File getProperty(String propertyName, File defaultValue) {
        String propertyValue = property.getProperty(propertyName);
        if (propertyValue == null) {
            if (logger != null) logger.warn(propertyName + " not found.  Using Default: " + defaultValue);
            return defaultValue;
        }
        else {
            try {
                return new File(propertyValue);
            }
            catch (Exception ex) {
                if (logger != null) logger.warn(propertyName + " is not a valid File.  Using Default: " + defaultValue);
                return defaultValue;
            }

        }
    }

    public void loadProperties(String propertyFileClasspathAddress) {
        loadProperties(propertyFileClasspathAddress, true);
    }

    public void loadProperties(String propertyFileClasspathAddress, boolean searchHostNameExtensions) {
        loadProperties(propertyFileClasspathAddress, searchHostNameExtensions, null);
    }

    /**
     * @param propertyFileClasspathAddress - In the form resource/JTrace if you want to look for
     *                                     JTrace.properties under the resource directory, relative to the classpath
     * @param searchHostNameExtensions     - if true will overlay property files based on the hostname.
     *                                     The behavior is such that it will try to load each file in progressive order to spell out
     *                                     the hostname.  For example, if the hostname is pdavies-w1, and propertyFileClasspathAddress
     *                                     is resource/JTrace, the files to be loaded are:
     *                                     resource/JTrace.properties
     *                                     resource/JTrace-p.properties
     *                                     resource/JTrace-pd.properties
     *                                     resource/JTrace-pda.properties
     *                                     etc.
     * @param overRideMap                  - a map whose key is a keyword substitution for final values as loaded.  For
     *                                     example, if the Map contains %SEQUENCER_MODEL% and 3730, if %SEQUENCER_MODEL% is found in
     *                                     a value for any of the properties, it will be replaced by 3730.
     */
    public void loadProperties(String propertyFileClasspathAddress, boolean searchHostNameExtensions,
                               Map overRideMap) {
        Properties p = new Properties();
        InputStream inStream = this.getClass().getClassLoader().getResourceAsStream(propertyFileClasspathAddress + ".properties");
        if (inStream != null) {
            if (logger != null) logger.info("Loading " + propertyFileClasspathAddress + ".properties");
            try {
                p.load(inStream);
            }
            catch (IOException ioEx) {
                if (logger != null)
                    logger.warn("Cannot load base property file: " + propertyFileClasspathAddress + ".properties");
            }
        }
        hostName = getHostName();
        if (searchHostNameExtensions && !hostName.equals(DEFAULT_HOSTNAME))
            loadHostnameProperties(propertyFileClasspathAddress, p, hostName, logger);
        List overRideKeyList = null;
        if (overRideMap != null) overRideKeyList = new ArrayList(overRideMap.keySet());
        Set propertyEntries = p.keySet();
        String key;
        String value;
        for (Object propertyEntry : propertyEntries) {
            key = (String) propertyEntry;
            value = p.getProperty(key);
            if (overRideMap != null) {
                for (Object anOverRideKeyList : overRideKeyList) {
                    value = value.replaceAll((String) anOverRideKeyList, (String) overRideMap.get(anOverRideKeyList));
                }
                p.put(key, value);
            }
            if (logger != null) logger.debug("Property: " + key + " : " + value);
        }
        load(p);
    }

    private String getHostName() {
        try {
            if (hostName.equals(DEFAULT_HOSTNAME))
                hostName = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException uhEx) {
            if (null != logger) logger.error("getHostName - UnknownHostException", uhEx);
        }
        return hostName;
    }

    public void load(Properties property) {
        Enumeration e = property.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            this.property.setProperty(key, property.getProperty(key));
        }
    }

    public static void loadHostnameProperties(String propertyFileClasspathAddress, Properties p, String hostName, Logger logger) {
        String partialName;
        String searchString;
        InputStream inStream;

        searchString = propertyFileClasspathAddress + ".properties";
        if (logger != null) logger.debug("Looking for " + searchString);
        inStream = PropertyHelper.class.getClassLoader().getResourceAsStream(searchString);
        if (inStream != null) {
            if (logger != null) logger.info("Loading " + searchString);
            try {
                p.load(inStream);
            }
            catch (IOException ioEx) {
                if (logger != null) logger.warn("Cannot load property file: " + searchString, ioEx);
            }
        }

        for (int i = 1; i < hostName.length() + 1; i++) {
            partialName = hostName.substring(0, i);
            searchString = propertyFileClasspathAddress + "-" + partialName + ".properties";
            if (logger != null) logger.debug("Looking for " + searchString);
            inStream = PropertyHelper.class.getClassLoader().getResourceAsStream(searchString);
            if (inStream != null) {
                if (logger != null) logger.info("Loading " + searchString);
                try {
                    p.load(inStream);
                }
                catch (IOException ioEx) {
                    if (logger != null) logger.warn("Cannot load property file: " + searchString, ioEx);
                }
            }
        }
    }

    public static Properties getHostnameProperties(String propertyFileClasspathAddress) throws Exception {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException uhEx) {
            if (null != logger) logger.error("getHostName - UnknownHostException", uhEx);
        }
        if (hostName == null) hostName = DEFAULT_HOSTNAME;

        return getHostnameProperties(propertyFileClasspathAddress, hostName);

        /*
        Properties p = new Properties();
        InputStream inStream = PropertyHelper.class.getClassLoader().getResourceAsStream(propertyFileClasspathAddress + ".properties");
        p.load(inStream);
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhEx) {
        }
        if (hostName==null) hostName = DEFAULT_HOSTNAME;
        loadHostnameProperties(propertyFileClasspathAddress, p, hostName, logger);
        return p;
        */
    }

    public static Properties getHostnameProperties(String propertyFileClasspathAddress, String hostName) throws Exception {
        // logger.info("Loading " + propertyFileClasspathAddress + ".properties");

        Properties p = new Properties();
        // InputStream inStream = PropertyHelper.class.getClassLoader().getResourceAsStream(propertyFileClasspathAddress + ".properties");
        // p.load(inStream);

        loadHostnameProperties(propertyFileClasspathAddress, p, hostName, logger);
        return p;
    }

    public Set<Object> keySet() {
        return property.keySet();
    }

}


