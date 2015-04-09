package org.janelia.it.jacs.compute.service.common;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * Utility to retrieve and store process data items with contextual logging.
 *
 * @author Eric Trautman
 */
public class ProcessDataAccessor {

    private static final int MAX_STRING_LENGTH = 1000;
    private static final int MAX_COLLECTION_SIZE = 5;

    private ContextLogger contextLogger;
    private IProcessData processData;

    public ProcessDataAccessor(IProcessData processData,
                               ContextLogger contextLogger) {
        this.processData = processData;
        this.contextLogger = contextLogger;
    }

    public Object getItem(String key) {
        final Object value = processData.getItem(key);
        if (contextLogger.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder(256);
            sb.append("Retrieved ");
            sb.append(key);
            sb.append(" value of ");
            appendValueToStringBuilder(value, sb);
            contextLogger.info(sb.toString());
        }
        return value;
    }

    public boolean getItemAsBoolean(String key) {
        boolean booleanValue = false;
        final Object value = getItem(key);
        if (value instanceof String) {
            booleanValue = Boolean.parseBoolean((String) value);
        } else if (value instanceof Boolean) {
            booleanValue = (Boolean) value;
        }
        return booleanValue;
    }

    public String getItemAsString(String key) {
        return (String) getItem(key);
    }

    public Long getItemAsLong(String key) throws NumberFormatException {
        Long longValue = null;
        Object value = getItem(key);
        if (value instanceof Long) {
            longValue = (Long) value;
        } else if (value instanceof String) {
            longValue = Long.parseLong((String) value);
        } else if (value instanceof Number) {
            longValue = ((Number) value).longValue();
        } else if (value != null) {
            throw new NumberFormatException("cannot convert " + value.getClass().getName() +
                                            " value " + value + " to a Long");
        }
        return longValue;
    }

    public List<String> getItemAsCsvStringList(String key) {
        final String value = getItemAsString(key);
        return Task.listOfStringsFromCsvString(value);
    }

    public List<String> getRequiredItemAsCsvStringList(String key) {
        final String value = getRequiredItemAsString(key);
        return Task.listOfStringsFromCsvString(value);
    }
    
    public Object getRequiredItem(String key) throws IllegalArgumentException {
        final Object value = getItem(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must be specified");
        }
        return value;
    }

    public String getRequiredItemAsString(String key) throws IllegalArgumentException {
        final String value = getItemAsString(key);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(key + " must be specified");
        }
        return value;
    }

    public Long getRequiredItemAsLong(String key) throws NumberFormatException {
        final Long value = getItemAsLong(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must be specified");
        }
        return value;
    }

    public void putItem(String key,
                        Object value) {

        if (contextLogger.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder(256);
            sb.append("Putting ");
            appendValueToStringBuilder(value, sb);
            sb.append(" in ");
            sb.append(key);
            contextLogger.info(sb.toString());
        }

        processData.putItem(key, value);
    }

    private void appendValueToStringBuilder(Object value,
                                            StringBuilder sb) {

        if (value instanceof String) {

            sb.append('\'');

            String v = (String) value;
            if (v.length() > MAX_STRING_LENGTH) {
                sb.append(v.substring(0, MAX_STRING_LENGTH));
                sb.append("...");
            } else {
                sb.append(v);
            }

            sb.append('\'');

        } else if (value == null) {

            sb.append("null");

        } else {

            final Class clazz = value.getClass();
            if ((clazz != null) && Collection.class.isAssignableFrom(clazz)) {
                final int size = ((Collection) value).size();
                if (size > MAX_COLLECTION_SIZE) {
                    sb.append(size);
                    sb.append(" items");
                } else {
                    sb.append(value);
                }
            } else {
                sb.append(value);
            }

        }
    }
}
