package org.janelia.it.jacs.compute.service.entity;

import org.janelia.it.jacs.compute.api.EJBFactory;

/**
 * Trash a file node.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TrashFileNodeService extends AbstractEntityService {

    public void execute() throws Exception {
        
        Long fileNodeId = (Long)processData.getItem("FILE_NODE_ID");
        
        if (fileNodeId==null) {
        	throw new IllegalArgumentException("FILE_NODE_ID may not be null");
        }
        
        if (EJBFactory.getLocalComputeBean().trashNode(task.getOwner(), fileNodeId, true)) {
        	contextLogger.info("Removed result node: "+fileNodeId);
        }
        else {
        	contextLogger.error("Problem removing result node: "+fileNodeId);
        }
    }
}