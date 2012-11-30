package org.janelia.it.jacs.compute.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.compute.service.fly.MaskSampleAnnotationService;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.Category;
import org.janelia.it.jacs.model.ontology.types.EnumText;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;
import org.janelia.it.jacs.shared.annotation.PatternAnnotationDataManager;
import org.janelia.it.jacs.shared.annotation.RelativePatternAnnotationDataManager;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

public class AnnotationDAO extends ComputeBaseDAO {

	/** Most amount of nodes a tree can have before it's considered a "large" tree */
	private static final Long MAX_SMALL_TREE_SIZE = 5000L;
	
	/** Batch fetch size for JDBC result sets */
	protected final static int JDBC_FETCH_SIZE = 200;
	
    private static final Map<String, EntityType> entityByName = Collections.synchronizedMap(new HashMap<String, EntityType>());
    private static final Map<String, EntityAttribute> attrByName = Collections.synchronizedMap(new HashMap<String, EntityAttribute>());


    private static final List<String> SYSTEM_USER = Arrays.asList("system");

    private boolean debugDeletions = false;
    
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
        try {
        	deleteSmallEntityTree(owner, entity);
        	
        	// The large tree deletion algorithm is generally too slow these days, due to the growth of users'
        	// entity graphs.
        	
//        	if ("system".equals(owner)) {
//        		deleteSmallEntityTree(owner, entity);
//        	}
//        	else {
//    	    	Long count = countDescendants(owner, entity, MAX_SMALL_TREE_SIZE);
//    	    	if (count <= MAX_SMALL_TREE_SIZE) {
//    	    		_logger.info("Running small tree algorithm (count="+count+")");
//    	        	deleteSmallEntityTree(owner, entity);
//    	    	}
//    	    	else {
//    	    		_logger.info("Running large tree algorithm (count="+count+")");
//    	            deleteLargeEntityTree(owner, entity);
//    	    	}
//        	}
        } catch (Exception ex) {
            throw new DaoException(ex.getMessage(), ex);
        }
    }

    private void deleteLargeEntityTree(String owner, Entity entity) throws Exception {
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
        Map<Long,Set<Long>> nonOwnedParentEdMap=new HashMap<Long, Set<Long>>();
        Set<Long> commonRootSet =  new HashSet<Long>();
        _logger.info("Building entity graph for user "+owner);
        getEntityTreeForUserByJdbc(user.getUserId(), entityMap, parentMap, commonRootSet);
        _logger.info("Built entity graph for user "+owner+". entityMap.size="+entityMap.size()+" parentMap.size="+parentMap.size());
        getNonOwnedParents(user.getUserId(), nonOwnedParentEdMap);
        
        // Next, get the sub-entity-graph we care about
        Set<Long> entitySetCandidatesToDelete=walkEntityMap(entity.getId(), entityMap, new HashSet<Long>());
        
        if (debugDeletions) {
	        for(Long id : entitySetCandidatesToDelete) {
	        	_logger.info("entitySetCandidatesToDelete contains "+id);
	        }
        }
        
        // Get the subset which do not have external parents
        Set<Long> exclusionList=new HashSet<Long>();
        for (Long candidateId : entitySetCandidatesToDelete) {
            if (candidateId != entity.getId()) { /* we expect the top-level entity to have external parents but still want to delete it */
            	if (debugDeletions) _logger.info("Checking external parents of candidateId=" + candidateId);
                Set<Long> parentSet = parentMap.get(candidateId);
                boolean shouldExclude = false;
                for (Long parentId : parentSet) {
                	if (debugDeletions) _logger.info("Found parentId=" + parentId);
                    if (!entitySetCandidatesToDelete.contains(parentId)) {
                        shouldExclude = true;
                        break;
                    }
                }
                if (debugDeletions) _logger.info("Checking for common rootness of candidateId=" + candidateId);
                if (commonRootSet.contains(candidateId)) {
                	if (debugDeletions) _logger.info("Excluding common root candidateId=" + candidateId);
                	shouldExclude = true;
                }
                if (shouldExclude) {
                	if (debugDeletions) _logger.info("Marking candidateId=" + candidateId + " for exclusion");
                    exclusionList.add(candidateId);
                } else {
                	if (debugDeletions) _logger.info("Marking candidateId=" + candidateId + " OK to include");
                }
            }
        }

    	if (debugDeletions) _logger.info("Checking for additional exclusions...");
        int i = 0;
        Set<Long> additionalExclusions;
        do {
        	additionalExclusions = getAdditionalExclusions(parentMap, entitySetCandidatesToDelete, exclusionList);
        	if (debugDeletions) _logger.info("Got "+additionalExclusions.size()+" additional exclusions on iteration "+i);
        	exclusionList.addAll(additionalExclusions);
        }
        while (!additionalExclusions.isEmpty() && ++i<100);
        
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
            }
        }
        Set<Long> topParentSet=parentMap.get(entity.getId());
        if (topParentSet != null) {
            for (Long parentEntityId : topParentSet) {
            	if (debugDeletions) _logger.info("Checking top-level parentId=" + parentEntityId + " for entity data to be deleted");
                Set<Long[]> edSet = entityMap.get(parentEntityId);
                for (Long[] edArr : edSet) {
                	if (debugDeletions) _logger.info("  Found entityData entry id=" + edArr[0] + " childId=" + edArr[1]);
                    Long childEntityId = edArr[1];
                    if (childEntityId != null) {
                        if (childEntityId.longValue() == entity.getId().longValue()) {
                        	if (debugDeletions) _logger.info("    This matches the top-level entity, so including for deletion");
                            entityDataSetToDelete.add(edArr[0]);
                        } 
                        else if (entitySetToDelete.contains(childEntityId.longValue())) {
                        	if (debugDeletions) _logger.info("    This matches an internal node, so including for deletion");
                            entityDataSetToDelete.add(edArr[0]);
                        } 
                        else {
                        	if (debugDeletions) _logger.info("    This does not match the top-level entity or any internal entity, so excluding");
                        }
                    }
                }
            }
        }

        // Add all EDs from parents we don't own 
        for (Long entityId : entitySetToDelete) {
        	Set<Long> parentEdIds = nonOwnedParentEdMap.get(entityId);
        	if (parentEdIds!=null) {
	        	for(Long parentEdId : parentEdIds) {
	        		_logger.info("Adding ED reference (id="+parentEdId+") from non-owned entity, to owned entity with id="+entityId);	
	        	}
	        	entityDataSetToDelete.addAll(parentEdIds);
        	}
        }
        
        if (debugDeletions) {
	        for (Long entityId : entitySetToDelete) {
	            _logger.info("Entity for deletion="+entityId);
//	            for (Long ei : entityMap.keySet()) {
//	                Set<Long[]> childSet = entityMap.get(ei);
//	                for (Long[] cl : childSet) {
//	                    if (cl[1]!=null && cl[1].longValue()==entityId.longValue()) {
//	                        _logger.info("Found child entityDataId="+cl[0]+" parentEntityId="+ei);
//	                    }
//	                }
//	            }
	        }
        }

        // Do this in a transaction so that we're not left with orphans if things go wrong
        Connection connection = null;
        try {
        	connection = getJdbcConnection();
        	connection.setAutoCommit(false);
            // Now time to actually delete. First the entity-data, then the entities
            deleteIdSetByJdbc(connection, entityDataSetToDelete, "entityData", "id");
            deleteIdSetByJdbc(connection, entitySetToDelete, "entity", "id");	
        }
        catch (Exception e) {
        	connection.rollback();
        	throw e;
        }
        finally {
            connection.commit();
        	try {
                if (connection!=null) connection.close();	
        	}
        	catch (SQLException e) {
        		_logger.warn("Ignoring error encountered while closing JDBC connection",e);
        	}
        }
    }
    
    /**
     * Iteratively exclude entities based on if any of their ancestors were excluded.
     */
    protected Set<Long> getAdditionalExclusions(Map<Long, Set<Long>> parentMap, Set<Long> entitySetCandidatesToDelete, Set<Long> exclusionList) {
    	Set<Long> additionalExclusions=new HashSet<Long>();
    	for(Long candidateId : entitySetCandidatesToDelete) {
    		if (exclusionList.contains(candidateId)) continue; // Skip things we've already excluded
    		if (areAncestorsExcluded(candidateId, parentMap, exclusionList, new HashSet<Long>())) {
            	if (debugDeletions) _logger.info("    Marking candidateId=" + candidateId + " for exclusion");
    			additionalExclusions.add(candidateId);
    		}
    	}

    	if (debugDeletions) _logger.info("    Found "+additionalExclusions.size()+" additional exclusions");
    	return additionalExclusions;
    }
    
    protected boolean areAncestorsExcluded(Long entityId, Map<Long, Set<Long>> parentMap, Set<Long> exclusionList, Set<Long> visited) {

    	if (visited.contains(entityId)) return false;
    	visited.add(entityId);
    	
    	if (debugDeletions) _logger.info("    Checking if any ancestors of "+entityId+" were excluded");
    	Set<Long> parents = parentMap.get(entityId);
    	if (parents != null) {
			for(Long parentId : parents) {
				if (exclusionList.contains(parentId)) {
			    	if (debugDeletions) _logger.info("    Parent of "+entityId+" (parentId="+parentId+") was excluded");
					return true;
				}
				if (areAncestorsExcluded(parentId, parentMap, exclusionList, visited)) {
					if (debugDeletions) _logger.info("    Some ancestor of "+entityId+" (via parentId="+parentId+") was excluded");
					return true;
				}
			}
    	}
		return false;
    }

    protected void getEntityTreeForUserByJdbc(Long userId, Map<Long, Set<Long[]>> entityMap, Map<Long, Set<Long>> parentMap, Set<Long> commonRootSet) throws Exception {
    	
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	try {
	        conn = getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select e.id, ed.id, ce.id, ce.user_id, ea.name ");
	        sql.append("from entity e ");
	        sql.append("left outer join entityData ed on e.id=ed.parent_entity_id ");
	        sql.append("left outer join entityAttribute ea on ed.entity_att_id = ea.id ");
	        sql.append("left outer join entity ce on ed.child_entity_id=ce.id and ce.user_id="+userId+" ");
	        sql.append("where e.user_id="+userId+" ");

	        if (debugDeletions) _logger.info("getEntityTreeForUserByJdbc userId="+userId);
	        if (debugDeletions) _logger.info("getEntityTreeForUserByJdbc sql="+sql);
	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        
	        stmt.setFetchSize(JDBC_FETCH_SIZE);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Long entityId = rs.getBigDecimal(1).longValue();
				BigDecimal entityDataBD = rs.getBigDecimal(2);
				Long entityDataId = null;
				if (entityDataBD != null) {
					entityDataId = entityDataBD.longValue();
				}
				BigDecimal childIdBD = rs.getBigDecimal(3);
				Long childId = null;
				if (childIdBD != null) {
					childId = childIdBD.longValue();
				}
				BigDecimal childUserIdBD = rs.getBigDecimal(4);
				Long childUserId = null;
				if (childUserIdBD != null) {
					childUserId = childUserIdBD.longValue();
					if (!childUserId.equals(userId)) {
						// We don't own the child, so let's forget about it. This case should be prevented by the join condition on the last outer join above.
						childId = null;
						childUserId = null;
					}
				}
				
				// Handle common roots
				String attrName = rs.getString(5);
				if (EntityConstants.ATTRIBUTE_COMMON_ROOT.equals(attrName)) {
					commonRootSet.add(entityId);
				}
				
				// _logger.info("TreeResult entityId="+entityId+" entityDataId="+entityDataId+" childId="+(childId==null?"null":childId));
				Long[] childData = new Long[2];
				childData[0] = entityDataId;
				childData[1] = childId;
				// Handle child direction
				Set<Long[]> childSet = entityMap.get(entityId);
				if (childSet == null) {
					childSet = new HashSet<Long[]>();
					entityMap.put(entityId, childSet);
				}
				if (childData[0] != null) {
					childSet.add(childData);
				}
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
    }
    
    protected void getNonOwnedParents(Long userId, Map<Long, Set<Long>> nonOwnedParentEdMap) throws Exception {
    	
    	Connection conn = null;
    	PreparedStatement stmt = null;
    	try {
	        conn = getJdbcConnection();
	        
	        StringBuffer sql = new StringBuffer();
	        sql.append("select e.id, ed.id, ce.id, ce.user_id, ea.name ");
	        sql.append("from entity e ");
	        sql.append("left outer join entityData ed on e.id=ed.parent_entity_id ");
	        sql.append("left outer join entityAttribute ea on ed.entity_att_id = ea.id ");
	        sql.append("join entity ce on ed.child_entity_id=ce.id and ce.user_id="+userId+" ");
	        sql.append("where e.user_id!="+userId+" ");

	        if (debugDeletions) _logger.info("getNonOwnedParents userId="+userId);
	        if (debugDeletions) _logger.info("getNonOwnedParents sql="+sql);
	        stmt = conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	        
	        stmt.setFetchSize(JDBC_FETCH_SIZE);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				BigDecimal entityDataBD = rs.getBigDecimal(2);
				Long entityDataId = null;
				if (entityDataBD != null) {
					entityDataId = entityDataBD.longValue();
				}
				BigDecimal childIdBD = rs.getBigDecimal(3);
				Long childId = null;
				if (childIdBD != null) {
					childId = childIdBD.longValue();
				}
				
				if (childId != null) {
					Set<Long> parentSet = nonOwnedParentEdMap.get(childId);
					if (parentSet == null) {
						parentSet = new HashSet<Long>();
						nonOwnedParentEdMap.put(childId, parentSet);
					}
					parentSet.add(entityDataId);
				}
			}
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
    }


    protected void deleteIdSetByJdbc(Connection connection, Set<Long> idSet, String tableName, String identifierColumnName) throws Exception {
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
    }

    protected Set<Long> walkEntityMap(Long startEntityId, Map<Long, Set<Long[]>> treeMap, Set<Long> exclusionList) {
    	return walkEntityMap(startEntityId, treeMap, exclusionList, new HashSet<Long>());
    }
    
    protected Set<Long> walkEntityMap(Long startEntityId, Map<Long, Set<Long[]>> treeMap, Set<Long> exclusionList, Set<Long> visited) {
    	if (debugDeletions) _logger.info("walkEntityMap startEntityId="+startEntityId+" exclusionList.size="+exclusionList.size()+" visited.size="+visited.size());
    	Set<Long> inclusionList = new HashSet<Long>();
        if (!exclusionList.contains(startEntityId) && !visited.contains(startEntityId)) {
        	visited.add(startEntityId); // Let's not visit it again since we've seen it already
        	inclusionList.add(startEntityId);
            Set<Long[]> childSet = treeMap.get(startEntityId);
            if (childSet != null) {
                for (Long[] childInfo : childSet) {
                    Long childEntityId = childInfo[1];
                    if (childEntityId != null && !exclusionList.contains(childEntityId)) {
                        /* note: this excludes the subtree of the excluded node */
                        if (!inclusionList.contains(childEntityId))
                            inclusionList.add(childEntityId);
                        Set<Long> childList = walkEntityMap(childEntityId, treeMap, exclusionList, visited);
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

    public void deleteSmallEntityTree(String userLogin, Entity entity) throws DaoException {
    	deleteSmallEntityTree(userLogin, entity, false);
    }
    
    public void deleteSmallEntityTree(String userLogin, Entity entity, boolean unlinkMultipleParents) throws DaoException {
    	deleteSmallEntityTree(userLogin, entity, unlinkMultipleParents, 0, new HashSet<Long>());
    }
    
    public void deleteSmallEntityTree(String userLogin, Entity entity, boolean unlinkMultipleParents, int level, Set<Long> deleted) throws DaoException {

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
    		_logger.warn(indent+"Entity (id="+entity.getId()+") was already deleted in this session");
    		return;
    	}
    	
		// Ownership check
    	if (!entity.getUser().getUserLogin().equals(userLogin)) {
    		_logger.info(indent+"Cannot delete entity because owner ("+entity.getUser().getUserLogin()+") does not match invoker ("+userLogin+")");
    		return;
    	}

    	// Common root check
    	if (level>0 && entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
    		_logger.info(indent+"Cannot delete "+entity.getName()+" because it is a common root");
    		return;
    	}
    	
    	// Multiple parent check
    	Set<EntityData> eds = getParentEntityDatas(entity.getId());
    	boolean moreThanOneParent = eds.size() > 1;
        if (level>0 && moreThanOneParent && !unlinkMultipleParents) {
        	_logger.info(indent+"Cannot delete "+entity.getName()+" because more than one parent is pointing to it");
        	return;
        }
        
        // Delete all ancestors first
        for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
        	Entity child = ed.getChildEntity();
        	if (child != null) {
        		deleteSmallEntityTree(userLogin, child, unlinkMultipleParents, level + 1, deleted);
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
    		throw new DaoException("Entity type '"+name+"' already exists");
    	}
        Session session = getCurrentSession();
    	
    	try {
	    	EntityType entityType = new EntityType();
	    	entityType.setName(name);
	        session.saveOrUpdate(entityType);
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
    	
        addAttributeToEntityType(entityType, entityAttr);
        return entityAttr;
    }
    
    public EntityAttribute createNewEntityAttr(String entityTypeName, String attrName) throws ComputeException {
    	EntityType entityType = getEntityTypeByName(entityTypeName);
    	if (entityType == null) {
    		throw new ComputeException("Entity type '"+entityTypeName+"' does not exist");
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
            _logger.info("Added EntityAttribute '" + entityAttr.getName() + "' to EntityType '"+entityType.getName()+"'");
	        
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
            StringBuffer hql = new StringBuffer("select et from EntityType et");
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
            StringBuffer hql = new StringBuffer("select ea from EntityAttribute ea");
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getAllEntityAttributes");
        }
    }
    
    public List<Entity> getUserEntitiesByNameAndTypeName(String userLogin, String entityName, String entityTypeName) throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select e from Entity e ");
            hql.append("join fetch e.user ");
            hql.append("join fetch e.entityType ");
            hql.append("where e.name=:entityName ");
            hql.append("and e.entityType.name=:entityTypeName ");
            if (null != userLogin) {
                hql.append("and e.user.userLogin=:userLogin ");
            }
            Query query = getCurrentSession().createQuery(hql.toString());
            query.setString("entityName", entityName);
            query.setString("entityTypeName", entityTypeName);
            if (null != userLogin) {
                query.setString("userLogin", userLogin);
            }
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getUserEntitiesByNameAndTypeName");
        }
    }

    public List<Entity> getUserEntitiesByTypeName(List<String> userLoginList,
                                                  String entityTypeName) throws DaoException {
        try {
            StringBuilder hql = new StringBuilder(256);

            final boolean filterByUsers =
                    ((userLoginList != null) && (userLoginList.size() > 0));

            hql.append("select e from Entity e join fetch e.entityType");
            if (filterByUsers) {
                hql.append(" join fetch e.user");
            }
            hql.append(" where e.entityType.name = :entityTypeName");
            if (filterByUsers) {
                hql.append(" and e.user.userLogin in (:userLoginList)");
            }

            final Session currentSession = getCurrentSession();
            Query query = currentSession.createQuery(hql.toString());
            query.setParameter("entityTypeName", entityTypeName);
            if (filterByUsers) {
                query.setParameterList("userLoginList", userLoginList);
            }

            //noinspection unchecked
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


    public Set<Entity> getUserEntitiesByName(String userLogin, String name) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select e from Entity e ");
            hql.append("join fetch e.user ");
            hql.append("join fetch e.entityType ");
            hql.append("where e.name=? ");
            if (null != userLogin) {
                hql.append("and e.user.userLogin=? ");
            }
            hql.append("order by e.id ");
            
            Query query = session.createQuery(hql.toString());
            query.setString(0, name);
            if (null != userLogin) {
                query.setString(1, userLogin);
            }
            List l = query.list();
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


    public boolean deleteEntityById(Long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            Set<EntityData> parentEds = getParentEntityDatas(entityId);
            for(EntityData parentEd : parentEds) {
                session.delete(parentEd);
            	_logger.debug("The parent entity data with id=" + parentEd.getId() + " has been deleted.");
            }
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", Long.valueOf(entityId)));
            Entity entity = (Entity) c.uniqueResult();
            session.delete(entity);
            _logger.debug("The entity with id=" + entityId + " has been deleted.");
            return true;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getUserCommonRoots(String userLogin, String entityTypeName) throws DaoException {
        try {
        	EntityAttribute attr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select e from Entity e ");
            hql.append("join fetch e.user ");
            hql.append("join fetch e.entityType ");
            hql.append("join e.entityData as ed ");
            hql.append("where e.entityType.name=? ");
            hql.append("and ed.entityAttribute.id=? ");
            if (null != userLogin) {
                hql.append("and e.user.userLogin=? ");
            }
            hql.append("order by e.id ");
            Query query = session.createQuery(hql.toString());
            query.setString(0, entityTypeName);
            query.setLong(1, attr.getId());
            if (null != userLogin) {
                query.setString(2, userLogin);
            }
            return query.list();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Entity getCommonRootFolderByName(String userLogin, String folderName, boolean createIfNecessary) throws DaoException {

    	Entity folder = null;
    	for(Entity entity : getUserEntitiesByNameAndTypeName(userLogin, folderName, EntityConstants.TYPE_FOLDER)) {
    		if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT)!=null) {
    			if (folder!=null) {
    				throw new IllegalStateException("Multiple common roots owned by "+userLogin+" with name: "+folderName);
    			}
    			folder = entity;
    		}
    	}
    	
    	if (folder!=null) {
    		return folder;
    	}
    	
        if (createIfNecessary) {
            _logger.info("Creating new topLevelFolder with name=" + folderName);
            folder = createEntity(userLogin, EntityConstants.TYPE_FOLDER, folderName);
            folder.addAttributeAsTag(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            saveOrUpdate(folder);
            _logger.info("Saved top level folder as " + folder.getId());
        }
    	
        return folder;
    }
    
    public Entity getChildFolderByName(String userLogin, Long parentId, String folderName, boolean createIfNecessary) throws DaoException {

    	Entity parent = getUserEntityById(userLogin, parentId);
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
            folder = createEntity(userLogin, EntityConstants.TYPE_FOLDER, folderName);
            addEntityToParent(parent, folder, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        }
    	
        return folder;
    }
    
    public Set<EntityData> getParentEntityDatas(long childEntityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed from EntityData ed ");
            hql.append("join fetch ed.user ");
            hql.append("join fetch ed.entityAttribute ");
            hql.append("join fetch ed.childEntity ");
            hql.append("join fetch ed.childEntity.user ");
            hql.append("join fetch ed.childEntity.entityType ");
            hql.append("join fetch ed.parentEntity ");
            hql.append("join fetch ed.parentEntity.user ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.childEntity.id=?");
            Query query = session.createQuery(hql.toString()).setLong(0, childEntityId);
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Set<Long> getParentIdsForAttribute(long childEntityId, String attributeName) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity.id from EntityData ed ");
            hql.append("where ed.childEntity.id=? ");
            hql.append("and ed.entityAttribute.name=? ");
            Query query = session.createQuery(hql.toString());
            query.setLong(0, childEntityId);
            query.setString(1, attributeName);
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Set<Entity> getParentEntities(long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join fetch ed.parentEntity.user ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.childEntity.id=? ");
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public Set<Entity> getChildEntities(long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.childEntity from EntityData ed ");
            hql.append("join fetch ed.childEntity.user ");
            hql.append("join fetch ed.childEntity.entityType ");
            hql.append("where ed.parentEntity.id=? ");
            Query query = session.createQuery(hql.toString()).setLong(0, entityId);
            return new HashSet(query.list());
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public Map<Long, String> getChildEntityNames(long entityId) throws DaoException {
		Map<Long,String> nameMap = new LinkedHashMap<Long,String>();
        try {
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
	
    public List<Task> getAnnotationSessions(String userLogin) throws DaoException {
		try {
	        String hql = "select clazz from Task clazz where subclass='" + AnnotationSessionTask.TASK_NAME + "' and clazz.owner='" + userLogin + "' order by clazz.objectId";
	        Query query = sessionFactory.getCurrentSession().createQuery(hql);
	        return query.list();
	    } catch (Exception e) {
	        throw new DaoException(e);
	    }
    }

    public List<Entity> getAnnotationsForChildren(String userLogin, Long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select distinct targetEd.parentEntity from EntityData targetEd, EntityData childEd ");
            hql.append("join fetch targetEd.parentEntity.user ");
            hql.append("join fetch targetEd.parentEntity.entityType ");
            hql.append("where targetEd.entityAttribute.name = :attrName ");	
            hql.append("and childEd.childEntity.id is not null ");
            hql.append("and cast(childEd.childEntity.id as string) = targetEd.value ");
            hql.append("and childEd.parentEntity.id = :entityId ");
            if (userLogin!=null) {
            	hql.append("and (targetEd.parentEntity.user.userLogin=:userLogin or targetEd.parentEntity.user.userLogin='system') ");
            }
            
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        	query.setLong("entityId", entityId);
            if (userLogin!=null) {
            	query.setString("userLogin", userLogin);
            }
            
            return query.list();
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public List<Entity> getAnnotationsByEntityId(String userLogin, Long entityId) throws DaoException {
    	List<Long> entityIds = new ArrayList<Long>();
    	entityIds.add(entityId);
    	return getAnnotationsByEntityId(userLogin, entityIds);
    }
    
    public List<Entity> getAnnotationsByEntityId(String userLogin, List<Long> entityIds) throws DaoException {
        try {
        	if (entityIds.isEmpty()) {
        		return new ArrayList<Entity>();
        	}

            List<String> entityIdStrs = new ArrayList<String>();
        	for(Long id : entityIds) {
        		entityIdStrs.add(""+id);
        	}
            
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join fetch ed.parentEntity.user ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.entityAttribute.name = :attrName ");
            hql.append("and ed.value in (:entityIds) ");
            if (userLogin!=null) {
            	hql.append("and (ed.parentEntity.user.userLogin=:userLogin or ed.parentEntity.user.userLogin='system') ");
            }
            hql.append("order by ed.parentEntity.id ");
            
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
        	query.setParameterList("entityIds", entityIdStrs);
            if (userLogin!=null) {
            	query.setString("userLogin", userLogin);
            }
            
            return query.list();
        } 
        catch (Exception e) {
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
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join fetch ed.parentEntity.user ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.entityAttribute.name = ? ");
    		hql.append("and ed.value = ? ");
    		hql.append("order by ed.parentEntity.id ");
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
        
        return annotations;
	}
	
    public List<Entity> getPublicOntologies() throws ComputeException {

        EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
        EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
        
        StringBuffer hql = new StringBuffer("select e from Entity e ");
        hql.append("join fetch e.user ");
        hql.append("join fetch e.entityType ");
        hql.append("where e.entityType.id=? ");
        hql.append("and exists (from EntityData as attr where attr.parentEntity = e and attr.entityAttribute.id = ?)");
        Query query = getCurrentSession().createQuery(hql.toString());
        query.setLong(0, tmpType.getId());
        query.setLong(1, tmpAttr.getId());
        return query.list();
    }

    public Entity getErrorOntology() throws ComputeException {

        List<Entity> list = getPublicOntologies();
        for (Entity tmpEntity : list) {
            if (tmpEntity.getName().equals("Error Ontology")) {
                return tmpEntity;
            }
        }
        return null;
    }
    
    public List<Entity> getPrivateOntologies(String userLogin) throws ComputeException {
    	
        EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
        EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
        
        StringBuffer hql = new StringBuffer("select e from Entity e ");
        hql.append("join fetch e.user ");
        hql.append("join fetch e.entityType ");
        hql.append("where e.entityType.id=? ");
        if (null != userLogin) {
            hql.append("and e.user.userLogin=? ");
        }
        hql.append("and not exists (from EntityData as attr where attr.parentEntity = e and attr.entityAttribute.id = ?)");
        Query query = getCurrentSession().createQuery(hql.toString());
        query.setLong(0, tmpType.getId());
        query.setString(1, userLogin);
        query.setLong(2, tmpAttr.getId());
        return query.list();
    }
    
    public Entity createEntity(String userLogin, String entityTypeName, String entityName) throws DaoException {
    	if (entityTypeName==null) throw new DaoException("Error creating entity with null type");
        User owner = getUserByName(userLogin);
        Entity entity = newEntity(entityTypeName, entityName, owner);
        if (entity.getEntityType()==null) throw new DaoException("Error creating entity with unknown entity type: "+entityTypeName);
        saveOrUpdate(entity);
        return entity;
    }

    public EntityData addEntityToParent(Entity parent, Entity entity, Integer index, String attrName) throws DaoException {
    	if (attrName==null) throw new DaoException("Error adding entity child with null attribute name");
        EntityData ed = parent.addChildEntity(entity, attrName);
        ed.setOrderIndex(index);
        saveOrUpdate(ed);
        return ed;
    }

    public Entity createDataSet(String userLogin, String dataSetName) throws ComputeException {

        String dataSetIdentifier = EntityUtils.createDataSetIdentifierFromName(userLogin, dataSetName);
        
        if (!getUserEntitiesWithAttributeValue(null, EntityConstants.TYPE_DATA_SET, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier).isEmpty()) {
        	throw new ComputeException("Data Set with identifier '"+dataSetIdentifier+"' already exists.");
        }
        
        User tmpUser = getUserByName(userLogin);
        Entity newDataSet = newEntity(EntityConstants.TYPE_DATA_SET, dataSetName, tmpUser);
        saveOrUpdate(newDataSet);

        EntityData dataSetIdEd = newData(newDataSet, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, tmpUser);
        newDataSet.getEntityData().add(dataSetIdEd);
        dataSetIdEd.setValue(dataSetIdentifier);
        saveOrUpdate(dataSetIdEd);

        return newDataSet;
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

        // Add the type-specific parameters
        if (type instanceof EnumText) {

            EnumText enumText = (EnumText)type;

            EntityData lowerData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_ENUMTEXT_ENUMID, tmpUser);
            lowerData.setValue(enumText.getValueEnumId().toString());
            eds.add(lowerData);
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
        Entity newOntologyElement = newEntity(source.getEntityType(), targetName, targetUser);
        saveOrUpdate(newOntologyElement);

        _logger.info("newOntologyElement.id="+newOntologyElement.getId());
        
        // Add the children 
    	for(EntityData ed : source.getEntityData()) {

    		// Never clone "Is Public" attributes. Entities are cloned privately. 
    		if (ed.getEntityAttribute().getId().equals(tmpAttr.getId())) continue;
    		
    		Entity newChildEntity = null;
    		Entity childEntity = ed.getChildEntity();
    		if (childEntity != null) {
    			newChildEntity = cloneEntityTree(childEntity, targetUser, childEntity.getName(), false);	
    		}
    		
            EntityData newEd = newOntologyElement.addChildEntity(newChildEntity, ed.getEntityAttribute().getName());
            newEd.setOrderIndex(ed.getOrderIndex());
            newEd.setValue(ed.getValue());
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
    
    public void fixInternalOntologyConsistency(Long sourceRootId) throws DaoException {
    	Entity ontologyRoot = populateDescendants(getEntityById(sourceRootId));
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
				Entity oldEnum = getEntityById(oldEnumIdEd.getValue());
				if (oldEnum!=null) {
					Long newEnumId = enumMap.get(oldEnum.getName());
					if (newEnumId!=null) {
						oldEnumIdEd.setValue(newEnumId.toString());
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
    
	public Entity createOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException {

        try {
            User tmpUser = getUserByName(userLogin);
        	if (tmpUser == null) {
        		throw new IllegalArgumentException("User "+userLogin+" not found");
        	}
        	
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
		            List<Entity> existingAnnotations = getAnnotationsByEntityId(userLogin, annotation.getTargetEntityId());
		        	for(Entity existingAnnotation : existingAnnotations) {
		        		EntityData eaKeyEd = existingAnnotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
		        		if (eaKeyEd==null) continue;
    	        		if (eaKeyEd.getChildEntity().getId().equals(annotation.getKeyEntityId())) {
    	        			deleteEntityById(existingAnnotation.getId());
    	        		}
	            	}
            	}
            }
            
        	String tag = isCustom ? annotation.getValueString() : 
        				(annotation.getValueString()==null ? annotation.getKeyString() : 
        				 annotation.getKeyString() + " = " + annotation.getValueString());
            
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
				EntityData keyEntityData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID, tmpUser);
				keyEntityData.setChildEntity(keyEntity);
				keyEntityData.setValue(""+keyEntity.getId());
				eds.add(keyEntityData);
            }

			// Add the value entity
            if (annotation.getValueEntityId() != null) {
            	Entity valueEntity = getEntityById(annotation.getValueEntityId());
				EntityData valueEntityData = newData(newAnnotation,
						EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID, tmpUser);
				valueEntityData.setChildEntity(valueEntity);
				valueEntityData.setValue(""+valueEntity.getId());
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

	public Long removeOntologyAnnotation(String userLogin, long annotationId) throws ComputeException {
        Entity entity = getUserEntityById(userLogin, annotationId);
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
    public List<Entity> removeAllOntologyAnnotationsForSession(String userLogin, long sessionId) throws DaoException {

        try {
        	List<Entity> annotations = getAnnotationsForSession(userLogin, sessionId);
            for(Object o : annotations) {
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
            return annotations;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

	public List<Entity> getEntitiesInList(String entityIds) throws DaoException {
		String[] idStrs = entityIds.split("\\s*,\\s*");
		List<Long> ids = new ArrayList<Long>();
		for(String idStr : idStrs) {
			ids.add(new Long(idStr));
		}
		return getEntitiesInList(ids);
	}

	public List<Entity> getEntitiesInList(List<Long> entityIds) throws DaoException {
        try {
        	if (entityIds == null || entityIds.isEmpty()) {
        		return new ArrayList<Entity>();
        	}
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select e from Entity e ");
            hql.append("join fetch e.user ");
            hql.append("join fetch e.entityType ");
            hql.append("where e.id in (:ids) ");
            
            Query query = session.createQuery(hql.toString());
            query.setParameterList("ids", entityIds);

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
    public Entity populateDescendants(Entity entity) {
    	if (entity.getId()==null) {
    		_logger.warn("Cannot populate descendants for entity with null id");
    		return entity;
    	}
    	return populateDescendants(entity, new HashSet<Long>());
    }
    
    private Entity populateDescendants(Entity entity, Set<Long> visited) {
    	if (entity == null) return null;
    	if (visited.contains(entity.getId())) {
    		return entity;
    	}
    	visited.add(entity.getId());
    	for(EntityData ed : entity.getEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child != null) {
    			populateDescendants(child, visited);
    		}
    	}
    	return entity;
    }

    public Entity getEntityAndChildren(Long id) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Entity.class);
        c.add(Expression.eq("id",id));
        Entity parent = (Entity) c.uniqueResult();
        if (parent == null)
            return null;
        for (EntityData ed : parent.getEntityData()) {
            Entity child=ed.getChildEntity();
            if (child!=null) {
                String childName=child.getName(); // forces load of attributes but not subchild entities
            }
        }
        return parent;
    }

    public Long countDescendants(String owner, Entity entity, Long stopAfterReaching) {
    	return countDescendants(owner, entity, new HashSet<Long>(), stopAfterReaching);
    }

    private Long countDescendants(String owner, Entity entity, Set<Long> visited, Long stopAfterReaching) {
    	long count = 1;
    	if (entity == null) return null;
    	if (!entity.getUser().getUserLogin().equals(owner)) return new Long(0);
    	if (visited.contains(entity.getId())) {
    		return count;
    	}
    	visited.add(entity.getId());
    	for(EntityData ed : entity.getEntityData()) {
    		Entity child = ed.getChildEntity();
    		if (child != null) {
    			count += countDescendants(owner, child, visited, stopAfterReaching);
    			if (count > stopAfterReaching) return count;
    		}
    	}
    	return count;
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

    public List<List<Entity>> getEntityPathsToRoots(Entity entity) throws DaoException {
    	List<List<Entity>> paths = new ArrayList<List<Entity>>();
    	Set<Entity> parents = getParentEntities(entity.getId());
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
        		List<List<Entity>> ancestorPaths = getEntityPathsToRoots(parent);
        		for(List<Entity> ancestorPath : ancestorPaths) {
        			ancestorPath.add(entity);
        			paths.add(ancestorPath);
        		}
        	}
    	}
    	return paths;
    }

    public List<List<EntityData>> getEntityDataPathsToRoots(String userLogin, EntityData entityData) throws DaoException {
    	Entity entity = entityData.getParentEntity();
    	List<List<EntityData>> paths = new ArrayList<List<EntityData>>();
    	Set<EntityData> parents = getParentEntityDatas(entity.getId());
    	if (parents.isEmpty()) {
    		List<EntityData> path = new ArrayList<EntityData>();
			path.add(entityData);
			paths.add(path);
    	}
    	else {
        	for(EntityData parent : parents) {
        		String owner = parent.getUser().getUserLogin();
        		if (!owner.equals("system") && !owner.equals(userLogin)) continue;
        		List<List<EntityData>> ancestorPaths = getEntityDataPathsToRoots(userLogin, parent);
        		for(List<EntityData> ancestorPath : ancestorPaths) {
        			if (entityData.getId()!=null) ancestorPath.add(entityData);
        			paths.add(ancestorPath);
        		}
        	}
    	}
    	return paths;
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

    public List<MappedId> getProjectedResults(List<Long> entityIds, List<String> upProjection, List<String> downProjection) throws DaoException {
    	
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
    
    public List<Long> getImageIdsWithName(Connection connection, Long userId, String imagePath) throws DaoException {
    	
    	List<Long> sampleIds = new ArrayList<Long>();
        Connection conn = connection;
    	PreparedStatement stmt = null;
    	
    	try {
            StringBuffer sql = new StringBuffer("select distinct i.id from entity i ");
            sql.append("where i.name = ? ");
            if (userId!=null) {
            	sql.append("and i.user_id = ? ");
            }
            
            if (conn==null) {
            	conn = getJdbcConnection();
            }
	        stmt = conn.prepareStatement(sql.toString());
	        stmt.setString(1, imagePath);
            if (userId!=null) {
            	stmt.setLong(2, userId);
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

    public List<Entity> getUserEntitiesWithAttributeValue(String userLogin, String typeName, String attrName, String attrValue) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select ed.parentEntity from EntityData ed ");
            hql.append("join fetch ed.parentEntity.user ");
            hql.append("join fetch ed.parentEntity.entityType ");
            hql.append("where ed.entityAttribute.name=:attrName and ed.value like :value ");
            if (typeName != null) {
            	hql.append("and ed.parentEntity.entityType.name=:typeName ");
            }
            if (null != userLogin) {
                hql.append("and ed.parentEntity.user.userLogin=:userLogin ");
            }
            Query query = session.createQuery(hql.toString());
            query.setString("attrName", attrName);
            query.setString("value", attrValue);
            if (typeName != null) {
                query.setString("typeName", typeName);
            }
            if (null != userLogin) {
                query.setString("userLogin", userLogin);
            }
            return query.list();
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    public List<Entity> getUserEntitiesWithAttributeValue(String userLogin, String attrName, String attrValue) throws DaoException {
        return getUserEntitiesWithAttributeValue(userLogin, null, attrName, attrValue);
    }

    public void loadLazyEntity(Entity entity, boolean recurse) throws DaoException {
		
        if (!EntityUtils.areLoaded(entity.getEntityData())) {
            EntityUtils.replaceChildNodes(entity, getChildEntities(entity.getId()));
        }

        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(ed.getChildEntity(), true);
                }
            }
        }
    }

    public Entity annexEntityTree(Long entityId, String newOwner) throws ComputeException {
    	User newUser = getUserByName(newOwner);
    	Entity entity = getEntityById(entityId);
    	_logger.info(newOwner+" is annexing entity tree starting at "+entity.getName()+
    			" (id="+entity.getId()+") from "+entity.getUser().getUserLogin());
    	annexEntityTree(entity, newUser, "  ");	
    	disassociateTreeFromNonOwners(getEntityById(entity.getId()), newUser, "  ");
    	return entity;
    }
    
    private Entity annexEntityTree(Entity entity, User newOwner, String indent) throws ComputeException {
    	
    	if (!entity.getUser().getUserId().equals(newOwner.getUserId())) {
        	_logger.info(indent+"annexing entity "+entity.getName()+" (id="+entity.getId()+")");
        	entity.setUser(newOwner);
        	saveOrUpdate(entity);
    	}
    	loadLazyEntity(entity, false);
    	for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
    		if (!ed.getUser().getUserId().equals(entity.getUser().getUserId())) {
	    		ed.setUser(newOwner);
	    		saveOrUpdate(ed);
    		}
    		if (ed.getChildEntity()!=null) {
    			annexEntityTree(ed.getChildEntity(), newOwner, indent+"  ");
    		}
    	}
    	
    	// TODO: move files in filestore?
    	
    	return entity;
    }
    
    private Entity disassociateTreeFromNonOwners(Entity entity, User newOwner, String indent) throws ComputeException {
    	
    	for(EntityData parentEd : getParentEntityDatas(entity.getId())) {
    		if (parentEd.getUser().getUserLogin().equals(newOwner.getUserLogin())) continue;
    		_logger.info(indent+"deleting "+parentEd.getUser().getUserLogin()+"'s link ("+parentEd.getEntityAttribute().getName()+") from entity "+parentEd.getParentEntity().getName()+" to entity "+entity.getName());
    		parentEd.getParentEntity().getEntityData().remove(parentEd);
    		genericDelete(parentEd);
    	}
    	
    	loadLazyEntity(entity, false);
    	for(EntityData ed : new ArrayList<EntityData>(entity.getEntityData())) {
    		if (ed.getChildEntity()!=null) {
    			disassociateTreeFromNonOwners(ed.getChildEntity(), newOwner, indent+"  ");
    		}
    	}
    	
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

    private Entity newEntity(String entityTypeName, String name, User owner) {
        EntityType tmpType = getEntityTypeByName(entityTypeName);
        return newEntity(tmpType, name, owner);
    }

    private Entity newEntity(EntityType entityType, String name, User owner) {
    	Date date = new Date();
        return new Entity(null, name, owner, null, entityType, date, date, new HashSet<EntityData>());	
    }
    
    private EntityData newData(Entity parent, String attrName, User owner) {
        EntityAttribute attribute = getEntityAttributeByName(attrName);
        return newData(parent, attribute, owner);
    }
    
    private EntityData newData(Entity parent, EntityAttribute attribute, User owner) {
    	Date date = new Date();
        return new EntityData(null, attribute, parent, null, owner, null, date, date, null);
    }
    
    
    public void addChildren(String userLogin, Long parentId, List<Long> childrenIds, String attributeName) throws DaoException {
    	
    	User user = getUserByName(userLogin);
    	EntityAttribute attribute = getEntityAttributeByName(attributeName);
    	Date createDate = new Date();
    	
    	Entity parent = new Entity();
    	parent.setId(parentId);
    	
        for (Long childId : childrenIds) {
        	
        	Entity child = new Entity();
        	child.setId(childId);
        	
        	EntityData ed = new EntityData();
        	ed.setParentEntity(parent);
        	ed.setChildEntity(child);
        	ed.setUser(user);
            ed.setCreationDate(createDate);
            ed.setUpdatedDate(createDate);
            
            if (attribute!=null) ed.setEntityAttribute(attribute);
            
            saveOrUpdate(ed);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws DaoException {
        _logger.info("getPatternQuantifiersForScreenSample: starting search for entities of type="+EntityConstants.TYPE_SCREEN_SAMPLE);
        List<Entity> flyScreenSampleEntityList = getUserEntitiesByTypeName(SYSTEM_USER, EntityConstants.TYPE_SCREEN_SAMPLE);
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
        List<Entity> flyScreenSampleEntityList = getUserEntitiesByTypeName(SYSTEM_USER, EntityConstants.TYPE_SCREEN_SAMPLE);
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
            String resourceDirString= SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
            String quantifierSummaryFilename= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
            File summaryFile=new File(resourceDirString + File.separator+maskFolderName, quantifierSummaryFilename);
            File nameIndexFile=new File(resourceDirString + File.separator+maskFolderName, "maskNameIndex.txt");
            maskManager.loadMaskCompartmentList(nameIndexFile);
            Object[] mapObjects=maskManager.loadMaskSummaryFile(summaryFile);
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
    	if (root.getUser()==null || (root.getUser().getUserLogin()==null && root.getUser().getUserId()==null)) {
    		throw new IllegalArgumentException("Root entity must specify the username or user GUID");
    	}
    	return saveBulkEntityTree(root, root.getUser().getUserLogin());
    }
    
    public Entity saveBulkEntityTree(Entity root, String owner) throws DaoException {

    	final int batchSize = 800;	
    	
    	_logger.info("Saving bulk entity tree rooted at "+root.getName());
    	
    	Long userId = null;
        if (root.getUser()!=null && root.getUser().getUserId()!=null) {
        	userId =  root.getUser().getUserId();
        }
        else {
        	User user = getUserByName(owner);
        	userId = user.getUserId();
        }
        
        _logger.debug("Using user id: "+userId);
        
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
	        
	        String entitySql = "insert into entity (id,name,user_id,entity_type_id,creation_date,updated_date) values (?,?,?,?,?,?)";
	        stmtEntity = conn.prepareStatement(entitySql);
	        
	        String edSql = "insert into entityData (id,parent_entity_id,entity_att_id,value,user_id,creation_date,updated_date,orderIndex,child_entity_id) values (?,?,?,?,?,?,?,?,?)";
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
	        	stmtEntity.setLong(3, userId);
		        
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
			        stmtEd.setLong(5, userId);

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
}
