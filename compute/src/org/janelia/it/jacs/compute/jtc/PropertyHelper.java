package org.janelia.it.jacs.compute.jtc;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

public class PropertyHelper {
    private static Logger logger = Logger.getLogger(PropertyHelper.class);
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

    public void load(Properties property) {
        Enumeration e = property.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            this.property.setProperty(key, property.getProperty(key));
        }
    }

    public Set<Object> keySet() {
        return property.keySet();
    }

}


