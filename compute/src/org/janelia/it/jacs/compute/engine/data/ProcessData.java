package org.janelia.it.jacs.compute.engine.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.def.ActionDef;


/**
 * The class encapsulates the running state of process
 */
@XmlRootElement
@XmlType(name = "ProcessData")
public class ProcessData implements IProcessData {

    private static Logger logger = Logger.getLogger(ProcessData.class);

    @XmlElement
    private Map<String, Object> processObjects = new TreeMap<>();

    public void putItem(String key, Object value) {
        processObjects.put(key, value);
    }
    
    public Object getLiteralItem(String key) {
        return processObjects.get(key);
    }
    
    public Object getItem(String key) {
        Object value = getLiteralItem(key);
        if (value instanceof String) {
            String valueStr = (String) value;
            // If the value is wrapped in $V{} then we're interested in the value's value in processData
            if (valueStr.startsWith("$V{")) {
            	String key2 = valueStr.substring(valueStr.indexOf("$V{") + 3, valueStr.length() - 1);
            	if (key.equals(key2)) {
            		logger.error("ProcessData variable refers to itself: "+key);
            		return null;
            	}
                return getItem(key2);
            }
            else {
                return valueStr;
            }
        }
        else {
            return value;
        }
    }

    public Object getMandatoryItem(String key) throws MissingDataException {
        Object item = getItem(key);
        if (item == null) {
            throw new MissingDataException("Missing mandatory data item: " + key);
        }
        return item;
    }

    public void removeItem(String key) {
        processObjects.remove(key);
    }

    public Long getProcessId() throws MissingDataException {
        return (Long) getMandatoryItem(IProcessData.PROCESS_ID);
    }

    public void setProcessId(Long id) {
        putItem(IProcessData.PROCESS_ID, id);
    }

    public List getProcessIds() {
        Object obj = getItem(IProcessData.PROCESS_ID);
        if (obj instanceof List) {
            return (List) obj;
        }
        else {
            return null;
        }
    }

    public void setProcessIds(List<Long> ids) {
        putItem(IProcessData.PROCESS_ID, ids);
    }

    public String getProcessDefName() throws MissingDataException {
        return (String) getMandatoryItem(IProcessData.PROCESS_DEFINITION);
    }

    public void setProcessDefName(String processDefName) {
        putItem(IProcessData.PROCESS_DEFINITION, processDefName);
    }

    public ActionDef getProcessedAction() throws MissingDataException {
        return (ActionDef) getMandatoryItem(IProcessData.PROCESSED_ACTION);
    }

    public void setProcessedAction(ActionDef actionDef) {
        putItem(IProcessData.PROCESSED_ACTION, actionDef);
    }

    public ActionDef getActionToProcess() throws MissingDataException {
        return (ActionDef) getMandatoryItem(IProcessData.ACTION_TO_PROCESS);
    }

    public void setActionToProcess(ActionDef actionDef) {
        putItem(IProcessData.ACTION_TO_PROCESS, actionDef);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return processObjects.entrySet();
    }

    public void clear() {
        processObjects.clear();
    }

    public Long getLong(String key) {
        return ItemDataConverter.getItemAsLong(key, getItem(key));
    }

    public Integer getInt(String key) {
        return ItemDataConverter.getItemAsInt(key, getItem(key));
    }

    public Float getFloat(String key) {
        return ItemDataConverter.getItemAsFloat(key, getItem(key));
    }

    public Double getDouble(String key) {
        return ItemDataConverter.getItemAsDouble(key, getItem(key));
    }

    public Boolean getBoolean(String key) {
        return ItemDataConverter.getItemAsBoolean(key, getItem(key));
    }

    public String getString(String key) {
        return ItemDataConverter.getItemAsString(key, getItem(key));
    }

    public void copyFrom(Map<String, Object> processConfiguration) {
        if (processConfiguration != null) {
            processObjects.putAll(processConfiguration);
        }
    }

    public Logger getLogger() {
        return (Logger) processObjects.get("LOGGER");
    }

    public String toString() {
        return "ProcessData: " + processObjects.toString();
    }
}
