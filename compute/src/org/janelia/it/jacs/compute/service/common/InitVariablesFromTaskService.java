package org.janelia.it.jacs.compute.service.common;

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
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesFromTaskService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            final Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            final Task task = ProcessDataHelper.getTask(processData);
            final ContextLogger contextLogger = new ContextLogger(logger);
            contextLogger.appendToLogContext(task);
            final ProcessDataAccessor data = new ProcessDataAccessor(processData, contextLogger);
            
            boolean override = true;
            String overrideStr = data.getItemAsString("OVERRIDE");
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
                String value = task.getParameter(taskParamName);
                contextLogger.debug("Init "+processVarName+" with value '"+value+"' from task parameter "+taskParamName);
                if (value != null) {
                    // We specifically avoid overriding existing values in ProcessData, because if the process file
                    // is being <include>'d, then the task may not contain the parameter which is already in
                    // ProcessData.
                    if (override || processData.getItem(processVarName)==null) {
                        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                            data.putItem(processVarName, Boolean.valueOf(value));
                        } else {
                            data.putItem(processVarName, value);
                        }
                    }
                    else {
                        contextLogger.debug("  Will not override existing value '"+processData.getItem(processVarName)+"'");
                    }
                }
            }

            data.putItem("TASK_OWNER", task.getOwner());
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
