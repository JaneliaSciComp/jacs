package org.janelia.it.jacs.compute.access;

import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

public class AnnotationDAO extends ComputeBaseDAO {

    public AnnotationDAO(Logger logger) {
        super(logger);
    }

    /**
     * This method gets the annotations for an entity and then removes the one that matches the value annotated.
     * This would be better if we had the annotation entity id, though.
     * @param owner person who annotated the entity
     * @param annotatedEntityId the id of the entity that the annotation is for
     * @param tag the value of the annotation that is to be removed
     * @return returns boolean of success
     * @throws DaoException error trying to delete the annotation
     */
    public boolean deleteAnnotation(String owner, String annotatedEntityId, String tag) throws DaoException {
        try {
            if (null==tag || "".equals(tag)) { return false; }
            ArrayList<Long> tmpIds = new ArrayList<Long>();
            tmpIds.add(Long.parseLong(annotatedEntityId));
            List<Entity> tmpAnnotations = getAnnotationsByEntityId(owner, tmpIds);
            Entity tmpAnnotation=null;
            for (Entity annotation : tmpAnnotations) {
                if (annotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM).getParentEntity().getName().equals(tag)) {
                    tmpAnnotation = annotation;
                }
            }
            if (null == tmpAnnotation) {
                // This should never happen
                throw new DaoException("Cannot complete deletion when there are more than one annotation with that identifier.");
            }
            if (!tmpAnnotation.getUser().getUserLogin().equals(owner)) {
                throw new DaoException("Cannot delete the annotation as the requestor doesn't own the annotation.");
            }
            getCurrentSession().delete(tmpAnnotation);
            _logger.debug("The annotation has been deleted.");
            return true;
        }
        catch (Exception e) {
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
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select clazz.parentEntity from EntityData clazz where value=?");
        Query query = session.createQuery(hql.toString()).setString(0, filePath);
        List<Entity> entityList = (List<Entity>) query.list();
        return entityList;
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

    public Entity getEntityById(String targetId) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Entity.class);
        c.add(Expression.eq("id", Long.valueOf(targetId)));
        Entity entity = (Entity) c.uniqueResult();
        if (null != entity) {
            return entity;
        }
        return null;
    }

    public EntityAttribute getEntityAttribute(long entityAttributeConstant) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(EntityAttribute.class);
        c.add(Expression.eq("id", entityAttributeConstant));
        EntityAttribute attribute = (EntityAttribute) c.uniqueResult();
        if (null != attribute) {
            return attribute;
        }
        return null;
    }

    public boolean deleteOntologyTerm(String userLogin, String ontologyTermId) throws DaoException {
        Session session = getCurrentSession();
        try {
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", Long.valueOf(ontologyTermId)));
            Entity entity = (Entity) c.uniqueResult();
            if (null == entity) {
                // This should never happen
                throw new DaoException("Cannot complete deletion when there are no entities with that identifier.");
            }
            if (!entity.getUser().getUserLogin().equals(userLogin)) {
                throw new DaoException("Cannot delete the entity as the requestor doesn't own the item.");
            }

            _logger.info("Will delete tree rooted at Entity "+entity.getName());
            deleteEntityTree(userLogin, entity);
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
    	deleteEntityTree(owner, entity, true, false, 0);
    }
    
    private void deleteEntityTree(String owner, Entity entity, boolean ignoreRefs, boolean ignoreAncestorRefs, int level) throws DaoException {

    	StringBuffer indent = new StringBuffer();
		for (int i = 0; i < level; i++) {
			indent.append("  ");
		}
		
		// Null check
    	if (entity == null) {
    		_logger.warn(indent+"Cannot delete null entity");
    		return;
    	}
		
		// Ownership check
    	if (!entity.getUser().getUserLogin().equals(owner)) {
    		_logger.info(indent+"Cannot delete entity because owner ("+entity.getUser().getUserLogin()+") does not match invoker ("+owner+")");
    		return;
    	}

    	_logger.info(indent+"Deleting "+entity.getName());
    	
    	// Reference check - does this entity have more than one parent pointing to it?
    	Set<EntityData> eds = getParentEntityDatas(entity.getId());
    	boolean moreThanOneParent = eds.size() > 1;
        if (moreThanOneParent && !ignoreRefs) {
        	_logger.info(indent+"  Cannot delete "+entity.getName()+" because more than one parent is pointing to it");
        	return;
        }

        // Delete all ancestors first
        for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
        	Entity child = ed.getChildEntity();
        	if (child != null) {
        		deleteEntityTree(owner, child, ignoreAncestorRefs, ignoreAncestorRefs, level+1);
        	}
        	// We have to manually remove the EntityData from its parent, otherwise we get this error: 
        	// "deleted object would be re-saved by cascade (remove deleted object from associations)"
        	ed.getParentEntity().getEntityData().remove(ed);
    		getCurrentSession().delete(ed);
        }
        
        // Delete all parent EDs
        for(EntityData ed : eds) {
			// This ED points to the term to be deleted. We must delete the ED first to avoid violating constraints.
        	ed.getParentEntity().getEntityData().remove(ed);
    		getCurrentSession().delete(ed);
        }
        
        // Finally we can delete the entity itself
        getCurrentSession().delete(entity);
    }
    
    public EntityAttribute getEntityAttributeByName(String name) throws DaoException {
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

    public EntityType getEntityTypeByName(String name) throws DaoException {
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

    // This method should be cross-synchronized with the EntityConstants class until we write the code-generation
    // service to do this automatically.

    public void setupEntityTypes() throws DaoException {
        try {
            
//            //========== Status ============
//            createEntityStatus(EntityConstants.STATUS_DEPRECATED);
//
//            //========== Attribute ============
//            createEntityAttribute(EntityConstants.ATTRIBUTE_FILE_PATH);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_COMMON_ROOT);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ENTITY);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_IS_PUBLIC);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_ROOT_ID);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
//            createEntityAttribute(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
//
//            //========== Type ============
//            Set<String> lsmAttributeNameSet = new HashSet<String>();
//            lsmAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            createEntityType(EntityConstants.TYPE_LSM_STACK, lsmAttributeNameSet);
//
//            Set<String> ontologyElementAttributeNameSet = new HashSet<String>();
//            ontologyElementAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
//            ontologyElementAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
//            ontologyElementAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER);
//            ontologyElementAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER);
//            createEntityType(EntityConstants.TYPE_ONTOLOGY_ELEMENT, ontologyElementAttributeNameSet);
//
//            Set<String> ontologyRootAttributeNameSet = new HashSet<String>();
//            ontologyRootAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
//            ontologyRootAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
//            ontologyRootAttributeNameSet.add(EntityConstants.ATTRIBUTE_IS_PUBLIC);
//            createEntityType(EntityConstants.TYPE_ONTOLOGY_ROOT, ontologyRootAttributeNameSet);
//
//            Set<String> folderAttributeNameSet = new HashSet<String>();
//            folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_COMMON_ROOT);
//            folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//            createEntityType(EntityConstants.TYPE_FOLDER, folderAttributeNameSet);
//
//            Set<String> neuronSeparationAttributeNameSet = new HashSet<String>();
//            neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_INPUT);
//            neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//            createEntityType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, neuronSeparationAttributeNameSet);
//
//            Set<String> tif2DImageAttributeSet = new HashSet<String>();
//            tif2DImageAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            createEntityType(EntityConstants.TYPE_TIF_2D, tif2DImageAttributeSet);
//
//            Set<String> tif3DImageAttributeSet = new HashSet<String>();
//            tif3DImageAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            createEntityType(EntityConstants.TYPE_TIF_3D, tif3DImageAttributeSet);
//
//            Set<String> tif3DLabelMaskAttributeSet = new HashSet<String>();
//            tif3DLabelMaskAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            createEntityType(EntityConstants.TYPE_TIF_3D_LABEL_MASK, tif3DLabelMaskAttributeSet);
//
//            Set<String> sampleAttributeSet = new HashSet<String>();
//            sampleAttributeSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//            createEntityType(EntityConstants.TYPE_SAMPLE, sampleAttributeSet);
//
//            Set<String> lsmStackPairAttributeSet = new HashSet<String>();
//            lsmStackPairAttributeSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//            createEntityType(EntityConstants.TYPE_LSM_STACK_PAIR, lsmStackPairAttributeSet);

            Set<String> stitchedStackAttributeSet = new HashSet<String>();
            stitchedStackAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
            createEntityType(EntityConstants.TYPE_STITCHED_V3D_RAW, stitchedStackAttributeSet);
            
        }
        catch (Exception e) {
            e.printStackTrace();
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
        Session session = getCurrentSession();
    	
    	try {
	    	EntityType entityType = new EntityType();
	    	entityType.setName(name);
	        session.saveOrUpdate(entityType);
            _logger.info("Created new EntityType " + name);
            
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
            _logger.info("Created new EntityAttribute " + attrName);
	        
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    	
        addAttributeToEntityType(entityType, entityAttr);
        return entityAttr;
    }
    
    public EntityAttribute addAttributeToEntityType(EntityType entityType, EntityAttribute entityAttr) throws DaoException  {

        Session session = getCurrentSession();
    	
    	try {
    		Set<EntityAttribute> attrs = entityType.getAttributes();
    		if (attrs == null) {
    			attrs = new HashSet<EntityAttribute>();
    			entityType.setAttributes(attrs);
    		}
    		
    		attrs.add(entityAttr);
	        session.saveOrUpdate(entityType);
            _logger.info("Added EntityAttribute " + entityAttr.getName() + " to EntityType "+entityType.getName());
	        
	        return entityAttr;
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    }
    
    protected void createEntityType(String name, Set<String> attributeNameSet) {
        Session session = getCurrentSession();

        Criteria c = session.createCriteria(EntityType.class);
        List<EntityType> entityTypeList = (List<EntityType>) c.list();
        EntityType entityType = null;

        boolean containsType = false;
        for (EntityType et : entityTypeList) {
            if (et.getName().equals(name)) {
                _logger.info("Found EntityType name=" + name + " id=" + et.getId());
                entityType = et;
                containsType = true;
            }
        }
        if (!containsType) {
            entityType = new EntityType();
            entityType.setName(name);
            session.saveOrUpdate(entityType);
            _logger.info("Created EntityType name=" + name + " id=" + entityType.getId());
        }
        Set<EntityAttribute> currentAttributeSet = entityType.getAttributes();
        if (attributeNameSet != null) {
            Set<EntityAttribute> attributeSet = getEntityAttributesByName(attributeNameSet);
            entityType.setAttributes(attributeSet);
        }
        session.saveOrUpdate(entityType);
    }

    protected void createEntityAttribute(String name) {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(EntityAttribute.class);
            List<EntityAttribute> attributeList = (List<EntityAttribute>) c.list();
            boolean containsAttribute=false;
            for (EntityAttribute ea : attributeList) {
                if (ea.getName().equals(name)) {
                    containsAttribute=true;
                }
            }
            if (!containsAttribute) {
                EntityAttribute entityAttribute = new EntityAttribute();
                entityAttribute.setName(name);
                session.save(entityAttribute);
            }
        }
        catch (HibernateException e) {
            e.printStackTrace();
        }
    }

    protected Set<EntityAttribute> getEntityAttributesByName(Set<String> names) {
        Session session = getCurrentSession();

        Criteria c = session.createCriteria(EntityAttribute.class);
        List<EntityAttribute> attributeList = (List<EntityAttribute>) c.list();

        Set<EntityAttribute> resultSet = new HashSet<EntityAttribute>();
        for (EntityAttribute ea : attributeList) {
            if (names.contains(ea.getName())) {
                resultSet.add(ea);
            }
        }
        return resultSet;
    }


    public List<EntityType> getAllEntityTypes() throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select clazz from EntityType clazz");
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getAllEntityTypes");
        }
    }

    public List<EntityAttribute> getAllEntityAttributes() throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select clazz from EntityAttribute clazz");
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getAllEntityAttributes");
        }
    }

    public List<Entity> getUserEntitiesByTypeName(String userLogin, String entityTypeName) throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
            hql.append(" where clazz.entityType.name='").append(entityTypeName).append("'");
            if (null != userLogin) {
                hql.append(" and clazz.user.userLogin='").append(userLogin).append("'");
            }
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getUserEntitiesByTypeName");
        }
    }

    public Set<Entity> getEntitiesByName(String name) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("name", name));
            List l = c.list();
            Set<Entity> resultSet = new HashSet<Entity>();
            for (Object o : l) {
                Entity e = (Entity) o;
                resultSet.add(e);
            }
            return resultSet;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Entity getUserEntityById(String userLogin, long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", entityId));
            Entity tmpEntity = (Entity) c.uniqueResult();
            if (tmpEntity.getUser().getUserLogin().equals(userLogin)) {
                return tmpEntity;
            }
            else {
                throw new Exception("User " + userLogin + " does not own item " + entityId);
            }
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    // todo This probably needs to be changed to account for the Entity Data first
    public boolean deleteEntityById(Long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", Long.valueOf(entityId)));
            Entity entity = (Entity) c.uniqueResult();
            session.delete(entity);
            _logger.debug("The entity id=" + entityId + " has been deleted.");
            return true;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Set<EntityData> getParentEntityDatas(long childEntityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select clazz from EntityData clazz where clazz.childEntity.id=?");
            Query query = session.createQuery(hql.toString()).setLong(0, childEntityId);
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Set<Entity> getParentEntities(long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select clazz.parentEntity from EntityData clazz where clazz.childEntity.id=?");
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Set<Entity> getChildEntities(long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select clazz.childEntity from EntityData clazz where clazz.parentEntity.id=?");
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getAnnotationsByEntityId(String username, List<Long> entityIds) throws DaoException {
        try {
        	if (entityIds.isEmpty()) {
        		return new ArrayList<Entity>();
        	}
        	StringBuffer entityCommaList = new StringBuffer();
        	for(Long id : entityIds) {
        		if (entityCommaList.length()>0) entityCommaList.append(",");
        		entityCommaList.append(id);
        	}
        	
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed where " +
                    "ed.entityAttribute.name = ? " +
                    "and ed.value in ("+entityCommaList+") ");
            Query query = session.createQuery(hql.toString());
            query.setString(0, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
            // TODO: check userLogin if the session is private
            return query.list();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public List<Entity> getAnnotationsForSession(String userLogin, long sessionId) throws DaoException {

        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed where " +
                    "ed.entityAttribute.name = ? " +
                    "and ed.value = ? ");
            Query query = session.createQuery(hql.toString());
            query.setString(0, EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
            query.setString(1, ""+sessionId);
            // TODO: check userLogin if the session is private
            return query.list();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
	}

	public void removeOntologyAnnotation(String userLogin, long annotationId) throws DaoException {
        Entity entity = getUserEntityById(userLogin, annotationId);
        getCurrentSession().delete(entity);	
	}

    public void removeAllOntologyAnnotationsForSession(String userLogin, long sessionId) throws DaoException {

        try {
            for(Object o : getAnnotationsForSession(userLogin, sessionId)) {
                Entity entity = (Entity)o;
                if (!entity.getUser().getUserLogin().equals(userLogin)) {
                	_logger.info("Cannot remove annotation "+entity.getId()+" not owned by "+userLogin);
                }
                else {
                	_logger.info("Removing annotation "+entity.getId());
                    getCurrentSession().delete(entity);	
                }
            }
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public List<Entity> getEntitiesInList(String entityIds) throws DaoException {
        try {
        	if (entityIds == null || "".equals(entityIds)) {
        		return new ArrayList<Entity>();
        	}
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select e from Entity e where " +
                    "e.id in ("+entityIds+") ");
            Query query = session.createQuery(hql.toString());
            List<Entity> results = query.list();
            
            // Resort the results in the order that the ids were given
            
            Map<String,Entity> map = new HashMap<String,Entity>();
            for(Entity entity : results) {
            	map.put(entity.getId().toString(), entity);
            }
            
            List<Entity> sortedList = new ArrayList<Entity>();
            for(String entityId : entityIds.split("\\s+,\\s+")) {
            	sortedList.add(map.get(entityId));
            }
            
            return sortedList;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
	}

}
