package org.janelia.it.jacs.compute.service.common;

import java.util.Map;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Initializes any number of process variables from corresponding task parameters. Inputs:
 *   TASK_PARAMETER_{X}
 *   PROCESS_VARIABLE_{X}
 *   OVERRIDE=true|false
 *   
 * Alternatively, the parameter and variable names can be specified with TASK_PARAMETER_MAP
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesFromTaskService implements IService {

    private IProcessData processData;
    private ContextLogger contextLogger;
    private Task task;
    private boolean override;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.processData = processData;
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            this.contextLogger = new ContextLogger(logger);
            contextLogger.appendToLogContext(task);
            
            boolean override = true;
            String overrideStr = (String)processData.getItem("OVERRIDE");
            if (!StringUtils.isEmpty(overrideStr)) {
                override = Boolean.parseBoolean(overrideStr);
            }
            contextLogger.debug("Will " + (override ? "" : "not ") + "override existing variables");

            for (int i = 1; i < 100; i++) {
                String taskParamName = (String) processData.getItem("TASK_PARAMETER_" + i);
                if (taskParamName == null) {
                    break;
                }
                String processVarName = (String) processData.getItem("PROCESS_VARIABLE_" + i);
                processEntry(taskParamName, processVarName);
            }

            Map<String,String> processVarMap = (Map)processData.getItem("TASK_PARAMETER_MAP");
            if (processVarMap!=null) {
                for(Map.Entry<String, String> entry : processVarMap.entrySet()) {
                    processEntry(entry.getKey(), entry.getValue());
                }
            }
            
            processData.putItem("TASK_OWNER", task.getOwner());
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private void processEntry(String taskParamName, String processVarName) throws Exception {
        String value = task.getParameter(taskParamName);
        contextLogger.debug("Init "+processVarName+" with value '"+value+"' from task parameter "+taskParamName);
        if (value != null) {
            // We specifically avoid overriding existing values in ProcessData, because if the process file
            // is being <include>'d, then the task may not contain the parameter which is already in
            // ProcessData.
            if (override || processData.getItem(processVarName)==null) {
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    processData.putItem(processVarName, Boolean.valueOf(value));
                } else {
                    processData.putItem(processVarName, value);
                }
            }
            else {
                contextLogger.debug("  Will not override existing value '"+processData.getItem(processVarName)+"'");
            }
        }
    }
}
