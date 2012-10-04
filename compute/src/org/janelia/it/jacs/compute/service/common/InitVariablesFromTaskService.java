package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Initializes any number of process variables from corresponding task parameters. Inputs:
 *   TASK_PARAMETER_{X}
 *   PROCESS_VARIABLE_{X}
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitVariablesFromTaskService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            
        	int num = 1;
        	while (true) {
        		String taskParamName = (String)processData.getItem("TASK_PARAMETER_"+num);	
        		if (taskParamName == null || num>100) break;
        		String processVarName = (String)processData.getItem("PROCESS_VARIABLE_"+num);	
        		String value = task.getParameter(taskParamName);
        		if (value != null) {
        			// We specifically avoid overriding existing values in ProcessData, because if the process file
        			// is being <include>'d, then the task may not contain the parameter which is already in 
        			// ProcessData.
	            	logger.info("Putting value '"+value+"' in "+processVarName);
            		processData.putItem(processVarName, value);
            	}
                num++;
        	}
        	processData.putItem("TASK_OWNER", task.getOwner());
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
