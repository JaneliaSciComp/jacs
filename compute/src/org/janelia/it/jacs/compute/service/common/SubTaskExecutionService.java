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
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Creates and executes a sub-task with some parameters. Inputs:
 *   PROCESS_DEF_NAME - name of the process definition to execute the task with (also used as Task.taskName)
 *   DISPLAY_NAME - the display name of the task (Task.jobName)
 *   PARAMETER_{X}_KEY - name of a task parameter
 *   PARAMETER_{X}_VALUE - value of the same task parameter 
 *   WAIT_FOR_COMPLETION - if true then execute synchronously (wait until the subtask completes). Default to false.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubTaskExecutionService implements IService {
	
    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
        	Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();

            boolean waitForCompletion = false;
            String waitForCompletionStr = (String)processData.getItem("WAIT_FOR_COMPLETION");
        	if (waitForCompletionStr != null) {
        		waitForCompletion = Boolean.valueOf(waitForCompletionStr);
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
        		String strValue = value==null?"":value.toString();
                logger.info("Setting subtask parameter "+key+" = '"+strValue+"'");
        		subtask.setParameter(key, strValue);
                num++;
        	}
        	
            subtask.setParentTaskId(task.getObjectId());
            computeBean.saveOrUpdateTask(subtask);
            
            logger.info("Launching "+subtask.getJobName()+", task id="+task.getObjectId()+", subtask id="+subtask.getObjectId());
            computeBean.submitJob(processDefName, subtask.getObjectId());
            
            if (waitForCompletion) {
                logger.info("Waiting for completion of subtask "+processDefName+" (id="+subtask.getObjectId()+")");
            	boolean complete = false;
	            while (!complete) {
                    String[] statusTypeAndValue = EJBFactory.getLocalComputeBean().getTaskStatus(subtask.getObjectId());
                    if (statusTypeAndValue[0]!=null && Task.isDone(statusTypeAndValue[0])) {
                    	complete = true;
                    }
                    else {
                    	Thread.sleep(5000);
                    }
	            }
            }
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
