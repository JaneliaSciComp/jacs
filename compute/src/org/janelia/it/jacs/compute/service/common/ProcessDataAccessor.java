package org.janelia.it.jacs.compute.service.common;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.List;

/**
 * Utility to retrieve and store process data items with contextual logging.
 *
 * @author Eric Trautman
 */
public class ProcessDataAccessor {

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
                sb.append('\'');
                sb.append(value);
                sb.append('\'');
            } else {
                sb.append(value);
            }
            contextLogger.info(sb.toString());
        }
        return value;
    }

    public boolean getBooleanItem(String key) {
        boolean booleanValue = false;
        final Object value = getItem(key);
        if (value instanceof String) {
            booleanValue = Boolean.parseBoolean((String) value);
        } else if (value instanceof Boolean) {
            booleanValue = (Boolean) value;
        }
        return booleanValue;
    }

    public String getStringItem(String key) {
        return (String) getItem(key);
    }

    public Object getRequiredItem(String key) throws IllegalArgumentException {
        final Object value = getItem(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must be specified");
        }
        return value;
    }

    public String getRequiredStringItem(String key) throws IllegalArgumentException {
        final String value = getStringItem(key);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(key + " must be specified");
        }
        return value;
    }

    public List<String> getCsvStringItem(String key) {
        final String value = getStringItem(key);
        return Task.listOfStringsFromCsvString(value);
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
            } else {
                sb.append(value);
            }
            sb.append(" in ");
            sb.append(key);
            contextLogger.info(sb.toString());
        }

        processData.putItem(key, value);
    }

}
