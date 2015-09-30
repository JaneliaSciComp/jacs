package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.exceptions.CreateFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.SeparationResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Node that contains all output from pipeline operations run on a particular sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateSeparationResultFileNodeService implements IService {

    protected Logger logger;
    private Task task;
    private SeparationResultNode resultFileNode;
    private String visibility;

    public void execute(IProcessData processData) throws CreateFileNodeException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);

            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            
            createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());

            processData.putItem("SEPARATION_RESULT_FILE_NODE", resultFileNode);
            processData.putItem("SEPARATE_RESULT_FILE_NODE", createChildFileNode("separate"));
            
            logger.info("Created separation result node: "+resultFileNode.getDirectoryPath());
        }
        catch (Exception e) {
            throw new CreateFileNodeException(e);
        }
    }

    private void createResultFileNode() throws DaoException, IOException {
    	resultFileNode = new SeparationResultNode(task.getOwner(), task, "SeparationResultNode", 
                "SeparationResultNode for task " + task.getObjectId(), visibility, null);
    	
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);
        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
    }

    private FileNode createChildFileNode(String name) throws DaoException, IOException {
        FileNode fileNode = new NamedFileNode(task.getOwner(), task, name,
                "Child node '"+name+"' for task " + task.getObjectId(), visibility, null);
        fileNode.setPathOverride(resultFileNode.getDirectoryPath()+File.separator+name);
        
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(fileNode);
        FileUtil.ensureDirExists(fileNode.getDirectoryPath());
        FileUtil.cleanDirectory(fileNode.getDirectoryPath());
        
        return fileNode;
    }
}