package org.janelia.it.jacs.compute.service.domain.node;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Trash a file node.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TrashFileNodeService extends AbstractDomainService {

    public void execute() throws Exception {
        Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        Task task = ProcessDataHelper.getTask(processData);
        String username = task.getOwner();
        
        Long fileNodeId = (Long)processData.getItem("FILE_NODE_ID");
        
        if (fileNodeId==null) {
        	throw new IllegalArgumentException("FILE_NODE_ID may not be null");
        }
        
        if (EJBFactory.getLocalComputeBean().trashNode(username, fileNodeId, true)) {
        	logger.info("Removed result node: "+fileNodeId);
        }
        else {
        	logger.error("Problem removing result node: "+fileNodeId);
        }
    }
}