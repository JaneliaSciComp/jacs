
package org.janelia.it.jacs.compute.api;

import javax.ejb.Local;

import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.model.graph.entity.EntityNode;

/**
 * A local CRUD interface for manipulating the graph.
 * 
 * @see EntityBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface GraphBeanLocal extends GraphBeanRemote {
   
    public void loadRelationships(EntityNode entityNode, boolean recurse) throws ComputeException;
    public int bulkUpdateAttributeValue(String oldValue, String newValue) throws ComputeException;
    public int bulkUpdateAttributePrefix(String oldPrefix, String newPrefix) throws ComputeException;
    public EntityNode annexEntityNodeTree(Access access, Long objGuid) throws ComputeException;
    
}
