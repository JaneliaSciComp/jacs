package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Delete a file node.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DeleteFileNodeService implements IService {

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            String username = task.getOwner();
            
            Long fileNodeId = (Long)processData.getItem("FILE_NODE_ID");
            
            if (fileNodeId==null) {
            	throw new IllegalArgumentException("FILE_NODE_ID may not be null");
            }
            
            if (EJBFactory.getLocalComputeBean().deleteNode(username, fileNodeId, true)) {
            	logger.info("Deleted result node: "+fileNodeId);
            }
            else {
            	logger.error("Problem deleting result node: "+fileNodeId);
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}