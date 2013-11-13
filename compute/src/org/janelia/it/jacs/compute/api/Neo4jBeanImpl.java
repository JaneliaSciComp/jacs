
package org.janelia.it.jacs.compute.api;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.neo4j.Neo4jCSVExportDao;
import org.janelia.it.jacs.compute.access.neo4j.Neo4jEntityDAO;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

/**
 * Implementation of Neo4j database access via the Entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Stateless(name = "Neo4jEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class Neo4jBeanImpl implements Neo4jBeanLocal, Neo4jBeanRemote, EntityBeanLocal, EntityBeanRemote {
    
    private static final Logger _logger = Logger.getLogger(Neo4jBeanImpl.class);

    public void neo4jAllEntities(boolean clearDb) throws ComputeException {
    	try {
    		Neo4jCSVExportDao neo4jDAO = new Neo4jCSVExportDao(_logger);
    		if (clearDb) {
    			neo4jDAO.dropDatabase();
    		}
    		neo4jDAO.loadAllEntities();
    	}
    	catch (DaoException e) {
            _logger.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
    }

    private final AnnotationDAO annotationDao = new AnnotationDAO(_logger);
    private final Neo4jEntityDAO neo4jEntityDao = new Neo4jEntityDAO(_logger, annotationDao);
    
    private void updateIndex(Entity entity) {
        IndexingHelper.updateIndex(entity.getId());
    }
    
    /************************************************************************************/
    /** Some methods that still use the annotation DAO for now */
    /************************************************************************************/
    
    public EntityType createNewEntityType(String entityTypeName) throws ComputeException {
        try {
            EntityType entityType = annotationDao.createNewEntityType(entityTypeName); 
            return entityType;
        }
        catch (DaoException e) {
            _logger.error("Could not create entity type "+entityTypeName,e);
            throw new ComputeException("Could not create entity type "+entityTypeName,e);
        }
    }

    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException {
        try {
            return annotationDao.createNewEntityAttr(entityTypeName, attrName);    
        }
        catch (DaoException e) {
            _logger.error("Could not create entity attr "+attrName,e);
            throw new ComputeException("Could not create entity attr "+attrName,e);
        }
    }

    public List<EntityType> getEntityTypes() throws ComputeException {
        try {
            return annotationDao.getAllEntityTypes();
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity types", e);
            throw new ComputeException("Error trying to get the entity types",e);
        }
    }

    public List<EntityAttribute> getEntityAttributes() throws ComputeException {
        try {
            return annotationDao.getAllEntityAttributes();
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity attributes", e);
            throw new ComputeException("Error trying to get the entity attributes",e);
        }
    }

    public EntityType getEntityTypeByName(String entityTypeName) throws ComputeException {
        return annotationDao.getEntityTypeByName(entityTypeName);
    }
    
    public EntityAttribute getEntityAttributeByName(String attrName) throws ComputeException {
        return annotationDao.getEntityAttributeByName(attrName);
    }
    
    
    /************************************************************************************/
    /** Converted methods */
    /************************************************************************************/
    
    
    public Entity saveOrUpdateEntity(Entity entity) throws ComputeException {
        try {
            entity.setUpdatedDate(new Date());
            neo4jEntityDao.saveOrUpdate(entity);
            updateIndex(entity);
            return entity;
        } catch (DaoException e) {
            _logger.error("Error trying to save or update Entity");
            throw new ComputeException("Error trying to save or update Entity",e);
        }
    }

    public EntityData saveOrUpdateEntityData(EntityData newData) throws ComputeException {
        try {
            newData.setUpdatedDate(new Date());
            neo4jEntityDao.saveOrUpdate(newData);
            if (newData.getParentEntity()!=null) {
                updateIndex(newData.getParentEntity());
            }
            return newData;
        } catch (DaoException e) {
            _logger.error("Error trying to save or update EntityData");
            throw new ComputeException("Error trying to save or update EntityData",e);
        }
    }
    
    public void deleteEntityData(EntityData ed) throws ComputeException {
        try {
            neo4jEntityDao.genericDelete(ed);
        }
        catch (Exception e) {
            _logger.error("Error deleting entity data "+ed.getId());
            throw new ComputeException("Error deleting entity data "+ed.getId(),e);
        }
    }
    
    public Entity saveOrUpdateEntity(String subjectKey, Entity entity) throws ComputeException {
        try {
            boolean isNew = entity.getId()==null;
            if (!isNew) {
                Entity currEntity = getEntityById(subjectKey, entity.getId());
                if (!EntityUtils.hasWriteAccess(currEntity, annotationDao.getSubjectKeys(subjectKey))) {
                    throw new ComputeException("Subject "+subjectKey+" cannot change "+entity.getId());
                }
            }
            entity.setUpdatedDate(new Date());
            neo4jEntityDao.saveOrUpdate(entity);
            _logger.info(subjectKey+" "+(isNew?"created":"saved")+" entity "+entity.getId());
            updateIndex(entity);
            return entity;
        } 
        catch (DaoException e) {
            _logger.error("Error saving entity with name: "+entity.getName(),e);
            throw new ComputeException("Error saving entity with name: "+entity.getName(),e);
        }
    }

    public EntityData saveOrUpdateEntityData(String subjectKey, EntityData ed) throws ComputeException {
        try {
            boolean isNew = ed.getId()==null;
            if (!isNew) {
                Entity currEntity = getEntityById(subjectKey, ed.getParentEntity().getId());
                if (!EntityUtils.hasWriteAccess(currEntity, annotationDao.getSubjectKeys(subjectKey))) {
                    throw new ComputeException("Subject "+subjectKey+" cannot change "+ed.getParentEntity().getId());
                }
            }
            ed.setUpdatedDate(new Date());
            neo4jEntityDao.saveOrUpdate(ed);
            _logger.info(subjectKey+" "+(isNew?"created":"saved")+" entity data "+ed.getId());
            if (ed.getValue()!=null && ed.getParentEntity()!=null) {
                updateIndex(ed.getParentEntity());
            }
            return ed;
        } 
        catch (DaoException e) {
            _logger.error("Error saving entity data",e);
            throw new ComputeException("Error saving entity data",e);
        }
    }

    public Entity createEntity(String subjectKey, String entityTypeName, String entityName) throws ComputeException {
        try {
            Entity entity = neo4jEntityDao.createEntity(subjectKey, entityTypeName, entityName);
            _logger.info(subjectKey+" created entity "+entity.getId());
            updateIndex(entity);
            return entity;
        } catch (DaoException e) {
            _logger.error("Error trying to create entity",e);
            throw new ComputeException("Error trying to create entity",e);
        }
    }

    /**
     * This is a faster, security-free version of the normal addEntityToParent with no value, only exposed to local clients.
     */
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException {
        try {
            return neo4jEntityDao.addEntityToParent(parent, entity, index, attrName);
        } 
        catch (DaoException e) {
            _logger.error("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(), e);
            throw new ComputeException("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(),e);
        }
    }

    /**
     * This is a faster, security-free version of the normal addEntityToParent with value, only exposed to local clients.
     */
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName, String value) throws ComputeException {
        try {
            return neo4jEntityDao.addEntityToParent(parent, entity, index, attrName, value);
        } 
        catch (DaoException e) {
            _logger.error("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(), e);
            throw new ComputeException("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(),e);
        }
    }
    
    public EntityData addEntityToParent(String subjectKey, Long parentId, Long entityId, Integer index, String attrName) throws ComputeException {
        return addEntityToParent(subjectKey, parentId, entityId, index, attrName, null);
    }

    public EntityData addEntityToParent(String subjectKey, Long parentId, Long entityId, Integer index, String attrName, String value) throws ComputeException {
        try {
            Entity parent = getEntityById(subjectKey, parentId);
            if (parent==null) {
                throw new DaoException("Parent entity does not exist "+parent);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(parent, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+parentId);
            }
            Entity entity = getEntityById(subjectKey, entityId);
            if (entity==null) {
                throw new DaoException("Entity does not exist "+entityId);
            }
            EntityData ed = neo4jEntityDao.addEntityToParent(parent, entity, index, attrName, value);
            
            _logger.info(subjectKey+" added entity data "+ed.getId()+" (parent="+parent.getId()+",child="+entity.getId()+")");
            
            IndexingHelper.updateIndexAddAncestor(entityId, parentId);
            
            return ed;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to add entity (id="+entityId+") to parent "+parentId, e);
            throw new ComputeException("Error trying to add entity (id="+entityId+") to parent "+parentId,e);
        }
    }

    public void addChildren(String subjectKey, Long parentId, List<Long> childrenIds, String attributeName) throws ComputeException {
        try {
            Entity parent = neo4jEntityDao.getEntityById(parentId);
            if (parent==null) {
                throw new Exception("Entity not found: "+parentId);
            }
            if (!EntityUtils.hasWriteAccess(parent, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+parent.getId());
            }
            neo4jEntityDao.addChildren(subjectKey, parentId, childrenIds, attributeName);
            _logger.info("Subject "+subjectKey+" added "+childrenIds.size()+" children to parent "+parentId);
            
            for(Long childId : childrenIds) {
                IndexingHelper.updateIndexAddAncestor(childId, parentId);
            }
        } 
        catch (Exception e) {
            _logger.error("Error trying to add children to parent "+parentId, e);
            throw new ComputeException("Error trying to add children to parent "+parentId, e);
        }
    }

    public EntityData updateChildIndex(String subjectKey, EntityData entityData, Integer orderIndex) throws ComputeException {
         try {
            Entity parent = entityData.getParentEntity();
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(parent, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+parent.getId());
            }

            entityData.setOrderIndex(orderIndex);
            return saveOrUpdateEntityData(entityData);
         } 
         catch (Exception e) {
            _logger.error("Error trying to update order index for "+orderIndex, e);
            throw new ComputeException("Error trying to update order index for "+orderIndex,e);
         }
    }
    
    public EntityData setOrUpdateValue(String subjectKey, Long entityId, String attributeName, String value) throws ComputeException {
         try {
            Entity entity = getEntityById(subjectKey, entityId);
            if (entity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(entity, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            
            int c = 0;
            for(EntityData entityData : entity.getEntityData()) {
                if (entityData.getEntityAttribute().getName().equals(attributeName)) {
                    entityData.setValue(value);
                    c++;
                }
            }
            
            if (c>0) {
                throw new ComputeException("More than one "+attributeName+" value was found on enttiy "+entityId);
            }
            
            if (c==0) {
                entity.setValueByAttributeName(attributeName, value);
            }
            
            saveOrUpdateEntity(entity);
            return entity.getEntityDataByAttributeName(attributeName);

        } 
         catch (Exception e) {
            _logger.error("Error trying to get delete entity "+entityId, e);
            throw new ComputeException("Error deleting entity "+entityId,e);
        }
    }

    public EntityData updateChildIndex(EntityData entityData, Integer orderIndex) throws ComputeException {
        return updateChildIndex(null, entityData, orderIndex);
    }
    
    public EntityData setOrUpdateValue(Long entityId, String attributeName, String value) throws ComputeException {
        return setOrUpdateValue(null, entityId, attributeName, value);
    }
        
    public boolean deleteEntityById(Long entityId) throws ComputeException {
        return deleteEntityById(null, entityId);
    }
    
    public boolean deleteEntityById(String subjectKey, Long entityId) throws ComputeException {
         try {
            Entity currEntity = getEntityById(subjectKey, entityId);
            if (currEntity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            if (neo4jEntityDao.deleteEntityById(subjectKey, entityId)) {
                _logger.info(subjectKey+" deleted entity "+entityId);
                return true;
            }
            return false;
        } 
         catch (Exception e) {
            _logger.error("Error trying to get delete entity "+entityId, e);
            throw new ComputeException("Error deleting entity "+entityId,e);
        }
    }
    
    public boolean deleteEntityTree(String subjectKey, Long entityId) throws ComputeException {
        try {
            Entity currEntity = getEntityById(subjectKey, entityId);
            if (currEntity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            neo4jEntityDao.deleteEntityTree(subjectKey, currEntity);
            _logger.info(subjectKey+" deleted entity tree "+entityId);
            return true;
        }
        catch (Exception e) {
            _logger.error("Error deleting entity tree "+entityId,e);
            throw new ComputeException("Error deleting entity tree "+entityId,e);
        }
    }

    public boolean deleteSmallEntityTree(String subjectKey, Long entityId) throws ComputeException {
        return deleteSmallEntityTree(subjectKey, entityId, false);
    }

    public boolean deleteSmallEntityTree(String subjectKey, Long entityId, boolean unlinkMultipleParents) throws ComputeException {
        try {
            Entity currEntity = getEntityById(subjectKey, entityId);
            if (currEntity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            neo4jEntityDao.deleteSmallEntityTree(subjectKey, currEntity, unlinkMultipleParents);
            _logger.info(subjectKey+" deleted small entity tree "+entityId);
            return true;
        }
        catch (Exception e) {
            _logger.error("Error deleting entity tree "+entityId,e);
            throw new ComputeException("Error deleting entity tree "+entityId,e);
        }
    }
    
    public void deleteEntityData(String subjectKey, Long entityDataId) throws ComputeException {
        try {
            Entity currEntity = neo4jEntityDao.getEntityByEntityDataId(subjectKey, entityDataId);
            if (currEntity==null) {
                throw new Exception("Parent entity not found for entity data with id="+entityDataId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, annotationDao.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot delete entity data "+entityDataId);
            }
            EntityData toDelete = null;
            for(EntityData ed : currEntity.getEntityData()) {
                if (ed.getId().equals(entityDataId)) {
                    toDelete = ed;
                    break;
                }
            }
            neo4jEntityDao.genericDelete(toDelete);
            _logger.info(subjectKey+" deleted entity data "+entityDataId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete entity data "+entityDataId, e);
            throw new ComputeException("Unexpected error while trying to delete entity data "+entityDataId,e);
        }
    }
    
    public int bulkUpdateEntityDataValue(String oldValue, String newValue) throws ComputeException {
        try {
            return neo4jEntityDao.bulkUpdateEntityDataValue(oldValue, newValue);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while bulk updating entity data values", e);
            throw new ComputeException("Unexpected error while bulk updating entity data values",e);
        }
    }

    public int bulkUpdateEntityDataPrefix(String oldPrefix, String newPrefix) throws ComputeException {
        try {
            return neo4jEntityDao.bulkUpdateEntityDataPrefix(oldPrefix, newPrefix);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while bulk updating entity data values", e);
            throw new ComputeException("Unexpected error while bulk updating entity data values",e);
        }
    }
    
    public Entity getEntityById(String entityId) throws ComputeException {
        return getEntityById(new Long(entityId));
    }

    public Entity getEntityById(Long entityId) throws ComputeException {
        return getEntityById(null, entityId);
    }
    
    public Entity getEntityById(String subjectKey, Long entityId) throws ComputeException {
        try {
            return neo4jEntityDao.getEntityById(subjectKey, entityId);
        }
        catch (Exception e) {
            _logger.error("Error trying to get entity with id="+entityId,e);
            throw new ComputeException("Error trying to get entity",e);
        }
    }

    public List<Entity> getEntitiesById(List<Long> ids) throws ComputeException {
        return getEntitiesById(null, ids);
    }
    
    public List<Entity> getEntitiesById(String subjectKey, List<Long> ids) throws ComputeException {
        try {
            return neo4jEntityDao.getEntitiesInList(subjectKey, ids);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get multiple entities", e);
            throw new ComputeException("Could not get entities in session",e);
        }
    }
    

    public Set<Entity> getEntitiesByName(String subjectKey, String name) throws ComputeException {
        try {
            return new HashSet<Entity>(neo4jEntityDao.getEntitiesByName(subjectKey, name));
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities with name "+name+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }
    
    public List<Entity> getEntitiesByTypeName(String subjectKey, String entityTypeName) throws ComputeException {
        try {
            return neo4jEntityDao.getEntitiesByTypeName(subjectKey, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }

    public List<Entity> getEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws ComputeException {
        try {
            return neo4jEntityDao.getEntitiesByNameAndTypeName(subjectKey, entityName, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName+" with name "+entityName+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }
    
    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException {
        try {
            return neo4jEntityDao.getEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
        }
        catch (DaoException e) {
            _logger.error("Error searching for entities with "+attrName+" like "+attrValue,e);
            throw new ComputeException("Error searching for entities with "+attrName+" like "+attrValue,e);
        }
    }
    
    public List<Entity> getEntitiesWithTag(String subjectKey, String attrTag) throws ComputeException {
        try {
            return neo4jEntityDao.getEntitiesWithTag(subjectKey, attrTag);
        }
        catch (DaoException e) {
            _logger.error("Error searching for entities with tag "+attrTag,e);
            throw new ComputeException("Error searching for entities with tag "+attrTag,e);
        }
    }
    
    public Set<Entity> getUserEntitiesByName(String subjectKey, String name) throws ComputeException {
        try {
            return new HashSet<Entity>(neo4jEntityDao.getUserEntitiesByName(subjectKey, name));
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities with name "+name+" owned by "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }
    
    public List<Entity> getUserEntitiesByTypeName(String subjectKey, String entityTypeName) throws ComputeException {
        try {
            return neo4jEntityDao.getUserEntitiesByTypeName(subjectKey, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName+" owned by "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }

    public List<Entity> getUserEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws ComputeException {
        try {
            return neo4jEntityDao.getUserEntitiesByNameAndTypeName(subjectKey, entityName, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName+" with name "+entityName+" owned by "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }

    public List<Entity> getUserEntitiesWithAttributeValueAndTypeName(String subjectKey,
                                                                     String attrName,
                                                                     String attrValue,
                                                                     String entityTypeName)
            throws ComputeException {

        try {
            return neo4jEntityDao.getUserEntitiesWithAttributeValue(subjectKey,
                                                                    entityTypeName,
                                                                    attrName,
                                                                    attrValue);
        } catch (DaoException e) {
            final String msg = "Error searching for entities of type " + entityTypeName + " with " +
                               attrName + " like " + attrValue + " owned by " + subjectKey;
            _logger.error(msg, e);
            throw new ComputeException(msg, e);
        }
    }

    public List<Entity> getUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException {
        try {
            return neo4jEntityDao.getUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
        }
        catch (DaoException e) {
            _logger.error("Error searching for entities with "+attrName+" like "+attrValue+" owned by "+subjectKey,e);
            throw new ComputeException("Error searching for entities with",e);
        }
    }

    public long getCountUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException {
        try {
            return neo4jEntityDao.getCountUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
        }
        catch (DaoException e) {
            _logger.error("Error getting count for entities with "+attrName+" like "+attrValue+" owned by "+subjectKey,e);
            throw new ComputeException("Error searching for entities with",e);
        }
    }
    
    public Set<Entity> getEntitiesByName(String name) throws ComputeException {
        return getUserEntitiesByName(null, name);
    }
    
    public List<Entity> getEntitiesByTypeName(String entityTypeName) throws ComputeException {
        return getUserEntitiesByTypeName(null, entityTypeName);
    }
    
    public List<Entity> getEntitiesByNameAndTypeName(String entityName, String entityTypeName) throws ComputeException {
        return getUserEntitiesByNameAndTypeName(null, entityName, entityTypeName);
    }

    public List<Entity> getEntitiesWithAttributeValue(String attrName, String attrValue) throws ComputeException {
        return getUserEntitiesWithAttributeValue(null, attrName, attrValue);
    }
    
    public long getCountEntitiesWithAttributeValue(String attrName, String attrValue) throws ComputeException {
        return getCountUserEntitiesWithAttributeValue(null, attrName, attrValue);
    }

    public Entity getEntityTree(Long entityId) throws ComputeException {
        // We can use the more efficient populateDescendants instead of loadLazyEntity, because we don't care about
        // the permissions being loaded, since this method is for local use.
        return neo4jEntityDao.populateDescendants(null, getEntityById(null, entityId));
    }

    public Entity getEntityTree(String subjectKey, Long entityId) throws ComputeException {
        Entity entity = getEntityById(subjectKey, entityId);
        // We can't use populateDescendants because it doesn't load the permissions. 
        // TODO: This is a little slow to use for loading an entire tree recursively and should be improved:
        neo4jEntityDao.loadLazyEntity(subjectKey, entity, true);        
        return entity;
    }

    public Entity getEntityAndChildren(Long entityId) throws ComputeException {
        return neo4jEntityDao.getEntityAndChildren(null, entityId);
    }
    
    public Entity getEntityAndChildren(String subjectKey, Long entityId) throws ComputeException {
        return neo4jEntityDao.getEntityAndChildren(subjectKey, entityId);
    }
    
    public Set<Entity> getParentEntities(Long entityId) throws ComputeException {
        return getParentEntities(null, entityId);
    }
    
    public Set<Entity> getParentEntities(String subjectKey, Long entityId) throws ComputeException {
        try {
            return neo4jEntityDao.getParentEntities(subjectKey, entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent entities for id="+entityId, e);
            throw new ComputeException("Error trying to get parent entities",e);
        }
    }

    public Set<Entity> getChildEntities(Long entityId) throws ComputeException {
        return getChildEntities(null, entityId);
    }
    
    public Set<Entity> getChildEntities(String subjectKey, Long entityId) throws ComputeException {
        try {
            return neo4jEntityDao.getChildEntities(subjectKey, entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get child entities for id="+entityId, e);
            throw new ComputeException("Error trying to get child entities",e);
        }
    }
    
    public Map<Long,String> getChildEntityNames(Long entityId) throws ComputeException {
        try {
            return neo4jEntityDao.getChildEntityNames(entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get child entities for id="+entityId, e);
            throw new ComputeException("Error trying to get child entity names",e);
        }
    }

    public Set<EntityData> getParentEntityDatas(Long entityId) throws ComputeException {
        return getParentEntityDatas(null, entityId);
    }
    
    public Set<EntityData> getParentEntityDatas(String subjectKey, Long entityId) throws ComputeException {
        try {
            return neo4jEntityDao.getParentEntityDatas(subjectKey, entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent entity data for id="+entityId, e);
            throw new ComputeException("Error trying to get parent entities",e);
        }
    }

    public Set<Long> getParentIdsForAttribute(Long childEntityId, String attributeName) throws ComputeException {
        return getParentIdsForAttribute(null, childEntityId, attributeName);
    }
    
    public Set<Long> getParentIdsForAttribute(String subjectKey, Long childEntityId, String attributeName) throws ComputeException {
        try {
            return neo4jEntityDao.getParentIdsForAttribute(subjectKey, childEntityId, attributeName);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent ids for id="+childEntityId+", attr="+attributeName, e);
            throw new ComputeException("Error trying to get parent entities",e);
        }
    }

    public Entity getAncestorWithType(Entity entity, String type) throws ComputeException {
        return getAncestorWithType(null, entity.getId(), type);
    }
    
    public Entity getAncestorWithType(String subjectKey, Long entityId, String type) throws ComputeException {
        try {
            return neo4jEntityDao.getAncestorWithType(subjectKey, entityId, type);
        }
        catch (DaoException e) {
            _logger.error("Error finding ancestor of type "+type+" for "+entityId,e);
            throw new ComputeException("Error finding ancestor of type "+type+" for "+entityId,e);
        }
    }
    
    public List<List<EntityData>> getPathsToRoots(String subjectKey, Long entityId) throws ComputeException {
        try {
            EntityData fakeEd = new EntityData();
            fakeEd.setParentEntity(getEntityById(entityId));
            return neo4jEntityDao.getEntityDataPathsToRoots(subjectKey, fakeEd);
        }
        catch (DaoException e) {
            _logger.error("Error getting paths to root from "+entityId,e);
            throw new ComputeException("Error getting paths to root from "+entityId,e);
        }
    }
    
    public List<Long> getPathToRoot(Long entityId, Long rootId) throws ComputeException {
        return getPathToRoot(null, entityId, rootId);
    }
    
    public List<Long> getPathToRoot(String subjectKey, Long entityId, Long rootId) throws ComputeException {
        try {
            return neo4jEntityDao.getPathToRoot(subjectKey, entityId, rootId);
        }
        catch (DaoException e) {
            _logger.error("Error searching tree rooted at "+rootId+" for "+entityId,e);
            throw new ComputeException("Error searching tree rooted at "+rootId+" for "+entityId,e);
        }
    }
    
    public List<MappedId> getProjectedResults(String subjectKey, List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws ComputeException {
        try {
            return neo4jEntityDao.getProjectedResults(subjectKey, entityIds, upMapping, downMapping);
        } catch (DaoException e) {
            _logger.error("Error in getProjectedResults(): "+e.getMessage());
            throw new ComputeException("Error in getProjectedResults(): "+e.getMessage(), e);
        }
    }

    public void loadLazyEntity(Entity entity, boolean recurse) throws ComputeException {
        try {
            neo4jEntityDao.loadLazyEntity(null, entity, recurse);
        } catch (DaoException e) {
            _logger.error("Error in loadLazyEntity(): "+e.getMessage());
            throw new ComputeException("Error in loadLazyEntity(): "+e.getMessage(), e);
        }
    }

    public Entity annexEntityTree(String subjectKey, Long entityId) throws ComputeException {
        try {
            return neo4jEntityDao.annexEntityTree(subjectKey, entityId);
        } catch (DaoException e) {
            _logger.error("Error in annexEntityTree(): "+e.getMessage());
            throw new ComputeException("Error in annexEntityTree(): "+e.getMessage(), e);
        }
    }
    
    public Entity saveBulkEntityTree(Entity root) throws ComputeException {
        try {
            return neo4jEntityDao.saveBulkEntityTree(root);
        } catch (DaoException e) {
            _logger.error("Error in saveBulkEntityTree(): "+e.getMessage());
            throw new ComputeException("Error in saveBulkEntityTree(): "+e.getMessage(), e);
        }
    }

    public Set<EntityActorPermission> getFullPermissions(String subjectKey, Long entityId) throws ComputeException {
        try {
            return neo4jEntityDao.getFullPermissions(subjectKey, entityId);
        } catch (DaoException e) {
            _logger.error("Error in getFullPermissions(): "+e.getMessage());
            throw new ComputeException("Error in getFullPermissions(): "+e.getMessage(), e);
        }
    }
    
    public EntityActorPermission grantPermissions(String subjectKey, Long entityId, String granteeKey, String permissions, boolean recursive) throws ComputeException {
        try {
            return neo4jEntityDao.grantPermissions(subjectKey, entityId, granteeKey, permissions, recursive);
        }
        catch (DaoException e) {
            _logger.error("Error granting permission for "+entityId+" to "+granteeKey, e);
            throw new ComputeException("Error granting permission",e);
        }
    }
    
    public void revokePermissions(String subjectKey, Long entityId, String revokeeKey, boolean recursive) throws ComputeException {
        try {
            neo4jEntityDao.revokePermissions(subjectKey, entityId, revokeeKey, recursive);
        }
        catch (DaoException e) {
            _logger.error("Error revoking permission for "+entityId+" to "+revokeeKey, e);
            throw new ComputeException("Error revoking permission",e);
        }
    }
    
    public EntityActorPermission saveOrUpdatePermission(String subjectKey, EntityActorPermission eap) throws ComputeException {
        try {
            Entity entity = eap.getEntity();
            if (entity==null) {
                throw new ComputeException("Entity for permission cannot be null");
            }
            if (subjectKey!=null) {
                if (!subjectKey.equals(eap.getEntity().getOwnerKey())) {
                    throw new ComputeException("User "+subjectKey+" does not have the right to grant access to "+eap.getEntity().getId());
                }   
            }
            neo4jEntityDao.saveOrUpdate(eap);
            return eap;
        }
        catch (DaoException e) {
            _logger.error("Error saving permission", e);
            throw new ComputeException("Error saving permission",e);
        }
    }
}
