
package org.janelia.it.jacs.compute.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.Remote;

import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * A remote interface to generic queries against the Entity model. This interface does not concern itself with any
 * specific entity types such as Folders or Annotations, it only deals with entities and the meta model. 
 * 
 * All methods which create or modify entities will trigger reindexing events to asynchronously update the full-text 
 * index. Deletions do not trigger such updates because the client is smart enough to filter out results which are 
 * found in the index but not in the database. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Deprecated
@Remote
public interface EntityBeanRemote {
    
	public EntityType createNewEntityType(String entityTypeName) throws ComputeException;
    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException;
    public List<EntityType> getEntityTypes() throws ComputeException;
    public List<EntityAttribute> getEntityAttributes() throws ComputeException;
    public EntityType getEntityTypeByName(String name) throws ComputeException;
    public EntityAttribute getEntityAttributeByName(String name) throws ComputeException;
    
	public Entity saveOrUpdateEntity(String subjectKey, Entity entity) throws ComputeException;
    public EntityData saveOrUpdateEntityData(String subjectKey, EntityData newData) throws ComputeException;
    public Entity saveOrUpdateEntityDatas(String subjectKey, Entity entity) throws ComputeException;
    public Entity createEntity(String subjectKey, String entityTypeName, String entityName) throws ComputeException;
    public EntityData addEntityToParent(String subjectKey, Long parentId, Long entityId, Integer index, String attrName) throws ComputeException;
    public void addChildren(String subjectKey, Long parentId, List<Long> childrenIds, String attributeName) throws ComputeException;

    public EntityData updateChildIndex(String subjectKey, EntityData entityData, Integer orderIndex) throws ComputeException;
    public EntityData setOrUpdateValue(String subjectKey, Long entityId, String attributeName, String value) throws ComputeException;
    public Collection<EntityData> setOrUpdateValues(String subjectKey, Collection<Long> entityIds, String attributeName, String value) throws ComputeException;
    
    public boolean deleteEntityById(String subjectKey, Long entityId) throws ComputeException;
    
    /** @deprecated use deleteEntityTreeById */
    @Deprecated
    public boolean deleteEntityTree(String subjectKey, Long entityId) throws ComputeException;
    /** @deprecated use deleteEntityTreeById */
    @Deprecated
    public boolean deleteEntityTree(String subjectKey, Long entityId, boolean unlinkMultipleParents) throws ComputeException;
    
    public boolean deleteEntityTreeById(String subjectKey, Long entityId) throws ComputeException;
    public boolean deleteEntityTreeById(String subjectKey, Long entityId, boolean unlinkMultipleParents) throws ComputeException;
    public void deleteEntityData(String subjectKey, Long entityDataId) throws ComputeException;
    
    public Entity getEntityById(String subjectKey, Long entityId) throws ComputeException;
    public List<Entity> getEntitiesById(String subjectKey, List<Long> ids) throws ComputeException;

    public Set<Entity> getEntitiesByName(String subjectKey, String name) throws ComputeException;
    public List<Entity> getEntitiesByTypeName(String subjectKey, String entityTypeName) throws ComputeException;
    public List<Entity> getEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws ComputeException;
    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException;
    public List<Entity> getEntitiesWithTag(String subjectKey, String attrTag) throws ComputeException;
    
    public Set<Entity> getUserEntitiesByName(String subjectKey, String name) throws ComputeException;
    public List<Entity> getUserEntitiesByTypeName(String subjectKey, String entityTypeName) throws ComputeException;
    public List<Entity> getUserEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws ComputeException;
    public List<Entity> getUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException;
    public List<Entity> getUserEntitiesWithAttributeValueAndTypeName(String subjectKey, String attrName, String attrValue, String entityTypeName) throws ComputeException;
    public long getCountUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException;
    
    public List<byte[]> getB64DecodedEntityDataValues(Long entityId, String entityDataType) throws ComputeException;
    public byte[] getB64DecodedEntityDataValue(Long entityId, Long entityDataId, String entityDataType) throws ComputeException;
    public Entity getEntityTree(String subjectKey, Long entityId) throws ComputeException;
    public Entity getEntityAndChildren(String subjectKey, Long entityId) throws ComputeException;
    public Set<Entity> getParentEntities(String subjectKey, Long entityId) throws ComputeException;
    public Set<Entity> getChildEntities(String subjectKey, Long entityId) throws ComputeException;
    public Set<EntityData> getParentEntityDatas(String subjectKey, Long childEntityId) throws ComputeException;
    public Set<Long> getParentIdsForAttribute(String subjectKey, Long childEntityId, String attributeName) throws ComputeException;
    public Entity getAncestorWithType(String subjectKey, Long entityId, String type) throws ComputeException;
    public List<List<EntityData>> getPathsToRoots(String subjectKey, Long entityId) throws ComputeException;
    
    public List<MappedId> getProjectedResults(String subjectKey, List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws ComputeException;

    public Entity annexEntityTree(String subjectKey, Long entityId) throws ComputeException;
    
    public Set<EntityActorPermission> getFullPermissions(String subjectKey, Long entityId) throws ComputeException;
    public EntityActorPermission grantPermissions(String subjectKey, Long entityId, String granteeKey, String permissions, boolean recursive) throws ComputeException;
    public void revokePermissions(String subjectKey, Long entityId, String granteeKey,  boolean recursive) throws ComputeException;
    public EntityActorPermission saveOrUpdatePermission(String subjectKey, EntityActorPermission eap) throws ComputeException;

    public List<Entity> getWorkspaces(String subjectKey) throws ComputeException;
    public Entity getDefaultWorkspace(String subjectKey) throws ComputeException;
    public EntityData addRootToWorkspace(String subjectKey, Long workspaceId, Long entityId) throws ComputeException;
    public EntityData addRootToDefaultWorkspace(String subjectKey,Long entityId) throws ComputeException;
    public EntityData createFolderInWorkspace(String subjectKey, Long workspaceId, String entityName) throws ComputeException;
    public EntityData createFolderInDefaultWorkspace(String subjectKey, String entityName) throws ComputeException;
    
}
