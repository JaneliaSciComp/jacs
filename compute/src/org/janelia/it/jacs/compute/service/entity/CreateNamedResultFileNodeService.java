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
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;

/**
 * Dynamically create a file node with some name.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateNamedResultFileNodeService implements IService {

    protected Logger logger;
    private Task task;
    private NamedFileNode resultFileNode;
    private String visibility;

    public void execute(IProcessData processData) throws CreateFileNodeException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);

            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            
            String name = (String)processData.getItem("NAME");
            
            createResultFileNode(name);
            
            String outputVar = (String)processData.getItem("OUTPUT_VAR_NAME");
            
            if (outputVar!=null) {
            	processData.putItem(outputVar, resultFileNode);
            	processData.putItem(outputVar+"_ID", resultFileNode.getObjectId());
            }
            else {
            	processData.putItem(ProcessDataConstants.RESULT_FILE_NODE, resultFileNode);
            	processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_ID, resultFileNode.getObjectId());
                processData.putItem(ProcessDataConstants.RESULT_FILE_NODE_DIR, resultFileNode.getDirectoryPath());
            }
            
            logger.info("Created named result node: "+resultFileNode.getDirectoryPath());
        }
        catch (Exception e) {
            throw new CreateFileNodeException(e);
        }
    }

    private void createResultFileNode(String name) throws DaoException, IOException {
    	resultFileNode = new NamedFileNode(task.getOwner(), task, name, 
                "NamedResultNode for task " + task.getObjectId(), visibility, null);
    	
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);
        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
    }
}