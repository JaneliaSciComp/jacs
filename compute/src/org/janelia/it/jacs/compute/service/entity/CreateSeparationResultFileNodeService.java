package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.IOException;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.SeparationResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Node that contains all output from pipeline operations run on a particular sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateSeparationResultFileNodeService extends AbstractEntityService {

    private SeparationResultNode resultFileNode;
    private String visibility;

    public void execute() throws Exception {

        visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
        
        createResultFileNode();
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());

        processData.putItem("SEPARATION_RESULT_FILE_NODE", resultFileNode);
        processData.putItem("SEPARATE_RESULT_FILE_NODE", createChildFileNode("separate"));
        
        contextLogger.info("Created separation result node: "+resultFileNode.getDirectoryPath());
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