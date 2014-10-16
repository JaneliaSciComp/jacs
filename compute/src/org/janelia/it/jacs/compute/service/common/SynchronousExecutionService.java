package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.def.DefLoader;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.engine.launcher.ProcessLauncher;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;

/**
 * Creates and executes a dynamic process synchronously, with some parameters. Inputs:
 *   PROCESS_DEF_NAME - name of the process definition to execute the task with (also used as Task.taskName)
 *   ADD_TASK_NAME - value to add to the current task's name
 *   PARAMETER_{X}_VALUE - value of the same task parameter 
 *   
 * Invoking process files from within process files:
 * 1) to execute a dynamic process sychronously, use this service
 * 2) to execute a dynamic process asychronously, use the SubTaskExecutionService
 * 3) to execute a static process sychronously, use the <include> tag
 * 4) to execute a static process asychronously, use the SubTaskExecutionService
 * 
 * Note: a similar effect to this service can be achieved by calling the SubTaskExecutionService
 * with WAIT_FOR_COMPLETION=true. However, this means that the process file will be queued in 
 * an MDB Launcher, and will spawn a new Task, which is usually not desired if the process is to be 
 * synchronous. This service was created to address that issue. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SynchronousExecutionService implements IService {
	
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
            
        	String processDefName = (String)processData.getItem("PROCESS_DEF_NAME");
        	if (processDefName == null) {
        		throw new IllegalArgumentException("PROCESS_DEF_NAME may not be null");
        	}
        	
        	String addTaskName = (String)processData.getItem("ADD_TASK_NAME");
        	if (addTaskName != null) {
        		// Rename the task to include a list of processes that were executed
                Task task = ProcessDataHelper.getTask(processData);
                if (task instanceof GenericTask) {
                	logger.trace("Adding '"+addTaskName+"' to existing task name");
                	GenericTask genericTask = (GenericTask)task;
                	genericTask.addProcessToTaskName(addTaskName);
                	logger.trace("New task name: "+genericTask.getTaskName());
                	// Also save the new name to the database. This can't be done with the object above 
                	// because its already associated with another Hibernate session
                	GenericTask genericTask2 = (GenericTask)computeBean.getTaskById(task.getObjectId());
                	genericTask2.addProcessToTaskName(addTaskName);
                	computeBean.saveOrUpdateTask(genericTask2);
                }
                else {
                	logger.warn("ADD_TASK_NAME specified as "+addTaskName
                			+" but the task is not a GenericTask, so the task name cannot be added.");
                }
        	}
        	
        	DefLoader loader = new DefLoader();
        	ProcessDef processDef = loader.loadProcessDef(processDefName);
        	ProcessLauncher launcher = new ProcessLauncher();
            logger.info("Launching "+processDefName+" synchronously");
        	launcher.launch(processDef, processData);
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
