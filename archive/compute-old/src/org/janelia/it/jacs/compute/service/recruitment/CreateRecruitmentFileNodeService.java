
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerRecruitmentTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;
import java.util.Set;

/**
 * @author Todd Safford
 */
public class CreateRecruitmentFileNodeService implements IService {

    private Task task;
    private ComputeDAO computeDAO;
    private String sessionName;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO(logger);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            Long recruitmentNodeId = createFileNode();
            processData.putItem(ProcessDataConstants.RECRUITMENT_FILE_NODE_ID, recruitmentNodeId);
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }


    private Long createFileNode() throws DaoException, IOException {
        // if we get this far then we assume that no result persist exists and create one
        RecruitmentFileNode recruitmentFileNode = null;
        Task loadedTask = computeDAO.getTaskById(task.getObjectId());
        // This code block assumes a previous run died unexpectedly.  In that case, we delete all the old stuff
        // Physically delete the files but not the directory
        Set<Node> outputNodes = loadedTask.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof RecruitmentFileNode) {
                recruitmentFileNode = (RecruitmentFileNode) node;
                break;
            }
        }
        if (null == recruitmentFileNode) {
            String visibility = Node.VISIBILITY_PRIVATE;
            if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner())) {
                visibility = Node.VISIBILITY_PUBLIC;
            }
            recruitmentFileNode = new RecruitmentFileNode(loadedTask.getOwner(), loadedTask, "RecruitmentFileNode " +
                    task.getParameter(RecruitmentViewerRecruitmentTask.GENBANK_FILE_NAME),
                    "RecruitmentFileNode for createtask " + loadedTask.getObjectId(), visibility, sessionName);
            computeDAO.saveOrUpdate(recruitmentFileNode);
        }

        FileUtil.ensureDirExists(recruitmentFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(recruitmentFileNode.getDirectoryPath());
        return recruitmentFileNode.getObjectId();
    }

}