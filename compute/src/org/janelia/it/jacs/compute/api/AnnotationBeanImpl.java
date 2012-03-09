package org.janelia.it.jacs.compute.api;

import java.util.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

@Stateless(name = "AnnotationEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 60, timeout = 10000)
public class AnnotationBeanImpl implements AnnotationBeanLocal, AnnotationBeanRemote {
	
    private Logger _logger = Logger.getLogger(this.getClass());
    
    public static final String APP_VERSION = "jacs.version";
    public static final String SEARCH_EJB_PROP = "AnnotationEJB.Name";
    public static final String MDB_PROVIDER_URL_PROP = "AsyncMessageInterface.ProviderURL";

    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    private final SolrDAO _solrDAO = new SolrDAO(_logger);
    
    private static final Map<Long, Entity> entityTrees = Collections.synchronizedMap(new HashMap<Long, Entity>());


    public void removeEntityFromFolder(EntityData folderEntityData) throws ComputeException {
        try {
            _annotationDAO.genericDelete(folderEntityData);
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete folder entity data "+folderEntityData.getId());
            throw new ComputeException("Unexpected error while trying to delete folder entity data "+folderEntityData.getId(),e);
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

    public List<Task> getAnnotationSessionTasks(String username) throws ComputeException {
        try {
             return _annotationDAO.getAnnotationSessions(username);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for entities "+username, e);
            throw new ComputeException("Coud not get annotations for entities ",e);
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
        	return _annotationDAO.getAnnotationsForSession(username, sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations in session "+sessionId, e);
            throw new ComputeException("Coud not get annotations in session",e);
        }
    }
    
    public List<Entity> getEntitiesForAnnotationSession(String username, long sessionId) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesForAnnotationSession(username, sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get entities in session "+sessionId, e);
            throw new ComputeException("Coud not get entities in session",e);
        }
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
    
    public List<Entity> getCategoriesForAnnotationSession(String username, long sessionId) throws ComputeException {
        try {
            return _annotationDAO.getCategoriesForAnnotationSession(username, sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get categories in session "+sessionId, e);
            throw new ComputeException("Coud not get categories in session",e);
        }
    }

    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException {
        try {
            return _annotationDAO.getCompletedEntityIds(sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get completed entity ids in session "+sessionId, e);
            throw new ComputeException("Coud not get completed entities",e);
        }
    }

    public List<Entity> getEntitiesWithFilePath(String filePath) {
        try {
            return _annotationDAO.getEntitiesWithFilePath(filePath);
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
            
            return _annotationDAO.populateDescendants(root);
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
            return _annotationDAO.createOntologyRoot(userLogin, rootName);
        }
        catch (Exception e) {
            _logger.error("Error creating new ontology ("+rootName+") for user "+userLogin,e);
            throw new ComputeException("Error creating new ontology ("+rootName+") for user "+userLogin,e);
        }
    }

    public EntityData createOntologyTerm(String userLogin, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {
        try {
            return _annotationDAO.createOntologyTerm(userLogin, ontologyTermParentId, termName, type, orderIndex);
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
        try {
            return _annotationDAO.cloneEntityTree(sourceRootId, targetUserLogin, targetRootName);
        }
        catch (Exception e) {
            _logger.error("Error cloning ontology ("+sourceRootId+")",e);
            throw new ComputeException("Error cloning ontology",e);
        }
    }

    public Entity publishOntology(Long sourceRootId, String targetRootName) throws ComputeException {
        try {
            return _annotationDAO.publishOntology(sourceRootId, targetRootName);
        }
        catch (Exception e) {
            _logger.error("Error publishing ontology ("+sourceRootId+")",e);
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
            
            return _annotationDAO.populateDescendants(root);
        }
        catch (Exception e) {
            _logger.error("Error getting ontology tree",e);
            throw new ComputeException("Error getting ontology tree", e);
        }
    }
    
    public List<Entity> getPublicOntologies() throws ComputeException {
        try {
            return _annotationDAO.getPublicOntologies();
        }
        catch (Exception e) {
            _logger.error("Error getting public ontologies",e);
            throw new ComputeException("Error getting public ontologies",e);
        }
    }
    
    public List<Entity> getPrivateOntologies(String userLogin) throws ComputeException {
    	if (userLogin == null || "".equals(userLogin)) {
            return new ArrayList<Entity>();
    	}
    	
        try {
            return _annotationDAO.getPrivateOntologies(userLogin);
        }
        catch (Exception e) {
            _logger.error("Error getting private ontologies for "+userLogin,e);
            throw new ComputeException("Error getting private ontologies for "+userLogin,e);
        }
    }

	public Entity createOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException {

        try {
            return _annotationDAO.createOntologyAnnotation(userLogin, annotation);
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
        	entity.setUpdatedDate(new Date());
            _annotationDAO.saveOrUpdate(entity);
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
            return newData;
        } catch (DaoException e) {
            _logger.error("Error trying to save or update EntityData");
            throw new ComputeException("Error trying to save or update EntityData",e);
        }
    }

    public Entity createEntity(String userLogin, String entityTypeName, String entityName) throws ComputeException {
        try {
            return _annotationDAO.createEntity(userLogin, entityTypeName, entityName);
        } catch (DaoException e) {
            _logger.error("Error trying to create entity");
            throw new ComputeException("Error trying to create entity",e);
        }
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws ComputeException {
        try {
            return _annotationDAO.addEntityToParent(parent, entity, index, attrName);
        } catch (DaoException e) {
            _logger.error("Error trying to add entity to parent");
            throw new ComputeException("Error trying to add entity to parent",e);
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
    	return _annotationDAO.populateDescendants(getEntityById(id.toString()));
    }

    public Entity getEntityTreeQuery(Long id) {
        return _annotationDAO.getEntityTreeQuery(id);
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
            _logger.error("Error trying to get the entities of id "+entityId+" for user "+userLogin, e);
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
            _logger.error("Error trying to get the entity types", e);
        }
        return null;
    }
    
    public List<Entity> getCommonRootEntitiesByTypeName(String entityTypeName) {
    	return getCommonRootEntitiesByTypeName(null, entityTypeName);
    }
    
    public List<Entity> getCommonRootEntitiesByTypeName(String userLogin, String entityTypeName) {
        try {
            return _annotationDAO.getUserCommonRoots(userLogin, entityTypeName);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeName, e);
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

    public void setupEntityTypes() {
        try {
            _annotationDAO.setupEntityTypes();
        } catch (DaoException e) {
            _logger.error("Error calling annotationDAO.setupEntityTypes()",e);
        }
    }

    public Set<Entity> getEntitiesByName(String name) {
        try {
            return _annotationDAO.getEntitiesByName(name);
        } catch (DaoException e) {
            _logger.error("Error trying to get EntityType by name = " + name, e);
        }
        return null;
    }

    public boolean deleteEntityById(Long entityId) {
         try {
            return _annotationDAO.deleteEntityById(entityId);
        } catch (DaoException e) {
            _logger.error("Error trying to get delete Entity id="+entityId, e);
        }
        return false;
    }
    
    public boolean deleteEntityTree(String userLogin, long entityId) throws ComputeException {
    	try {
            Entity entity = _annotationDAO.getEntityById(""+entityId);
            if (entity==null) {
            	throw new Exception("Entity not found: "+entityId);
            }
    		_annotationDAO.deleteEntityTree(userLogin, entity);
    		return true;
    	}
        catch (Exception e) {
            _logger.error("Error deleting entity tree for user "+userLogin,e);
            throw new ComputeException("Error deleting entity tree for user "+userLogin,e);
        }
    }

    public boolean deleteSmallEntityTree(String userLogin, long entityId) throws ComputeException {
        try {
            Entity entity = _annotationDAO.getEntityById(""+entityId);
            if (entity==null) {
                throw new Exception("Entity not found: "+entityId);
            }
            _annotationDAO.deleteSmallEntityTree(userLogin, entity, true, false, 0);
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

    public List<Long> getPathToRoot(Long entityId, Long rootId) throws ComputeException {

    	try {
    		return _annotationDAO.getPathToRoot(entityId, rootId);
    	}
    	catch (DaoException e) {
            _logger.error("Error searching tree rooted at "+rootId+" for "+entityId,e);
    		throw new ComputeException("Error searching tree rooted at "+rootId+" for "+entityId,e);
    	}
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

    public void indexAllEntities(boolean clearIndex) throws ComputeException {
    	try {
    		if (clearIndex) {
    			_annotationDAO.clearIndex();
    		}
    		_annotationDAO.indexAllEntities();
    	}
    	catch (DaoException e) {
            _logger.error("Error indexing all entities",e);
    		throw new ComputeException("Error indexing all entities",e);
    	}
    }

    public List<Entity> searchEntities(String username, Long rootId, String queryString, Integer start, Integer rows) throws ComputeException {
    	List<Long> ids = new ArrayList<Long>();
    	for(Map doc : search(username, rootId, queryString, start, rows)) {
    		String idStr = (String)doc.get("id");
    		try {
    			if (idStr!=null) {
    				Long id = new Long(idStr);
    				if (id!=null) ids.add(id);
    			}
    		} 
    		catch (NumberFormatException e) {
    			_logger.warn("Error parsing id from index: "+idStr);
    			continue;
    		}
    	}
    	return getEntitiesById(ids);
    }
    
    public List<Map> search(String username, Long rootId, String queryString, Integer start, Integer rows) throws ComputeException {
    	
    	if (queryString==null || "".equals(queryString)) return new ArrayList<Map>();
    	
    	StringBuffer query = new StringBuffer();
    	
    	query.append("(username:system");
    	if (username!=null) {
    		query.append(" OR username:"+username);
    	}
    	query.append(") ");
    	
    	if (rootId != null) {
    		query.append("AND (ancestor_ids:"+rootId+") ");
    	}
    	query.append("AND "+queryString);
    	return search(query.toString(), start, rows);
    }
    
    private List<Map> search(String queryString, Integer start, Integer rows) throws ComputeException {
    	try {
    		SolrDocumentList docs = _solrDAO.search(queryString, start, rows);
    		List<Map> list = new ArrayList<Map>();
    		
    		Iterator<SolrDocument> i = docs.iterator();
    		while (i.hasNext()) {
    			SolrDocument doc = i.next();
    			Map docMap = new HashMap<String,Object>(doc);
    			list.add(docMap);
    		}
    		return list;
    	}
    	catch (DaoException e) {
            _logger.error("Error searching index",e);
    		throw new ComputeException("Error searching index",e);
    	}
    }
    
    public EntityType getEntityTypeByName(String entityTypeName) {
		return _annotationDAO.getEntityTypeByName(entityTypeName);
    }
    
    public EntityAttribute getEntityAttributeByName(String attrName) {
    	return _annotationDAO.getEntityAttributeByName(attrName);
    }
    
}
