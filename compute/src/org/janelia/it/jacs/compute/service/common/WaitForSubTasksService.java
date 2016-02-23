package org.janelia.it.jacs.compute.service.common;

import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Waits for all subtask of the current task.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WaitForSubTasksService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {

            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();

            logger.info("Waiting for completion of all subtasks of task "+task.getObjectId());
            
            boolean complete = false;
            while (!complete) {
                
                int numActiveSubtasks = 0;
                List<Task> childTasks = computeBean.getChildTasksByParentTaskId(task.getObjectId());
                for(Task subtask : childTasks) {
                    if (!subtask.isDone()) {
                        numActiveSubtasks++;
                    }
                }
            
                if (numActiveSubtasks==0) {
                    logger.info("All subtasks have completed for parent task "+task.getObjectId());
                    complete = true;
                }
                else {
                    logger.info("Waiting on "+numActiveSubtasks+" subtasks for parent task "+task.getObjectId());
                    Thread.sleep(5000);
                }
            }

        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
