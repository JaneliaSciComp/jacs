
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectRecruitmentSamplingTask;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 24, 2010
 * Time: 11:43:16 AM
 * I need this class to study the list of recruitments and update the UI as to the status of the jobs
 */
public class ChildProcessMonitor implements IService {
    @Override
    public void execute(IProcessData processData) throws ServiceException {
        Logger _logger=null;
        try {
            _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            GenomeProjectRecruitmentSamplingTask task = (GenomeProjectRecruitmentSamplingTask) ProcessDataHelper.getTask(processData);
//            EJBFactory.getRemoteComputeBean().saveEvent(task.getObjectId(), "Full Recruitment Running", "Full Recruitment Running", new Date());
            RecruitmentFileNode resultNode = (RecruitmentFileNode) EJBFactory.getLocalComputeBean().getNodeById(processData.getLong(ProcessDataConstants.RECRUITMENT_FILE_NODE_ID));
            // Read in the jobs running
            HashMap<String, String> moleculeToTaskMap = new HashMap<String, String>();
            File recruitingFile = new File(resultNode.getFilePathByTag(RecruitmentFileNode.TAG_RECRUITMENT_TASKS));
            Scanner scanner = new Scanner(recruitingFile);
            while (scanner.hasNextLine()){
                String[] values = scanner.nextLine().split("\t");
                moleculeToTaskMap.put(values[0], values[1]);
            }
            int totalRecruitmentTasks = moleculeToTaskMap.keySet().size();
            int numberRecruitmentTasksCompleted = 0;
            boolean subTasksCompleted = false;
            while (!subTasksCompleted) {
                HashMap<String, String> childTaskStatusMap = EJBFactory.getLocalComputeBean().getChildTaskStatusMap(task.getObjectId());
                for (String childTaskId : childTaskStatusMap.keySet()) {
                    String status = childTaskStatusMap.get(childTaskId);
                    if (moleculeToTaskMap.values().contains(childTaskId) && Task.isDone(status)){
                        numberRecruitmentTasksCompleted++;
                    }
                }
                if (totalRecruitmentTasks==numberRecruitmentTasksCompleted) { subTasksCompleted=true; }
                if (!subTasksCompleted) {
                    _logger.debug("Recruitment task "+task.getObjectId()+" has "+numberRecruitmentTasksCompleted+" / "+totalRecruitmentTasks+" completed.  Waiting.");
                    EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(), numberRecruitmentTasksCompleted+"/"+totalRecruitmentTasks+" completed.");
                    childTaskStatusMap.clear();
                    numberRecruitmentTasksCompleted=0;
                    try {
                        Thread.sleep(300000);
                    }
                    catch (InterruptedException e) {
                        // Ignore the interruption
                    }
                }
            }
            processData.putItem("RESULT_DIR", resultNode.getDirectoryPath());
            _logger.debug("Recruitment task "+task.getObjectId()+" is complete.");
        }
        catch (Exception e) {
            if (null!=_logger) {
                _logger.error("Unable to update the status of the recruitment subtasks.");
            }
            throw new ServiceException("There was a failure monitoring the recruitment jobs.", e);
        }
    }
}
