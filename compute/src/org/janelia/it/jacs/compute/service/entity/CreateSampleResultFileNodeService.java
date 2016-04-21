package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.IOException;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.SampleResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Node that contains processing operations (e.g. merging and stitching) run on a particular sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateSampleResultFileNodeService extends AbstractEntityService {

    private SampleResultNode resultFileNode;
    private String sessionName;
    private String visibility;

    public void execute() throws Exception {
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);

        visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
        
        createResultFileNode();
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
        processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());

        processData.putItem("SAMPLE_RESULT_FILE_NODE", resultFileNode);
        processData.putItem("MERGE_RESULT_FILE_NODE", createChildFileNode("merge"));
        processData.putItem("GROUP_RESULT_FILE_NODE", createChildFileNode("group"));
        processData.putItem("STITCH_RESULT_FILE_NODE", createChildFileNode("stitch"));
        processData.putItem("MIPS_RESULT_FILE_NODE", createChildFileNode("mips"));
        
        contextLogger.info("Created sample result node: "+resultFileNode.getDirectoryPath());
    }

    private void createResultFileNode() throws DaoException, IOException {
    	resultFileNode = new SampleResultNode(task.getOwner(), task, "SampleResultNode", 
                "SampleResultNode for task " + task.getObjectId(), visibility, sessionName);
    	
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