
package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import javax.ejb.Local;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A local interface for invoking queries against the entity model. Affords access to everything in EntityBeanRemote
 * and a few other methods, such as security-less versions of saving methods (maybe those shouldn't exist in the long
 * run, but for now they're for legacy reasons).
 * 
 * @see EntityBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface EntityBeanLocal extends EntityBeanRemote {

    public Entity getEntityById(String entityId) throws ComputeException;
    public Entity getEntityById(Long entityId) throws ComputeException;
    public Entity getEntityTree(Long entityId) throws ComputeException;
    public Entity getEntityAndChildren(Long entityId) throws ComputeException;
    public Set<Entity> getParentEntities(Long entityId) throws ComputeException;
    public Set<Entity> getChildEntities(Long entityId) throws ComputeException;
    public List<Entity> getEntitiesById(List<Long> ids) throws ComputeException;

    public Set<Entity> getEntitiesByName(String name) throws ComputeException;
    public List<Entity> getEntitiesByTypeName(String entityTypeName) throws ComputeException;
    public List<Entity> getEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws ComputeException;
    public List<Entity> getEntitiesWithAttributeValue(String attrName, String attrValue) throws ComputeException;

    public Set<EntityData> getParentEntityDatas(Long childEntityId) throws ComputeException;
    public Set<Long> getParentIdsForAttribute(Long childEntityId, String attributeName) throws ComputeException;
    public Entity getAncestorWithType(Entity entity, String type) throws ComputeException;
    public List<Long> getPathToRoot(Long entityId, Long rootId) throws ComputeException;
    public Map<Long,String> getChildEntityNames(Long entityId) throws ComputeException;
    
	public Entity saveOrUpdateEntity(Entity entity) throws ComputeException;
    public EntityData saveOrUpdateEntityData(EntityData newData) throws ComputeException;
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException;
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName, String value) throws ComputeException;
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName, boolean fanout) throws ComputeException;
    
    public EntityData updateChildIndex(EntityData entityData, Integer orderIndex) throws ComputeException;
    public EntityData setOrUpdateValue(Long entityId, String attributeName, String value) throws ComputeException;

    public boolean deleteEntityById(Long entityId) throws ComputeException;
    public void deleteEntityData(EntityData ed) throws ComputeException;
    public int bulkUpdateEntityDataValue(String oldValue, String newValue) throws ComputeException;
    public int bulkUpdateEntityDataPrefix(String oldPrefix, String newPrefix) throws ComputeException;
    
    public void loadLazyEntity(Entity entity, boolean recurse) throws ComputeException;
    public Entity saveBulkEntityTree(Entity root) throws ComputeException;
    
    public List<Long> getOrphanEntityIds(String subjectKey) throws ComputeException;
    public Entity cloneEntityTree(Long sourceRootId, String targetSubjectKey, String targetRootName, boolean clonePermissions) throws ComputeException;

//    public List<String> getSummaryFilesForLSMs(List<String> lsmNames) throws ComputeException;
}
