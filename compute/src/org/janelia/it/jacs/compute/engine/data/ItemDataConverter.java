package org.janelia.it.jacs.compute.engine.data;

/**
 * Created by goinac on 1/15/16.
 */
class ItemDataConverter {
    static Long getItemAsLong(String key, Object itemValue) {
        if (itemValue == null) {
            return null;
        } else if (itemValue instanceof Long) {
            return (Long) itemValue;
        } else if (itemValue instanceof String) {
            try {
                return Long.parseLong((String) itemValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " property is not a long");
            }
        } else {
            throw new IllegalArgumentException(key + " property is not a long");
        }
    }

    static Integer getItemAsInt(String key, Object itemValue) {
        if (itemValue == null) {
            return null;
        } else if (itemValue instanceof Integer) {
            return (Integer) itemValue;
        } else if (itemValue instanceof String) {
            try {
                return Integer.parseInt((String) itemValue);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " property is not an int");
            }
        } else {
            throw new IllegalArgumentException(key + " property is not an int");
        }
    }

    static Float getItemAsFloat(String key, Object itemValue) {
        if (itemValue == null) {
            return null;
        } else if (itemValue instanceof Float) {
            return (Float) itemValue;
        } else if (itemValue instanceof String) {
            try {
                return Float.parseFloat((String) itemValue);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " property is not a float");
            }
        } else {
            throw new IllegalArgumentException(key + " property is not a float");
        }
    }

    static Double getItemAsDouble(String key, Object itemValue) {
        if (itemValue == null) {
            return null;
        } else if (itemValue instanceof Double) {
            return (Double) itemValue;
        } else if (itemValue instanceof String) {
            try {
                return Double.parseDouble((String) itemValue);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " property is not a double");
            }
        }
        else {
            throw new IllegalArgumentException(key + " property is not a double");
        }
    }

    static Boolean getItemAsBoolean(String key, Object itemValue) {
        if (itemValue == null) {
            return false;
        } else if (itemValue instanceof Boolean) {
            return (Boolean) itemValue;
        } else if (itemValue instanceof String) {
            return Boolean.valueOf((String) itemValue);
        } else {
            throw new IllegalArgumentException(key + " property is not a boolean");
        }
    }

    static String getItemAsString(String key, Object itemValue) {
        if (itemValue == null) {
            return null;
        } else if (itemValue instanceof String) {
            return (String) itemValue;
        } else {
            throw new IllegalArgumentException(key + " property value is not a String");
        }
    }

}
