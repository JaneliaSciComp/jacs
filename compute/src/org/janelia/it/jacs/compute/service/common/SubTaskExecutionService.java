package org.janelia.it.jacs.compute.service.common;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Creates and executes a sub-task with some parameters. Inputs:
 *   PROCESS_DEF_NAME - name of the process definition to execute the task with
 *   TASK_CLASS - fully qualified class name of the task to create
 *   PARAMETER_{X}_KEY - name of a task parameter
 *   PARAMETER_{X}_VALUE - value of the same task parameter 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubTaskExecutionService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
        	Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
            User user = computeBean.getUserByName(task.getOwner());

        	String processDefName = (String)processData.getItem("PROCESS_DEF_NAME");
        	if (processDefName == null) {
        		throw new IllegalArgumentException("PROCESS_DEF_NAME may not be null");
        	}
        	
        	String taskClassName = (String)processData.getItem("TASK_CLASS");
        	if (taskClassName == null) {
        		throw new IllegalArgumentException("TASK_CLASS may not be null");
        	}
            
        	Task subtask = null;
        	try {
            	subtask = (Task)Class.forName(taskClassName).newInstance();
            	if (subtask == null) throw new Exception("Null task");
            	subtask.setInputNodes(new HashSet<Node>());
            	subtask.setOwner(user.getUserLogin());
            	subtask.setEvents(new ArrayList<Event>());
            	subtask.setTaskParameterSet(new HashSet<TaskParameter>());
        	}
        	catch (Exception e) {
        		throw new ServiceException("Could not instantiate task class "+taskClassName,e);
        	}
        	
        	int num = 1;
        	while (true) {
        		String key = processData.getString("PARAMETER_"+num+"_KEY");	
        		if (key == null || num>100) break;
        		Object value = processData.getItem("PARAMETER_"+num+"_VALUE");
        		String strValue = value==null?"":value.toString();
                logger.info("Setting subtask parameter "+key+" = '"+strValue+"'");
        		subtask.setParameter(key, strValue);
                num++;
        	}
        	
            subtask.setParentTaskId(task.getObjectId());
            computeBean.saveOrUpdateTask(subtask);
        	
            subtask.setJobName("Subtask "+processDefName+" for "+task.getJobName());
            
            logger.info("Launching "+subtask.getJobName()+", task id = "+task.getObjectId()+" subtask id="+subtask.getObjectId());
            computeBean.submitJob(processDefName, subtask.getObjectId());
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
