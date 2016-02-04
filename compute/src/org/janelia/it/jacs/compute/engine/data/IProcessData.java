
package org.janelia.it.jacs.compute.engine.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.engine.def.ActionDef;

/**
 * This interface is implemented by ProcessData and QueueMessage
 *
 * @author Tareq Nabeel
 */
public interface IProcessData {

    public List getProcessIds();

    public void setProcessIds(List<Long> ids);

    public Long getProcessId() throws MissingDataException;

    public void setProcessId(Long id);

    public Object getMandatoryItem(String key) throws MissingDataException;

    public Object getLiteralItem(String key);
    
    public Object getItem(String key);

    public void putItem(String key, Object value);

    public void removeItem(String key);

    public Long getLong(String property);

    public Integer getInt(String property);

    public Float getFloat(String property);

    public Double getDouble(String property);

    public String getString(String property);

    public Boolean getBoolean(String property);

    public String getProcessDefName() throws MissingDataException;

    public void setProcessDefName(String processDefName);

    public ActionDef getProcessedAction() throws MissingDataException;

    public void setProcessedAction(ActionDef actionDef);

    public ActionDef getActionToProcess() throws MissingDataException;

    public void setActionToProcess(ActionDef actionDef);

    public Set<Map.Entry<String, Object>> entrySet();

    public static final String PROCESS_ID = "PROCESS_ID";
    public static final String TASK = "TASK";
    public static final String USER_NAME = "USER_NAME";
    public static final String JOB_NAME = "JOB_NAME";
    public static final String PROCESSED_SUCCESSFULLY = "PROCESSED_SUCCESSFULLY";
    public static final String ACTION_TO_PROCESS = "ACTION_TO_PROCESS";   // FOR JMS QUEUE
    public static final String PROCESSED_ACTION = "PROCESSED_ACTION";   //JMS REPLY
    public static final String PROCESS_DEFINITION = "PROCESS_DEFINITION";
    public static final String ORIGINAL_MESSAGE_ID = "ORIGINAL_MESSAGE_ID";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String PROCESSING_EXCEPTION = "PROCESSING_EXCEPTION";
    public static final String QUEUE_OVERRIDE = "QUEUE_OVERRIDE";


}
