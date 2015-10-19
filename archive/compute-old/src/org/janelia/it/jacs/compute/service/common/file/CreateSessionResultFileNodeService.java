
package org.janelia.it.jacs.compute.service.common.file;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.SessionFileNode;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 21, 2009
 * Time: 3:24:08 PM
 */
public class CreateSessionResultFileNodeService implements IService {

    private Task task;
    private ComputeDAO computeDAO;
    private SessionFileNode resultFileNode;
    Long resultNodeId;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO(logger);
            resultNodeId = createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNodeId);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private Long createResultFileNode() throws DaoException, IOException {
        // if we get this far then we assume that no result persist exists and create one
        String visibility = Node.VISIBILITY_PRIVATE;

        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner())) {
            visibility = Node.VISIBILITY_PUBLIC;
        }
        resultFileNode = new SessionFileNode(task.getOwner(), task,
                task.getJobName(),
                task.getJobName() + ": " + task.getObjectId(), visibility, null);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());

        FileWriter writer = new FileWriter(new File(resultFileNode.getDirectoryPath() + File.separator + "README.txt"));
        writer.write(resultFileNode.getName() + "\n");
        writer.write(resultFileNode.getDescription() + "\n");
        writer.flush();
        writer.close();
        return resultFileNode.getObjectId();
    }

}
