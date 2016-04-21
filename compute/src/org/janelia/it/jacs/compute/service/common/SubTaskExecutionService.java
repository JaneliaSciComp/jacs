package org.janelia.it.jacs.compute.service.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Creates and executes a sub-task with some parameters. Inputs:
 *   PROCESS_DEF_NAME - name of the process definition to execute the task with (also used as Task.taskName)
 *   DISPLAY_NAME - the display name of the task (Task.jobName)
 *   WAIT_FOR_COMPLETION - if true then execute synchronously (wait until the subtask completes). Default to false.
 *   
 *   There are three ways of getting parameters into the task:
 *   
 *   1) Numbered process variables to set task parameters
 *   PARAMETER_{X}_KEY - name of a task parameter
 *   PARAMETER_{X}_VALUE - value of the same task parameter 
 *   
 *   2) Map to set task parameters
 *   TASK_PARAMETER_MAP - map of task parameter names to values
 *   
 *   3) Map to set process data configuration
 *   PROCESS_DATA_MAP - map of process data variable names to values
 *   
 * Alternatively, the parameter keys and values can be specified with TASK_PARAMETER_MAP
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubTaskExecutionService implements IService {
	
    private Logger logger;
    private ContextLogger contextLogger;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
            Task task = ProcessDataHelper.getTask(processData);
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();

        	this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.contextLogger = new ContextLogger(this.logger);
            this.contextLogger.appendToLogContext(task);
            
            boolean waitForCompletion = false;
            String waitForCompletionStr = (String)processData.getItem("WAIT_FOR_COMPLETION");
        	if (waitForCompletionStr != null) {
        		waitForCompletion = Boolean.valueOf(waitForCompletionStr);
        	}

            boolean throwExceptionOnError = false;
            String throwExceptionOnErrorStr = (String)processData.getItem("EXCEPTION_ON_ERROR");
            if (throwExceptionOnErrorStr != null) {
                throwExceptionOnError = Boolean.valueOf(throwExceptionOnErrorStr);
            }
        	
        	String processDefName = (String)processData.getItem("PROCESS_DEF_NAME");
        	if (processDefName == null) {
        		throw new IllegalArgumentException("PROCESS_DEF_NAME may not be null");
        	}

            String displayName = (String)processData.getItem("DISPLAY_NAME");
            if (displayName==null) {
                displayName = processDefName+" Task";
            }
            
            GenericTask subtask = new GenericTask(new HashSet<Node>(), task.getOwner(), new ArrayList<Event>(), new HashSet<TaskParameter>(), processDefName, displayName);
        	
        	int num = 1;
        	while (true) {
        		String key = processData.getString("PARAMETER_"+num+"_KEY");	
        		if (key == null || num>100) break;
        		Object value = processData.getItem("PARAMETER_"+num+"_VALUE");
                setTaskParameter(subtask, key, value);
                num++;
        	}

            Map<String,String> taskParameterMap = (Map)processData.getItem("TASK_PARAMETER_MAP");
            if (taskParameterMap!=null) {
                for(Map.Entry<String, String> entry : taskParameterMap.entrySet()) {
                    Object value = getValue(entry.getValue(), processData);
                    setTaskParameter(subtask, entry.getKey(), value);
                }
            }
            
            subtask.setParentTaskId(task.getObjectId());
            computeBean.saveOrUpdateTask(subtask);
            
            Map<String, Object> processConfiguration = new HashMap<>();
            processConfiguration.put(ProcessDataConstants.PROCESS_ID, subtask.getObjectId());
            
            Map<String,String> processDataMap = (Map)processData.getItem("PROCESS_DATA_MAP");
            if (processDataMap!=null) {
                for(Map.Entry<String, String> entry : processDataMap.entrySet()) {
                    Object value = getValue(entry.getValue(), processData);
                    setProcessDataParameter(processConfiguration, entry.getKey(), value);
                }
            }

            contextLogger.info("Launching "+subtask.getJobName()+", task id="+task.getObjectId()+", subtask id="+subtask.getObjectId());
            computeBean.submitJob(processDefName, processConfiguration);
            
            if (waitForCompletion) {
            	contextLogger.info("Waiting for completion of subtask "+processDefName+" (id="+subtask.getObjectId()+")");
            	boolean complete = false;
	            while (!complete) {
	                
                    String[] statusTypeAndValue = EJBFactory.getLocalComputeBean().getTaskStatus(subtask.getObjectId());
                    String statusType = statusTypeAndValue[ComputeDAO.STATUS_TYPE];
                    
                    if (statusType!=null && Task.isDone(statusType)) {
                        if (throwExceptionOnError && statusType.equals(Event.ERROR_EVENT)) {
                            throw new ServiceException("Sub task "+subtask.getObjectId()+" ended in error state");
                        }
                    	complete = true;
                    }
                    else {
                    	Thread.sleep(5000);
                    }
	            }
            }
            
            contextLogger.info("Putting "+subtask.getObjectId()+" in SUBTASK_ID");
            processData.putItem("SUBTASK_ID", subtask.getObjectId().toString());
        } 
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private Object getValue(Object value, IProcessData processData) {
        Object actualValue = value;
        if (value instanceof String) {
            String valueStr = value.toString();
            if (valueStr.startsWith("$V{")) {
                String key2 = valueStr.substring(valueStr.indexOf("$V{") + 3, valueStr.length() - 1);
                return processData.getItem(key2);
            }
            else {
                return valueStr;
            }
        }
        return actualValue;
    }
    
    private String getStringValue(Object value) {
        if (value instanceof List) {
            return Task.csvStringFromCollection((List<?>)value);
        }
        else {
            return value.toString();
        }
    }
    
    private void setTaskParameter(GenericTask subtask, String key, Object value) {
        if (value==null) return;
        String strValue = getStringValue(value);
        String printValue = strValue;
        if (printValue.length()>1000) {
            printValue = printValue.substring(0, 1000)+"...";
        }
        contextLogger.info("Setting subtask parameter "+key+" = '"+printValue+"'");
        subtask.setParameter(key, strValue);
    }
    
    private void setProcessDataParameter(Map<String, Object> processConfiguration, String key, Object value) {
        if (value==null) return;
        String printValue = getStringValue(value);
        if (printValue.length()>1000) {
            printValue = printValue.substring(0, 1000)+"...";
        }
        contextLogger.info("Setting process data parameter "+key+" = '"+printValue+"'");
        processConfiguration.put(key, value);
    }
}
