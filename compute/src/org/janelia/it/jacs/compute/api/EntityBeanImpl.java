
package org.janelia.it.jacs.compute.api;

import java.util.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.support.EntityMapStep;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.interceptor.UsageInterceptor;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingManagerManagement;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.annotation.ejb.Depends;
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
@Interceptors({UsageInterceptor.class})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
@Depends({"jboss:custom=IndexingManager"})
public class EntityBeanImpl implements EntityBeanLocal, EntityBeanRemote {
	
    private static Logger _logger = Logger.getLogger(EntityBeanImpl.class);
    
    @Depends({"jboss:custom=IndexingManager"})
	private IndexingManagerManagement indexingManager;
	
    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    
    private boolean updateIndexOnChange = true;
    
    public void setUpdateIndexOnChange(boolean updateIndexOnChange) {
    	this.updateIndexOnChange = updateIndexOnChange;
    }

    private void updateIndex(Entity entity) {
    	if (updateIndexOnChange) indexingManager.scheduleIndexing(entity.getId());
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

    public List<EntityType> getEntityTypes() {
        try {
            List<EntityType> returnList = _annotationDAO.getAllEntityTypes();
            _logger.debug("Entity types returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity types", e);
        }
        return null;
    }

    public List<EntityAttribute> getEntityAttributes() {
        try {
            List<EntityAttribute> returnList = _annotationDAO.getAllEntityAttributes();
            _logger.debug("Entity attributes returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity attributes", e);
        }
        return null;
    }

    public EntityType getEntityTypeByName(String entityTypeName) {
		return _annotationDAO.getEntityTypeByName(entityTypeName);
    }
    
    public EntityAttribute getEntityAttributeByName(String attrName) {
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
    
    public boolean deleteEntityById(Long entityId) throws ComputeException {
         try {
            return _annotationDAO.deleteEntityById(entityId);
        } 
         catch (Exception e) {
            _logger.error("Error trying to get delete entity "+entityId, e);
            throw new ComputeException("Error deleting entity "+entityId,e);
        }
    }
    
    public void deleteEntityData(EntityData ed) throws ComputeException {
        try {
            _annotationDAO.genericDelete(ed);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete entity data "+ed.getId());
            throw new ComputeException("Unexpected error while trying to delete entity data "+ed.getId(),e);
        }
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException {
        try {
            return _annotationDAO.addEntityToParent(parent, entity, index, attrName);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to add entity (id="+entity.getId()+") to parent "+parent.getId(), e);
            throw new ComputeException("Error trying to add entity (id="+entity.getId()+") to parent "+parent.getId(),e);
        }
    }
   
    
    public Entity saveOrUpdateEntity(String userLogin, Entity entity) throws ComputeException {
        try {
        	boolean isNew = entity.getId()==null;
        	if (!entity.getUser().getUserLogin().equals(userLogin)) {
        		throw new ComputeException("User "+userLogin+" cannot change "+entity.getId());
        	}
        	entity.setUpdatedDate(new Date());
            _annotationDAO.saveOrUpdate(entity);
            _logger.info(userLogin+" "+(isNew?"created":"saved")+" entity "+entity.getId());
            updateIndex(entity);
            return entity;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to save or update Entity",e);
            throw new ComputeException("Error trying to save or update Entity",e);
        }
    }

    public EntityData saveOrUpdateEntityData(String userLogin, EntityData ed) throws ComputeException {
        try {
        	boolean isNew = ed.getId()==null;
        	if (!ed.getUser().getUserLogin().equals(userLogin)) {
        		throw new ComputeException("User "+userLogin+" cannot change "+ed.getId());
        	}
        	ed.setUpdatedDate(new Date());
            _annotationDAO.saveOrUpdate(ed);
        	_logger.info(userLogin+" "+(isNew?"created":"saved")+" entity data "+ed.getId());
            if (ed.getValue()!=null && ed.getParentEntity()!=null) {
            	updateIndex(ed.getParentEntity());
            }
            return ed;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to save or update EntityData",e);
            throw new ComputeException("Error trying to save or update EntityData",e);
        }
    }

    public Entity createEntity(String userLogin, String entityTypeName, String entityName) throws ComputeException {
        try {
            Entity entity = _annotationDAO.createEntity(userLogin, entityTypeName, entityName);
        	_logger.info(userLogin+" created entity "+entity.getId());
        	updateIndex(entity);
            return entity;
        } catch (DaoException e) {
            _logger.error("Error trying to create entity",e);
            throw new ComputeException("Error trying to create entity",e);
        }
    }

    public EntityData addEntityToParent(String userLogin, Entity parent, Entity entity, Integer index, String attrName) throws ComputeException {
        try {
        	if (!parent.getUser().getUserLogin().equals(userLogin)) {
        		throw new ComputeException("User "+userLogin+" cannot add children to "+parent.getId());
        	}
            EntityData ed = _annotationDAO.addEntityToParent(parent, entity, index, attrName);
        	_logger.info(userLogin+" added entity data "+ed.getId());
        	return ed;
        } 
        catch (DaoException e) {
            _logger.error("Error trying to add entity (id="+entity.getId()+") to parent "+parent.getId(), e);
            throw new ComputeException("Error trying to add entity (id="+entity.getId()+") to parent "+parent.getId(),e);
        }
    }
    
    public void addChildren(String userLogin, Long parentId, List<Long> childrenIds, String attributeName) throws ComputeException {
        try {
        	Entity parent = _annotationDAO.getEntityById(""+parentId);
            if (parent==null) {
                throw new Exception("Entity not found: "+parentId);
            }
        	if (!parent.getUser().getUserLogin().equals(userLogin)) {
        		throw new ComputeException("User "+userLogin+" cannot add children to "+parent.getId());
        	}
        	_annotationDAO.addChildren(userLogin, parentId, childrenIds, attributeName);
        	_logger.info(userLogin+" added "+childrenIds.size()+" children to parent "+parentId);
        } 
        catch (Exception e) {
            _logger.error("Error trying to add children to parent "+parentId, e);
            throw new ComputeException("Error trying to add children to parent "+parentId, e);
        }
    }
    
    public boolean deleteEntityById(String userLogin, Long entityId) throws ComputeException {
         try {
        	Entity entity = _annotationDAO.getEntityById(""+entityId);
            if (entity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
        	if (!entity.getUser().getUserLogin().equals(userLogin)) {
        		throw new ComputeException("User "+userLogin+" cannot delete "+entity.getId());
        	}
            if (_annotationDAO.deleteEntityById(entityId)) {
            	_logger.info(userLogin+" deleted entity "+entityId);
            	return true;
            }
            return false;
        } 
         catch (Exception e) {
            _logger.error("Error trying to get delete entity "+entityId, e);
            throw new ComputeException("Error deleting entity "+entityId,e);
        }
    }
    
    public boolean deleteEntityTree(String userLogin, long entityId) throws ComputeException {
    	try {
            Entity entity = _annotationDAO.getEntityById(""+entityId);
            if (entity==null) {
            	throw new Exception("Entity not found: "+entityId);
            }
    		_annotationDAO.deleteEntityTree(userLogin, entity);
    		_logger.info(userLogin+" deleted entity tree "+entityId);
    		return true;
    	}
        catch (Exception e) {
            _logger.error("Error deleting entity tree "+entityId,e);
            throw new ComputeException("Error deleting entity tree "+entityId,e);
        }
    }

    public boolean deleteSmallEntityTree(String userLogin, long entityId) throws ComputeException {
        try {
            Entity entity = _annotationDAO.getEntityById(""+entityId);
            if (entity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            _annotationDAO.deleteSmallEntityTree(userLogin, entity, true, false, 0);
            _logger.info(userLogin+" deleted small entity tree "+entityId);
            return true;
        }
        catch (Exception e) {
            _logger.error("Error deleting entity tree "+entityId,e);
            throw new ComputeException("Error deleting entity tree "+entityId,e);
        }
    }
    
    public void deleteEntityData(String userLogin, EntityData ed) throws ComputeException {
        try {
        	if (!ed.getUser().getUserLogin().equals(userLogin)) {
        		throw new ComputeException("User "+userLogin+" cannot delete entity data "+ed.getId());
        	}
            _annotationDAO.genericDelete(ed);
            _logger.info(userLogin+" deleted entity data "+ed.getId());
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete entity data "+ed.getId(), e);
            throw new ComputeException("Unexpected error while trying to delete entity data "+ed.getId(),e);
        }
    }
    
    public Entity getEntityById(Long id) {
        try {
            return _annotationDAO.getEntityById(""+id);
        }
        catch (Exception e) {
            _logger.error("Error trying to get the entity with id "+id,e);
        }
        return null;
    }
    
    public Entity getEntityById(String id) {
        try {
            return _annotationDAO.getEntityById(id);
        }
        catch (Exception e) {
            _logger.error("Error trying to get the entity with id "+id,e);
        }
        return null;
    }

    public List<Entity> getEntitiesById(List<Long> ids) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesInList(ids);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get multiple entities", e);
            throw new ComputeException("Coud not get entities in session",e);
        }
    }
    
    public Entity getUserEntityById(String userLogin, long entityId) {
        try {
            return _annotationDAO.getUserEntityById(userLogin, entityId);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of id "+entityId+" for user "+userLogin, e);
        }
        return null;
    }

    public Set<Entity> getEntitiesByName(String name) {
        try {
            return _annotationDAO.getEntitiesByName(name);
        } catch (DaoException e) {
            _logger.error("Error trying to get EntityType by name = " + name, e);
        }
        return null;
    }

    public List<Entity> getEntitiesByTypeName(String entityTypeName) {
        try {
            List<Entity> returnList = _annotationDAO.getUserEntitiesByTypeName(null, entityTypeName);
            _logger.debug("Entities returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName, e);
        }
        return null;
    }
    
    public List<Entity> getEntitiesWithAttributeValue(String attrName, String attrValue) throws ComputeException {
    	try {
    		return _annotationDAO.getEntitiesWithAttributeValue(attrName, attrValue);
    	}
    	catch (DaoException e) {
            _logger.error("Error searching for entities with "+attrName+" like "+attrValue,e);
    		throw new ComputeException("Error searching for entities with "+attrName+" like "+attrValue,e);
    	}
    }
    
    public Entity getEntityTree(Long id) {
    	return _annotationDAO.populateDescendants(getEntityById(id.toString()));
    }
    
    public Set<Entity> getParentEntities(long entityId) {
        try {
            return _annotationDAO.getParentEntities(entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent entities for id="+entityId, e);
        }
        return null;
    }

    public Set<Entity> getChildEntities(long entityId) {
        try {
            return _annotationDAO.getChildEntities(entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get child entities for id="+entityId, e);
        }
        return null;
    }
    
    public Set<EntityData> getParentEntityDatas(long entityId) {
        try {
            return _annotationDAO.getParentEntityDatas(entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent entity data for id="+entityId, e);
        }
        return null;
    }
    
    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName) {
        try {
            return _annotationDAO.getParentIdsForAttribute(childEntityId, attributeName);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent ids for id="+childEntityId+", attr="+attributeName, e);
        }
        return null;
    }
    
    public Entity getAncestorWithType(Entity entity, String type) throws ComputeException {
    	try {
    		return _annotationDAO.getAncestorWithType(entity, type);
    	}
    	catch (DaoException e) {
            _logger.error("Error finding ancestor of type "+type+" for "+entity.getId(),e);
    		throw new ComputeException("Error finding ancestor of type "+type+" for "+entity.getId(),e);
    	}
    }
    
    public List<List<Long>> searchTreeForNameStartingWith(Long rootId, String searchString) throws ComputeException {
    	try {
    		return _annotationDAO.searchTree(rootId, searchString+"%");
    	}
    	catch (DaoException e) {
            _logger.error("Error searching tree rooted at "+rootId+" for "+searchString,e);
    		throw new ComputeException("Error searching tree rooted at "+rootId+" for "+searchString,e);
    	}
    }
    
    public List<List<EntityData>> getPathsToRoots(String userLogin, Entity entity) throws ComputeException {
    	try {
    		EntityData fakeEd = new EntityData();
    		fakeEd.setParentEntity(entity);
    		return _annotationDAO.getEntityDataPathsToRoots(userLogin, fakeEd);
    	}
    	catch (DaoException e) {
            _logger.error("Error getting paths to root from "+entity.getId(),e);
    		throw new ComputeException("Error getting paths to root from "+entity.getId(),e);
    	}
    }
    
    public List<Long> getPathToRoot(Long entityId, Long rootId) throws ComputeException {
    	try {
    		return _annotationDAO.getPathToRoot(entityId, rootId);
    	}
    	catch (DaoException e) {
            _logger.error("Error searching tree rooted at "+rootId+" for "+entityId,e);
    		throw new ComputeException("Error searching tree rooted at "+rootId+" for "+entityId,e);
    	}
    }
    
    public List<MappedId> getProjectedResults(List<Long> entityIds, List<EntityMapStep> upMapping, List<EntityMapStep> downMapping) throws ComputeException {
    	try {
        	return _annotationDAO.getProjectedResults(entityIds, upMapping, downMapping);
        } catch (DaoException e) {
            _logger.error("Error in getProjectedResults(): "+e.getMessage());
            throw new ComputeException("Error in getProjectedResults(): "+e.getMessage(), e);
        }
    }

    public void loadLazyEntity(Entity entity, boolean recurse) throws DaoException {
		
        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            Set<Entity> childEntitySet = _annotationDAO.getChildEntities(entity.getId());
            Map<Long, Entity> childEntityMap = new HashMap<Long, Entity>();
            for (Entity childEntity : childEntitySet) {
                childEntityMap.put(childEntity.getId(), childEntity);
            }

            // Replace the entity data with real objects
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    ed.setChildEntity(childEntityMap.get(ed.getChildEntity().getId()));
                }
            }
        }

        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(ed.getChildEntity(), true);
                }
            }
        }
    }

    public void setupEntityTypes() {
        try {
            _annotationDAO.setupEntityTypes();
        } catch (DaoException e) {
            _logger.error("Error calling annotationDAO.setupEntityTypes()",e);
        }
    }
}
