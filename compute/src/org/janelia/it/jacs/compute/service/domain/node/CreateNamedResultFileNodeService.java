package org.janelia.it.jacs.compute.service.domain.node;

import java.io.IOException;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Dynamically create a file node with some name.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateNamedResultFileNodeService extends AbstractDomainService {

    private NamedFileNode resultFileNode;
    private String visibility;

    public void execute() throws Exception {
    	
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
        
        contextLogger.info("Created named result node: "+resultFileNode.getDirectoryPath());
    }

    private void createResultFileNode(String name) throws DaoException, IOException {
    	resultFileNode = new NamedFileNode(task.getOwner(), task, name, 
                "NamedResultNode for task " + task.getObjectId(), visibility, null);
    	
        EJBFactory.getLocalComputeBean().saveOrUpdateNode(resultFileNode);
        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
    }
}