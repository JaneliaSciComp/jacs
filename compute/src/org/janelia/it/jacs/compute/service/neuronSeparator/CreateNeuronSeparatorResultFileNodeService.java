package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.exceptions.CreateFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;

/**
 * @author Todd Safford
 */
public class CreateNeuronSeparatorResultFileNodeService implements IService {

    private Task task;
    private NeuronSeparatorResultNode resultFileNode;
    private String sessionName;

    public void execute(IProcessData processData) throws CreateFileNodeException {
        try {
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());
        }
        catch (Exception e) {
            throw new CreateFileNodeException(e);
        }
    }

    private void createResultFileNode() throws DaoException, IOException {
        // if we get this far then we assume that no result persist exists and create one
        String visibility = Node.VISIBILITY_PRIVATE;

        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner())) {
            visibility = Node.VISIBILITY_PUBLIC;
        }
        resultFileNode = new NeuronSeparatorResultNode(task.getOwner(), task,
                "NeuronSeparatorResultNode",
                "NeuronSeparatorResultNode for createtask " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
    }

}