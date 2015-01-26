
package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.Collection;
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
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

/**
 * Implementation of queries against the entity model. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Stateless(name = "EntityEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = StrictMaxPool.class, maxSize = 500, timeout = 10000)
public class EntityBeanImpl implements EntityBeanLocal, EntityBeanRemote {
	
    private static final Logger _logger = Logger.getLogger(EntityBeanImpl.class);

    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    
    private void updateIndex(Entity entity) {
    	IndexingHelper.updateIndex(entity.getId());
    }
    
    public EntityType createNewEntityType(String entityTypeName) throws ComputeException {
    	try {
    		EntityType entityType = _annotationDAO.createNewEntityType(entityTypeName);	
    		return entityType;
    	}
    	catch (DaoException e) {
            _logger.error("Could not create entity type "+entityTypeName,e);
    		throw new ComputeException("Could not create entity type "+entityTypeName,e);
    	}
    }

    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException {
    	try {
    		return _annotationDAO.createNewEntityAttr(entityTypeName, attrName);	
    	}
    	catch (DaoException e) {
            _logger.error("Could not create entity attr "+attrName,e);
    		throw new ComputeException("Could not create entity attr "+attrName,e);
    	}
    }

    public List<EntityType> getEntityTypes() throws ComputeException {
        try {
            return _annotationDAO.getAllEntityTypes();
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity types", e);
            throw new ComputeException("Error trying to get the entity types",e);
        }
    }

    public List<EntityAttribute> getEntityAttributes() throws ComputeException {
        try {
            return _annotationDAO.getAllEntityAttributes();
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity attributes", e);
            throw new ComputeException("Error trying to get the entity attributes",e);
        }
    }

    public EntityType getEntityTypeByName(String entityTypeName) throws ComputeException {
		return _annotationDAO.getEntityTypeByName(entityTypeName);
    }
    
    public EntityAttribute getEntityAttributeByName(String attrName) throws ComputeException {
    	return _annotationDAO.getEntityAttributeByName(attrName);
    }
    
    public Entity saveOrUpdateEntity(Entity entity) throws ComputeException {
        try {
        	_annotationDAO.checkAttributeTypes(entity);
            _annotationDAO.saveOrUpdateEntity(entity);
            updateIndex(entity);
            return entity;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to save or update Entity");
            throw new ComputeException("Error trying to save or update Entity",e);
        }
    }
    
    public EntityData saveOrUpdateEntityData(EntityData newData) throws ComputeException {
        try {
        	_annotationDAO.checkAttributeTypes(newData);
            _annotationDAO.saveOrUpdateEntityData(newData);
            if (newData.getParentEntity()!=null) {
            	updateIndex(newData.getParentEntity());
            }
            return newData;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to save or update EntityData");
            throw new ComputeException("Error trying to save or update EntityData",e);
        }
    }
    
    public Entity saveOrUpdateEntity(String subjectKey, Entity entity) throws ComputeException {
        try {
        	boolean isNew = entity.getId()==null;
        	if (!isNew) {
    	        Entity currEntity = getEntityById(subjectKey, entity.getId());
    	        if (!EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                    throw new ComputeException("Subject "+subjectKey+" cannot change "+entity.getId());
    	        }
                _annotationDAO.getCurrentSession().evict(currEntity);
    	    }
            saveOrUpdateEntity(entity);
            _logger.info(subjectKey+" "+(isNew?"created":"saved")+" entity "+entity.getId());
            return entity;
        } 
        catch (DaoException e) {
            _logger.error("Error saving entity with name: "+entity.getName(),e);
            throw new ComputeException("Error saving entity with name: "+entity.getName(),e);
        }
    }

    public Entity saveOrUpdateEntityDatas(String subjectKey, Entity entity) throws ComputeException {
        try {
            if (!EntityUtils.hasWriteAccess(entity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entity.getId());
            }
            int n = 0;
            for(EntityData ed : entity.getEntityData()) {
                _annotationDAO.saveOrUpdateEntityData(ed);
                n++;
            }
            _logger.info(subjectKey+" updated "+n+" entity datas on entity "+entity.getId());
            updateIndex(entity);
            return entity;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to save or update Entity Datas",e);
            throw new ComputeException("Error trying to save or update Entity Datas",e);
        }
    }

    public EntityData saveOrUpdateEntityData(String subjectKey, EntityData ed) throws ComputeException {
        try {
        	boolean isNew = ed.getId()==null;
            if (!isNew) {
                Entity currEntity = getEntityById(subjectKey, ed.getParentEntity().getId());
                if (!EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                    throw new ComputeException("Subject "+subjectKey+" cannot change "+ed.getParentEntity().getId());
                }
                _annotationDAO.getCurrentSession().evict(currEntity);
            }
            saveOrUpdateEntityData(ed);
        	_logger.info(subjectKey+" "+(isNew?"created":"saved")+" entity data "+ed.getId());
            return ed;
        } 
        catch (DaoException e) {
            _logger.error("Error saving entity data",e);
            throw new ComputeException("Error saving entity data",e);
        }
    }

    public Entity createEntity(String subjectKey, String entityTypeName, String entityName) throws ComputeException {
        try {
            Entity entity = _annotationDAO.createEntity(subjectKey, entityTypeName, entityName);
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
        	_annotationDAO.checkEntityTypeSupportsAttribute(parent.getEntityTypeName(), attrName);
            return _annotationDAO.addEntityToParent(parent, entity, index, attrName);
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
        	_annotationDAO.checkEntityTypeSupportsAttribute(parent.getEntityTypeName(), attrName);
            return _annotationDAO.addEntityToParent(parent, entity, index, attrName, value, true);
        } 
        catch (DaoException e) {
            _logger.error("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(), e);
            throw new ComputeException("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(),e);
        }
    }

    /**
     * This is a faster, security-free version of the normal addEntityToParent with value, only exposed to local clients.
     */
    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName, boolean workspaceCheck) throws ComputeException {
        try {
        	_annotationDAO.checkEntityTypeSupportsAttribute(parent.getEntityTypeName(), attrName);
            return _annotationDAO.addEntityToParent(parent, entity, index, attrName, null, workspaceCheck);
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
                throw new DaoException("Parent entity does not exist: "+parentId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(parent, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+parentId);
            }
            Entity entity = getEntityById(subjectKey, entityId);
            if (entity==null) {
                throw new DaoException("Entity does not exist: "+entityId);
            }
            
            _annotationDAO.checkEntityTypeSupportsAttribute(parent.getEntityTypeName(), attrName);
            
            EntityData ed = _annotationDAO.addEntityToParent(parent, entity, index, attrName, value, true);
            
            _logger.info(subjectKey+" added entity data "+ed.getId()+" (parent="+parent.getId()+",child="+entity.getId()+")");
            
            IndexingHelper.updateIndexAddAncestor(entityId, parentId);
            
            return ed;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to add entity (id="+entityId+") to parent "+parentId, e);
            throw new ComputeException("Error trying to add entity (id="+entityId+") to parent "+parentId,e);
        }
    }

    public void addChildren(String subjectKey, Long parentId, List<Long> childrenIds, String attrName) throws ComputeException {
        try {
        	Entity parent = _annotationDAO.getEntityById(parentId);
            if (parent==null) {
                throw new Exception("Entity not found: "+parentId);
            }
            if (!EntityUtils.hasWriteAccess(parent, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+parent.getId());
            }
            
            _annotationDAO.checkEntityTypeSupportsAttribute(parent.getEntityTypeName(), attrName);
            
        	_annotationDAO.addChildren(subjectKey, parentId, childrenIds, attrName);
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
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(parent, _annotationDAO.getSubjectKeys(subjectKey))) {
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
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(entity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            
            int c = 0;
            for(EntityData entityData : entity.getEntityData()) {
                if (entityData.getEntityAttrName().equals(attributeName)) {
                    entityData.setValue(value);
                    c++;
                }
            }
            
            if (c>1) {
                throw new ComputeException("More than one "+attributeName+" value was found on entity "+entityId);
            }
            
            if (c==0) {
                entity.setValueByAttributeName(attributeName, value);
            }
            
            saveOrUpdateEntity(entity);
            return entity.getEntityDataByAttributeName(attributeName);

        } 
         catch (Exception e) {
            _logger.error("Error trying to set or update value on "+entityId, e);
            throw new ComputeException("Error trying to set or update value on "+entityId,e);
        }
    }
    
    public Collection<EntityData> setOrUpdateValues(String subjectKey, Collection<Long> entityIds, String attributeName, String value) throws ComputeException {
        // This will cut out a lot of round-tripping between the client and
        // middleware.  It will not cut down on middleware-to-database traffic.
        Collection<EntityData> returnList = new ArrayList<EntityData>();
        for ( Long entityId: entityIds ) {
            returnList.add( setOrUpdateValue( subjectKey, entityId, attributeName, value ) );
        }
        return returnList;
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
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            if (_annotationDAO.deleteEntityById(subjectKey, entityId)) {
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

    /** @deprecated use deleteEntityTreeById */
    @Deprecated
    public boolean deleteEntityTree(String subjectKey, Long entityId) throws ComputeException {
        throw new UnsupportedOperationException("This client version is no longer supported. Please restart the workstation client to upgrade to the latest version.");
    }

    /** @deprecated use deleteEntityTreeById */
    @Deprecated
    public boolean deleteEntityTree(String subjectKey, Long entityId, boolean unlinkMultipleParents) throws ComputeException {
        throw new UnsupportedOperationException("This client version is no longer supported. Please restart the workstation client to upgrade to the latest version.");
    }
    
    public boolean deleteEntityTreeById(String subjectKey, Long entityId) throws ComputeException {
        return deleteEntityTreeById(subjectKey, entityId, false);
    }

    public boolean deleteEntityTreeById(String subjectKey, Long entityId, boolean unlinkMultipleParents) throws ComputeException {
        try {
            Entity currEntity = getEntityById(subjectKey, entityId);
            if (currEntity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            _annotationDAO.deleteEntityTree(subjectKey, currEntity, unlinkMultipleParents);
            _logger.info(subjectKey+" deleted entity tree "+entityId);
            return true;
        }
        catch (Exception e) {
            _logger.error("Error deleting entity tree "+entityId,e);
            throw new ComputeException("Error deleting entity tree "+entityId,e);
        }
    }
    
    public void deleteEntityData(String subjectKey, Long entityDataId) throws ComputeException {
        try {
            Entity currEntity = _annotationDAO.getEntityByEntityDataId(subjectKey, entityDataId);
            if (currEntity==null) {
                throw new Exception("Parent entity not found for entity data with id="+entityDataId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot delete entity data "+entityDataId);
            }
            EntityData toDelete = null;
            for(EntityData ed : currEntity.getEntityData()) {
                if (ed.getId().equals(entityDataId)) {
                    toDelete = ed;
                    break;
                }
            }
            deleteEntityData(toDelete);
            _logger.info(subjectKey+" deleted entity data "+entityDataId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete entity data "+entityDataId, e);
            throw new ComputeException("Unexpected error while trying to delete entity data "+entityDataId,e);
        }
    }

    public void deleteEntityData(EntityData ed) throws ComputeException {
        try {
            _annotationDAO.deleteEntityData(ed);
        }
        catch (Exception e) {
            _logger.error("Error deleting entity data "+ed.getId());
            throw new ComputeException("Error deleting entity data "+ed.getId(),e);
        }
    }
    
    public int bulkUpdateEntityDataValue(String oldValue, String newValue) throws ComputeException {
        try {
            return _annotationDAO.bulkUpdateEntityDataValue(oldValue, newValue);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while bulk updating entity data values", e);
            throw new ComputeException("Unexpected error while bulk updating entity data values",e);
        }
    }

    public int bulkUpdateEntityDataPrefix(String oldPrefix, String newPrefix) throws ComputeException {
        try {
            return _annotationDAO.bulkUpdateEntityDataPrefix(oldPrefix, newPrefix);
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
            return _annotationDAO.getEntityById(subjectKey, entityId);
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
            return _annotationDAO.getEntitiesInList(subjectKey, ids);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get multiple entities", e);
            throw new ComputeException("Could not get entities in session",e);
        }
    }
    

    public Set<Entity> getEntitiesByName(String subjectKey, String name) throws ComputeException {
        try {
            return new HashSet<Entity>(_annotationDAO.getEntitiesByName(subjectKey, name));
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities with name "+name+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }
    
    public List<Entity> getEntitiesByTypeName(String subjectKey, String entityTypeName) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesByTypeName(subjectKey, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }

    public List<Entity> getEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesByNameAndTypeName(subjectKey, entityName, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName+" with name "+entityName+" for user "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }
    
    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
        }
        catch (DaoException e) {
            _logger.error("Error searching for entities with "+attrName+" like "+attrValue,e);
            throw new ComputeException("Error searching for entities with "+attrName+" like "+attrValue,e);
        }
    }
    
    public List<Entity> getEntitiesWithTag(String subjectKey, String attrTag) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesWithTag(subjectKey, attrTag);
        }
        catch (DaoException e) {
            _logger.error("Error searching for entities with tag "+attrTag,e);
            throw new ComputeException("Error searching for entities with tag "+attrTag,e);
        }
    }
    
    public Set<Entity> getUserEntitiesByName(String subjectKey, String name) throws ComputeException {
        try {
            return new HashSet<Entity>(_annotationDAO.getUserEntitiesByName(subjectKey, name));
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities with name "+name+" owned by "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }
    
    public List<Entity> getUserEntitiesByTypeName(String subjectKey, String entityTypeName) throws ComputeException {
        try {
            return _annotationDAO.getUserEntitiesByTypeName(subjectKey, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName+" owned by "+subjectKey, e);
            throw new ComputeException("Error trying to get entities",e);
        }
    }

    public List<Entity> getUserEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws ComputeException {
        try {
            return _annotationDAO.getUserEntitiesByNameAndTypeName(subjectKey, entityName, entityTypeName);
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
            return _annotationDAO.getUserEntitiesWithAttributeValue(subjectKey,
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
    		return _annotationDAO.getUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
    	}
    	catch (DaoException e) {
            _logger.error("Error searching for entities with "+attrName+" like "+attrValue+" owned by "+subjectKey,e);
    		throw new ComputeException("Error searching for entities with",e);
    	}
    }

    public long getCountUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws ComputeException {
        try {
            return _annotationDAO.getCountUserEntitiesWithAttributeValue(subjectKey, attrName, attrValue);
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
        return getEntityTree(null, entityId);
    }

    public Entity getEntityTree(String subjectKey, Long entityId) throws ComputeException {
        Entity entity = getEntityById(subjectKey, entityId);
    	_annotationDAO.loadLazyEntity(subjectKey, entity, true);    	
    	return entity;
    }

    public Entity getEntityAndChildren(Long entityId) throws ComputeException {
        return _annotationDAO.getEntityAndChildren(null, entityId);
    }
    
    public Entity getEntityAndChildren(String subjectKey, Long entityId) throws ComputeException {
        return _annotationDAO.getEntityAndChildren(subjectKey, entityId);
    }
    
    public Set<Entity> getParentEntities(Long entityId) throws ComputeException {
    	return getParentEntities(null, entityId);
    }
    
    public Set<Entity> getParentEntities(String subjectKey, Long entityId) throws ComputeException {
        try {
            return _annotationDAO.getParentEntities(subjectKey, entityId);
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
            return _annotationDAO.getChildEntities(subjectKey, entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get child entities for id="+entityId, e);
            throw new ComputeException("Error trying to get child entities",e);
        }
    }
    
    public Map<Long,String> getChildEntityNames(Long entityId) throws ComputeException {
    	try {
            return _annotationDAO.getChildEntityNames(entityId);
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
            return _annotationDAO.getParentEntityDatas(subjectKey, entityId);
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
            return _annotationDAO.getParentIdsForAttribute(subjectKey, childEntityId, attributeName);
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
    		return _annotationDAO.getAncestorWithType(subjectKey, entityId, type);
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
    		return _annotationDAO.getEntityDataPathsToRoots(subjectKey, fakeEd);
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
    		return _annotationDAO.getPathToRoot(subjectKey, entityId, rootId);
    	}
    	catch (DaoException e) {
            _logger.error("Error searching tree rooted at "+rootId+" for "+entityId,e);
    		throw new ComputeException("Error searching tree rooted at "+rootId+" for "+entityId,e);
    	}
    }
    
    public List<MappedId> getProjectedResults(String subjectKey, List<Long> entityIds, List<String> upMapping, List<String> downMapping) throws ComputeException {
    	try {
        	return _annotationDAO.getProjectedResults(subjectKey, entityIds, upMapping, downMapping);
        } catch (DaoException e) {
            _logger.error("Error in getProjectedResults(): "+e.getMessage());
            throw new ComputeException("Error in getProjectedResults(): "+e.getMessage(), e);
        }
    }

    public void loadLazyEntity(Entity entity, boolean recurse) throws ComputeException {
    	try {
        	_annotationDAO.loadLazyEntity(null, entity, recurse);
        } catch (DaoException e) {
            _logger.error("Error in loadLazyEntity(): "+e.getMessage());
            throw new ComputeException("Error in loadLazyEntity(): "+e.getMessage(), e);
        }
    }

    public Entity annexEntityTree(String subjectKey, Long entityId) throws ComputeException {
    	try {
        	return _annotationDAO.annexEntityTree(subjectKey, entityId);
        } catch (DaoException e) {
            _logger.error("Error in annexEntityTree(): "+e.getMessage());
            throw new ComputeException("Error in annexEntityTree(): "+e.getMessage(), e);
        }
    }
    
    public Entity saveBulkEntityTree(Entity root) throws ComputeException {
    	try {
        	return _annotationDAO.saveBulkEntityTree(root);
        } catch (DaoException e) {
            _logger.error("Error in saveBulkEntityTree(): "+e.getMessage());
            throw new ComputeException("Error in saveBulkEntityTree(): "+e.getMessage(), e);
        }
    }

    public Set<EntityActorPermission> getFullPermissions(String subjectKey, Long entityId) throws ComputeException {
        try {
            return _annotationDAO.getFullPermissions(subjectKey, entityId);
        } catch (DaoException e) {
            _logger.error("Error in getFullPermissions(): "+e.getMessage());
            throw new ComputeException("Error in getFullPermissions(): "+e.getMessage(), e);
        }
    }
    
    public EntityActorPermission grantPermissions(String subjectKey, Long entityId, String granteeKey, String permissions, boolean recursive) throws ComputeException {
        try {
        	return _annotationDAO.grantPermissions(subjectKey, entityId, granteeKey, permissions, recursive);
        }
        catch (DaoException e) {
        	_logger.error("Error granting permission for "+entityId+" to "+granteeKey, e);
        	throw new ComputeException("Error granting permission",e);
        }
    }
    
    public void revokePermissions(String subjectKey, Long entityId, String revokeeKey, boolean recursive) throws ComputeException {
        try {
        	_annotationDAO.revokePermissions(subjectKey, entityId, revokeeKey, recursive);
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
        	_annotationDAO.saveOrUpdate(eap);
        	return eap;
        }
        catch (DaoException e) {
        	_logger.error("Error saving permission", e);
        	throw new ComputeException("Error saving permission",e);
        }
    }

    public List<Long> getOrphanEntityIds(String subjectKey) throws ComputeException {
        try {
            return _annotationDAO.getOrphanEntityIds(subjectKey);
        }
        catch (DaoException e) {
            _logger.error("Error getting orphan entity ids for "+subjectKey, e);
            throw new ComputeException("Error getting orphan entity ids for "+subjectKey,e);
        }
    }

    public List<Entity> getWorkspaces(String subjectKey) throws ComputeException {
        try {
            return _annotationDAO.getWorkspaces(subjectKey);
        }
        catch (DaoException e) {
            _logger.error("Error getting workspaces for "+subjectKey, e);
            throw new ComputeException("Error getting workspaces for "+subjectKey,e);
        }
    }

    public Entity getDefaultWorkspace(String subjectKey) throws ComputeException {
        try {
            return _annotationDAO.getDefaultWorkspace(subjectKey);
        }
        catch (DaoException e) {
            _logger.error("Error getting default workspace for "+subjectKey, e);
            throw new ComputeException("Error getting default workspace for "+subjectKey,e);
        }
    }

    public EntityData addRootToDefaultWorkspace(String subjectKey, Long entityId) throws ComputeException {
        try {
            return _annotationDAO.addRootToDefaultWorkspace(subjectKey, entityId);
        }
        catch (DaoException e) {
            _logger.error("Error adding root to default workspace for "+subjectKey, e);
            throw new ComputeException("Error adding root to default workspace for "+subjectKey,e);
        }
    }
    
	public EntityData addRootToWorkspace(String subjectKey, Long workspaceId, Long entityId) throws ComputeException {
        try {
            return _annotationDAO.addRootToWorkspace(subjectKey, workspaceId, entityId);
        }
        catch (DaoException e) {
            _logger.error("Error adding root to workspace for "+subjectKey, e);
            throw new ComputeException("Error adding root to workspace for "+subjectKey,e);
        }
	}

    public EntityData createFolderInDefaultWorkspace(String subjectKey, String entityName) throws ComputeException {
        try {
            return _annotationDAO.createFolderInDefaultWorkspace(subjectKey, entityName);
        }
        catch (DaoException e) {
            _logger.error("Error creating folder in default workspace for "+subjectKey, e);
            throw new ComputeException("Error creating folder in default workspace for "+subjectKey,e);
        }
    }
    
	public EntityData createFolderInWorkspace(String subjectKey, Long workspaceId, String entityName) throws ComputeException {
        try {
            return _annotationDAO.createFolderInWorkspace(subjectKey, workspaceId, entityName);
        }
        catch (DaoException e) {
            _logger.error("Error creating folder in workspace for "+subjectKey, e);
            throw new ComputeException("Error creating folder in workspace for "+subjectKey,e);
        }
	}
	
	public Entity cloneEntityTree(Long sourceRootId, String targetSubjectKey, String targetRootName, boolean clonePermissions) throws ComputeException {
        try {
            return _annotationDAO.cloneEntityTree(sourceRootId, targetSubjectKey, targetRootName, clonePermissions);
        }
        catch (DaoException e) {
            _logger.error("Error cloning entity tree "+sourceRootId, e);
            throw new ComputeException("Error cloning entity tree "+sourceRootId,e);
        }
	}
}
