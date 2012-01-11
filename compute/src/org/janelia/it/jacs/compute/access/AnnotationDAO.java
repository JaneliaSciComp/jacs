package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.Expression;
import org.hibernate.hql.ast.tree.Statement;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.Category;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.user_data.User;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AnnotationDAO extends ComputeBaseDAO {

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

    public Entity getEntityById(Long targetId) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Entity.class);
        c.add(Expression.eq("id",targetId));
        Entity entity = (Entity) c.uniqueResult();
        if (null != entity) {
            return entity;
        }
        return null;
    }
    
    public Entity getEntityById(String targetId) {
        return getEntityById(Long.valueOf(targetId));
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
    	//deleteEntityTree(owner, entity, true, false, 0);
        try {
            deleteEntityTree3(owner, entity);
        } catch (Exception ex) {
            throw new DaoException(ex.getMessage(), ex);
        }
    }

    private void deleteEntityTree3(String owner, Entity entity) throws Exception {
        Session session = getCurrentSession();

        // First, get the user id
        StringBuffer hqlUser = new StringBuffer("select clazz from User clazz where clazz.userLogin=?");
        Query userQuery = session.createQuery(hqlUser.toString()).setString(0, owner);
        List<User> userList=userQuery.list();
        if (userList==null || userList.size()!=1) {
            throw new Exception("Could not find user for userLogin="+owner);
        }
        User user=userList.get(0);

        Map<Long,Set<Long[]>> entityMap=new HashMap<Long, Set<Long[]>>();
        Map<Long,Set<Long>> parentMap=new HashMap<Long, Set<Long>>();
        getEntityTreeForUserByJdbc(user.getUserId(), entityMap, parentMap);

        // Next, get the sub-entity-graph we care about
        Set<Long> entitySetCandidatesToDelete=walkEntityMap(entity.getId(), entityMap, null /* exclusion list */);

        // Get the subset which do not have external parents
        Set<Long> exclusionList=new HashSet<Long>();
        for (Long candidateId : entitySetCandidatesToDelete) {
            if (candidateId != entity.getId()) { /* we expect the top-level entity to have external parents but still want to delete it */
                //_logger.info("Checking external parents of candidateId=" + candidateId);
                Set<Long> parentSet = parentMap.get(candidateId);
                boolean shouldExclude = false;
                for (Long parentId : parentSet) {
                    //_logger.info("Found parentId=" + parentId);
                    if (!entitySetCandidatesToDelete.contains(parentId)) {
                        shouldExclude = true;
                        break;
                    }
                }
                if (shouldExclude) {
                    //_logger.info("Marking canidateId=" + candidateId + " for exclusion");
                    exclusionList.add(candidateId);
                } else {
                    //_logger.info("Marking candidateId=" + candidateId + " OK to include");
                }
            }
        }
        Set<Long> entitySetToDelete=walkEntityMap(entity.getId(), entityMap, exclusionList);

        // We now have our list of entities to delete. First, we need to collect a list
        // of entityData to delete. We can construct such a list by simply adding the
        // entity-data children of all the entities-to-delete, and then adding to this
        // the entity-data pointing-to the top entity.
        Set<Long> entityDataSetToDelete=new HashSet<Long>();
        for (Long entityId : entitySetToDelete) {
            Set<Long[]> edSet = entityMap.get(entityId);
            if (edSet!=null) {
                for (Long[] edArr : edSet) {
                    entityDataSetToDelete.add(edArr[0]);
                }
            } else {
                //_logger.info("Warning: found null edSet for entityId="+entityId);
            }
        }
        Set<Long> topParentSet=parentMap.get(entity.getId());
        if (topParentSet != null) {
            for (Long parentEntityId : topParentSet) {
                //_logger.info("Checking top-level parentId=" + parentEntityId + " for entity data to be deleted");
                Set<Long[]> edSet = entityMap.get(parentEntityId);
                for (Long[] edArr : edSet) {
                    //_logger.info("Found entityData entry id=" + edArr[0] + " childId=" + edArr[1]);
                    Long childEntityId = edArr[1];
                    if (childEntityId != null) {
                        if (childEntityId.longValue() == entity.getId().longValue()) {
                            //_logger.info("This matches the top-level entity, so including for deletion");
                            entityDataSetToDelete.add(edArr[0]);
                        } else {
                            //_logger.info("This does not match the top-level entity, so excluding");
                        }
                    }
                }
            }
        }

        // Now time to actually delete. First the entity-data, then the entities
        deleteIdSetByJdbc(entityDataSetToDelete, "entityData", "id");

        // debug
        for (Long entityId : entitySetToDelete) {
            //_logger.info("Entity for deletion="+entityId);
            for (Long ei : entityMap.keySet()) {
                Set<Long[]> childSet = entityMap.get(ei);
                for (Long[] cl : childSet) {
                    if (cl[1]!=null && cl[1].longValue()==entityId.longValue()) {
                        //_logger.info("Found child entityDataId="+cl[0]+" parentEntityId="+ei);
                    }
                }
            }
        }


        deleteIdSetByJdbc(entitySetToDelete, "entity", "id");
    }

    protected void getEntityTreeForUserByJdbc(Long userId, Map<Long, Set<Long[]>> entityMap, Map<Long, Set<Long>> parentMap) throws Exception   {
        Connection connection=getJdbcConnection();
        StringBuffer sql = new StringBuffer("select e.id, ed.id, ed.child_entity_id from entity e, entityData ed where e.user_id="+userId+" and ed.parent_entity_id=e.id");
        java.sql.Statement statement=connection.createStatement();
        ResultSet rs=statement.executeQuery(sql.toString());
        while(rs.next()) {
            Long entityId=rs.getBigDecimal(1).longValue();
            Long entityDataId=rs.getBigDecimal(2).longValue();
            BigDecimal childIdBD=rs.getBigDecimal(3);
            Long childId=null;
            if (childIdBD!=null) {
                childId=childIdBD.longValue();
            }
            //_logger.info("TreeResult entityId="+entityId+" entityDataId="+entityDataId+" childId="+(childId==null?"null":childId));
            Long[] childData=new Long[2];
            childData[0]=entityDataId;
            childData[1]=childId;
            // Handle child direction
            Set<Long[]> childSet=entityMap.get(entityId);
            if (childSet==null) {
                childSet=new HashSet<Long[]>();
                entityMap.put(entityId, childSet);
            }
            childSet.add(childData);
            // Handle parent direction
            if (childId != null) {
                Set<Long> parentSet = parentMap.get(childId);
                if (parentSet == null) {
                    parentSet = new HashSet<Long>();
                    parentMap.put(childId, parentSet);
                }
                parentSet.add(entityId);
            }
        }
        statement.close();
        connection.close();
    }

    protected void deleteIdSetByJdbc(Set<Long> idSet, String tableName, String identifierColumnName) throws Exception {
        Connection connection=getJdbcConnection();
        int batchSize=200;
        int batchStart=0;
        Iterator<Long> edIter=idSet.iterator();
        int deleteCount=0;
        for (int position=0;position<idSet.size();position++) {
            if ( (position!=0 && position%batchSize==0) || (position==idSet.size()-1)) {
                java.sql.Statement statement=connection.createStatement();
                StringBuffer deleteSqlBuffer=new StringBuffer("DELETE FROM "+tableName+" WHERE "+identifierColumnName+" IN (");
                if (position==(idSet.size()-1)) {
                    position++; // to trigger inclusion of all members on last batch
                }
                for (int i=batchStart;i<position;i++) {
                    if (i!=batchStart) {
                        deleteSqlBuffer.append(", ");
                    }
                    deleteSqlBuffer.append(edIter.next());
                    deleteCount++;
                }
                deleteSqlBuffer.append(")");
                _logger.info("Calling executeUpdate on=" + deleteSqlBuffer.toString());
                statement.executeUpdate(deleteSqlBuffer.toString());
                statement.close();
                batchStart=position;
            }
        }
        if (deleteCount!=idSet.size()) {
            throw new Exception("Delete count="+deleteCount+" does not match set size="+idSet.size());
        }
        connection.commit();
        connection.close();
    }

    protected Set<Long> walkEntityMap(Long startEntityId, Map<Long, Set<Long[]>> treeMap, Set<Long> exclusionList) {
        Set<Long> inclusionList = new HashSet<Long>();
        if (exclusionList==null) {
            exclusionList=new HashSet<Long>(); /* empty */
        }
        if (!exclusionList.contains(startEntityId)) {
            inclusionList.add(startEntityId);
            Set<Long[]> childSet = treeMap.get(startEntityId);
            if (childSet != null) {
                for (Long[] childInfo : childSet) {
                    Long childEntityId = childInfo[1];
                    if (childEntityId != null && !exclusionList.contains(childEntityId)) {
                        /* note: this excludes the subtree of the excluded node */
                        if (!inclusionList.contains(childEntityId))
                            inclusionList.add(childEntityId);
                        Set<Long> childList = walkEntityMap(childEntityId, treeMap, exclusionList);
                        for (Long cl : childList) {
                            if (!inclusionList.contains(cl) && !exclusionList.contains(cl))
                                inclusionList.add(cl);
                        }
                    }
                }
            }
        }
        return inclusionList;
    }

    void getEntitySetFromTree(Entity entity, Set<Long> entityIdSet) throws Exception {
        if (!entityIdSet.contains(entity.getId())) {
            entityIdSet.add(entity.getId());
        }
        for (EntityData ed : entity.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null && !entityIdSet.contains(child.getId())) {
                entityIdSet.add(child.getId());
                getEntitySetFromTree(child, entityIdSet);
            }
        }
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

//              createEntityAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
//              createEntityAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_QM_SCORE);

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

//            Set<String> screenSampleAttributeSet = new HashSet<String>();
//            screenSampleAttributeSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//            screenSampleAttributeSet.add(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
//            createEntityType(EntityConstants.TYPE_SCREEN_SAMPLE, screenSampleAttributeSet);
//
//            Set<String> alignedStackAttributeSet = new HashSet<String>();
//            alignedStackAttributeSet.add(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
//            alignedStackAttributeSet.add(EntityConstants.ATTRIBUTE_ALIGNMENT_QM_SCORE);
//            alignedStackAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            alignedStackAttributeSet.add(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
//            createEntityType(EntityConstants.TYPE_ALIGNED_BRAIN_STACK, alignedStackAttributeSet);

            Set<String> flyLineAttributeSet = new HashSet<String>();
            flyLineAttributeSet.add(EntityConstants.ATTRIBUTE_ENTITY);
            createEntityType(EntityConstants.TYPE_FLY_LINE, flyLineAttributeSet);
//
//            Set<String> lsmStackPairAttributeSet = new HashSet<String>();
//            lsmStackPairAttributeSet.add(EntityConstants.ATTRIBUTE_ENTITY);
//            createEntityType(EntityConstants.TYPE_LSM_STACK_PAIR, lsmStackPairAttributeSet);

//            Set<String> stitchedStackAttributeSet = new HashSet<String>();
//            stitchedStackAttributeSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
//            createEntityType(EntityConstants.TYPE_STITCHED_V3D_RAW, stitchedStackAttributeSet);
            
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
    	if (getEntityTypeByName(name) != null) {
    		throw new DaoException("Entity type "+name+" already exists");
    	}
        Session session = getCurrentSession();
    	
    	try {
	    	EntityType entityType = new EntityType();
	    	entityType.setName(name);
	        session.saveOrUpdate(entityType);
            _logger.info("Created new EntityType " + name);

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
            _logger.info("Created new EntityAttribute " + attrName);
	        
    	}
    	catch (Exception e) {
    		throw new DaoException(e);
    	}
    	
        addAttributeToEntityType(entityType, entityAttr);
        return entityAttr;
    }
    
    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException {
    	EntityType entityType = getEntityTypeByName(entityTypeName);
    	if (entityType == null) {
    		throw new ComputeException("Entity type "+entityTypeName+" does not exist");
    	}
    	EntityAttribute entityAttr = getEntityAttributeByName(attrName);
    	if (entityAttr != null) {
    		entityAttr = addAttributeToEntityType(entityType, entityAttr);	
    		return entityAttr;
    	}
    	else {
    		entityAttr = addAttributeToEntityType(entityType, attrName);	
    		return entityAttr;
    	}

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

    public List<Entity> getUserCommonRoots(String userLogin, String entityTypeName) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select e from Entity e");
            hql.append(" where e.entityType.name=?");
            if (null != userLogin) {
                hql.append(" and e.user.userLogin=?");
            }
            hql.append(" and exists (from EntityData as attr where attr.parentEntity = e " +
            		"and attr.entityAttribute.name = '"+EntityConstants.ATTRIBUTE_COMMON_ROOT+"')");
            Query query = session.createQuery(hql.toString()).setString(0, entityTypeName);
            if (null != userLogin) {
                query.setString(1, userLogin);
            }
            return query.list();
        } catch (Exception e) {
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

        Task task = getTaskById(sessionId);
        if (task == null) 
            throw new DaoException("Session not found");
        
		if (!(task instanceof AnnotationSessionTask)) 
			throw new DaoException("Task is not an annotation session");
		
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

    public List<Entity> getEntitiesForAnnotationSession(String username, long sessionId) throws ComputeException {
        
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
        	List<Entity> entities = getEntitiesInList(entityIds);	
        	for(Entity entity : entities) {
        		populateDescendants(entity);
        	}
        	return entities;
        }
    }
	
    public List<Entity> getCategoriesForAnnotationSession(String username, long sessionId) throws ComputeException {
        
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
        	return getEntitiesInList(entityIds);	
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
    
	private void updateAnnotationSession(long sessionId) throws ComputeException {

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
					for(Entity p : getParentEntities(termId)) {
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
	}
	
    public List<Entity> getPublicOntologies() throws ComputeException {

        EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
        EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
        
        StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
        hql.append(" where clazz.entityType.id=?");
        hql.append(" and exists (from EntityData as attr where attr.parentEntity = clazz and attr.entityAttribute.id = ?)");
        Query query = getCurrentSession().createQuery(hql.toString());
        query.setLong(0, tmpType.getId());
        query.setLong(1, tmpAttr.getId());
        return query.list();
    }
    
    public List<Entity> getPrivateOntologies(String userLogin) throws ComputeException {
    	
        EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
        EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
        
        StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
        hql.append(" where clazz.entityType.id=?");
        if (null != userLogin) {
            hql.append(" and clazz.user.userLogin=?");
        }
        hql.append(" and not exists (from EntityData as attr where attr.parentEntity = clazz and attr.entityAttribute.id = ?)");
        Query query = getCurrentSession().createQuery(hql.toString());
        query.setLong(0, tmpType.getId());
        query.setString(1, userLogin);
        query.setLong(2, tmpAttr.getId());
        return query.list();
    }
    
    public Entity createOntologyRoot(String userLogin, String rootName) throws ComputeException {
        
        User tmpUser = getUserByName(userLogin);
        Entity newOntologyRoot = newEntity(EntityConstants.TYPE_ONTOLOGY_ROOT, rootName, tmpUser);
        saveOrUpdate(newOntologyRoot);

        // Add the type
        EntityData termData = newData(newOntologyRoot, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, tmpUser);
        termData.setValue(Category.class.getSimpleName());
        saveOrUpdate(termData);

        return newOntologyRoot;
    }

    public EntityData createOntologyTerm(String userLogin, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {

        User tmpUser = getUserByName(userLogin);
        Entity parentOntologyElement = getEntityById(ontologyTermParentId.toString());

        // Check first to see if it's necessary to coerce the parent's type
        EntityData parentTypeED = parentOntologyElement.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
        _logger.info("Parent's type is "+parentTypeED.getValue());
        if (parentTypeED!=null && "Tag".equals(parentTypeED.getValue())) {
            // Adding a child to a Tag, so it must be coerced into a Category
        	parentTypeED.setValue(Category.class.getSimpleName());
            saveOrUpdate(parentTypeED);
        }
        
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
        saveOrUpdate(newOntologyElement);
        
        // Associate the entity to the parent
        EntityData childData = newData(parentOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT, tmpUser);
        childData.setChildEntity(newOntologyElement);
        childData.setOrderIndex(orderIndex);
        saveOrUpdate(childData);
        
        return childData;
    }
    
    public Entity cloneEntityTree(Long sourceRootId, String targetUserLogin, String targetRootName) throws ComputeException {

    	Entity sourceRoot = getEntityById(sourceRootId.toString());    	
    	if (sourceRoot == null) {
    		throw new DaoException("Cannot find the source root.");
    	}
    	
        User targetUser = getUserByName(targetUserLogin);
        if (targetUser == null) {
            throw new DaoException("Cannot find the target user.");
        }

    
        Entity cloned = cloneEntityTree(sourceRoot, targetUser, targetRootName, true);
        return cloned;
    }

	// TODO: detect cycles
    private Entity cloneEntityTree(Entity source, User targetUser, String targetName, boolean isRoot) throws DaoException {

        EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
        
        // Create new ontology element
        Entity newOntologyElement = new Entity(null, targetName, targetUser, null, source.getEntityType(), new Date(), new Date(), null);
        saveOrUpdate(newOntologyElement);

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

            saveOrUpdate(newEd);
    	}

    	return newOntologyElement;
    }

    public Entity publishOntology(Long sourceRootId, String targetRootName) throws ComputeException {

    	Entity sourceRoot = getEntityById(sourceRootId.toString());    	
    	if (sourceRoot == null) {
    		throw new DaoException("Cannot find the source root.");
    	}
	
        Entity clonedEntity = cloneEntityTree(sourceRoot, sourceRoot.getUser(), targetRootName, true);

        // Add the public tag
        EntityData publicEd = newData(clonedEntity, EntityConstants.ATTRIBUTE_IS_PUBLIC, sourceRoot.getUser());
        publicEd.setValue("true");
        saveOrUpdate(publicEd);
        
        return clonedEntity;
    }
    
	public Entity createOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException {

        try {
        	// TODO: enable this sanity check
//        	Entity targetEntity = getEntityById(targetEntityId);
//        	if (targetEntity == null) {
//        		throw new IllegalArgumentException("Target entity with id="+targetEntityId+" not found");
//        	}
        	
            User tmpUser = getUserByName(userLogin);
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
            
            saveOrUpdate(newAnnotation);
            
            // Notify the session 
            if (annotation.getSessionId() != null) {
            	updateAnnotationSession(annotation.getSessionId());
            }
            
            return newAnnotation;
        }
        catch (Exception e) {
            _logger.error("Error creating ontology annotation for user "+userLogin,e);
            throw new ComputeException("Error creating ontology annotation for user "+userLogin,e);
        }
	}

	public void removeOntologyAnnotation(String userLogin, long annotationId) throws ComputeException {
        Entity entity = getUserEntityById(userLogin, annotationId);
        getCurrentSession().delete(entity);	
        
        // Notify the session 
        String sessionId = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_SESSION_ID);
        if (sessionId != null) updateAnnotationSession(Long.parseLong(sessionId));
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
            // Notify the session 
            updateAnnotationSession(sessionId);
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
	public List<Entity> getEntitiesInList(List<Long> entityIds) throws DaoException {
		StringBuffer entityIdStr = new StringBuffer();
		for(Long entityId : entityIds) {
			if (entityIdStr.length()>0) entityIdStr.append(",");
			entityIdStr.append(entityId);
		}
		return getEntitiesInList(entityIdStr.toString());
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
            for(String entityId : entityIds.split("\\s*,\\s*")) {
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
    public Entity populateDescendants(Entity entity) {
    	if (entity == null) return null;
    	for(EntityData ed : entity.getEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child != null) {
    			populateDescendants(child);
    		}
    	}
    	return entity;
    }

    /**
     * Perform combination of queries to populate entity graph.
     * @param id
     * @return
     */
    public Entity getEntityTreeQuery(Long id) {
        Session session=this.getCurrentSession();
        Criteria c = session.createCriteria(Entity.class);
        List allEntities=c.list();
        return null;
    }
    
    /**
     * Searches the tree for the given entity and returns its ancestor of a given type, or null if the entity or 
     * ancestor does not exist.
     * @param entity
     * @param type
     * @return
     */
    public Entity getAncestorWithType(Entity entity, String type) throws DaoException {

    	if (entity.getEntityType().getName().equals(type)) return entity;
    	
    	for(Entity parent : getParentEntities(entity.getId())) {
    		Entity ancestor = getAncestorWithType(parent, type);
    		if (ancestor != null) return ancestor;
    	}
    	
    	return null;
    }

    private static final int MAX_SEARCH_PATHS = 10;
    public List<List<Long>> searchTree(Long rootId, String searchString) throws DaoException {

        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select e from Entity e where e.name like ?");
        Query query = session.createQuery(hql.toString()).setString(0, searchString);
        List<Entity> results = query.list();

        _logger.info("searchTree got "+results.size());
        
        List<List<Long>> matches = new ArrayList<List<Long>>();
        for(Entity entity : results) {
        	List<Long> path = getPathToRoot(entity.getId(), rootId);
        	if (path != null) matches.add(path);
        	if (matches.size()>=MAX_SEARCH_PATHS) break;
        }
        
        _logger.info("searchTree limited to "+matches.size());
        
        return matches;
    }
    
    public List<Long> getPathToRoot(Long entityId, Long rootId) throws DaoException {

		List<Long> ids = new ArrayList<Long>();
		
    	if (entityId.equals(rootId)) {
    		ids.add(rootId);
    		return ids;
    	}
    	
    	for(Entity parent : getParentEntities(entityId)) {
    		List<Long> path = getPathToRoot(parent.getId(), rootId);
    		if (path != null) {
    			path.add(entityId);
    			return path;
    		}
    	}
    	
    	// No path to the given root
    	return null;
    }

    public List<Entity> getEntitiesWithAttributeValue(String attrName, String attrValue) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed where ed.entityAttribute.name=? and ed.value like ?");
            Query query = session.createQuery(hql.toString()).setString(0, attrName).setString(1, attrValue);
            return query.list();
        } catch (Exception e) {
            throw new DaoException(e);
        }
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
    
}
