
package org.janelia.it.jacs.compute.api;

import java.util.List;
import java.util.Set;

import javax.ejb.Remote;

import org.janelia.it.jacs.compute.api.support.EntityMapStep;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.Entity;
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
@Remote
public interface EntityBeanRemote {
    
	public EntityType createNewEntityType(String entityTypeName) throws ComputeException;
    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException;
    public List<EntityType> getEntityTypes();
    public List<EntityAttribute> getEntityAttributes();
    public EntityType getEntityTypeByName(String name);
    public EntityAttribute getEntityAttributeByName(String name);
    
	public Entity saveOrUpdateEntity(String userLogin, Entity entity) throws ComputeException;
    public EntityData saveOrUpdateEntityData(String userLogin, EntityData newData) throws ComputeException;
    public Entity createEntity(String userLogin, String entityTypeName, String entityName) throws ComputeException;
    public EntityData addEntityToParent(String userLogin, Entity parent, Entity entity, Integer index, String attrName) throws ComputeException;
    public void addChildren(String userLogin, Long parentId, List<Long> childrenIds, String attributeName) throws ComputeException;

    public boolean deleteEntityById(String userLogin, Long entityId) throws ComputeException;
    public boolean deleteEntityTree(String userLogin, long entityId) throws ComputeException;
    public boolean deleteSmallEntityTree(String userLogin, long entityId) throws ComputeException;
    public void deleteEntityData(String userLogin, EntityData ed) throws ComputeException;
    
    public Entity getEntityById(String targetId);
    public List<Entity> getEntitiesById(List<Long> ids) throws ComputeException;
    public Entity getUserEntityById(String userLogin, long entityId);
    
    public Set<Entity> getEntitiesByName(String name);
    public List<Entity> getEntitiesByTypeName(String entityTypeName);
    public List<Entity> getEntitiesWithAttributeValue(String attrName, String attrValue) throws ComputeException;

    public Entity getEntityTree(Long id);
    public Set<Entity> getParentEntities(long entityId);
    public Set<Entity> getChildEntities(long entityId);
    public Set<EntityData> getParentEntityDatas(long childEntityId);
    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName);
    public Entity getAncestorWithType(Entity entity, String type) throws ComputeException;
    public List<List<Long>> searchTreeForNameStartingWith(Long rootId, String searchString) throws ComputeException;
    public List<List<EntityData>> getPathsToRoots(String userLogin, Entity entity) throws ComputeException;
    public List<Long> getPathToRoot(Long entityId, Long rootId) throws ComputeException;
    
    public List<MappedId> getProjectedResults(List<Long> entityIds, List<EntityMapStep> upMapping, List<EntityMapStep> downMapping) throws ComputeException;
}
