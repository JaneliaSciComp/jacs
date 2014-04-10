package org.janelia.it.jacs.compute.service.common;

import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Utility to retrieve and store process data items with contextual logging.
 *
 * @author Eric Trautman
 */
public class ProcessDataAccessor {

    private static final int MAX_STRING_LENGTH = 1000;
    
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
            if (value instanceof String) {
                String v = (String)value;
                if (v.length()>MAX_STRING_LENGTH) {
                    v = v.substring(0, MAX_STRING_LENGTH)+"...";
                }
                sb.append('\'');
                sb.append(v);
                sb.append('\'');
            } else {
                sb.append(value);
            }
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

    public Boolean getRequiredItemAsBoolean(String key) {
        final Boolean value = getItemAsBoolean(key);
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
            if (value instanceof String) {
                sb.append('\'');
                sb.append(value);
                sb.append('\'');
            }
            else if (Collection.class.isAssignableFrom(value.getClass())) {
                sb.append(((Collection)value).size());
                sb.append(" items");
            }
            else {
                sb.append(value);
            }
            sb.append(" in ");
            sb.append(key);
            contextLogger.info(sb.toString());
        }

        processData.putItem(key, value);
    }

}
