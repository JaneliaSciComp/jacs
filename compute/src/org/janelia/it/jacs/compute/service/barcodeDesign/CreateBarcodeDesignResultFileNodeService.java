
package org.janelia.it.jacs.compute.service.barcodeDesign;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.barcodeDesign.BarcodeDesignResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;

/**
 * @author Todd Safford
 */
public class CreateBarcodeDesignResultFileNodeService implements IService {

    private Task task;
    private BarcodeDesignResultNode resultFileNode;
    private String sessionName;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            Long resultNodeId = createResultFileNode();
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
        resultFileNode = new BarcodeDesignResultNode(task.getOwner(), task,
                "BarcodeDesignResultNode",
                "BarcodeDesignResultNode for createtask " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode.getObjectId();
    }

}