package org.janelia.it.jacs.compute.service.vaa3d;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.neuron.NeuronMergeResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Node that contains all output from merging two neurons.
 * 
 * @author <a href="mailto:saffordt@janelia.hhmi.org">Todd Safford</a>
 */
public class CreateNeuronMergeResultFileNodeService implements IService {

    protected Logger logger;
    private Task task;
    private NeuronMergeResultNode resultFileNode;
    private String visibility;
    private File separationDir;
    private String sessionName;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);

            visibility = User.SYSTEM_USER_KEY.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;

            // Find the parent location
            Entity separationEntity = EJBFactory.getLocalEntityBean().getEntityById(task.getParameter(NeuronMergeTask.PARAM_separationEntityId));
            if (null!=separationEntity && null!=separationEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)) {
                String tmpPath = separationEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH).getValue();
                separationDir = new File(tmpPath).getParentFile();
            }
            createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());

            logger.info("Created separation result node: "+resultFileNode.getDirectoryPath());
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private void createResultFileNode() throws DaoException, IOException {
    	resultFileNode = new NeuronMergeResultNode(task.getOwner(), task, "NeuronMergeResultNode",
                "NeuronMergeResultNode for task " + task.getObjectId(), visibility, sessionName);
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);
        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
    }

}