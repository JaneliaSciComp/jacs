
package org.janelia.it.jacs.compute.service.reversePsiBlast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 24, 2008
 * Time: 3:19:59 PM
 */
public class ReversePsiBlastCreateResultNodeService implements IService {
    private Task task;
    private ComputeDAO computeDAO;
    private String sessionName;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO(logger);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            ReversePsiBlastResultNode resultNode = createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultNode.getObjectId());
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_DIR, resultNode.getDirectoryPath());
            logger.debug("Created ReversePsiBlastResultNode and placed in processData id=" + resultNode.getObjectId());
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private ReversePsiBlastResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        if (task == null) throw new ServiceException("Task is null");
        // if we get this far then we assume that no result persist exists and create one
        ReversePsiBlastResultNode resultFileNode;
        Task loadedTask = computeDAO.getTaskById(task.getObjectId());
        // This code block assumes a previous run died unexpectedly.  In that case, we delete all the old stuff
        // Physically delete the files but not the directory
        Set<Node> outputNodes = loadedTask.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof ReversePsiBlastResultNode) {
                return (ReversePsiBlastResultNode) node;
            }
        }

        resultFileNode = new ReversePsiBlastResultNode(loadedTask.getOwner(), loadedTask, "ReversePsiBlastResultNode",
                "ReversePsiBlastResultNode for createtask " + loadedTask.getObjectId(), Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

}