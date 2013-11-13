package org.janelia.it.jacs.compute.access.neo4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.neo4j.rest.NodeResult;
import org.janelia.it.jacs.compute.access.neo4j.rest.QueryDefinition;
import org.janelia.it.jacs.compute.access.neo4j.rest.QueryResults;
import org.janelia.it.jacs.compute.access.neo4j.rest.RelationshipResult;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.service.fly.MaskSampleAnnotationService;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityStatus;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.Category;
import org.janelia.it.jacs.model.ontology.types.EnumText;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;
import org.janelia.it.jacs.shared.annotation.RelativePatternAnnotationDataManager;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

import com.google.common.collect.ImmutableSet;

public class Neo4jEntityDAO extends Neo4jDAO implements AbstractEntityLoader {
	
    private final AnnotationDAO annotationDao;
    
    public Neo4jEntityDAO(Logger logger, AnnotationDAO annotationDao) {
        super(logger);
        this.annotationDao = annotationDao;
    }

    public boolean deleteAnnotationSession(String owner, String uniqueIdentifier) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public List<Entity> getEntitiesWithFilePath(String filePath) {
        throw new UnsupportedOperationException();
    }

    public Entity getEntityById(String subjectKey, Long entityId) throws DaoException {
    	try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getEntityById(subjectKey="+subjectKey+", entityId="+entityId+")");
            }
            
            List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
    
            QueryDefinition query = new QueryDefinition(
                    "start e=node:entity(entity_id={entityId}) match e<-[r?:PERMISSION]-s where (e.owner_key! in [{subjectKeyList}] or s.subject_key! in [{subjectKeyList}]) return distinct e");
                    
            query.addParam("entityId", entityId);
            query.addParam("subjectKeyList", subjectKeyList);
            
            QueryResults results = getCypherResults(query, NodeResult.class);
            for(Entity entity : results.getEntityResults()) {
                return entity;
            }
            return null;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Entity getEntityByEntityDataId(String subjectKey, Long entityDataId) {
        throw new UnsupportedOperationException();
    }

    public Entity getEntityById(Long targetId) throws DaoException {
    	return getEntityById(null, targetId);
    }

    public Set<EntityActorPermission> getFullPermissions(Entity entity) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public Set<EntityActorPermission> getFullPermissions(String subjectKey, Long entityId) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public EntityActorPermission grantPermissions(String subjectKey, Long entityId, String granteeKey, String permissions, 
    		boolean recursive) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public EntityActorPermission grantPermissions(final Entity rootEntity, final String granteeKey, 
            final String permissions, boolean recursive) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public void revokePermissions(String subjectKey, Long entityId, String revokeeKey, boolean recursive) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public void revokePermissions(final Entity rootEntity, final String rootOwner, final String revokeeKey, boolean recursive) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public boolean deleteOntologyTerm(String subjectKey, String ontologyTermId) throws DaoException {
        throw new UnsupportedOperationException();
    }

    /**
     * Delete the given entities and all children entities underneath it. Only deletes entities belonging to the given
     * owner. 
     * @param owner The owner to operate as.
     * @param entity The "root" entity at which to begin recursive deletion.
     * @throws DaoException
     */
    public void deleteEntityTree(String owner, Entity entity) throws DaoException {
    	deleteSmallEntityTree(owner, entity);
    }

    public void deleteSmallEntityTree(String subjectKey, Entity entity) throws DaoException {
    	deleteSmallEntityTree(subjectKey, entity, false);
    }
    
    public void deleteSmallEntityTree(String subjectKey, Entity entity, boolean unlinkMultipleParents) throws DaoException {
    	deleteSmallEntityTree(subjectKey, entity, unlinkMultipleParents, 0, new HashSet<Long>());
    }
    
    private void deleteSmallEntityTree(String subjectKey, Entity entity, boolean unlinkMultipleParents, int level, Set<Long> deleted) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public int bulkUpdateEntityDataValue(String oldValue, String newValue) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public int bulkUpdateEntityDataPrefix(String oldPrefix, String newPrefix) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    protected void createEntityStatus(String name) {
        throw new UnsupportedOperationException();
    }
    
    public Collection<Entity> getEntitiesByName(String subjectKey, String entityName) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getEntitiesByName(subjectKey="+subjectKey+",entityId="+entityName+")");  
            }

            List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);

            QueryDefinition query = new QueryDefinition(
                    "start e=node(*) match e<-[r?:PERMISSION]-s where (e.owner_key! in [{subjectKeyList}] or s.subject_key! in [{subjectKeyList}]) and e.name! = {entityName} return distinct e");
            
//            start e=node(*) match e<-[r?:PERMISSION]-s where (e.owner_key! in ["user:rokickik"] or s.subject_key! in ["user:rokickik"]) and e.name! = "Shared Data" return distinct e
            
            query.addParam("entityName", entityName);
            query.addParam("subjectKeyList", subjectKeyList);
            
            QueryResults results = getCypherResults(query, NodeResult.class);
            return results.getEntityResults();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getEntitiesByTypeName(String subjectKey, String entityTypeName) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getEntitiesByTypeName(subjectKey="+subjectKey+",entityTypeName="+entityTypeName+")");
        	}

            List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
    
            QueryDefinition query = new QueryDefinition(
                    "start e=node:entity() where e.entity_type = {entityType} and e.username in [{subjectKeyList}] return distinct e");
            
            query.addParam("entityType", entityTypeName);
            query.addParam("subjectKeyList", subjectKeyList);
            
            QueryResults results = getCypherResults(query, NodeResult.class);
            return results.getEntityResults();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getEntitiesByNameAndTypeName(subjectKey="+subjectKey+",entityName="+entityName+",entityTypeName=entityTypeName)");
            }

            List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
    
            QueryDefinition query = new QueryDefinition(
                    "start e=node:entity() where e.name = {entityName} and e.entity_type = {entityType} and e.username in [{subjectKeyList}] return distinct e");
            
            query.addParam("entityName", entityName);
            query.addParam("entityType", entityTypeName);
            query.addParam("subjectKeyList", subjectKeyList);
            
            QueryResults results = getCypherResults(query, NodeResult.class);
            return results.getEntityResults();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String typeName, String attrName, String attrValue) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getEntitiesWithAttributeValue(subjectKey="+subjectKey+", typeName="+typeName+", attrName="+attrName+", attrValue="+attrValue+")");
            }
//
//            List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
//    
//            QueryDefinition query = new QueryDefinition(
//                    "start e=node:entity() where e.name = {entityName} and e.entity_type = {entityType} and e.username in [{subjectKeyList}] return distinct e");
//            
//            query.addParam("entityName", entityName);
//            query.addParam("entityType", entityTypeName);
//            query.addParam("subjectKeyList", subjectKeyList);
//            
//            QueryResults results = getCypherResults(query, NodeResult.class);
//            return results.getEntityResults();
//            
            return null;
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws DaoException {
        return getUserEntitiesWithAttributeValue(subjectKey, null, attrName, attrValue);
    }
    
    public Collection<Entity> getUserEntitiesByName(String subjectKey, String entityName) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getUserEntitiesByName(subjectKey="+subjectKey+",entityId="+entityName+")");  
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            if (subjectKey!=null) {
                hql.append("and e.ownerKey = :subjectKey ");
            }
            
            final Session currentSession = null;
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            if (subjectKey!=null) {
                query.setParameter("subjectKey", subjectKey);
            }
            
            return filterDuplicates(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getUserEntitiesByTypeName(String subjectKey, String entityTypeName) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getUserEntitiesByTypeName(subjectKey="+subjectKey+",entityTypeName="+entityTypeName+")");
            }
            
            StringBuilder hql = new StringBuilder(256);

            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.entityType.name = :entityTypeName ");
            if (subjectKey!=null) {
                hql.append("and e.ownerKey = :subjectKey ");
            }

            final Session currentSession = null;
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityTypeName", entityTypeName);
            if (subjectKey!=null) {
                query.setParameter("subjectKey", subjectKey);
            }

            return filterDuplicates(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }


    public List<Entity> getUserEntitiesByNameAndTypeName(String subjectKey, String entityName, String entityTypeName) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getUserEntitiesByNameAndTypeName(subjectKey="+subjectKey+",entityName="+entityName+",entityTypeName=entityTypeName)");
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            hql.append("and e.entityType.name=:entityTypeName ");
            if (null != subjectKey) {
                hql.append("and e.ownerKey = :subjectKey ");
            }
            
            final Session currentSession = null;
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            query.setParameter("entityTypeName", entityTypeName);
            if (null != subjectKey) {
                query.setParameter("subjectKey", subjectKey);
            }
            
            return filterDuplicates(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getUserEntitiesWithAttributeValue(String subjectKey, String typeName, String attrName, String attrValue) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getUserEntitiesWithAttributeValue(subjectKey="+subjectKey+", typeName="+typeName+", attrName="+attrName+", attrValue="+attrValue+")");
            }
            
            Session session = null;
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.entityAttribute.name=:attrName and ed.value like :value ");
            if (typeName != null) {
                hql.append("and ed.parentEntity.entityType.name=:typeName ");
            }
            if (null != subjectKey) {
                hql.append("and ed.parentEntity.ownerKey=:subjectKey ");
            }
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrName);
            query.setString("value", attrValue);
            if (typeName != null) {
                query.setString("typeName", typeName);
            }
            if (null != subjectKey) {
                query.setString("subjectKey", subjectKey);
            }
            return filterDuplicates(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws DaoException {
        return getUserEntitiesWithAttributeValue(subjectKey, null, attrName, attrValue);
    }

    public long getCountUserEntitiesWithAttributeValue(String subjectKey, String typeName, String attrName, String attrValue) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getCountUserEntitiesWithAttributeValue(subjectKey="+subjectKey+", typeName="+typeName+", attrName="+attrName+", attrValue="+attrValue+")");
            }
            
            Session session = null;
            StringBuffer hql = new StringBuffer("select count(ed.parentEntity) from EntityData ed ");
            if (typeName != null) {
                hql.append("join ed.parentEntity.entityType ");
            }
            hql.append("where ed.entityAttribute.name=:attrName and ed.value like :value ");
            if (typeName != null) {
                hql.append("and ed.parentEntity.entityType.name=:typeName ");
            }
            if (null != subjectKey) {
                hql.append("and ed.parentEntity.ownerKey=:subjectKey ");
            }
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrName);
            query.setString("value", attrValue);
            if (typeName != null) {
                query.setString("typeName", typeName);
            }
            if (null != subjectKey) {
                query.setString("subjectKey", subjectKey);
            }
            return (Long)query.list().get(0);
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public long getCountUserEntitiesWithAttributeValue(String subjectKey, String attrName, String attrValue) throws DaoException {
        return getCountUserEntitiesWithAttributeValue(subjectKey, null, attrName, attrValue);
    }
    
    public boolean deleteEntityById(String subjectKey, Long entityId) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public List<Entity> getEntitiesWithTag(String subjectKey, String attrTag) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getCommonRoots(subjectKey="+subjectKey+")");
        	}
        	
        	List<String> subjectKeyList = null;
        	
        	EntityAttribute attr = annotationDao.getEntityAttributeByName(attrTag);
            Session session = null;
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("join fetch e.entityType ");
            hql.append("join e.entityData as ed ");
            hql.append("where ed.entityAttribute.id=:attrName ");
            if (null != subjectKey) {
                hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            hql.append("order by e.id ");
            Query query = session.createQuery(hql.toString());
            query.setLong("attrName", attr.getId());
            if (null != subjectKey) {
                subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filterDuplicates(query.list());
            
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public Entity getCommonRootFolderByName(String subjectKey, String folderName, boolean createIfNecessary) throws DaoException {

    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getCommonRootFolderByName(subjectKey="+subjectKey+",folderName="+folderName+",createIfNecessary="+createIfNecessary+")");
    	}
    	
    	Entity folder = null;
    	for(Entity entity : getUserEntitiesByNameAndTypeName(subjectKey, folderName, EntityConstants.TYPE_FOLDER)) {
    		if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
    			if (folder!=null) {
    				throw new IllegalStateException("Multiple common roots owned by "+subjectKey+" with name: "+folderName);
    			}
    			folder = entity;
    		}
    	}
    	
    	if (folder!=null) {
    		return folder;
    	}
    	
        if (createIfNecessary) {
            _logger.info("Creating new topLevelFolder with name=" + folderName);
            folder = createEntity(subjectKey, EntityConstants.TYPE_FOLDER, folderName);
            EntityUtils.addAttributeAsTag(folder, EntityConstants.ATTRIBUTE_COMMON_ROOT);
            saveOrUpdate(folder);
            _logger.info("Saved top level folder as " + folder.getId());
        }
    	
        return folder;
    }
    
    public Entity getChildFolderByName(String subjectKey, Long parentId, String folderName, boolean createIfNecessary) throws DaoException {

    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getChildFolderByName(subjectKey="+subjectKey+",parentId="+parentId+",folderName="+folderName+",createIfNecessary="+createIfNecessary+")");
    	}
    	
    	Entity parent = getEntityById(subjectKey, parentId);
    	if (parent==null) {
    		throw new IllegalArgumentException("Parent folder does not exist: "+parentId);
    	}
    	
    	for(Entity child : parent.getChildren()) {
    		if (child.getName().equals(folderName)) {
    			return child;
    		}
    	}
    	
    	Entity folder = null;
        if (createIfNecessary) {
            _logger.info("Creating new child folder with name=" + folderName);
            folder = createEntity(subjectKey, EntityConstants.TYPE_FOLDER, folderName);
            addEntityToParent(parent, folder, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        }
    	
        return folder;
    }
    
    public Set<EntityData> getParentEntityDatas(String subjectKey, Long childEntityId) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getParentEntityDatas(subjectKey="+childEntityId+", childEntityId="+childEntityId+")");
        	}
        	
            Session session = null;
            StringBuffer hql = new StringBuffer("select ed from EntityData ed ");
            hql.append("join fetch ed.entityAttribute ");
            hql.append("join fetch ed.childEntity ");
            hql.append("join fetch ed.childEntity.entityType ");
            hql.append("join fetch ed.parentEntity ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.childEntity.id=?");
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, childEntityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Set<Long> getParentIdsForAttribute(String subjectKey, Long childEntityId, String attributeName) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getParentIdsForAttribute(childEntityId="+childEntityId+",attributeName="+attributeName+")");
        	}
        	
            Session session = null;
            StringBuffer hql = new StringBuffer("select ed.parentEntity.id from EntityData ed ");
            hql.append("left outer join ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.childEntity.id=:childEntityId ");
            hql.append("and ed.entityAttribute.name=:attrName ");
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString());
            query.setLong("childEntityId", childEntityId);
            query.setString("attrName", attributeName);
            if (null != subjectKey) {
                List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Set<Entity> getParentEntities(String subjectKey, Long entityId) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getParentEntities(subjectKey="+subjectKey+", entityId="+entityId+")");
        	}
        	
            Session session = null;
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.childEntity.id=? ");
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Set<Entity> getChildEntities(String subjectKey, Long entityId) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getChildEntities(subjectKey="+subjectKey+", entityId="+entityId+")");
        	}
        	
            Session session = null;
            StringBuffer hql = new StringBuffer("select ed.childEntity from EntityData ed ");
            hql.append("left outer join fetch ed.childEntity.entityActorPermissions p ");
            hql.append("join fetch ed.childEntity.entityType ");
            hql.append("where ed.parentEntity.id=? ");
            if (null != subjectKey) {
                hql.append("and (ed.childEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public Map<Long, String> getChildEntityNames(Long entityId) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getChildEntityNames(entityId="+entityId+")");
        	}
        	Map<Long,String> nameMap = new LinkedHashMap<Long,String>();
            Session session = null;
            StringBuffer hql = new StringBuffer("select ed.childEntity.id, ed.childEntity.name from EntityData ed ");
            hql.append("where ed.parentEntity.id=? ");
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            List results = query.list();
            for(Object o : results) {
            	Object[] row = (Object[])o;
            	nameMap.put((Long)row[0],(String)row[1]);
            }
            return nameMap;
        } catch (Exception e) {
            throw new DaoException(e);
        }
	}
	
    public List<Task> getAnnotationSessions(String subjectKey) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public List<Entity> getAnnotationsForChildren(String subjectKey, Long entityId) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getAnnotationsForChildren(subjectKey="+subjectKey+",entityId="+entityId+")");
        	}
            
            Session session = null;
            StringBuffer hql = new StringBuffer("select distinct targetEd.parentEntity from EntityData targetEd, EntityData childEd ");
            hql.append("join fetch targetEd.parentEntity.entityType ");
            hql.append("left outer join fetch targetEd.parentEntity.entityActorPermissions p ");
            hql.append("where targetEd.entityAttribute.name = :attrName ");	
            hql.append("and childEd.childEntity.id is not null ");
            hql.append("and cast(childEd.childEntity.id as string) = targetEd.value ");
            hql.append("and childEd.parentEntity.id = :entityId ");
            if (null != subjectKey) {
            	hql.append("and (targetEd.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        	query.setLong("entityId", entityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
	            query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filterDuplicates(query.list());
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getAnnotationsByEntityId(String subjectKey, Long entityId) throws DaoException {
    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getAnnotationsByEntityId(subjectKey="+subjectKey+",entityId="+entityId+")");
    	}
    	
    	List<Long> entityIds = new ArrayList<Long>();
    	entityIds.add(entityId);
    	return filterDuplicates(getAnnotationsByEntityId(subjectKey, entityIds));
    }
    
    public List<Entity> getAnnotationsByEntityId(String subjectKey, List<Long> entityIds) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getAnnotationsByEntityId(subjectKey="+subjectKey+",entityIds="+entityIds+")");
        	}
        	
        	if (entityIds.isEmpty()) {
        		return new ArrayList<Entity>();
        	}

            List<String> entityIdStrs = new ArrayList<String>();
        	for(Long id : entityIds) {
        		entityIdStrs.add(""+id);
        	}
            
            Session session = null;
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("where ed.entityAttribute.name = :attrName ");
            hql.append("and ed.value in (:entityIds) ");
            if (subjectKey!=null) {
            	hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            hql.append("order by ed.parentEntity.id ");
            
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        	query.setParameterList("entityIds", entityIdStrs);
            if (subjectKey!=null) {
                List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
	            query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filterDuplicates(query.list());
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public List<Entity> getAnnotationsForSession(String subjectKey, long sessionId) throws DaoException {
	    throw new UnsupportedOperationException();
	}

    public List<Entity> getEntitiesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException {
        throw new UnsupportedOperationException();
    }
	
    public List<Entity> getCategoriesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException {
        throw new UnsupportedOperationException();
    }

    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Updates the given session and returns all the annotations within it. 
     * @param sessionId
     * @throws ComputeException
     */
	private List<Entity> updateAnnotationSession(long sessionId) throws ComputeException {
	    throw new UnsupportedOperationException();
	}

    public Entity getErrorOntology() throws ComputeException {

        // TODO: this ontology should be owned by the system user
        List<Entity> list = getEntitiesByNameAndTypeName("group:flylight", "Error Ontology", EntityConstants.TYPE_ONTOLOGY_ROOT);
        if (list.isEmpty()) {
            throw new ComputeException("Cannot find Error Ontology");
        }
        else if (list.size()>1) {
            _logger.warn("Found more than one Error Ontology, using the first one, "+list.get(0).getId());
        }
        return list.get(0);
    }
    
    public Entity createEntity(String subjectKey, String entityTypeName, String entityName) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws DaoException {
        return addEntityToParent(parent, entity, index, attrName, null);
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName, String value) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public void addChildren(String subjectKey, Long parentId, List<Long> childrenIds, String attributeName) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    private void propagatePermissions(Entity parent, Entity child, boolean recursive) throws DaoException {
        for(EntityActorPermission permission : getFullPermissions(parent)) {
            grantPermissions(child, permission.getSubjectKey(), permission.getPermissions(), recursive);    
        }
    }
    
    public Entity createDataSet(String subjectKey, String dataSetName) throws ComputeException {
        throw new UnsupportedOperationException();
    }
    
    public Entity createOntologyRoot(String subjectKey, String rootName) throws ComputeException {
        throw new UnsupportedOperationException();
    }

    public EntityData createOntologyTerm(String subjectKey, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {
        throw new UnsupportedOperationException();
    }
    
    public Entity cloneEntityTree(Long sourceRootId, String targetSubjectKey, String targetRootName) throws ComputeException {

    	Entity sourceRoot = getEntityById(sourceRootId);    	
    	if (sourceRoot == null) {
    		throw new DaoException("Cannot find the source root.");
    	}
    	
        Entity cloned = cloneEntityTree(sourceRoot, targetSubjectKey, targetRootName, true);
        return cloned;
    }

	// TODO: detect cycles
    private Entity cloneEntityTree(Entity source, String targetSubjectKey, String targetName, boolean isRoot) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public Entity publishOntology(Long sourceRootId, String targetRootName) throws ComputeException {
        throw new UnsupportedOperationException();
    }
    
    public void fixInternalOntologyConsistency(Long sourceRootId) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public void buildEnumMap(Entity entity, Map<String,Long> enumMap) {
        throw new UnsupportedOperationException();
    }

    public void updateEnumTexts(Entity entity, Map<String,Long> enumMap) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
	public Entity createOntologyAnnotation(String subjectKey, OntologyAnnotation annotation) throws ComputeException {
        throw new UnsupportedOperationException();
	}

	public Long removeOntologyAnnotation(String subjectKey, long annotationId) throws ComputeException {
        throw new UnsupportedOperationException();
	}

	/**
	 * Removes all annotations in the given session and then returns them.
	 * @param userLogin
	 * @param sessionId
	 * @return
	 * @throws DaoException
	 */
    public List<Entity> removeAllOntologyAnnotationsForSession(String subjectKey, long sessionId) throws DaoException {
        throw new UnsupportedOperationException();
    }

	public List<Entity> getEntitiesInList(String subjectKey, String entityIds) throws DaoException {
		String[] idStrs = entityIds.split("\\s*,\\s*");
		List<Long> ids = new ArrayList<Long>();
		for(String idStr : idStrs) {
			ids.add(new Long(idStr));
		}
		return getEntitiesInList(subjectKey, ids);
	}

	public List<Entity> getEntitiesInList(String subjectKey, List<Long> entityIds) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getEntitiesInList(entityIds="+entityIds+")");
        	}
        	
        	if (entityIds == null || entityIds.isEmpty()) {
        		return new ArrayList<Entity>();
        	}
            Session session = null;
            StringBuffer hql = new StringBuffer("select e from Entity e ");
            hql.append("join fetch e.entityType ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.id in (:ids) ");
            if (null != subjectKey) {
            	hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            Query query = session.createQuery(hql.toString());
            query.setParameterList("ids", entityIds);
            if (null != subjectKey) {
                List<String> subjectKeyList = annotationDao.getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            List<Entity> results = query.list();
            
            // Resort the results in the order that the ids were given
            
            Map<Long,Entity> map = new HashMap<Long,Entity>();
            for(Entity entity : results) {
            	map.put(entity.getId(), entity);
            }
            
            List<Entity> sortedList = new ArrayList<Entity>();
            for(Long entityId : entityIds) {
            	Entity entity = map.get(entityId);
            	if (entity != null) {
            		sortedList.add(entity);
            	}
            }
            
            return sortedList;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
	}

    /**
     * Iterate recursively through all children in the Entity graph in order to preload them.
     * @param entity
     * @return
     */
    public Set<Long> getDescendantIds(String subjectKey, Entity entity) {
        Set<String> subjectKeys = annotationDao.getSubjectKeySet(subjectKey);
        Set<Long> visited = new HashSet<Long>();
        getDescendantIds(subjectKeys, entity, visited);
        return visited;
    }
    
    private void getDescendantIds(Set<String> subjectKeys, Entity entity, Set<Long> visited) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("getDescendantIds(subjectKeys="+subjectKeys+",entity.id="+entity.getId()+")");
        }
        
        if (entity == null) return;
        if (subjectKeys!=null && !subjectKeys.contains(entity.getOwnerKey())) return;
        if (visited.contains(entity.getId())) return;
        visited.add(entity.getId());
        
        for(EntityData ed : entity.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null) {
                getDescendantIds(subjectKeys, child, visited);
            }
        }
    }
    
    public Entity getEntityAndChildren(String subjectKey, Long entityId) throws DaoException {
    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getEntityAndChildren(entityId="+entityId+")");
    	}
    	Entity parent = getEntityById(subjectKey, entityId);
        if (parent == null)
            return null;
        for (EntityData ed : parent.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                String childName = child.getName(); // forces load of attributes but not subchild entities
            }
        }
        return parent;
    }

    /**
     * Searches the tree for the given entity and returns its ancestor of a given type, or null if the entity or 
     * ancestor does not exist.
     * @param entity
     * @param type
     * @return
     */
    public Entity getAncestorWithType(String subjectKey, Long entityId, String type) throws DaoException {

        if (_logger.isDebugEnabled()) {
            _logger.debug("getAncestorWithType(entity.getId()="+entityId+",type="+type+")");
        }
        
    	return getAncestorWithType(subjectKey, entityId, type, true);
    }
    
    private Entity getAncestorWithType(String subjectKey, Long entityId, String type, boolean start) throws DaoException {

        Entity entity = getEntityById(entityId);
        // Do not return the starting node as the ancestor, even if type matches
        if (!start && entity.getEntityType().getName().equals(type)) return entity;
        
        for(Entity parent : getParentEntities(subjectKey, entityId)) {
            Entity ancestor = getAncestorWithType(subjectKey, parent.getId(), type, false);
            if (ancestor != null) return ancestor;
        }
        
        return null;
    }

    public List<List<Entity>> getEntityPathsToRoots(String subjectKey, Long entityId) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public List<List<EntityData>> getEntityDataPathsToRoots(String subjectKey, EntityData entityData) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public List<Long> getPathToRoot(String subjectKey, Long entityId, Long rootId) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public List<MappedId> getProjectedResults(String subjectKey, List<Long> entityIds, List<String> upProjection, List<String> downProjection) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public List<Long> getImageIdsWithName(Connection connection, String subjectKey, String imagePath) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public List<Long> getOrphanAnnotationIdsMissingTargets(String subjectKey) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public void loadLazyEntity(String subjectKey, Entity entity, boolean recurse) throws DaoException {

        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            EntityUtils.replaceChildNodes(entity, getChildEntities(subjectKey, entity.getId()));
        }

        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(subjectKey, ed.getChildEntity(), true);
                }
            }
        }
    }

    public Entity annexEntityTree(String subjectKey, Long entityId) throws ComputeException {
        throw new UnsupportedOperationException();
    }
    
    private Entity annexEntityTree(String subjectKey, Entity entity, String indent) throws ComputeException {
        throw new UnsupportedOperationException();
    }
    
    private Entity newEntity(String entityTypeName, String name, String owner) {
        throw new UnsupportedOperationException();
    }

    private Entity newEntity(EntityType entityType, String name, String subjectKey) {
        throw new UnsupportedOperationException();
    }
    
    private EntityData newData(Entity parent, String attrName, String subjectKey) {
        throw new UnsupportedOperationException();
    }
    
    private EntityData newData(Entity parent, EntityAttribute attribute, String subjectKey) {
        throw new UnsupportedOperationException();
    }
    

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws DaoException {
        throw new UnsupportedOperationException();
    }


    public Map<Entity, Map<String, Double>> getMaskQuantifiers(String maskFolderName) throws DaoException {
        throw new UnsupportedOperationException();
    }


    // This method returns two objects,  Map<Long, Map<String, String>> sampleInfoMap, Map<Long, List<Double>> quantifierInfoMap
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws DaoException {
        throw new UnsupportedOperationException();
    }

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public PatternAnnotationDataManager getPatternAnnotationDataManagerByType(String type) throws DaoException {
        throw new UnsupportedOperationException();
    }

    public Entity saveBulkEntityTree(Entity root) throws DaoException {
    	if (root.getOwnerKey()==null) {
    		throw new IllegalArgumentException("Root entity must specify the owner key");
    	}
    	return saveBulkEntityTree(root, root.getOwnerKey());
    }
    
    public Entity saveBulkEntityTree(Entity root, String subjectKey) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    protected int getEntitiesInTree(Entity entity, List<Entity> allEntities) throws Exception {
        throw new UnsupportedOperationException();
    }
    
    private List filterDuplicates(List list) {
        return ImmutableSet.copyOf(list).asList();
	}

	@Override
	public Set<EntityData> getParents(Entity entity) throws Exception {
		return getParentEntityDatas(null, entity.getId());
	}

	@Override
	public Entity populateChildren(Entity entity) throws Exception {
    	for(EntityData ed : entity.getEntityData()) {
    		Entity child = ed.getChildEntity(); // Force Hibernate to load the child entity
    	}
    	return entity;
	}

    /**
     * Iterate recursively through all children in the Entity graph in order to preload them.
     * @param entity
     * @return
     */
    public Entity populateDescendants(String subjectKey, Entity entity) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("populateDescendants(subjectKey="+subjectKey+",entity.name="+entity.getName()+")");
        }
        Set<String> subjectKeys = annotationDao.getSubjectKeySet(subjectKey);
        return populateDescendants(subjectKeys, entity, new HashSet<Long>(), "");
    }
    
    private Entity populateDescendants(Set<String> subjectKeys, Entity entity, Set<Long> visited, String indent) {
        if (_logger.isTraceEnabled()) {
            _logger.trace(indent+entity.getName());
        }
        
        if (entity == null) return entity;
        
        if (subjectKeys!=null && !subjectKeys.contains(entity.getOwnerKey())) return entity;
        
        if (visited.contains(entity.getId())) return entity;
        visited.add(entity.getId());

        // Populate descendants
        for(EntityData ed : entity.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null) {
                populateDescendants(subjectKeys, child, visited, indent+" ");
            }
        }
        return entity;
    }
    
    public void deleteAttribute(String ownerKey, String attributeName) throws DaoException {
        throw new UnsupportedOperationException();
    }
    
    public Entity createAlignmentBoard(String subjectKey, String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws ComputeException {
        throw new UnsupportedOperationException();
    }

    public Object genericGet(Class c, Long id) {
        return null;
    }

    public Object genericLoad(Class c, Long id) throws DaoException {
        try {
            return null;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void genericSave(Object object) throws DaoException {
        try {
//            getCurrentSession().saveOrUpdate(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public void genericDelete(Object object) throws DaoException {
        try {
//            getCurrentSession().delete(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Object genericCreateAndReturn(Object object) throws DaoException {
        try {
//            getCurrentSession().save(object);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
        return object;
    }

    public void saveOrUpdate(Object item) throws DaoException {
        try {
//            getCurrentSession().saveOrUpdate(item);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
}
