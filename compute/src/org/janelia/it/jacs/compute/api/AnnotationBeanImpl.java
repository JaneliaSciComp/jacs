package org.janelia.it.jacs.compute.api;

import java.util.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.Category;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.user_data.User;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

@Stateless(name = "AnnotationEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 15, timeout = 10000)
public class AnnotationBeanImpl implements AnnotationBeanLocal, AnnotationBeanRemote {
	
    private Logger _logger = Logger.getLogger(this.getClass());
    
    public static final String APP_VERSION = "jacs.version";
    public static final String SEARCH_EJB_PROP = "AnnotationEJB.Name";
    public static final String MDB_PROVIDER_URL_PROP = "AsyncMessageInterface.ProviderURL";

    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    private final ComputeDAO _computeDAO = new ComputeDAO(_logger);

    private final Map<String, EntityType> entityByName = new HashMap<String, EntityType>();
    private final Map<String, EntityAttribute> attrByName = new HashMap<String, EntityAttribute>();
    private static final Map<Long, Entity> entityTrees = new HashMap<Long, Entity>();

    private void preloadData() {

        try {
            if (entityByName.isEmpty()) {
                _logger.debug("  Preloading entity types...");
                for(EntityType entityType : _annotationDAO.getAllEntityTypes()) {
                    _logger.debug("    Loaded entity type: "+entityType.getName());
                    entityByName.put(entityType.getName(), entityType);
                }
            }

            if (attrByName.isEmpty()) {
                _logger.debug("  Preloading attribute types...");
                for(EntityAttribute entityAttr : _annotationDAO.getAllEntityAttributes()) {
                    _logger.debug("    Loaded entity attr: "+entityAttr.getName());
                    attrByName.put(entityAttr.getName(), entityAttr);
                }
            }
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying preload models.", e);
        }
    }

    public void removeEntityFromFolder(EntityData folderEntityData) throws ComputeException {
        try {
            _annotationDAO.genericDelete(folderEntityData);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete folder entity data "+folderEntityData.getId());
            throw new ComputeException("Unexpected error while trying to delete folder entity data "+folderEntityData.getId(),e);
        }
    }

    public List<Entity> getAnnotationsForEntities(String username, List<Long> entityIds) throws ComputeException {
        try {
            return _annotationDAO.getAnnotationsByEntityId(username, entityIds);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for entities "+username, e);
            throw new ComputeException("Coud not get annotations for entities ",e);
        }
    }

    public List<Entity> getAnnotationsForEntity(String username, long entityId) throws ComputeException {
    	List<Long> entityIds = new ArrayList<Long>();
    	entityIds.add(entityId);
    	return getAnnotationsForEntities(username, entityIds);
    }

    public List<Entity> getAnnotationsForSession(String username, long sessionId) throws ComputeException {
        try {
            Task task = _annotationDAO.getTaskById(sessionId);
            if (task == null) {
                throw new Exception("Session not found");
            }
        	return _annotationDAO.getAnnotationsForSession(username, sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations in session "+sessionId, e);
            throw new ComputeException("Coud not get annotations in session",e);
        }
    }
    
    public List<Entity> getEntitiesForAnnotationSession(String username, long sessionId) throws ComputeException {
        try {
            Task task = _annotationDAO.getTaskById(sessionId);
            if (task == null) {
                throw new Exception("Session not found");
            }
        	
            String entityIds = task.getParameter(AnnotationSessionTask.PARAM_annotationTargets);
            if (entityIds == null || "".equals(entityIds)) {
            	return new ArrayList<Entity>();
            }
            else {
            	return _annotationDAO.getEntitiesInList(entityIds);	
            }
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get entities in session "+sessionId, e);
            throw new ComputeException("Coud not get entities in session",e);
        }
    }
	
    public List<Entity> getCategoriesForAnnotationSession(String username, long sessionId) throws ComputeException {
        try {
            Task task = _annotationDAO.getTaskById(sessionId);
            if (task == null) {
                throw new Exception("Session not found");
            }
        	
            String entityIds = task.getParameter(AnnotationSessionTask.PARAM_annotationCategories);
            if (entityIds == null || "".equals(entityIds)) {
            	return new ArrayList<Entity>();
            }
            else {
            	return _annotationDAO.getEntitiesInList(entityIds);	
            }
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get categories in session "+sessionId, e);
            throw new ComputeException("Coud not get categories in session",e);
        }
    }

    public List<Entity> getEntitiesWithFilePath(String filePath) {
        try {
            List<Entity> matchingEntities = _annotationDAO.getEntitiesWithFilePath(filePath);
            return matchingEntities;
        } catch (Exception e) {
            _logger.error("Unexpected error finding Entities matching path = " + filePath);
        }
        return null;
    }
    
    public Entity getFolderTree(Long id) throws ComputeException {

        try {
            Entity root = getEntityById(id.toString());
            if (root == null) return null;

            if (!root.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER)) {
            	throw new DaoException("Entity (id="+id+") is not a folder");
            }
            
            return populateDescendants(root);
        }
        catch (Exception e) {
            _logger.error("Error getting folder tree",e);
            throw new ComputeException("Error getting folder tree",e);
        }
    }
    
    /**
     * Ontology Section
     */

    public Entity createOntologyRoot(String userLogin, String rootName) throws ComputeException {
        try {
            User tmpUser = _computeDAO.getUserByName(userLogin);
            Entity newOntologyRoot = newEntity(EntityConstants.TYPE_ONTOLOGY_ROOT, rootName, tmpUser);
            _annotationDAO.saveOrUpdate(newOntologyRoot);

            // Add the type
            EntityData termData = newData(newOntologyRoot, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, tmpUser);
            termData.setValue(Category.class.getSimpleName());
            _annotationDAO.saveOrUpdate(termData);

            return newOntologyRoot;
        }
        catch (Exception e) {
            _logger.error("Error creating new ontology ("+rootName+") for user "+userLogin,e);
            throw new ComputeException("Error creating new ontology ("+rootName+") for user "+userLogin,e);
        }
    }

    public EntityData createOntologyTerm(String userLogin, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {

        try {
            User tmpUser = _computeDAO.getUserByName(userLogin);
            Entity parentOntologyElement = _annotationDAO.getEntityById(ontologyTermParentId.toString());
            
            // Create and save the new entity
            Entity newOntologyElement = newEntity(EntityConstants.TYPE_ONTOLOGY_ELEMENT, termName, tmpUser);

            
            // If no order index is given then we add in last place
            if (orderIndex == null) {
            	int max = 0;
            	for(EntityData data : parentOntologyElement.getEntityData()) {
            		if (data.getOrderIndex() != null && data.getOrderIndex() > max) max = data.getOrderIndex(); 
            	}
            	orderIndex = max + 1;
            }

            Set<EntityData> eds = new HashSet<EntityData>();
            newOntologyElement.setEntityData(eds);
            
            // Add the type
            EntityData termData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, tmpUser);
            termData.setValue(type.getClass().getSimpleName());
            eds.add(termData);

            // Add the type-specific parameters
            if (type instanceof Interval) {

                Interval interval = (Interval)type;

                EntityData lowerData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER, tmpUser);
                lowerData.setValue(interval.getLowerBound().toString());
                eds.add(lowerData);

                EntityData upperData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER, tmpUser);
                upperData.setValue(interval.getUpperBound().toString());
                eds.add(upperData);
            }

            // Save the new element
            _annotationDAO.saveOrUpdate(newOntologyElement);
            
            // Associate the entity to the parent
            EntityData childData = newData(parentOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT, tmpUser);
            childData.setChildEntity(newOntologyElement);
            childData.setOrderIndex(orderIndex);
            _annotationDAO.saveOrUpdate(childData);
            
            return childData;
        }
        catch (Exception e) {
            _logger.error("Error creating new term ("+termName+") for user "+userLogin,e);
            throw new ComputeException("Error creating new term ("+termName+") for user "+userLogin,e);
        }
    }

    public void removeOntologyTerm(String userLogin, Long ontologyTermId) throws ComputeException {
    	
    	try {
    		_annotationDAO.deleteOntologyTerm(userLogin, ontologyTermId.toString());
    	}
    	catch (DaoException e) {
    		_logger.error("Could not delete ontology term with id="+ontologyTermId+" for user "+userLogin);
    		throw new ComputeException("Could not delete ontology term", e);
    	}
    }
   
    public Entity cloneEntityTree(Long sourceRootId, String targetUserLogin, String targetRootName) throws ComputeException {

    	Entity sourceRoot = getEntityById(sourceRootId.toString());    	
    	if (sourceRoot == null) {
    		throw new DaoException("Cannot find the source root.");
    	}
    	
        User targetUser = _annotationDAO.getUserByName(targetUserLogin);
        if (targetUser == null) {
            throw new DaoException("Cannot find the target user.");
        }

        try {
            Entity cloned = cloneEntityTree(sourceRoot, targetUser, targetRootName, true);
            return cloned;
        }
        catch (Exception e) {
            _logger.error("Error cloning ontology ("+sourceRoot.getId()+")",e);
            throw new ComputeException("Error cloning ontology",e);
        }
    }

	// TODO: detect cycles
    private Entity cloneEntityTree(Entity source, User targetUser, String targetName, boolean isRoot) throws DaoException {

        EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
        
        // Create new ontology element
        Entity newOntologyElement = new Entity(null, targetName, targetUser, null, source.getEntityType(), new Date(), new Date(), null);
        _annotationDAO.saveOrUpdate(newOntologyElement);

        // Add the children 
    	for(EntityData ed : source.getEntityData()) {

    		// Never clone "Is Public" attributes. Entities are cloned privately. 
    		if (ed.getEntityAttribute().getId().equals(tmpAttr.getId())) continue;
    		
    		Entity newChildEntity = null;
    		Entity childEntity = ed.getChildEntity();
    		if (childEntity != null) {
    			newChildEntity = cloneEntityTree(childEntity, targetUser, childEntity.getName(), false);	
    		}

            EntityData newEd = new EntityData(null, ed.getEntityAttribute(), newOntologyElement, newChildEntity,
            		targetUser, ed.getValue(), new Date(), new Date(), ed.getOrderIndex());

            _annotationDAO.saveOrUpdate(newEd);
    	}

    	return newOntologyElement;
    }
    
    public Entity publishOntology(Long sourceRootId, String targetRootName) throws ComputeException {

    	Entity sourceRoot = getEntityById(sourceRootId.toString());    	
    	if (sourceRoot == null) {
    		throw new DaoException("Cannot find the source root.");
    	}
    	
        try {
            
            Entity clonedEntity = cloneEntityTree(sourceRoot, sourceRoot.getUser(), targetRootName, true);

            // Add the public tag
            EntityData publicEd = newData(clonedEntity, EntityConstants.ATTRIBUTE_IS_PUBLIC, sourceRoot.getUser());
            publicEd.setValue("true");
            _annotationDAO.saveOrUpdate(publicEd);
            
            return clonedEntity;
        }
        catch (Exception e) {
            _logger.error("Error publishing ontology ("+sourceRoot.getId()+")",e);
            throw new ComputeException("Error publishing ontology",e);
        }
    }

    public Entity getOntologyTree(String userLogin, Long id) throws ComputeException {

        try {
            Entity root = getEntityById(id.toString());
            if (root == null) return null;

            if (!root.getUser().getUserLogin().equals(userLogin)) {
            	if (root.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PUBLIC) == null) {
            		throw new DaoException("User '"+userLogin+"' does not have access to this private ontology");
            	}
            }
            
            return populateDescendants(root);
        }
        catch (Exception e) {
            _logger.error("Error getting ontology tree",e);
            throw new ComputeException("Error getting ontology tree", e);
        }
    }
    
    public List<Entity> getPublicOntologies() throws ComputeException {

        try {
            EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
            EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
            
            StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
            hql.append(" where clazz.entityType.id=?");
            hql.append(" and exists (from EntityData as attr where attr.parentEntity = clazz and attr.entityAttribute.id = ?)");
            Query query = _annotationDAO.getCurrentSession().createQuery(hql.toString());
            query.setLong(0, tmpType.getId());
            query.setLong(1, tmpAttr.getId());
            return query.list();
        }
        catch (Exception e) {
            _logger.error("Error getting public ontologies",e);
            throw new ComputeException("Error getting public ontologies",e);
        }
    }
    
    public List<Entity> getPrivateOntologies(String userLogin) throws ComputeException {
    	
        try {
            EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
            EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
            
            StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
            hql.append(" where clazz.entityType.id=?");
            if (null != userLogin) {
                hql.append(" and clazz.user.userLogin=?");
            }
            hql.append(" and not exists (from EntityData as attr where attr.parentEntity = clazz and attr.entityAttribute.id = ?)");
            Query query = _annotationDAO.getCurrentSession().createQuery(hql.toString());
            query.setLong(0, tmpType.getId());
            query.setString(1, userLogin);
            query.setLong(2, tmpAttr.getId());
            return query.list();
        }
        catch (Exception e) {
            _logger.error("Error getting private ontologies for "+userLogin,e);
            throw new ComputeException("Error getting private ontologies for "+userLogin,e);
        }
    }

	public Entity createOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException {

        try {
        	// TODO: enable this sanity check
//        	Entity targetEntity = getEntityById(targetEntityId);
//        	if (targetEntity == null) {
//        		throw new IllegalArgumentException("Target entity with id="+targetEntityId+" not found");
//        	}
        	
            User tmpUser = _computeDAO.getUserByName(userLogin);
        	if (tmpUser == null) {
        		throw new IllegalArgumentException("User "+userLogin+" not found");
        	}
        	
        	String tag = (annotation.getValueString() == null) ? 
        				annotation.getKeyString() : 
        				annotation.getKeyString() + " = " + annotation.getValueString();
            
            Entity newAnnotation = newEntity(EntityConstants.TYPE_ANNOTATION, tag, tmpUser);
            
            Set<EntityData> eds = new HashSet<EntityData>();
            newAnnotation.setEntityData(eds);
            
			// Add the target id
			EntityData targetIdData = newData(newAnnotation, 
					EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID, tmpUser);
			targetIdData.setValue(""+annotation.getTargetEntityId());
			eds.add(targetIdData);
				
			// Add the key string
			EntityData keyData = newData(newAnnotation,
					EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM, tmpUser);
			keyData.setValue(annotation.getKeyString());
			eds.add(keyData);

			// Add the value string
			EntityData valueData = newData(newAnnotation,
					EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, tmpUser);
			valueData.setValue(annotation.getValueString());
			eds.add(valueData);
			
			// Add the key entity
            if (annotation.getKeyEntityId() != null) {
            	Entity keyEntity = getEntityById(annotation.getKeyEntityId());
				EntityData keyEntityData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID, tmpUser);
				keyEntityData.setChildEntity(keyEntity);
				eds.add(keyEntityData);
            }

			// Add the value entity
            if (annotation.getValueEntityId() != null) {
            	Entity valueEntity = getEntityById(annotation.getValueEntityId());
				EntityData valueEntityData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID, tmpUser);
				valueEntityData.setChildEntity(valueEntity);
				eds.add(valueEntityData);
            }
            
			// Add the session id
            if (annotation.getSessionId() != null) {
				EntityData sessionIdData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID, tmpUser);
				sessionIdData.setValue(""+annotation.getSessionId());
				eds.add(sessionIdData);
            }
            
            _annotationDAO.saveOrUpdate(newAnnotation);
            
            return newAnnotation;
        }
        catch (Exception e) {
            _logger.error("Error creating ontology annotation for user "+userLogin,e);
            throw new ComputeException("Error creating ontology annotation for user "+userLogin,e);
        }
	}

	public void removeOntologyAnnotation(String userLogin, long annotationId) throws ComputeException {
        try {
            _annotationDAO.removeOntologyAnnotation(userLogin, annotationId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to removeAnnotation");
            throw new ComputeException("Error trying to removeAnnotation",e);
        }
	}
	
	public void removeAllOntologyAnnotationsForSession(String userLogin, long sessionId) throws ComputeException {
        try {
            _annotationDAO.removeAllOntologyAnnotationsForSession(userLogin, sessionId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to removeAllOntologyAnnotationsForSession");
            throw new ComputeException("Error trying to removeAllOntologyAnnotationsForSession",e);
        }
	}
	
    public Entity saveOrUpdateEntity(Entity entity) throws ComputeException {
        try {
            _annotationDAO.saveOrUpdate(entity);
            return entity;
        } catch (DaoException e) {
            _logger.error("Error trying to save or update Entity");
            throw new ComputeException("Error trying to save or update Entity",e);
        }
    }

    public EntityData saveOrUpdateEntityData(EntityData newData) throws ComputeException {
        try {
            _annotationDAO.saveOrUpdate(newData);
            return newData;
        } catch (DaoException e) {
            _logger.error("Error trying to save or update EntityData");
            throw new ComputeException("Error trying to save or update EntityData",e);
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
    
    public Entity getEntityTree(Long id) {
    	return populateDescendants(getEntityById(id.toString()));
    }
    
    public Entity getCachedEntityTree(Long id) {
    	Entity entity = entityTrees.get(id);

    	if (entity == null) {
    		entity = getEntityTree(id);
    		entityTrees.put(id, entity);
    		_logger.info("Caching entity with id="+id);
    	}

    	_logger.info("Returning cached entity tree for id="+id);
    	
    	return entity;
    }
    
    public Entity getUserEntityById(String userLogin, long entityId) {
        try {
            return _annotationDAO.getUserEntityById(userLogin, entityId);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of id "+entityId+" for user "+userLogin);
        }
        return null;
    }

    public List<EntityType> getEntityTypes() {
        try {
            List<EntityType> returnList = _annotationDAO.getAllEntityTypes();
            _logger.debug("Entity types returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity types");
        }
        return null;
    }

    public List<Entity> getCommonRootEntitiesByTypeName(String entityTypeName) {
        try {
            List<Entity> returnList = new ArrayList<Entity>();
            List<Entity> tmpList = _annotationDAO.getUserEntitiesByTypeName(null, entityTypeName);
            // todo This is a little brute-force and could probably have a better query
            for (Entity entity : tmpList) {
                for (EntityData entityData : entity.getEntityData()) {
                    if (EntityConstants.ATTRIBUTE_COMMON_ROOT.equals(entityData.getEntityAttribute().getName())) {
                        returnList.add(entity);
                        break;
                    }
                }
            }

            _logger.debug("Entities returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName);
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
            _logger.error("Error trying to get the entities of type "+entityTypeName);
        }
        return null;
    }

    public void setupEntityTypes() {
        try {
            _annotationDAO.setupEntityTypes();
        } catch (DaoException e) {
            _logger.error("Error calling annotationDAO.setupEntityTypes()");
        }
    }

    public Set<Entity> getEntitiesByName(String name) {
        try {
            return _annotationDAO.getEntitiesByName(name);
        } catch (DaoException e) {
            _logger.error("Error trying to get EntityType by name = " + name);
        }
        return null;
    }

    public boolean deleteEntityById(Long entityId) {
         try {
            return _annotationDAO.deleteEntityById(entityId);
        } catch (DaoException e) {
            _logger.error("Error trying to get delete Entity id="+entityId);
        }
        return false;
    }
    
    public boolean deleteEntityTree(String userLogin, long entityId) throws ComputeException {
    	try {
            Entity entity = _annotationDAO.getEntityById(""+entityId);
    		_annotationDAO.deleteEntityTree(userLogin, entity);
    		return true;
    	}
        catch (Exception e) {
            _logger.error("Error deleting entity tree for user "+userLogin,e);
            throw new ComputeException("Error deleting entity tree for user "+userLogin,e);
        }
    }
    
    public Set<Entity> getParentEntities(long entityId) {
        try {
            return _annotationDAO.getParentEntities(entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent entities for id="+entityId+" message: "+e.getMessage());
        }
        return null;
    }

    public Set<Entity> getChildEntities(long entityId) {
        try {
            return _annotationDAO.getChildEntities(entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get child entities for id="+entityId+" message: "+e.getMessage());
        }
        return null;
    }
    
    public Set<EntityData> getParentEntityDatas(long entityId) {

        try {
            return _annotationDAO.getParentEntityDatas(entityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to get parent entity data for id="+entityId+" message: "+e.getMessage());
        }
        return null;
    }

    public EntityType getEntityTypeByName(String entityTypeName) {
    	preloadData();
        return entityByName.get(entityTypeName);	
    }
    
    public EntityAttribute getEntityAttributeByName(String attrName) {
    	preloadData();
        return attrByName.get(attrName);	
    }
    
    private Entity newEntity(String entityTypeName, String value, User owner) {
        EntityType tmpType = getEntityTypeByName(entityTypeName);
        return new Entity(null, value, owner, null, tmpType, new Date(), new Date(), null);	
    }
    
    private EntityData newData(Entity parent, String attrName, User owner) {
        EntityAttribute ontologyTypeAttribute = getEntityAttributeByName(attrName);
        return new EntityData(null, ontologyTypeAttribute, parent, null, owner, null, new Date(), new Date(), null);
    }
    
    /**
     * Iterate recursively through all children in the Entity graph in order to preload them.
     * @param entity
     * @return
     */
    private Entity populateDescendants(Entity entity) {
    	for(EntityData ed : entity.getEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child != null) {
    			populateDescendants(child);
    		}
    	}
    	return entity;
    }
}
