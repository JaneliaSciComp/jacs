
package org.janelia.it.jacs.web.gwt.common.client.util;

import com.google.gwt.i18n.client.Dictionary;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * @author: lkagan
 * <p/>
 * This is a client side wrapper for SystemConfigurationProperties
 */
public class SystemProps {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.util.SystemProps");
    private static Dictionary propDictionary;

    static {
        // load properties from JS array
        try {
            propDictionary = Dictionary.getDictionary("_system_properties");
            _logger.debug("Found and loaded _system_properties dictionary");
        }
        catch (Exception e) { // Throws exception if no dictionary found
            _logger.error("_system_properties NOT loaded: " + e.getMessage());
            propDictionary = null;
        }
    }


    /**
     * Used by the getXXX methods to retrieve the property value
     *
     * @param name
     * @param defaultValue
     * @return the value of the property if it exists or defaultValue if it doesn't
     */
    private static String getPropertyValue(String name, String defaultValue) {
        String val = defaultValue;
        // code below is based on synchorously retrieved Dictionary
        if (propDictionary != null) {
            val = propDictionary.get(name);
            val = (StringUtils.hasValue(val) ? val : defaultValue);
        }
        _logger.debug("Retrieved property on request: Key='" + name + "'; Value='" + val + "'; Default='" + defaultValue + "'");
        return val;
    }

    /**
     * Same as getPropertyValue(name) method
     *
     * @param name
     * @param defaultValue
     * @return the property value
     */
    public static String getString(String name, String defaultValue) {
        return getPropertyValue(name, defaultValue);
    }

    /**
     * This method returns the property value as a primitive boolean value
     * If the property value is not "true" or "false" then defaultValue is
     * returned
     *
     * @param name
     * @return the property value as a boolean.
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        String val = getPropertyValue(name, String.valueOf(defaultValue));
        return (val == null ? defaultValue : Boolean.valueOf(val));
    }

    /**
     * This method returns the property value as a primitive int value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an int, then an exception is thrown.
     *
     * @param name
     * @return the property value as an int.
     */
    public static int getInt(String name, int defaultValue) {
        String value = getPropertyValue(name, String.valueOf(defaultValue));
        try {
            return (value == null ? defaultValue : Integer.parseInt(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an int");
        }
    }

    /**
     * This method returns the property value as a primitive long value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a long, then an exception is thrown.
     *
     * @param name
     * @return the property value as an long.
     */
    public static long getLong(String name, long defaultValue) {
        String value = getPropertyValue(name, String.valueOf(defaultValue));
        try {
            return (value == null ? defaultValue : Long.parseLong(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an long");
        }
    }

    /**
     * This method returns the property value as a primitive float value
     * If the property does not exist then defaultValue is returned.
     * If the property is not a float, then an exception is thrown.
     *
     * @param name
     * @return the property value as an float.
     */
    public static float getFloat(String name, float defaultValue) {
        String value = getPropertyValue(name, String.valueOf(defaultValue));
        try {
            return (value == null ? defaultValue : Float.parseFloat(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an float");
        }
    }

    /**
     * This method returns the property value as a primitive double value
     * If the property does not exist then defaultValue is returned.
     * If the property is not an double, then an exception is thrown.
     *
     * @param name
     * @return the property value as an double.
     */
    public static double getDouble(String name, float defaultValue) {
        String value = getPropertyValue(name, String.valueOf(defaultValue));
        try {
            return (value == null ? defaultValue : Double.parseDouble(value));
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(name + " property is not an float");
        }
    }

}
