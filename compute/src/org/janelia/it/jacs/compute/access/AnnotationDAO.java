package org.janelia.it.jacs.compute.access;

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

public class AnnotationDAO extends ComputeBaseDAO implements AbstractEntityLoader {
	
	/** Batch fetch size for JDBC result sets */
	protected final static int JDBC_FETCH_SIZE = 200;
	
    private static final Map<String, EntityType> entityByName = Collections.synchronizedMap(new HashMap<String, EntityType>());
    private static final Map<String, EntityAttribute> attrByName = Collections.synchronizedMap(new HashMap<String, EntityAttribute>());

    public AnnotationDAO(Logger logger) {
        super(logger);
    }

    private void preloadData() {
        try {
            if (entityByName.isEmpty()) {
                for(EntityType entityType : getAllEntityTypes()) {
                    entityByName.put(entityType.getName(), entityType);
                }
            }

            if (attrByName.isEmpty()) {
                for(EntityAttribute entityAttr : getAllEntityAttributes()) {
                    attrByName.put(entityAttr.getName(), entityAttr);
                }
            }
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying preload metamodel", e);
        }
    }

    public List<User> getAllUsers() throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select u from User u order by u.id");
            Query query = session.createQuery(hql.toString());
            return query.list();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    // This should be more generic!  Pass the Entity class and that should be it
    public boolean deleteAnnotationSession(String owner, String uniqueIdentifier) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(AnnotationSessionTask.class);
            c.add(Expression.eq("id", Long.valueOf(uniqueIdentifier)));
            AnnotationSessionTask tmpAnnotation = (AnnotationSessionTask) c.uniqueResult();
            if (null == tmpAnnotation) {
                // This should never happen
                throw new DaoException("Cannot complete deletion when there are more than one annotation sessions with that identifier.");
            }
            if (!tmpAnnotation.getOwner().equals(owner)) {
                throw new DaoException("Cannot delete the annotation session as the requestor doesn't own the annotation.");
            }
            session.delete(tmpAnnotation);
            _logger.debug("The annotation session has been deleted.");
            return true;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getEntitiesWithFilePath(String filePath) {
    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getEntitiesWithFilePath(filePath="+filePath+")");	
    	}
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select clazz.parentEntity from EntityData clazz where value=?");
        Query query = session.createQuery(hql.toString()).setString(0, filePath);
        return filterDuplicates(query.list());
    }

    public EntityType getEntityType(Long entityTypeConstant) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(EntityType.class);
        c.add(Expression.eq("id", entityTypeConstant));
        EntityType entityType = (EntityType) c.uniqueResult();
        if (null != entityType) {
            return entityType;
        }
        return null;
    }
    
    public Entity getEntityById(String subjectKey, Long entityId) {
    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getEntityById(subjectKey="+subjectKey+", entityId="+entityId+")");
    	}

        StringBuilder hql = new StringBuilder();
        hql.append("select e from Entity e ");
        hql.append("left outer join fetch e.entityActorPermissions p ");
        hql.append("where e.id = :entityId ");
        if (null != subjectKey) {
        	hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
        }
        
        final Session currentSession = getCurrentSession();
        Query query = currentSession.createQuery(hql.toString());
        query.setParameter("entityId", entityId);
        if (null != subjectKey) {
            List<String> subjectKeyList = getSubjectKeys(subjectKey);
            query.setParameterList("subjectKeyList", subjectKeyList);
        }

        return (Entity)query.uniqueResult();
    }

    public Entity getEntityByEntityDataId(String subjectKey, Long entityDataId) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("getEntityByEntityDataId(subjectKey="+subjectKey+", entityDataId="+entityDataId+")");
        }

        StringBuilder hql = new StringBuilder();
        hql.append("select ed.parentEntity from EntityData ed ");
        hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
        hql.append("where ed.id = :entityDataId ");
        if (null != subjectKey) {
            hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
        }
        
        final Session currentSession = getCurrentSession();
        Query query = currentSession.createQuery(hql.toString());
        query.setParameter("entityDataId", entityDataId);
        if (null != subjectKey) {
            List<String> subjectKeyList = getSubjectKeys(subjectKey);
            query.setParameterList("subjectKeyList", subjectKeyList);
        }

        return (Entity)query.uniqueResult();
    }

    public Entity getEntityById(Long targetId) {
    	return getEntityById(null, targetId);
    }

    public Set<EntityActorPermission> getFullPermissions(Entity entity) throws DaoException {
        if (EntityUtils.isInitialized(entity.getEntityActorPermissions())) {
            return entity.getEntityActorPermissions();
        }
        return getFullPermissions(entity.getOwnerKey(), entity.getId());
    }
    
    public Set<EntityActorPermission> getFullPermissions(String subjectKey, Long entityId) throws DaoException {
        
        Entity entity = getEntityById(entityId);
        if (entity==null) {
            throw new IllegalArgumentException("Unknown entity: "+entityId);
        }
        if (subjectKey!=null) {
            List<String> subjectKeyList = getSubjectKeys(subjectKey);
            if (!EntityUtils.hasWriteAccess(entity, subjectKeyList)) {
                throw new DaoException("User "+subjectKey+" does not have the right to view all permissions for "+entity.getId());
            }
        }
        
        return entity.getEntityActorPermissions();
    }
    
    public EntityActorPermission grantPermissions(String subjectKey, Long entityId, String granteeKey, String permissions, 
    		boolean recursive) throws DaoException {
    	
    	Entity entity = getEntityById(entityId);
    	if (entity==null) {
    		throw new IllegalArgumentException("Unknown entity: "+entityId);
    	}

    	if (subjectKey!=null) {
    		if (!subjectKey.equals(entity.getOwnerKey())) {
    			throw new DaoException("User "+subjectKey+" does not have the right to grant access to "+entity.getId());
    		}	
    	}
    	
    	return grantPermissions(entity, granteeKey, permissions, recursive);
    }
    
    public EntityActorPermission grantPermissions(final Entity rootEntity, final String granteeKey, 
            final String permissions, boolean recursive) throws DaoException {

    	Subject subject = getSubjectByNameOrKey(granteeKey);
    	if (subject==null) {
    		throw new IllegalArgumentException("Unknown subject: "+granteeKey);
    	}
    	
    	if (rootEntity.getOwnerKey().equals(granteeKey)) {
    		throw new IllegalArgumentException("Subject already has owner permission");
    	}
    	
    	try {
        	EntityVistationBuilder visitationBuilder = new EntityVistationBuilder(this).startAt(rootEntity);
    		visitationBuilder = recursive ? visitationBuilder.ancestors() : visitationBuilder.root();
        	visitationBuilder.run(new EntityVisitor() {
    			@Override
    			public void visit(Entity entity) throws Exception {
    				EntityActorPermission existingPerm = null;
    		    	for(EntityActorPermission eap : entity.getEntityActorPermissions()) {
    		    		if (eap.getSubjectKey().equals(granteeKey)) {
    		    			existingPerm = eap;
    		    			break;
    		    		}
    		    	}

    		    	if (existingPerm != null) {
		    			if (!existingPerm.getPermissions().equals(permissions)) {
		    				existingPerm.setPermissions(permissions);
		    				saveOrUpdate(existingPerm);
		    				_logger.info("Updated "+permissions+" permission to "+granteeKey+" for "+entity.getId());
		    			}
    		    	}
    		    	else {
	    		    	EntityActorPermission eap = new EntityActorPermission(entity, granteeKey, permissions);
	    		    	entity.getEntityActorPermissions().add(eap);
	    				saveOrUpdate(eap);
	    				_logger.info("Granted "+permissions+" permission to "+granteeKey+" for "+entity.getId());
    		    	}
    			}
        	});
        	
        	// Check if the grantee already has a link to the entity
        	boolean granteeHasLink = false;
        	for(Entity parent : getParentEntities(null, rootEntity.getId())) {
        	    if (EntityUtils.hasReadAccess(parent, getSubjectKeys(granteeKey))) {
        	        granteeHasLink = true;
        	        break;
        	    }
        	}
        	
        	if (!granteeHasLink) {
        	    // Grantee has no link, so expose it in the Shared Data folder
                Entity sharedDataFolder = null;
                List<Entity> entities = getUserEntitiesByNameAndTypeName(granteeKey, EntityConstants.NAME_SHARED_DATA, EntityConstants.TYPE_FOLDER);
                if (entities != null && !entities.isEmpty()) {
                    sharedDataFolder = entities.get(0);
                }
                
                if (sharedDataFolder==null) {
                    sharedDataFolder = createEntity(granteeKey, EntityConstants.TYPE_FOLDER, EntityConstants.NAME_SHARED_DATA);
                    EntityUtils.addAttributeAsTag(sharedDataFolder, EntityConstants.ATTRIBUTE_COMMON_ROOT);
                    EntityUtils.addAttributeAsTag(sharedDataFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
                    saveOrUpdate(sharedDataFolder);
                }
                
                addEntityToParent(sharedDataFolder, rootEntity, sharedDataFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        	}
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    	
    	for(EntityActorPermission eap : rootEntity.getEntityActorPermissions()) {
    		if (eap.getSubjectKey().equals(granteeKey)) {
    			return eap;
    		}
    	}
    	return null;
    }

    public void revokePermissions(String subjectKey, Long entityId, String revokeeKey, boolean recursive) throws DaoException {
    	
    	Entity entity = getEntityById(entityId);
    	if (entity==null) {
    		throw new IllegalArgumentException("Unknown entity: "+entityId);
    	}
    	
    	if (subjectKey!=null) {
    		if (!subjectKey.equals(entity.getOwnerKey())) {
    			throw new DaoException("User "+subjectKey+" does not have the right to grant access to "+entity.getId());
    		}	
    	}
    	
    	revokePermissions(entity, entity.getOwnerKey(), revokeeKey, recursive);
    }
    
    public void revokePermissions(final Entity rootEntity, final String rootOwner, final String revokeeKey, boolean recursive) throws DaoException {
    	
    	Subject subject = getSubjectByNameOrKey(revokeeKey);
    	if (subject==null) {
    		throw new IllegalArgumentException("Unknown subject: "+revokeeKey);
    	}
    	
    	if (rootEntity.getOwnerKey().equals(revokeeKey)) {
    		throw new IllegalArgumentException("Cannot revoke permission from entity owner");
    	}

    	try {
    	    final Set<Long> revokedIds = new HashSet<Long>();
        	EntityVistationBuilder visitationBuilder = new EntityVistationBuilder(this).startAt(rootEntity);
    		visitationBuilder = recursive ? visitationBuilder.ancestors() : visitationBuilder.root();
        	visitationBuilder.run(new EntityVisitor() {
    			@Override
    			public void visit(Entity entity) throws Exception {
    		    	for(Iterator<EntityActorPermission> i = entity.getEntityActorPermissions().iterator(); i.hasNext(); ) {
    		    		EntityActorPermission eap = i.next();
    		    		if (eap.getSubjectKey().equals(revokeeKey)) {
    		    			i.remove();
    		    			genericDelete(eap);
    		    			_logger.info("Revoked permission to "+revokeeKey+" for "+entity.getId());
    		    			revokedIds.add(eap.getEntity().getId());
    		    		}
    		    	}
    			}
        	});

        	Entity sharedDataFolder = null;
        	List<Entity> entities = getUserEntitiesByNameAndTypeName(revokeeKey, EntityConstants.NAME_SHARED_DATA, EntityConstants.TYPE_FOLDER);
        	if (entities != null && !entities.isEmpty()) {
        		sharedDataFolder = entities.get(0);
        	}
        	
        	if (sharedDataFolder!=null) {
        		Set<EntityData> toDelete = new HashSet<EntityData>();
        		for(EntityData ed : sharedDataFolder.getEntityData()) {
        			Entity child = ed.getChildEntity();
        			if (child!=null && revokedIds.contains(child.getId())) {
        				toDelete.add(ed);
        			}
        		}
        		for(EntityData ed : toDelete) {
        		    _logger.info("Removed "+rootEntity.getId()+" from "+revokeeKey+"'s Shared Data");
        			sharedDataFolder.getEntityData().remove(ed);
        			genericDelete(ed);
        		}	
        	}
        	
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public boolean deleteOntologyTerm(String subjectKey, String ontologyTermId) throws DaoException {
        Session session = getCurrentSession();
        try {
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", Long.valueOf(ontologyTermId)));
            Entity entity = (Entity) c.uniqueResult();
            if (null == entity) {
                // This should never happen
                throw new DaoException("Cannot complete deletion when there are no entities with that identifier.");
            }
            if (!entity.getOwnerKey().equals(subjectKey)) {
                throw new DaoException("Cannot delete the entity as the requestor doesn't own the item.");
            }

            _logger.info("Will delete tree rooted at Entity "+entity.getName());
            deleteEntityTree(subjectKey, entity);
            return true;
        }
        catch (Exception e) {
        	_logger.error("Error deleting ontology term "+ontologyTermId,e);
            throw new DaoException(e);
        }
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

    	StringBuffer indent = new StringBuffer();
		for (int i = 0; i < level; i++) {
			indent.append("    ");
		}

		// Null check
    	if (entity == null) {
    		_logger.warn(indent+"Cannot delete null entity");
    		return;
    	}

    	_logger.info(indent+"Deleting entity "+entity.getName()+" (id="+entity.getId()+")");
    	
    	if (deleted.contains(entity.getId())) {
    		_logger.warn(indent+"Cannot delete entity which was already deleted");
    		return;
    	}
        
        List<String> subjectKeys = getSubjectKeys(subjectKey);
        
		// Permission check - can't delete entities that we don't have write access to
    	if (!EntityUtils.hasWriteAccess(entity, subjectKeys)) {
    		_logger.info(indent+"Cannot delete entity because owner ("+entity.getOwnerKey()+") does not match invoker ("+subjectKey+")");
    		return;
    	}

    	// Common root check - can't delete entities that are held in place by virtue of being common roots
    	if (level>0 && entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
    		_logger.info(indent+"Cannot delete "+entity.getName()+" because it is a common root");
    		return;
    	}
    	
    	// Multiple parent check - can't delete entities that we have references to elsewhere
    	Set<EntityData> eds = getParentEntityDatas(null, entity.getId());
    	Set<EntityData> ownedEds = new HashSet<EntityData>();
    	for(EntityData ed : eds) {
    	    if (EntityUtils.isOwner(ed.getParentEntity(), subjectKeys)) {
    	        ownedEds.add(ed);
    	    }
    	}
    	
    	boolean moreThanOneParent = ownedEds.size() > 1;
        if (level>0 && moreThanOneParent && !unlinkMultipleParents) {
        	_logger.info(indent+"Cannot delete "+entity.getName()+" because more than one parent is pointing to it");
        	return;
        }
        
        // Delete all descendants first
        for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
        	Entity child = ed.getChildEntity();
        	if (child != null) {
        		deleteSmallEntityTree(subjectKey, child, unlinkMultipleParents, level + 1, deleted);
        	}
        	// We have to manually remove the EntityData from its parent, otherwise we get this error: 
        	// "deleted object would be re-saved by cascade (remove deleted object from associations)"
        	_logger.debug(indent+"Deleting child entity data (id="+ed.getId()+")");
        	ed.getParentEntity().getEntityData().remove(ed);
        	if (deleted.contains(ed.getId())) {
        		_logger.debug(indent+"EntityData (id="+ed.getId()+") was already deleted in this session");
        		continue;
        	}
    		getCurrentSession().delete(ed);
            deleted.add(ed.getId());
        }
        
        // Delete all parent EDs
        for(EntityData ed : eds) {
			// This ED points to the term to be deleted. We must delete the ED first to avoid violating constraints.
        	_logger.debug(indent+"Deleting parent entity data (id="+ed.getId()+")");
        	ed.getParentEntity().getEntityData().remove(ed);
        	if (deleted.contains(ed.getId())) {
        		_logger.debug(indent+"EntityData (id="+ed.getId()+") was already deleted in this session");
        		continue;
        	}
    		getCurrentSession().delete(ed);
            deleted.add(ed.getId());
        }
        
        // Finally we can delete the entity itself
        getCurrentSession().delete(entity);
        deleted.add(entity.getId());
    }
    
    public EntityAttribute fetchEntityAttributeByName(String name) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(EntityAttribute.class);
            c.add(Expression.eq("name", name));
            // SHould be using uniqueResult
            return (EntityAttribute) c.uniqueResult();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public EntityType fetchEntityTypeByName(String name) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(EntityType.class);
            c.add(Expression.eq("name", name));
            return (EntityType) c.uniqueResult();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public int bulkUpdateEntityDataValue(String oldValue, String newValue) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("bulkUpdateEntityDataValue(oldValue="+oldValue+",newValue="+newValue+")");  
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("update EntityData set value = :newValue where value = :oldValue ");
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("newValue", newValue);
            query.setParameter("oldValue", oldValue);
            
            int rows = query.executeUpdate();
            _logger.debug("Bulk updated entity data value for "+rows+" rows");
            return rows;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public int bulkUpdateEntityDataPrefix(String oldPrefix, String newPrefix) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("bulkUpdateEntityDataPrefix(oldPrefix="+oldPrefix+",newPrefix="+newPrefix+")");  
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("update EntityData ed set ed.value = concat(:newPrefix,substring(ed.value, :prefixOffset)) "); 
            hql.append("where ed.value like :oldPrefix");
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("newPrefix", newPrefix);
            query.setParameter("prefixOffset", oldPrefix.length()+1);
            query.setParameter("oldPrefix", oldPrefix+"%");
                        
            int rows = query.executeUpdate();
            _logger.debug("Bulk updated entity data prefix for "+rows+" rows");
            return rows;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    protected void createEntityStatus(String name) {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(EntityStatus.class);
            List<EntityStatus> entityStatusList = (List<EntityStatus>) c.list();

            boolean containsStatus = false;
            for (EntityStatus status : entityStatusList) {
                if (status.getName().equals(name)) {
                    containsStatus=true;
                }
            }
            if (!containsStatus) {
                EntityStatus entityStatus = new EntityStatus();
                entityStatus.setName(name);
                session.save(entityStatus);
            }
        }
        catch (HibernateException e) {
            e.printStackTrace();
        }
    }
    
    public EntityType createNewEntityType(String name) throws DaoException {
        EntityType entityType = getEntityTypeByName(name);
    	if (entityType != null) {
    		return entityType;
    	}
    	try {
	    	entityType = new EntityType();
	    	entityType.setName(name);
	        saveOrUpdate(entityType);
            _logger.info("Created new EntityType '" + name + "'");
    		entityByName.put(name, entityType);
	        return entityType;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public EntityAttribute addAttributeToEntityType(EntityType entityType, String attrName) throws DaoException  {

        Session session = getCurrentSession();
        EntityAttribute entityAttr = null;
        
    	try {
	    	entityAttr = new EntityAttribute();
	    	entityAttr.setName(attrName);
	        session.saveOrUpdate(entityAttr);
    		attrByName.put(attrName, entityAttr);
            _logger.info("Created new EntityAttribute '" + attrName + "'");
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    	
        return addAttributeToEntityType(entityType, entityAttr);
    }
    
    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException {
    	EntityType entityType = getEntityTypeByName(entityTypeName);
    	if (entityType == null) {
    		throw new ComputeException("Entity type '"+entityTypeName+"' does not exist");
    	}
    	EntityAttribute entityAttr = getEntityAttributeByName(attrName);
    	if (entityAttr != null) {
    		return addAttributeToEntityType(entityType, entityAttr);
    	}
    	else {
    	    return addAttributeToEntityType(entityType, attrName);	
    	}
    }
    
    public EntityAttribute addAttributeToEntityType(EntityType entityType, EntityAttribute entityAttr) throws DaoException  {
    	try {
    	    Session session = getCurrentSession();
    		Set<EntityAttribute> attrs = entityType.getAttributes();
    		if (attrs == null) {
    			attrs = new HashSet<EntityAttribute>();
    			entityType.setAttributes(attrs);
    		}
    		
    		for(EntityAttribute currAttr : attrs) {
    		    if (currAttr.getName().equals(entityAttr.getName())) {
    		        _logger.info("EntityAttribute '" + entityAttr.getName() + "' already exists on EntityType '"+entityType.getName()+"'");
    		        return currAttr;
    		    }
    		}
    		
    		attrs.add(entityAttr);
	        session.saveOrUpdate(entityType);
            _logger.info("Added EntityAttribute '" + entityAttr.getName() + "' to EntityType '"+entityType.getName()+"'");
	        return entityAttr;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }

    public List<EntityType> getAllEntityTypes() throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select et from EntityType et");
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<EntityAttribute> getAllEntityAttributes() throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select ea from EntityAttribute ea");
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    

    public Collection<Entity> getEntitiesByName(String subjectKey, String entityName) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getEntitiesByName(subjectKey="+subjectKey+",entityId="+entityName+")");  
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            if (subjectKey!=null) {
                hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            if (subjectKey!=null) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filterDuplicates(query.list());
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
        	
            StringBuilder hql = new StringBuilder(256);

            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.entityType.name = :entityTypeName ");
            if (null != subjectKey) {
            	hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }

            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityTypeName", entityTypeName);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }

            return filterDuplicates(query.list());
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
            
            StringBuilder hql = new StringBuilder();
            hql.append("select e from Entity e ");
            hql.append("left outer join fetch e.entityActorPermissions p ");
            hql.append("where e.name = :entityName ");
            hql.append("and e.entityType.name=:entityTypeName ");
            if (null != subjectKey) {
                hql.append("and (e.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityName", entityName);
            query.setParameter("entityTypeName", entityTypeName);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filterDuplicates(query.list());
        }
        catch (Exception e) {
            throw handleException(e, "getUserEntitiesByNameAndTypeName");
        }
    }

    public List<Entity> getEntitiesWithAttributeValue(String subjectKey, String typeName, String attrName, String attrValue) throws DaoException {
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getEntitiesWithAttributeValue(subjectKey="+subjectKey+", typeName="+typeName+", attrName="+attrName+", attrValue="+attrValue+")");
            }
            
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.entityAttribute.name=:attrName and ed.value like :value ");
            if (typeName != null) {
                hql.append("and ed.parentEntity.entityType.name=:typeName ");
            }
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrName);
            query.setString("value", attrValue);
            if (typeName != null) {
                query.setString("typeName", typeName);
            }
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
                query.setParameterList("subjectKeyList", subjectKeyList);
            }
            return filterDuplicates(query.list());
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
            
            final Session currentSession = getCurrentSession();
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

            final Session currentSession = getCurrentSession();
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
            
            final Session currentSession = getCurrentSession();
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
            
            Session session = getCurrentSession();
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
            
            Session session = getCurrentSession();
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
        Session session = null;
        Transaction transaction = null;
        try {
            session = getCurrentSession();
            transaction = session.beginTransaction();
            Set<EntityData> parentEds = getParentEntityDatas(subjectKey, entityId);
            for(EntityData parentEd : parentEds) {
                session.delete(parentEd);
            	_logger.debug("The parent entity data with id=" + parentEd.getId() + " has been deleted.");
            }
            Entity entity = getEntityById(subjectKey, entityId);
            if (entity!=null && (subjectKey==null || entity.getOwnerKey().equals(subjectKey))) {
	            session.delete(entity);
	            _logger.debug("The entity with id=" + entityId + " has been deleted.");
            }
            else {
            	throw new DaoException("Could not find entity with id="+entityId+" owned by "+subjectKey);
            }
            transaction.commit();
            return true;
        }
        catch (Exception e) {
        	transaction.rollback();
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getEntitiesWithTag(String subjectKey, String attrTag) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getCommonRoots(subjectKey="+subjectKey+")");
        	}
        	
        	List<String> subjectKeyList = null;
        	
        	EntityAttribute attr = getEntityAttributeByName(attrTag);
            Session session = getCurrentSession();
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
                subjectKeyList = getSubjectKeys(subjectKey);
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
        	
            Session session = getCurrentSession();
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
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
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
        	
            Session session = getCurrentSession();
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
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
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
        	
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.childEntity.id=? ");
            if (null != subjectKey) {
                hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
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
        	
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.childEntity from EntityData ed ");
            hql.append("left outer join fetch ed.childEntity.entityActorPermissions p ");
            hql.append("join fetch ed.childEntity.entityType ");
            hql.append("where ed.parentEntity.id=? ");
            if (null != subjectKey) {
                hql.append("and (ed.childEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            if (null != subjectKey) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
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
            Session session = getCurrentSession();
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
		try {
	        String hql = "select clazz from Task clazz where subclass='" + AnnotationSessionTask.TASK_NAME + "' and clazz.owner='" + subjectKey + "' order by clazz.objectId";
	        Query query = sessionFactory.getCurrentSession().createQuery(hql);
	        return query.list();
	    } catch (Exception e) {
	        throw new DaoException(e);
	    }
    }

    public List<Entity> getAnnotationsForChildren(String subjectKey, Long entityId) throws DaoException {
        try {
        	if (_logger.isDebugEnabled()) {
        		_logger.debug("getAnnotationsForChildren(subjectKey="+subjectKey+",entityId="+entityId+")");
        	}
            
            Session session = getCurrentSession();
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
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
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
            
            Session session = getCurrentSession();
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
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
	            query.setParameterList("subjectKeyList", subjectKeyList);
            }
            
            return filterDuplicates(query.list());
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public List<Entity> getAnnotationsForSession(String subjectKey, long sessionId) throws DaoException {

        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
		
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("left outer join fetch ed.parentEntity.entityActorPermissions p ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.entityAttribute.name = :attrName ");
    		hql.append("and ed.value = :sessionId ");
            if (subjectKey!=null) {
            	hql.append("and (ed.parentEntity.ownerKey in (:subjectKeyList) or p.subjectKey in (:subjectKeyList)) ");
            }
    		hql.append("order by ed.parentEntity.id ");
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
            query.setString("sessionId", ""+sessionId);
            if (subjectKey!=null) {
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
	            query.setParameterList("subjectKeyList", subjectKeyList);
            }
            // TODO: check userLogin if the session is private
            return filterDuplicates(query.list());
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
	}

    public List<Entity> getEntitiesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException {
        
        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
    	
        String entityIds = task.getParameter(AnnotationSessionTask.PARAM_annotationTargets);
        if (entityIds == null || "".equals(entityIds)) {
        	return new ArrayList<Entity>();
        }
        else {
        	List<Entity> entities = getEntitiesInList(subjectKey, entityIds);	
        	for(Entity entity : entities) {
        		populateDescendants(entity.getOwnerKey(), entity);
        	}
        	return filterDuplicates(entities);
        }
    }
	
    public List<Entity> getCategoriesForAnnotationSession(String subjectKey, long sessionId) throws ComputeException {
        
        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
    	
        String entityIds = task.getParameter(AnnotationSessionTask.PARAM_annotationCategories);
        if (entityIds == null || "".equals(entityIds)) {
        	return new ArrayList<Entity>();
        }
        else {
        	return filterDuplicates(getEntitiesInList(subjectKey, entityIds));	
        }
    }

    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException {
    
        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
        
        Set<Long> completedEntityIds = new HashSet<Long>();
        String entityIds = task.getParameter(AnnotationSessionTask.PARAM_completedTargets);
        if (entityIds == null || "".equals(entityIds)) return completedEntityIds;
        
        for(String id : entityIds.split("\\s*,\\s*")) {
			try {
				completedEntityIds.add(Long.parseLong(id));
			} 
			catch (NumberFormatException e) {
				_logger.warn("Error parsing id in AnnotationSessionTask.PARAM_completedTargets: "+id,e);
			}
        }
        
        return completedEntityIds;
    }
    
    /**
     * Updates the given session and returns all the annotations within it. 
     * @param sessionId
     * @throws ComputeException
     */
	private List<Entity> updateAnnotationSession(long sessionId) throws ComputeException {

		Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
		
		List<Entity> categories = getCategoriesForAnnotationSession(task.getOwner(), sessionId);
		List<Entity> annotations = getAnnotationsForSession(task.getOwner(), sessionId);
	
        Map<String, List<Entity>> map = new HashMap<String, List<Entity>>();
        for (Entity annotation : annotations) {
            EntityData ed = annotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
            if (ed == null) continue;
            String entityId = ed.getValue();
            List<Entity> entityAnnots = map.get(entityId);
            if (entityAnnots == null) {
                entityAnnots = new ArrayList<Entity>();
                map.put(entityId, entityAnnots);
            }
            entityAnnots.add(annotation);
        }
	    
        Set<String> completed = new HashSet<String>();
        
        for(String entityId : map.keySet()) {
        	List<Entity> entityAnnotations = map.get(entityId);
        	
        	Set<Long> termIds = new HashSet<Long>();

			for(Entity annotation : entityAnnotations) {
				EntityData keyTermED = annotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
				if (keyTermED==null) continue;
				Entity keyTerm = keyTermED.getChildEntity();
				if (keyTerm==null) continue;
				Long termId = keyTerm.getId();
    			String termType = keyTerm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
				
				if ("Tag".equals(termType)) {
					Entity parent = null;
					for(Entity p : getParentEntities(null, termId)) {
						String parentTypeName = p.getEntityType().getName();
						if (parentTypeName.equals(EntityConstants.TYPE_ONTOLOGY_ELEMENT) 
								|| parentTypeName.equals(EntityConstants.TYPE_ONTOLOGY_ROOT)) {
							parent = p;
							break;
						}
					}
					if (parent != null) termId = parent.getId();
				}
				
				termIds.add(termId);
			}
        	
        	
        	// Ensure this entity is annotated with a term in each category
        	int c = 0;
    		for(Entity category : categories) {
				if (termIds.contains(category.getId())) {
					c++;
				}
    		}	
    		
    		if (c == categories.size()) {
    			completed.add(entityId);
    		}
        }
        
        StringBuffer buf = new StringBuffer();
        for(String entityId : completed) {
        	if (buf.length()>0) buf.append(",");
        	buf.append(entityId);
        }
		
        task.setParameter(AnnotationSessionTask.PARAM_completedTargets, buf.toString());
        saveOrUpdate(task);
        
        return annotations;
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
    	if (entityTypeName==null) throw new DaoException("Error creating entity with null type");
        Entity entity = newEntity(entityTypeName, entityName, subjectKey);
        if (entity.getEntityType()==null) throw new DaoException("Error creating entity with unknown entity type: "+entityTypeName);
        saveOrUpdate(entity);
        return entity;
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws DaoException {
        return addEntityToParent(parent, entity, index, attrName, null);
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName, String value) throws DaoException {
        if (attrName==null) throw new DaoException("Error adding entity child with null attribute name");
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        if (value!=null) {
            ed.setValue(value);
        }
        saveOrUpdate(ed);
        propagatePermissions(parent, entity, true);
        return ed;
    }
    
    public void addChildren(String subjectKey, Long parentId, List<Long> childrenIds, String attributeName) throws DaoException {
        
        Entity parent = getEntityById(parentId);
        
        Set<Long> existingChildrenIds = new HashSet<Long>();
        for(EntityData entityData : parent.getEntityData()) {
            if (entityData.getChildEntity()!=null) { 
                existingChildrenIds.add(entityData.getChildEntity().getId());
            }
        }
        
        EntityAttribute attribute = parent.getAttributeByName(attributeName);
        if (attribute==null) {
            throw new IllegalArgumentException(
                    "Attribute "+attributeName+" not found for entity type "+parent.getEntityType().getName());
        }
            
        Date createDate = new Date();
        
        for (Long childId : childrenIds) {
            if (existingChildrenIds.contains(childId)) continue;
            
            Entity child = new Entity();
            child.setId(childId);
            
            EntityData ed = new EntityData();
            ed.setParentEntity(parent);
            ed.setChildEntity(child);
            ed.setOwnerKey(subjectKey);
            ed.setCreationDate(createDate);
            ed.setUpdatedDate(createDate);
            ed.setEntityAttribute(attribute);
            
            saveOrUpdate(ed);

            propagatePermissions(parent, child, true);
        }
    }
    
    private void propagatePermissions(Entity parent, Entity child, boolean recursive) throws DaoException {
        for(EntityActorPermission permission : getFullPermissions(parent)) {
            grantPermissions(child, permission.getSubjectKey(), permission.getPermissions(), recursive);    
        }
    }
    
    public Entity createDataSet(String subjectKey, String dataSetName) throws ComputeException {

        String dataSetIdentifier = EntityUtils.createDenormIdentifierFromName(subjectKey, dataSetName);
        
        if (!getUserEntitiesWithAttributeValue(null, EntityConstants.TYPE_DATA_SET, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier).isEmpty()) {
        	throw new ComputeException("Data Set with identifier '"+dataSetIdentifier+"' already exists.");
        }
        
        Entity newDataSet = newEntity(EntityConstants.TYPE_DATA_SET, dataSetName, subjectKey);
        saveOrUpdate(newDataSet);

        EntityData dataSetIdEd = newData(newDataSet, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, subjectKey);
        newDataSet.getEntityData().add(dataSetIdEd);
        dataSetIdEd.setValue(dataSetIdentifier);
        saveOrUpdate(dataSetIdEd);

        return newDataSet;
    }
    
    public Entity createOntologyRoot(String subjectKey, String rootName) throws ComputeException {
        
        Entity newOntologyRoot = newEntity(EntityConstants.TYPE_ONTOLOGY_ROOT, rootName, subjectKey);
        saveOrUpdate(newOntologyRoot);

        // Add the type
        newOntologyRoot.setValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, Category.class.getSimpleName());
        saveOrUpdate(newOntologyRoot);
        
        return newOntologyRoot;
    }

    public EntityData createOntologyTerm(String subjectKey, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {

        Entity parentOntologyElement = getEntityById(ontologyTermParentId);

        // You need write access to the parent ontology to make a new element in this ontology
        List<String> subjectKeyList = getSubjectKeys(subjectKey);
        if (!EntityUtils.hasWriteAccess(parentOntologyElement, subjectKeyList)) {
            throw new ComputeException(subjectKey+" has no write access to ontology term "+ontologyTermParentId);
        }
        
        // The new term will be owned by the ontology owner, even if someone else is creating it
        String newTermOwner = parentOntologyElement.getOwnerKey();
        
        // Create and save the new entity
        Entity newOntologyElement = newEntity(EntityConstants.TYPE_ONTOLOGY_ELEMENT, termName, newTermOwner);

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
        EntityData termData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, newTermOwner);
        termData.setValue(type.getClass().getSimpleName());
        eds.add(termData);

        // Add the type-specific parameters
        if (type instanceof Interval) {

            Interval interval = (Interval)type;

            EntityData lowerData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER, newTermOwner);
            lowerData.setValue(interval.getLowerBound().toString());
            eds.add(lowerData);

            EntityData upperData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER, newTermOwner);
            upperData.setValue(interval.getUpperBound().toString());
            eds.add(upperData);
        }

        // Add the type-specific parameters
        if (type instanceof EnumText) {
            EnumText enumText = (EnumText)type;
            Entity valueEnumEntity = getEntityById(enumText.getValueEnumId());
            EntityData lowerData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID, newTermOwner);
            lowerData.setValue(enumText.getValueEnumId().toString());
            lowerData.setChildEntity(valueEnumEntity);
            eds.add(lowerData);
        }
        
        // Save the new element
        saveOrUpdate(newOntologyElement);
        
        // Associate the entity to the parent
        EntityData childData = addEntityToParent(parentOntologyElement, newOntologyElement, orderIndex, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
                
        return childData;
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

        EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
        
        // Create new ontology element
        Entity newOntologyElement = newEntity(source.getEntityType(), targetName, targetSubjectKey);
        saveOrUpdate(newOntologyElement);

        _logger.info("newOntologyElement.id="+newOntologyElement.getId());
        
        // Add the children 
    	for(EntityData ed : source.getEntityData()) {

    		// Never clone "Is Public" attributes. Entities are cloned privately. 
    		if (ed.getEntityAttribute().getId().equals(tmpAttr.getId())) continue;
    		
    		Entity newChildEntity = null;
    		Entity childEntity = ed.getChildEntity();
    		if (childEntity != null) {
    			newChildEntity = cloneEntityTree(childEntity, targetSubjectKey, childEntity.getName(), false);	
    		}
    		
            EntityData newEd = newOntologyElement.addChildEntity(newChildEntity, ed.getEntityAttribute().getName());
            newEd.setOrderIndex(ed.getOrderIndex());
            newEd.setValue(ed.getValue());
            saveOrUpdate(newEd);
    	}

    	return newOntologyElement;
    }

    public Entity publishOntology(Long sourceRootId, String targetRootName) throws ComputeException {

    	Entity sourceRoot = getEntityById(sourceRootId);    	
    	if (sourceRoot == null) {
    		throw new DaoException("Cannot find the source root.");
    	}
	
        Entity clonedEntity = cloneEntityTree(sourceRoot, sourceRoot.getOwnerKey(), targetRootName, true);

        // Add the public tag
        EntityData publicEd = newData(clonedEntity, EntityConstants.ATTRIBUTE_IS_PUBLIC, sourceRoot.getOwnerKey());
        publicEd.setValue("true");
        saveOrUpdate(publicEd);
        
        return clonedEntity;
    }
    
    public void fixInternalOntologyConsistency(Long sourceRootId) throws DaoException {
    	Entity ontologyRoot = getEntityById(sourceRootId);
    	ontologyRoot = populateDescendants(ontologyRoot.getOwnerKey(), ontologyRoot);
    	if (ontologyRoot==null) return;
    	_logger.warn("Fixing internal consistency for ontology "+ontologyRoot.getName()+" (id="+ontologyRoot.getId()+")");
    	Map<String,Long> enumMap = new HashMap<String,Long>();
    	buildEnumMap(ontologyRoot, enumMap);
    	updateEnumTexts(ontologyRoot, enumMap);
    }
    
    public void buildEnumMap(Entity entity, Map<String,Long> enumMap) {
		if ("Enum".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE))) {
			enumMap.put(entity.getName(), entity.getId());
		}
    	for(Entity child : entity.getChildren()) {
    		buildEnumMap(child, enumMap);
    	}
    }

    public void updateEnumTexts(Entity entity, Map<String,Long> enumMap) throws DaoException {
		if ("EnumText".equals(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE))) {
			EntityData oldEnumIdEd = entity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID);
			if (oldEnumIdEd != null && oldEnumIdEd.getValue()!=null) {
				Entity oldEnum = getEntityById(new Long(oldEnumIdEd.getValue()));
				if (oldEnum!=null) {
					Long newEnumId = enumMap.get(oldEnum.getName());
					if (newEnumId!=null) {
						oldEnumIdEd.setValue(newEnumId.toString());
						oldEnumIdEd.setChildEntity(getEntityById(newEnumId));
						_logger.warn("Updating EnumText "+entity.getName()+" to reference the correct Enum id="+newEnumId);
						saveOrUpdate(oldEnumIdEd);
					}
					else {
						_logger.warn("Cannot find enum with name "+oldEnum.getName()+" in ontology");
					}
				}
				else {
					_logger.warn("Cannot find old EnumText entity with id="+oldEnumIdEd.getValue());
				}
			}
			else {
				_logger.warn("EnumText (id="+entity.getId()+") does not reference an Enum");
			}
		}
    	for(Entity child : entity.getChildren()) {
    		updateEnumTexts(child, enumMap);
    	}
    }
    
	public Entity createOntologyAnnotation(String subjectKey, OntologyAnnotation annotation) throws ComputeException {

        try {        	
        	Entity keyEntity = null;
        	boolean isCustom = false;
        	
            if (annotation.getKeyEntityId() != null) {
            	keyEntity = getEntityById(annotation.getKeyEntityId());
            	String termType = keyEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
            	
            	isCustom = keyEntity!=null && "Custom".equals(termType);
            	if (isCustom && StringUtils.isEmpty(annotation.getValueString())) {
            		throw new ComputeException("Value cannot be empty for custom annotation");
            	}

            	if (!"Text".equals(termType) && !"Custom".equals(termType) && !"Tag".equals(termType)) {
            		// Non-text annotations are exclusive, so delete existing annotations first
		            List<Entity> existingAnnotations = getAnnotationsByEntityId(subjectKey, annotation.getTargetEntityId());
		        	for(Entity existingAnnotation : existingAnnotations) {
		        		EntityData eaKeyEd = existingAnnotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
		        		if (eaKeyEd==null) continue;
    	        		if (eaKeyEd.getChildEntity().getId().equals(annotation.getKeyEntityId())) {
    	        			deleteEntityById(subjectKey, existingAnnotation.getId());
    	        		}
	            	}
            	}
            }
            
        	String tag = isCustom ? annotation.getValueString() : 
        				(annotation.getValueString()==null ? annotation.getKeyString() : 
        				 annotation.getKeyString() + " = " + annotation.getValueString());
            
            Entity newAnnotation = newEntity(EntityConstants.TYPE_ANNOTATION, tag, subjectKey);
            
            Set<EntityData> eds = new HashSet<EntityData>();
            newAnnotation.setEntityData(eds);
            
			// Add the target id
			EntityData targetIdData = newData(newAnnotation, 
					EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID, subjectKey);
			targetIdData.setValue(""+annotation.getTargetEntityId());
			eds.add(targetIdData);
				
			// Add the key string
			EntityData keyData = newData(newAnnotation,
					EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM, subjectKey);
			keyData.setValue(annotation.getKeyString());
			eds.add(keyData);

			// Add the value string
			EntityData valueData = newData(newAnnotation,
					EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, subjectKey);
			valueData.setValue(annotation.getValueString());
			eds.add(valueData);
			
			// Add the key entity
            if (annotation.getKeyEntityId() != null) {
				EntityData keyEntityData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID, subjectKey);
				keyEntityData.setChildEntity(keyEntity);
				keyEntityData.setValue(""+keyEntity.getId());
				eds.add(keyEntityData);
            }

			// Add the value entity
            if (annotation.getValueEntityId() != null) {
            	Entity valueEntity = getEntityById(annotation.getValueEntityId());
				EntityData valueEntityData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID, subjectKey);
				valueEntityData.setChildEntity(valueEntity);
				valueEntityData.setValue(""+valueEntity.getId());
				eds.add(valueEntityData);
            }
            
			// Add the session id
            if (annotation.getSessionId() != null) {
				EntityData sessionIdData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID, subjectKey);
				sessionIdData.setValue(""+annotation.getSessionId());
				eds.add(sessionIdData);
            }
            
            saveOrUpdate(newAnnotation);
            
            // Notify the session 
            if (annotation.getSessionId() != null) {
            	updateAnnotationSession(annotation.getSessionId());
            }
            
            return newAnnotation;
        }
        catch (Exception e) {
            _logger.error("Error creating ontology annotation for subject "+subjectKey,e);
            throw new ComputeException("Error creating ontology annotation for subject "+subjectKey,e);
        }
	}

	public Long removeOntologyAnnotation(String subjectKey, long annotationId) throws ComputeException {
        Entity entity = getEntityById(subjectKey, annotationId);
        getCurrentSession().delete(entity);	
        
        // Notify the session 
        String sessionId = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
        if (sessionId != null) updateAnnotationSession(Long.parseLong(sessionId));
        
        return new Long(entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID));
	}

	/**
	 * Removes all annotations in the given session and then returns them.
	 * @param userLogin
	 * @param sessionId
	 * @return
	 * @throws DaoException
	 */
    public List<Entity> removeAllOntologyAnnotationsForSession(String subjectKey, long sessionId) throws DaoException {

        try {
        	List<Entity> annotations = getAnnotationsForSession(subjectKey, sessionId);
            for(Object o : annotations) {
                Entity entity = (Entity)o;
                if (!entity.getOwnerKey().equals(subjectKey)) {
                	_logger.info("Cannot remove annotation "+entity.getId()+" not owned by "+subjectKey);
                }
                else {
                	_logger.info("Removing annotation "+entity.getId());
                    getCurrentSession().delete(entity);	
                }
            }
            // Notify the session 
            updateAnnotationSession(sessionId);
            return annotations;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
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
            Session session = getCurrentSession();
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
                List<String> subjectKeyList = getSubjectKeys(subjectKey);
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
        Set<String> subjectKeys = getSubjectKeySet(subjectKey);
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
    
    public Entity getEntityAndChildren(String subjectKey, Long entityId) {
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

    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getEntityPathsToRoots(entity.getId()="+entityId+")");
    	}
    	
    	Entity entity = getEntityById(entityId);
    	List<List<Entity>> paths = new ArrayList<List<Entity>>();
    	Set<Entity> parents = getParentEntities(subjectKey, entityId);
    	StringBuffer sb = new StringBuffer();
    	for(Entity parent : parents) {
    		if (sb.length()>0) sb.append(", ");
    		sb.append(parent.getName());
    	}
    	if (parents.isEmpty()) {
    		List<Entity> path = new ArrayList<Entity>();
			path.add(entity);
			paths.add(path);
    	}
    	else {
        	for(Entity parent : parents) {
        		List<List<Entity>> ancestorPaths = getEntityPathsToRoots(subjectKey, parent.getId());
        		for(List<Entity> ancestorPath : ancestorPaths) {
        			ancestorPath.add(entity);
        			paths.add(ancestorPath);
        		}
        	}
    	}
    	return paths;
    }

    public List<List<EntityData>> getEntityDataPathsToRoots(String subjectKey, EntityData entityData) throws DaoException {
    	
    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getEntityPathsToRoots(subjectKey="+subjectKey+", entityData.getId()="+entityData.getId()+")");
    	}
    	
    	Entity entity = entityData.getParentEntity();
    	List<List<EntityData>> paths = new ArrayList<List<EntityData>>();
    	Set<EntityData> parents = getParentEntityDatas(subjectKey, entity.getId());
    	if (parents.isEmpty()) {
    		List<EntityData> path = new ArrayList<EntityData>();
			path.add(entityData);
			paths.add(path);
    	}
    	else {
    		Set<String> subjectKeys = getSubjectKeySet(subjectKey);
        	for(EntityData parent : parents) {
        		if (subjectKeys==null || subjectKeys.contains(parent.getOwnerKey())) {
	        		List<List<EntityData>> ancestorPaths = getEntityDataPathsToRoots(subjectKey, parent);
	        		for(List<EntityData> ancestorPath : ancestorPaths) {
	        			if (entityData.getId()!=null) ancestorPath.add(entityData);
	        			paths.add(ancestorPath);
	        		}
        		}
        	}
    	}
    	return paths;
    }
    
    public List<Long> getPathToRoot(String subjectKey, Long entityId, Long rootId) throws DaoException {

    	if (_logger.isDebugEnabled()) {
    		_logger.debug("getPathToRoot(entityId="+entityId+", rootId="+rootId+")");
    	}
    	
		List<Long> ids = new ArrayList<Long>();
		
    	if (entityId.equals(rootId)) {
    		ids.add(rootId);
    		return ids;
    	}
    	
    	for(Entity parent : getParentEntities(subjectKey, entityId)) {
    		List<Long> path = getPathToRoot(subjectKey, parent.getId(), rootId);
    		if (path != null) {
    			path.add(entityId);
    			return path;
    		}
    	}
    	
    	// No path to the given root
    	return null;
    }

    public List<MappedId> getProjectedResults(String subjectKey, List<Long> entityIds, List<String> upProjection, List<String> downProjection) throws DaoException {
    	
    	// TODO: filter results by subjectKey?
    	
    	if (entityIds.isEmpty()) {
    		throw new DaoException("getProjectedResults: entity ids cannot be empty");
    	}
    	
    	if (upProjection.isEmpty() && downProjection.isEmpty()) {
    		throw new DaoException("getProjectedResults: both up and down projections cannot be empty");
    	}
    	
    	List<MappedId> list = new ArrayList<MappedId>();
    	
    	StringBuffer entityCommaList = new StringBuffer();
    	for(Long id : entityIds) {
    		if (entityCommaList.length()>0) entityCommaList.append(",");
    		entityCommaList.append(id);
    	}
    	

    	List<Long> upTypeProjection = new ArrayList<Long>();
    	for(String entityType : upProjection) {
    		upTypeProjection.add(getEntityTypeByName(entityType).getId());
    	}

    	List<Long> downTypeProjection = new ArrayList<Long>();
    	for(String entityType : downProjection) {
    		downTypeProjection.add(getEntityTypeByName(entityType).getId());
    	}
    	
        Connection conn = null;
    	PreparedStatement stmt = null;
    	try {
            StringBuffer sql = new StringBuffer();
            
            int i = 1;
            String prevTable = "i";
            String prevFk = "id";
            String targetIdAlias = "i.id";
            
            for(Long attrId : upTypeProjection) {
            	sql.append("join entityData ed"+i+" on ed"+i+".child_entity_id = "+prevTable+"."+prevFk+" \n");	
            	sql.append("join entity e"+i+" on ed"+i+".parent_entity_id = e"+i+".id \n");
            	prevTable = "ed"+i;
            	prevFk = "parent_entity_id";
            	targetIdAlias = prevTable+".parent_entity_id";
            	i++;
            }
            
            for(Long attrId : downTypeProjection) {
            	sql.append("join entityData ed"+i+" on ed"+i+".parent_entity_id = "+prevTable+"."+prevFk+" \n");	
            	sql.append("join entity e"+i+" on ed"+i+".child_entity_id = e"+i+".id \n");
            	prevTable = "ed"+i;
            	prevFk = "child_entity_id";
            	targetIdAlias = prevTable+".child_entity_id";
            	i++;
            }
            
            sql.insert(0, "select distinct i.id,"+targetIdAlias+" from entity i \n");
            sql.append("where i.id in ("+entityCommaList+") \n");
            
            i = 1;
            
            for(Long typeId : upTypeProjection) {
            	sql.append("and e"+i+".entity_type_id = "+typeId+" \n");	
            	i++;
            }

            for(Long typeId : downTypeProjection) {
            	sql.append("and e"+i+".entity_type_id = "+typeId+" \n");	
            	i++;
            }
            
            sql.append("and "+targetIdAlias+" is not null");
            
            _logger.debug(sql);
            
	        conn = getJdbcConnection();
	        stmt = conn.prepareStatement(sql.toString());
	        	        
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Long entityId = rs.getBigDecimal(1).longValue();
				Long projId = rs.getBigDecimal(2).longValue();
				list.add(new MappedId(entityId, projId));
			}
		}
    	catch (SQLException e) {
    		throw new DaoException(e);
    	}
	    finally {
	    	try {
	            if (stmt!=null) stmt.close();
	            if (conn!=null) conn.close();	
	    	}
	    	catch (SQLException e) {
	    		_logger.warn("Ignoring error encountered while closing JDBC connection",e);
	    	}
	    }
        
    	return list;
    }
    
    public List<Long> getImageIdsWithName(Connection connection, String subjectKey, String imagePath) throws DaoException {
    	
    	List<Long> sampleIds = new ArrayList<Long>();
        Connection conn = connection;
    	PreparedStatement stmt = null;
    	
    	try {
            StringBuffer sql = new StringBuffer("select distinct i.id from entity i ");
            sql.append("where i.name = ? ");
            if (subjectKey!=null) {
            	sql.append("and i.ownerKey = ? ");
            }
            
            if (conn==null) {
            	conn = getJdbcConnection();
            }
	        stmt = conn.prepareStatement(sql.toString());
	        stmt.setString(1, imagePath);
            if (subjectKey!=null) {
            	stmt.setString(2, subjectKey);
            }
	        
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				sampleIds.add(rs.getBigDecimal(1).longValue());
			}
		}
    	catch (SQLException e) {
    		throw new DaoException(e);
    	}
	    finally {
	    	try {
	            if (stmt!=null) stmt.close();
	            // only close the connection if it didn't come from outside
	            if (connection==null && conn!=null) conn.close(); 
	    	}
	    	catch (SQLException e) {
	    		_logger.warn("Ignoring error encountered while closing JDBC connection",e);
	    	}
	    }
        
    	return sampleIds;
    }

    public List<Long> getOrphanAnnotationIdsMissingTargets(String subjectKey) throws DaoException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        List<Long> annotationIds = new ArrayList<Long>();
        
        EntityType annotationType = getEntityTypeByName(EntityConstants.TYPE_ANNOTATION);
        EntityAttribute targetAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        
        try {
            StringBuffer sql = new StringBuffer("select a.id from entity a ");
            sql.append("join entityData aed on aed.parent_entity_id=a.id and aed.entity_att_id=? ");
            sql.append("left outer join entity target on aed.value=target.id ");
            sql.append("where a.entity_type_id=? ");
            sql.append("and target.id is null ");
            if (subjectKey!=null) {
                sql.append("and a.ownerKey = ? ");
            }
            
            conn = getJdbcConnection();
            stmt = conn.prepareStatement(sql.toString());
            stmt.setLong(1, targetAttr.getId());
            stmt.setLong(2, annotationType.getId());
            if (subjectKey!=null) {
                stmt.setString(3, subjectKey);
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                annotationIds.add(rs.getBigDecimal(1).longValue());
            }
        }
        catch (SQLException e) {
            throw new DaoException(e);
        }
        finally {
            try {
                if (stmt!=null) stmt.close();
                if (conn!=null) conn.close(); 
            }
            catch (SQLException e) {
                _logger.warn("Ignoring error encountered while closing JDBC connection",e);
            }
        }
        
        return annotationIds;
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
    	Entity entity = getEntityById(entityId);
    	_logger.info(subjectKey+" is annexing entity tree starting at "+entity.getName()+
    			" (id="+entity.getId()+") from "+entity.getOwnerKey());
    	annexEntityTree(subjectKey, entity, "  ");	
    	disassociateTreeFromNonOwners(subjectKey, getEntityById(entity.getId()), "  ");
    	return entity;
    }
    
    private Entity annexEntityTree(String subjectKey, Entity entity, String indent) throws ComputeException {
    	
    	if (!entity.getOwnerKey().equals(subjectKey)) {
        	_logger.info(indent+"annexing entity "+entity.getName()+" (id="+entity.getId()+")");
        	entity.setOwnerKey(subjectKey);
        	saveOrUpdate(entity);
    	}
    	loadLazyEntity(subjectKey, entity, false);
    	for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
    		if (!ed.getOwnerKey().equals(entity.getOwnerKey())) {
	    		ed.setOwnerKey(subjectKey);
	    		saveOrUpdate(ed);
    		}
    		if (ed.getChildEntity()!=null) {
    			annexEntityTree(subjectKey, ed.getChildEntity(), indent+"  ");
    		}
    	}
    	
    	// TODO: move files in filestore?
    	
    	return entity;
    }
    
    private Entity disassociateTreeFromNonOwners(String subjectKey, Entity entity, String indent) throws ComputeException {
    	
    	for(EntityData parentEd : getParentEntityDatas(subjectKey, entity.getId())) {
    		if (parentEd.getOwnerKey().equals(subjectKey)) continue;
    		_logger.info(indent+"deleting "+parentEd.getOwnerKey()+"'s link ("+parentEd.getEntityAttribute().getName()+") from entity "+parentEd.getParentEntity().getName()+" to entity "+entity.getName());
    		parentEd.getParentEntity().getEntityData().remove(parentEd);
    		genericDelete(parentEd);
    	}
    	
    	// This would be the correct thing to do, but it makes this far too slow.
    	// In practice it's probably not needed with the current state of the data, but maybe it will be in the future.
//    	loadLazyEntity(subjectKey, entity, false);
//    	for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
//    		if (ed.getChildEntity()!=null) {
//    			disassociateTreeFromNonOwners(subjectKey, ed.getChildEntity(), indent+"  ");
//    		}
//    	}
    	
    	return entity;
    }
    
    
    public EntityType getEntityTypeByName(String entityTypeName) {
    	preloadData();
        return entityByName.get(entityTypeName);	
    }
    
    public EntityAttribute getEntityAttributeByName(String attrName) {
    	preloadData();
        return attrByName.get(attrName);	
    }

    private Entity newEntity(String entityTypeName, String name, String owner) {
        EntityType tmpType = getEntityTypeByName(entityTypeName);
        return newEntity(tmpType, name, owner);
    }

    private Entity newEntity(EntityType entityType, String name, String subjectKey) {
    	Date date = new Date();
        return new Entity(null, name, subjectKey, null, entityType, date, date, new HashSet<EntityData>());	
    }
    
    private EntityData newData(Entity parent, String attrName, String subjectKey) {
        EntityAttribute attribute = getEntityAttributeByName(attrName);
        return newData(parent, attribute, subjectKey);
    }
    
    private EntityData newData(Entity parent, EntityAttribute attribute, String subjectKey) {
    	Date date = new Date();
        return new EntityData(null, attribute, parent, null, subjectKey, null, date, date, null);
    }
    

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws DaoException {
        _logger.info("getPatternQuantifiersForScreenSample: starting search for entities of type="+EntityConstants.TYPE_SCREEN_SAMPLE);
        List<Entity> flyScreenSampleEntityList = getUserEntitiesByTypeName(null, EntityConstants.TYPE_SCREEN_SAMPLE);
        _logger.info("getPatternQuantifiersForScreenSample: found "+flyScreenSampleEntityList.size()+" entities of type="+EntityConstants.TYPE_SCREEN_SAMPLE);
        Map<Entity, Map<String, Double>> entityQuantMap=new HashMap<Entity, Map<String, Double>>();
        long count=0;
        for (Entity screenSample : flyScreenSampleEntityList) {
            // Get the file-path for the quantifier file
            _logger.info("Exploring screenSample name="+screenSample.getName() + " index="+count+" of "+flyScreenSampleEntityList.size());
            Set<Entity> children=screenSample.getChildren();
            for (Entity child : children) {
               // _logger.info("Child id="+child.getId()+" type="+child.getEntityType().getName()+" name="+child.getName());
                if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) &&
                        child.getName().toLowerCase().equals("pattern annotation")) {
                    Set<Entity> children2=child.getChildren();
                    for (Entity child2 : children2) {
                       // _logger.info("Child2 id="+child2.getId()+" type="+child2.getEntityType().getName()+" name="+child2.getName());
                        if (child2.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) &&
                                child2.getName().toLowerCase().equals("supportingfiles")) {
                            Set<Entity> children3=child2.getChildren();
                            for (Entity child3 : children3) {
                                //_logger.info("Child3 id="+child3.getId()+" type="+child3.getEntityType().getName()+" name="+child3.getName());
                                if (child3.getEntityType().getName().equals(EntityConstants.TYPE_TEXT_FILE) && child3.getName().endsWith("quantifiers.txt")) {
                                    String quantifierFilePath=child3.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                                    _logger.info("Quantifier file path="+quantifierFilePath);
                                    File quantFile=new File(quantifierFilePath);
                                    Map<String, Double> quantMap=new HashMap<String, Double>();
                                    try {
                                        BufferedReader br=new BufferedReader(new FileReader(quantFile));
                                        String currentLine="";
                                        while((currentLine=br.readLine())!=null) {
                                            String[] kv=currentLine.split("=");
                                            if (kv.length==2) {
                                                Double d=new Double(kv[1].trim());
                                                quantMap.put(kv[0], d);
                                            }
                                        }
                                        br.close();
                                        _logger.info("Added "+quantMap.size()+" entries");
                                        entityQuantMap.put(screenSample, quantMap);
                                    } catch (Exception ex) {
                                        throw new DaoException(ex);
                                    }

                                }
                            }
                        }
                    }
                }
            }
            count++;
        }
        return entityQuantMap;
    }


    public Map<Entity, Map<String, Double>> getMaskQuantifiers(String maskFolderName) throws DaoException {
        _logger.info("getMaskQuantifiers() folder name=" + maskFolderName + " : starting search for entities of type=" + EntityConstants.TYPE_SCREEN_SAMPLE);
        List<Entity> flyScreenSampleEntityList = getUserEntitiesByTypeName(null, EntityConstants.TYPE_SCREEN_SAMPLE);
        _logger.info("getPatternQuantifiersForScreenSample: found " + flyScreenSampleEntityList.size() + " entities of type=" + EntityConstants.TYPE_SCREEN_SAMPLE);
        Map<Entity, Map<String, Double>> entityQuantMap = new HashMap<Entity, Map<String, Double>>();
        long count = 0;
        for (Entity screenSample : flyScreenSampleEntityList) {
            // Get the file-path for the quantifier file
            _logger.info("Exploring screenSample name=" + screenSample.getName() + " index=" + count + " of " + flyScreenSampleEntityList.size());
            Set<Entity> topChildren = screenSample.getChildren();
            for (Entity topChild : topChildren) {
                if (topChild.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) &&
                        topChild.getName().equals(MaskSampleAnnotationService.MASK_ANNOTATION_FOLDER_NAME)) {
                    Set<Entity> children = topChild.getChildren();
                    for (Entity child : children) {
                        if (child.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) &&
                                child.getName().equals(maskFolderName)) {
                            Set<Entity> children2 = child.getChildren();
                            for (Entity child2 : children2) {
                                // _logger.info("Child2 id="+child2.getId()+" type="+child2.getEntityType().getName()+" name="+child2.getName());
                                if (child2.getEntityType().getName().equals(EntityConstants.TYPE_FOLDER) &&
                                        child2.getName().toLowerCase().equals("supportingfiles")) {
                                    Set<Entity> children3 = child2.getChildren();
                                    for (Entity child3 : children3) {
                                        //_logger.info("Child3 id="+child3.getId()+" type="+child3.getEntityType().getName()+" name="+child3.getName());
                                        if (child3.getEntityType().getName().equals(EntityConstants.TYPE_TEXT_FILE) && child3.getName().endsWith("quantifiers.txt")) {
                                            String quantifierFilePath = child3.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                                            _logger.info("Quantifier file path=" + quantifierFilePath);
                                            File quantFile = new File(quantifierFilePath);
                                            Map<String, Double> quantMap = new HashMap<String, Double>();
                                            try {
                                                BufferedReader br = new BufferedReader(new FileReader(quantFile));
                                                String currentLine = "";
                                                while ((currentLine = br.readLine()) != null) {
                                                    String[] kv = currentLine.split("=");
                                                    if (kv.length == 2) {
                                                        Double d = new Double(kv[1].trim());
                                                        quantMap.put(kv[0], d);
                                                    }
                                                }
                                                br.close();
                                                _logger.info("Added " + quantMap.size() + " entries");
                                                entityQuantMap.put(screenSample, quantMap);
                                            }
                                            catch (Exception ex) {
                                                throw new DaoException(ex);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            count++;
        }
        return entityQuantMap;
    }


    // This method returns two objects,  Map<Long, Map<String, String>> sampleInfoMap, Map<Long, List<Double>> quantifierInfoMap
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws DaoException {
        try {
            //Object[] mapObjects = PatternAnnotationDataManager.loadPatternAnnotationQuantifierSummaryFile();
            //return mapObjects;
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
    }

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws DaoException {
        try {
            MaskAnnotationDataManager maskManager=new MaskAnnotationDataManager();
            String resourceDirString= SystemConfigurationProperties.getString("FileStore.CentralDir")+
                    SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
            String quantifierSummaryFilename= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
            File summaryFile=new File(resourceDirString + File.separator+maskFolderName, quantifierSummaryFilename);
            File nameIndexFile=new File(resourceDirString + File.separator+maskFolderName, "maskNameIndex.txt");
            maskManager.loadMaskCompartmentList(nameIndexFile.toURI().toURL());
            Object[] mapObjects=maskManager.loadMaskSummaryFile(summaryFile.toURI().toURL());
            return mapObjects;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
    }

    public PatternAnnotationDataManager getPatternAnnotationDataManagerByType(String type) throws DaoException {
        try {
            PatternAnnotationDataManager dataManager=null;
            if (type.equals(RelativePatternAnnotationDataManager.RELATIVE_TYPE)) {
                dataManager=new RelativePatternAnnotationDataManager();
                dataManager.setup();
                return dataManager;
            } else {
                throw new Exception("Do not recognize PatternAnnotationDataManager type="+type);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
    }

    public Entity saveBulkEntityTree(Entity root) throws DaoException {
    	if (root.getOwnerKey()==null) {
    		throw new IllegalArgumentException("Root entity must specify the owner key");
    	}
    	return saveBulkEntityTree(root, root.getOwnerKey());
    }
    
    public Entity saveBulkEntityTree(Entity root, String subjectKey) throws DaoException {

    	final int batchSize = 800;	
    	
    	_logger.info("Saving bulk entity tree rooted at "+root.getName());
    	
        _logger.debug("Using owner key: "+subjectKey);
        
        java.sql.Date defaultDate = new java.sql.Date(new Date().getTime());
        
    	Connection conn = null;
    	PreparedStatement stmtEntity = null;
    	PreparedStatement stmtEd = null;
    	try {
    		List<Entity> entities = new ArrayList<Entity>();
    		int count = getEntitiesInTree(root, entities);
    		_logger.info("Found "+entities.size()+" entities in tree, and "+count+" objects to be persisted.");
    		
    		List<Long> ids = TimebasedIdentifierGenerator.generateIdList(count);
    		
    		conn = getJdbcConnection();
	        conn.setAutoCommit(false);
	        
	        String entitySql = "insert into entity (id,name,owner_dkey,entity_type_id,creation_date,updated_date) values (?,?,?,?,?,?)";
	        stmtEntity = conn.prepareStatement(entitySql);
	        
	        String edSql = "insert into entityData (id,parent_entity_id,entity_att_id,value,owner_dkey,creation_date,updated_date,orderIndex,child_entity_id) values (?,?,?,?,?,?,?,?,?)";
	        stmtEd = conn.prepareStatement(edSql);
	        
	        int idIndex = ids.size()-1;
	        int entityCount = 0;
	        int edCount = 0;
	        Long newEntityId = null;
	        
	        for(Entity entity : entities) {
		    
	        	newEntityId = ids.get(idIndex--);
	        	entity.setId(newEntityId);
	        	
	        	stmtEntity.setLong(1, newEntityId);
	        	stmtEntity.setString(2, entity.getName());
	        	stmtEntity.setString(3, subjectKey);
		        
		    	if (entity.getEntityType().getId()==null) {
		    		stmtEntity.setLong(4, getEntityTypeByName(entity.getEntityType().getName()).getId());
		    	}
		    	else {
		    		stmtEntity.setLong(4, entity.getEntityType().getId());	
		    	}
		    	
		        if (entity.getCreationDate()!=null) {
		        	stmtEntity.setDate(5, new java.sql.Date(entity.getCreationDate().getTime()));	
		        }
		        else {
		        	stmtEntity.setDate(5, defaultDate);
		        }

		        if (entity.getUpdatedDate()!=null) {
		        	stmtEntity.setDate(6, new java.sql.Date(entity.getUpdatedDate().getTime()));
		        }
		        else {
		        	stmtEntity.setDate(6, defaultDate);
		        }
		        
		        stmtEntity.addBatch();
		        
		        for(EntityData ed : entity.getEntityData()) {

		        	Long newEdId = ids.get(idIndex--);
		        	ed.setId(newEdId);
		        	
		        	stmtEd.setLong(1, newEdId);
		        	stmtEd.setLong(2, newEntityId);

			    	if (entity.getEntityType().getId()==null) {
			    		stmtEd.setLong(3, getEntityAttributeByName(ed.getEntityAttribute().getName()).getId());
			    	}
			    	else {
			    		stmtEd.setLong(3, ed.getEntityAttribute().getId());	
			    	}
			    	
		        	stmtEd.setString(4, ed.getValue());
			        stmtEd.setString(5, subjectKey);

			        if (ed.getCreationDate()!=null) {
			        	stmtEd.setDate(6, new java.sql.Date(ed.getCreationDate().getTime()));	
			        }
			        else {
			        	stmtEd.setDate(6, defaultDate);
			        }
			        
			        if (ed.getUpdatedDate()!=null) {
			        	stmtEd.setDate(7, new java.sql.Date(ed.getUpdatedDate().getTime()));
			        }
			        else {
			        	stmtEd.setDate(7, defaultDate);
			        }
			        
			        if (ed.getOrderIndex()==null) {
			        	stmtEd.setNull(8, java.sql.Types.INTEGER); 	
			        }
			        else {
			        	stmtEd.setObject(8, ed.getOrderIndex());	
			        }

			        if (ed.getChildEntity()==null) {
			        	stmtEd.setNull(9, java.sql.Types.BIGINT); 	
			        }
			        else {
			        	stmtEd.setObject(9, ed.getChildEntity().getId());	
			        }
			        
			        stmtEd.addBatch();
		        }
		        
		        if (++entityCount % batchSize == 0) {
		        	stmtEntity.executeBatch();
		        }
		        if (++edCount % batchSize == 0) {
		        	stmtEd.executeBatch();
		        }
	        }
	        
	    	stmtEntity.executeBatch();
	    	stmtEd.executeBatch();
	    	
	    	conn.commit();
	    	
	        _logger.info("Saved bulk entity tree with root id="+newEntityId);
	        Entity saved = getEntityById(newEntityId);
	        if (saved==null) {
	        	throw new DaoException("Unknown error saving bulk entity tree");
	        }
	        return saved;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
        finally {
        	try {
                if (stmtEntity!=null) stmtEntity.close();
                if (stmtEd!=null) stmtEd.close();
                if (conn!=null) conn.close();	
        	}
        	catch (SQLException e) {
        		_logger.warn("Ignoring error encountered while closing JDBC connection",e);
        	}
        }
    }
    
    protected int getEntitiesInTree(Entity entity, List<Entity> allEntities) throws Exception {

    	int count = 1;
    	
    	for(EntityData ed : entity.getEntityData()) {
    		count++;   
    		Entity child = ed.getChildEntity();
    		if (child!=null) {
    			count += getEntitiesInTree(child, allEntities);
    		}
    	}
    	
    	allEntities.add(entity);
    	return count;
    }

    public List<String> getSubjectKeys(String subjectKey) {
    	 List<String> subjectKeyList = new ArrayList<String>();
    	 if (subjectKey==null || "".equals(subjectKey.trim())) return subjectKeyList;
    	 subjectKeyList.add(subjectKey);
    	 subjectKeyList.addAll(getGroupKeysForUsernameOrSubjectKey(subjectKey));
    	 return subjectKeyList;
    }
    
    public Set<String> getSubjectKeySet(String subjectKey) {
    	Set<String> subjectKeys = null;
    	if (subjectKey!=null) {
    		subjectKeys = new HashSet<String>(getSubjectKeys(subjectKey));
    	}
    	return subjectKeys;
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
        Set<String> subjectKeys = getSubjectKeySet(subjectKey);
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
        try {
            if (_logger.isDebugEnabled()) {
                _logger.debug("deleteAttribute(attributeName="+attributeName+")");  
            }
            
            EntityAttribute attr = getEntityAttributeByName(attributeName);
            if (attr==null) {
                throw new Exception("No such attribute: "+attributeName);
            }
            
            StringBuilder hql = new StringBuilder();
            hql.append("delete EntityData ed ");
            hql.append("where ed.entityAttribute.id=:attrId ");
            if (ownerKey!=null) {
                hql.append(" and ed.ownerKey = :ownerKey ");
            }
            
            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("attrId", attr.getId());
            if (ownerKey!=null) {
                query.setParameter("ownerKey", ownerKey);
            }
            
            int rows = query.executeUpdate();
            _logger.debug("Bulk deleted "+rows+" EntityData rows");
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Entity createAlignmentBoard(String subjectKey, String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws ComputeException {
        
        Entity board = newEntity(EntityConstants.TYPE_ALIGNMENT_BOARD, alignmentBoardName, subjectKey);
        board.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE, alignmentSpace);
        board.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, opticalRes);
        board.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, pixelRes);
        saveOrUpdate(board);

        Entity alignmentBoardFolder = getCommonRootFolderByName(subjectKey, EntityConstants.NAME_ALIGNMENT_BOARDS, true);
        if (alignmentBoardFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)==null) {
            EntityUtils.addAttributeAsTag(alignmentBoardFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            saveOrUpdate(alignmentBoardFolder);
        }
        
        addEntityToParent(alignmentBoardFolder, board, alignmentBoardFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        
        return board;
    }
    
}
