package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Initializes any number of task variables. Inputs:
 *   TASK_PARAMETER_{X}
 *   TASK_PARAMETER_VALUE_{X}
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitTaskVariablesService implements IService {

    private ContextLogger contextLogger;
    
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            this.contextLogger = new ContextLogger(logger);
            contextLogger.appendToLogContext(task);
            
        	int num = 1;
        	while (true) {
        		String taskParamName = (String)processData.getItem("TASK_PARAMETER_"+num);	
        		if (taskParamName == null || num>100) break;
        		Object value = processData.getItem("TASK_PARAMETER_VALUE_"+num);	
        		String strValue = value==null?"":value.toString();
        		contextLogger.info("Setting task parameter '"+taskParamName+"' = "+strValue);
        		task.setParameter(taskParamName, strValue);
                num++;
        	}
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
