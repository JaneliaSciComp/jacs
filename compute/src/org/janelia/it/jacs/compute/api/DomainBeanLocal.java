
package org.janelia.it.jacs.compute.api;

import javax.ejb.Local;

import org.janelia.it.jacs.compute.api.support.Access;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;

/**
 * A local CRUD interface for manipulating Domain Objects. 
 * 
 * @see EntityBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface DomainBeanLocal extends DomainBeanRemote {
   
    public void loadRelationships(DomainObject domainObject, boolean recurse) throws ComputeException;
    public int bulkUpdateAttributeValue(String oldValue, String newValue) throws ComputeException;
    public int bulkUpdateAttributePrefix(String oldPrefix, String newPrefix) throws ComputeException;
    public DomainObject annexDomainObjectTree(Access access, Long objGuid) throws ComputeException;
    
}
