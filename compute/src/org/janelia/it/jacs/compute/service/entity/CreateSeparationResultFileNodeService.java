package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.SampleResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Node that contains all output from pipeline operations run on a particular sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateSeparationResultFileNodeService implements IService {

    protected Logger logger;
    private Task task;
    private SampleResultNode resultFileNode;
    private String sessionName;
    private String visibility;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);

            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            
            createResultFileNode();
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
            processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());

            processData.putItem("SEPARATION_RESULT_FILE_NODE", resultFileNode);
            processData.putItem("SEPARATE_RESULT_FILE_NODE", createChildFileNode("separate"));
            processData.putItem("COMPRESS_RESULT_FILE_NODE", createChildFileNode("compress"));
            processData.putItem("MIPS_RESULT_FILE_NODE", createChildFileNode("mips"));
            processData.putItem("CONVERT_RESULT_FILE_NODE", createChildFileNode("convert"));
            
            logger.info("Created sample result node: "+resultFileNode.getDirectoryPath());
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
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