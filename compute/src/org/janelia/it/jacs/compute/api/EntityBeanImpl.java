
package org.janelia.it.jacs.compute.api;

import java.util.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

/**
 * Implementation of queries against the entity model. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Stateless(name = "EntityEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 500, timeout = 10000)
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
        	entity.setUpdatedDate(new Date());
            _annotationDAO.saveOrUpdate(entity);
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
            _annotationDAO.saveOrUpdate(newData);
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
            _annotationDAO.genericDelete(ed);
        }
        catch (Exception e) {
            _logger.error("Error deleting entity data "+ed.getId());
            throw new ComputeException("Error deleting entity data "+ed.getId(),e);
        }
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException {
        try {
            return _annotationDAO.addEntityToParent(parent, entity, index, attrName);
        } 
        catch (DaoException e) {
            _logger.error("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(), e);
            throw new ComputeException("Error adding entity (id="+entity.getId()+") to parent "+parent.getId(),e);
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
        	entity.setUpdatedDate(new Date());
            _annotationDAO.saveOrUpdate(entity);
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
                if (!EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                    throw new ComputeException("Subject "+subjectKey+" cannot change "+ed.getParentEntity().getId());
                }
                _annotationDAO.getCurrentSession().evict(currEntity);
            }
        	ed.setUpdatedDate(new Date());
            _annotationDAO.saveOrUpdate(ed);
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
            Entity entity = _annotationDAO.createEntity(subjectKey, entityTypeName, entityName);
        	_logger.info(subjectKey+" created entity "+entity.getId());
        	updateIndex(entity);
            return entity;
        } catch (DaoException e) {
            _logger.error("Error trying to create entity",e);
            throw new ComputeException("Error trying to create entity",e);
        }
    }

    public EntityData addEntityToParent(String subjectKey, Long parentId, Long entityId, Integer index, String attrName) throws ComputeException {
        try {
            Entity parent = getEntityById(subjectKey, parentId);
            if (parent==null) {
                throw new DaoException("Parent entity does not exist "+parent);
            }
            if (!EntityUtils.hasWriteAccess(parent, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+parentId);
            }
            Entity entity = getEntityById(subjectKey, entityId);
            if (entity==null) {
                throw new DaoException("Entity does not exist "+entityId);
            }
            EntityData ed = _annotationDAO.addEntityToParent(parent, entity, index, attrName);
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
        	Entity parent = _annotationDAO.getEntityById(parentId);
            if (parent==null) {
                throw new Exception("Entity not found: "+parentId);
            }
            if (!EntityUtils.hasWriteAccess(parent, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot add children to "+parent.getId());
            }
        	_annotationDAO.addChildren(subjectKey, parentId, childrenIds, attributeName);
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
    
    public boolean deleteEntityTree(String subjectKey, Long entityId) throws ComputeException {
    	try {
            Entity currEntity = getEntityById(subjectKey, entityId);
            if (currEntity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
    		_annotationDAO.deleteEntityTree(subjectKey, currEntity);
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
            if (subjectKey!=null && !EntityUtils.hasWriteAccess(currEntity, _annotationDAO.getSubjectKeys(subjectKey))) {
                throw new ComputeException("Subject "+subjectKey+" cannot change "+entityId);
            }
            _annotationDAO.deleteSmallEntityTree(subjectKey, currEntity, unlinkMultipleParents);
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
            _annotationDAO.getCurrentSession().evict(currEntity);
            _annotationDAO.genericDelete(toDelete);
            _logger.info(subjectKey+" deleted entity data "+entityDataId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete entity data "+entityDataId, e);
            throw new ComputeException("Unexpected error while trying to delete entity data "+entityDataId,e);
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
        // We can use the more efficient populateDescendants instead of loadLazyEntity, because we don't care about
        // the permissions being loaded, since this method is for local use.
        return _annotationDAO.populateDescendants(null, getEntityById(null, entityId));
    }

    public Entity getEntityTree(String subjectKey, Long entityId) throws ComputeException {
        Entity entity = getEntityById(subjectKey, entityId);
        // We can't use populateDescendants because it doesn't load the permissions. 
        // TODO: This is a little slow to use for loading an entire tree recursively and should be improved:
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
}
