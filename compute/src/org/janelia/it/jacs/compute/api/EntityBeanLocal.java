
package org.janelia.it.jacs.compute.api;

import javax.ejb.Local;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * A local interface for invoking queries against the entity model. Affords access to everything in EntityBeanRemote
 * and a few other methods, such as security-less versions of saving methods (maybe those shouldn't exist in the long
 * run, but for now they're for legacy reasons).
 * 
 * By default, the local version of this bean returned by EJBFactory does not update the SOLR index after making a 
 * change to the entity model. To enable index updates, call setUpdateIndexOnChange(true) after retrieving an instance. 
 * 
 * @see EntityBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface EntityBeanLocal extends EntityBeanRemote {

	public void setUpdateIndexOnChange(boolean updateIndexOnChange);
	
	public Entity saveOrUpdateEntity(Entity entity) throws ComputeException;
    public EntityData saveOrUpdateEntityData(EntityData newData) throws ComputeException;
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException;
    
    public boolean deleteEntityById(Long entityId) throws ComputeException;
    public void deleteEntityData(EntityData ed) throws ComputeException;
    
    public void loadLazyEntity(Entity entity, boolean recurse) throws DaoException;
	public void setupEntityTypes();
}
