package org.janelia.it.jacs.compute.service.maskSearch;

import org.apache.log4j.Logger;
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
import org.janelia.it.jacs.model.user_data.maskSearch.MaskSearchResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;

/**
 * Node that contains all output from merging two neurons.
 * 
 * @author <a href="mailto:saffordt@janelia.hhmi.org">Todd Safford</a>
 */
public class CreateMaskSearchResultFileNodeService implements IService {

    protected Logger logger;
    private Task task;
    private MaskSearchResultNode resultFileNode;
    private String visibility;
    private String sessionName;

    public void execute(IProcessData processData) throws CreateFileNodeException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);

            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;

            createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());

            logger.info("Created mask search result node: "+resultFileNode.getDirectoryPath());
        }
        catch (Exception e) {
            throw new CreateFileNodeException(e);
        }
    }

    private void createResultFileNode() throws DaoException, IOException {
    	resultFileNode = new MaskSearchResultNode(task.getOwner(), task, "MaskSearchResultNode",
                "MaskSearchResultNode for task " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);
        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
    }

}