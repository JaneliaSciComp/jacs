package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.PatternSearchDAO;
import org.janelia.it.jacs.compute.access.solr.SolrDAO;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import com.google.common.collect.ComparisonChain;

@Stateless(name = "AnnotationEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = StrictMaxPool.class, maxSize = 500, timeout = 10000)
public class AnnotationBeanImpl implements AnnotationBeanLocal, AnnotationBeanRemote {
	
    private static final Logger _logger = Logger.getLogger(AnnotationBeanImpl.class);

    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);

    private void updateIndex(Long entityId) {
        if (entityId==null) {
            _logger.warn("Cannot update index for null entity id");
            return;
        }
    	IndexingHelper.updateIndex(entityId);
    }

    public Entity createOntologyRoot(String subjectKey, String rootName) throws ComputeException {
        try {
            Entity ontologyRoot = _annotationDAO.createOntologyRoot(subjectKey, rootName);
        	_logger.info(subjectKey+" creating ontology "+ontologyRoot.getId());
        	return ontologyRoot;
        }
        catch (Exception e) {
            _logger.error("Error creating new ontology ("+rootName+") for subject "+subjectKey,e);
            throw new ComputeException("Error creating new ontology ("+rootName+") for user "+subjectKey,e);
        }
    }

    public EntityData createOntologyTerm(String subjectKey, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {
        try {
        	EntityData ed = _annotationDAO.createOntologyTerm(subjectKey, ontologyTermParentId, termName, type, orderIndex);
            _logger.info(subjectKey+" created ontology term "+ed.getChildEntity().getId());
            return ed;
        }
        catch (Exception e) {
            _logger.error("Error creating new term ("+termName+") for user "+subjectKey,e);
            throw new ComputeException("Error creating new term ("+termName+") for user "+subjectKey,e);
        }
    }

    public void removeOntologyTerm(String subjectKey, Long ontologyTermId) throws ComputeException {
    	try {
    		_annotationDAO.deleteOntologyTerm(subjectKey, ontologyTermId.toString());
    		_logger.info(subjectKey+" deleted ontology term "+ontologyTermId);
    	}
    	catch (DaoException e) {
    		_logger.error("Could not delete ontology term with id="+ontologyTermId+" for user "+subjectKey);
    		throw new ComputeException("Could not delete ontology term", e);
    	}
    }

	public Entity createOntologyAnnotation(String subjectKey, OntologyAnnotation annotation) throws ComputeException {

        try {
            Entity annotEntity = _annotationDAO.createOntologyAnnotation(subjectKey, annotation);
            _logger.info("Subject "+subjectKey+" added annotation "+annotEntity.getId());
            updateIndex(annotation.getTargetEntityId());
            return annotEntity;
        }
        catch (Exception e) {
            _logger.error("Error creating ontology annotation for user "+subjectKey,e);
            throw new ComputeException("Error creating ontology annotation for subject "+subjectKey,e);
        }
	}
	
	public Entity createSilentOntologyAnnotation(String subjectKey, OntologyAnnotation annotation) throws ComputeException {

        try {
        	return _annotationDAO.createOntologyAnnotation(subjectKey, annotation);
        }
        catch (Exception e) {
            _logger.error("Error creating ontology annotation for subject "+subjectKey,e);
            throw new ComputeException("Error creating ontology annotation for subject "+subjectKey,e);
        }
	}


	public void removeOntologyAnnotation(String subjectKey, long annotationId) throws ComputeException {
        try {
            Long targetEntityId = _annotationDAO.removeOntologyAnnotation(subjectKey, annotationId);
            _logger.info("Subject "+subjectKey+" removed annotation "+annotationId);
            updateIndex(targetEntityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to removeAnnotation");
            throw new ComputeException("Error trying to removeAnnotation",e);
        }
	}
	
	public void removeAllOntologyAnnotationsForSession(String subjectKey, long sessionId) throws ComputeException {
        try {
            List<Entity> annotations = _annotationDAO.removeAllOntologyAnnotationsForSession(subjectKey, sessionId);
            _logger.info("Subject "+subjectKey+" removed all annotations for session "+sessionId);
            updateIndexForAnnotationTargets(annotations);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to removeAllOntologyAnnotationsForSession");
            throw new ComputeException("Error trying to removeAllOntologyAnnotationsForSession",e);
        }
	}
	
	@Deprecated
	/**
	 * Use of this method should be replaced with EntityBeanRmote.getEntityTree, it does exactly the same thing.
	 */
    public Entity getOntologyTree(String subjectKey, Long entityId) throws ComputeException {
        try {
            Entity entity = _annotationDAO.getEntityById(subjectKey, entityId);
            return _annotationDAO.loadLazyEntity(subjectKey, entity, true);
        }
        catch (Exception e) {
            _logger.error("Error getting ontology tree",e);
            throw new ComputeException("Error getting ontology tree", e);
        }
    }

    public Entity getErrorOntology() throws ComputeException {
        try {
            return _annotationDAO.getErrorOntology();
        }
        catch (Exception e) {
            _logger.error("Error getting error ontology",e);
            throw new ComputeException("Error getting error ontology",e);
        }
    }

    public List<Entity> getOntologyRootEntities(String subjectKey) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesByTypeName(subjectKey, EntityConstants.TYPE_ONTOLOGY_ROOT);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get ontology root entities for "+subjectKey, e);
            throw new ComputeException("Error getting ontology root entities", e);
        }
    }
    
    public List<Task> getAnnotationSessionTasks(String username) throws ComputeException {
        try {
             return _annotationDAO.getAnnotationSessions(username);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for entities "+username, e);
            throw new ComputeException("Could not get annotations for entities ",e);
        }
    }

    public List<Entity> getAnnotationsForChildren(String username, long entityId) throws ComputeException {
        try {
            return _annotationDAO.getAnnotationsForChildren(username, entityId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for child of "+username, e);
            throw new ComputeException("Could not get annotations for entities ",e);
        }
    }
    
    public List<Entity> getAnnotationsForEntities(String subjectKey, List<Long> entityIds) throws ComputeException {
        try {
            return _annotationDAO.getAnnotationsByEntityId(subjectKey, entityIds);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for "+subjectKey, e);
            throw new ComputeException("Could not get annotations for entities ",e);
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
            throw new ComputeException("Could not get annotations in session",e);
        }
    }
    
    public List<Entity> getEntitiesAnnotatedWithTerm(String subjectKey, String annotationName) throws ComputeException {
        try {
            List<Long> entityIds = new ArrayList<Long>();
            for(Entity annotation : _annotationDAO.getEntitiesByNameAndTypeName(subjectKey, annotationName, EntityConstants.TYPE_ANNOTATION)) {
                String target = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
                entityIds.add(new Long(target));
            }
            return _annotationDAO.getEntitiesInList(subjectKey, entityIds);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to entities annotated with "+annotationName, e);
            throw new ComputeException("Unexpected error occurred while trying to entities annotated with "+annotationName,e);
        }
    }
    
    public List<Entity> getEntitiesForAnnotationSession(String username, long sessionId) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesForAnnotationSession(username, sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get entities in session "+sessionId, e);
            throw new ComputeException("Unexpected error occurred while trying to get entities in session "+sessionId,e);
        }
    }
    
    public List<Entity> getCategoriesForAnnotationSession(String username, long sessionId) throws ComputeException {
        try {
            return _annotationDAO.getCategoriesForAnnotationSession(username, sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get categories in session "+sessionId, e);
            throw new ComputeException("Could not get categories in session",e);
        }
    }

    public Set<Long> getCompletedEntityIds(long sessionId) throws ComputeException {
        try {
            return _annotationDAO.getCompletedEntityIds(sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get completed entity ids in session "+sessionId, e);
            throw new ComputeException("Could not get completed entities",e);
        }
    }

    public long getNumDescendantsAnnotated(Long entityId) throws ComputeException {
        try {
			SolrDAO solrDAO = new SolrDAO(_logger, false, false);
			SolrQuery query = new SolrQuery("(id:"+entityId+" OR ancestor_ids:"+entityId+") AND all_annotations:*");
			return solrDAO.search(query).getResults().getNumFound();
	    }
	    catch (DaoException e) {
	    	_logger.error("Error getting number of annotations: "+entityId, e);
	    	throw new ComputeException("Error getting number of annotations: "+entityId, e);
	    }
    }

    @Override
    public List<Long> getEntityIdsInAlignmentSpace(String opticalRes, String pixelRes, List<Long> guids) throws ComputeException {
        try {
            return _annotationDAO.getEntityIdsInAlignmentSpace( opticalRes, pixelRes, guids);

        } 
        catch ( DaoException daoe ) {
            _logger.error("Error getting applicable subset of entity ids for an alignment space.");
            throw new ComputeException("Error paring down entity ids for list", daoe);
        }
	}
    
    @Override
    public List<Long> getAllEntityIdsByType(String entityTypeName) throws ComputeException {
        try {
            return _annotationDAO.getAllEntityIdsByType(entityTypeName);
        } catch (DaoException daoe) {
            _logger.error("Error getting entity id list for type=" + entityTypeName);
            throw new ComputeException("Error getting entity id list for type=" + entityTypeName, daoe);
        }
    }

	
    public List<Entity> getCommonRootEntities(String subjectKey) throws ComputeException {
        try {
            List<Entity> entities = _annotationDAO.getEntitiesWithTag(subjectKey, EntityConstants.ATTRIBUTE_COMMON_ROOT);
            List<String> subjectKeyList = _annotationDAO.getSubjectKeys(subjectKey);
            // We only consider common roots that the user owns, or one of their groups owns. Other common roots
            // which the user has access to through an ACL are already referenced in the Shared Data folder.
            // The reason this is a post-processing step, is because we want an accurate ACL on the object from the 
            // outer fetch join. 
            List<Entity> commonRoots = new ArrayList<Entity>();
            if (null != subjectKey) {
                for (Entity commonRoot : entities) {
                    if (subjectKeyList.contains(commonRoot.getOwnerKey())) {
                        commonRoots.add(commonRoot);
                    }
                }
            }
            else {
                commonRoots.addAll(entities);
            }
            
            return commonRoots;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get common root entities for "+subjectKey, e);
	    	throw new ComputeException("Error getting common root entities", e);
        }
    }
    
//    public Entity getCommonRootFolderByName(String subjectKey, String folderName, boolean createIfNecessary) throws ComputeException {
//        try {
//            return _annotationDAO.getCommonRootFolderByName(subjectKey, folderName, createIfNecessary);
//        }
//        catch (DaoException e) {
//            _logger.error("Error trying to get common root called "+folderName, e);
//        }
//        return null;
//    }

    public List<Entity> getAlignmentSpaces(String subjectKey) throws ComputeException {
        try {
            return _annotationDAO.getEntitiesByTypeName(subjectKey, EntityConstants.TYPE_ALIGNMENT_SPACE);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get common root entities for "+subjectKey, e);
            throw new ComputeException("Error getting common root entities", e);
        }
    }
    
    public Entity getChildFolderByName(String subjectKey, Long parentId, String folderName, boolean createIfNecessary) throws ComputeException {
        try {
            return _annotationDAO.getChildFolderByName(subjectKey, parentId, folderName, createIfNecessary);
        }
        catch (DaoException e) {
            _logger.error("Error trying to get common root called "+folderName, e);
        }
        return null;
    }
    
    public List<Entity> getEntitiesWithFilePath(String filePath) {
        try {
            return _annotationDAO.getEntitiesWithFilePath(filePath);
        } catch (Exception e) {
            _logger.error("Unexpected error finding Entities matching path = " + filePath);
        }
        return null;
    }

    public Map<Entity, Map<String, Double>> getPatternAnnotationQuantifiers() throws ComputeException {
        try {
            return _annotationDAO.getPatternAnnotationQuantifiers();
        } catch (DaoException e) {
            _logger.error("Error in getPatternAnnotationQuantifiers(): "+e.getMessage());
            throw new ComputeException("Error in getPatternAnnotationQuantifiers(): "+e.getMessage(), e);
        }
    }

    public Map<Entity, Map<String, Double>> getMaskQuantifiers(String maskFolderName) throws ComputeException {
        try {
            return _annotationDAO.getMaskQuantifiers(maskFolderName);
        } catch (DaoException e) {
            _logger.error("Error in getMaskQuantifiers(): "+e.getMessage());
            throw new ComputeException("Error in getMaskQuantifiers(): "+e.getMessage(), e);
        }
    }

    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws ComputeException {
        try {
            return _annotationDAO.getPatternAnnotationQuantifierMapsFromSummary();
        } catch (DaoException e) {
            _logger.error("Error in getPatternAnnotationQuantifierMapsFromSummary(): "+e.getMessage());
            throw new ComputeException("Error in getPatternAnnotationQuantifierMapsFromSummary(): "+e.getMessage(), e);
        }
    }

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws ComputeException {
        try {
            return _annotationDAO.getMaskQuantifierMapsFromSummary(maskFolderName);
        } catch (DaoException e) {
            _logger.error("Error in getMaskQuantifierMapsFromSummary(): "+e.getMessage());
            throw new ComputeException("Error in getMaskQuantifierMapsFromSummary(): "+e.getMessage(), e);
        }
    }

    private void updateIndexForAnnotationTargets(List<Entity> annotations) {
    	if (annotations==null) return;
    	for(Entity annotation : annotations) {
    		Long entityId = null;
    		try {
    			entityId = new Long(annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID));
            }
            catch (Exception e) {
            	_logger.error("Error getting entity id for annotation id="+annotation.getId(),e);
            }
            
            if (entityId!=null) {
            	updateIndex(entityId);
            }
    	}
    }

    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) {
        return PatternSearchDAO.getDataDescriptors(type);
    }

    public int patternSearchGetState() {
        return PatternSearchDAO.getState();
    }

    public List<String> patternSearchGetCompartmentList(String type) {
        return PatternSearchDAO.getCompartmentList(type);
    }

    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) {
        return PatternSearchDAO.getFilteredResults(type, filterMap);
    }

    public Entity createDataSet(String subjectKey, String dataSetName) throws ComputeException {
        try {
            Entity dataSet = _annotationDAO.createDataSet(subjectKey, dataSetName);
        	_logger.info("Created data set "+dataSetName+" (id="+dataSet.getId()+") for subject "+subjectKey);
        	return dataSet;
        }
        catch (Exception e) {
            _logger.error("Error creating new data set ("+dataSetName+") for subject "+subjectKey,e);
            throw new ComputeException("Error creating new data set ("+dataSetName+") for subject "+subjectKey,e);
        }
    }
    
    public List<Entity> getAllDataSets() throws ComputeException {
    	try {
    		return _annotationDAO.getUserEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET);
	    }
	    catch (DaoException e) {
	        _logger.error("Error getting data sets", e);
	        throw new ComputeException("Error getting data sets",e);
	    }
    }
    
    public List<Entity> getDataSets(String subjectKey) throws ComputeException {
        try {
            List<String> subjectKeys = _annotationDAO.getSubjectKeys(subjectKey);
            if (EntityUtils.isAdmin(subjectKeys)) {
                return getAllDataSets();
            }

            Set<Entity> dataSets = new LinkedHashSet<Entity>();
            dataSets.addAll(_annotationDAO.getUserEntitiesByTypeName(subjectKey, EntityConstants.TYPE_DATA_SET));
            
            User user = _annotationDAO.getUserByNameOrKey(subjectKey);
            for(SubjectRelationship relation : user.getGroupRelationships())  {
                if (relation.getRelationshipType().equals(SubjectRelationship.TYPE_GROUP_ADMIN) 
                        || relation.getRelationshipType().equals(SubjectRelationship.TYPE_GROUP_OWNER)) {
                    dataSets.addAll(_annotationDAO.getUserEntitiesByTypeName(relation.getGroup().getKey(), EntityConstants.TYPE_DATA_SET));        
                }
            }
            
            return new ArrayList<Entity>(dataSets);
        }
        catch (DaoException e) {
            _logger.error("Error getting data sets", e);
            throw new ComputeException("Error getting data sets",e);
        }
    }
    
    public List<Entity> getUserDataSets(List<String> userLoginList) throws ComputeException {
        try {
            Set<Entity> dataSets = new LinkedHashSet<Entity>();
            for(String userLogin : userLoginList)  {
                dataSets.addAll(_annotationDAO.getUserEntitiesByTypeName(userLogin, EntityConstants.TYPE_DATA_SET));
            }
            return new ArrayList<Entity>(dataSets);
        }
        catch (DaoException e) {
            final String msg = "Error getting data sets for " + userLoginList;
            _logger.error(msg, e);
            throw new ComputeException(msg, e);
        }
    }

    public Entity getUserDataSetByName(String subjectKey, String dataSetName) throws ComputeException {
        try {
        	List<Entity> dataSets = _annotationDAO.getUserEntitiesByNameAndTypeName(subjectKey, dataSetName, EntityConstants.TYPE_DATA_SET);
        	if (dataSets.size()>1) {
        		_logger.warn("Found more than one data set for name: "+dataSetName);
        	}
            return dataSets.isEmpty() ? null : dataSets.get(0);
        }
        catch (DaoException e) {
            _logger.error("Error getting data set: "+dataSetName, e);
            throw new ComputeException("Error getting data set: "+dataSetName+" for subject "+subjectKey,e);
        }
    }
    
    public Entity getUserDataSetByIdentifier(String dataSetIdentifier) throws ComputeException {
        try {
        	List<Entity> dataSets = _annotationDAO.getUserEntitiesWithAttributeValue(null, EntityConstants.TYPE_DATA_SET, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier);
        	if (dataSets.size()>1) {
        		_logger.warn("Found more than one data set for identifier: "+dataSetIdentifier);
        	}
            return dataSets.isEmpty() ? null : dataSets.get(0);
        }
        catch (DaoException e) {
        	_logger.error("Error getting data set: "+dataSetIdentifier, e);
        	throw new ComputeException("Error getting data set: "+dataSetIdentifier,e);
        }
    }
    

    public Entity createFlyLineRelease(String subjectKey, String releaseName) throws ComputeException {
        try {
            Entity dataSet = _annotationDAO.createFlyLineRelease(subjectKey, releaseName);
            _logger.info("Created fly line release "+releaseName+" (id="+dataSet.getId()+") for subject "+subjectKey);
            return dataSet;
        }
        catch (Exception e) {
            _logger.error("Error creating new data set ("+releaseName+") for subject "+subjectKey,e);
            throw new ComputeException("Error creating new data set ("+releaseName+") for subject "+subjectKey,e);
        }
    }
    
    public List<Entity> getUserFlyLineReleases(List<String> subjectKeyList) throws ComputeException {
        try {
            Set<Entity> dataSets = new LinkedHashSet<Entity>();
            for(String userLogin : subjectKeyList)  {
                dataSets.addAll(_annotationDAO.getUserEntitiesByTypeName(userLogin, EntityConstants.TYPE_FLY_LINE_RELEASE));
            }
            return new ArrayList<Entity>(dataSets);
        }
        catch (DaoException e) {
            final String msg = "Error getting fly lines releases for " + subjectKeyList;
            _logger.error(msg, e);
            throw new ComputeException(msg, e);
        }
    }
    
    public void deleteAttribute(String ownerKey, String attributeName) throws ComputeException {
        try {
            _annotationDAO.deleteAttribute(ownerKey, attributeName);
        }
        catch (DaoException e) {
            _logger.error("Error deleting attribute: "+attributeName, e);
            throw new ComputeException("Error deleting attribute: "+attributeName,e);
        }
    }

    public List<Long> getOrphanAnnotationIdsMissingTargets(String subjectKey) throws ComputeException {
        try {
            return _annotationDAO.getOrphanAnnotationIdsMissingTargets(subjectKey);
        }
        catch (DaoException e) {
            _logger.error("Error getting orphan annotation ids for: "+subjectKey, e);
            throw new ComputeException("Error getting orphan annotation ids for:: "+subjectKey,e);
        }
    }

    public Entity createAlignmentBoard(String subjectKey, String alignmentBoardName, String alignmentSpace, String opticalRes, String pixelRes) throws ComputeException {
        try {
            Entity alignmentBoard = _annotationDAO.createAlignmentBoard(subjectKey, alignmentBoardName, alignmentSpace, opticalRes, pixelRes);
            _logger.info("Created alignment board "+alignmentBoardName+" (id="+alignmentBoard.getId()+") for subject "+subjectKey);
            return alignmentBoard;
        }
        catch (Exception e) {
            _logger.error("Error creating new alignment board ("+alignmentBoardName+") for subject "+subjectKey,e);
            throw new ComputeException("Error creating new alignment board ("+alignmentBoardName+") for subject "+subjectKey,e);
        }
    }
    
    public EntityData addAlignedItem(Entity parentEntity, Entity child, String alignedItemName, boolean visible) throws ComputeException {
        try {
            return _annotationDAO.addAlignedItem(parentEntity, child, alignedItemName, visible);
        }
        catch (DaoException e) {
            _logger.error("Error adding aligned item: "+child.getName(), e);
            throw new ComputeException("Error adding aligned item: "+child.getName(), e);
        }
    }

    public void createWorkspace(String ownerKey) throws ComputeException {

    	for(Entity workspace : _annotationDAO.getEntitiesByTypeName(ownerKey, EntityConstants.TYPE_WORKSPACE)) {
    		if (workspace.getOwnerKey().equals(ownerKey)) {
    			_logger.info("User "+ownerKey+" already has at least one workspace, skipping creation step.");
        		return;
    		}
    	}
    	
        List<Entity> roots = getCommonRootEntities(ownerKey);
        Collections.sort(roots, new EntityRootComparator(ownerKey));

        Entity newRoot = _annotationDAO.createEntity(ownerKey, EntityConstants.TYPE_WORKSPACE, EntityConstants.NAME_DEFAULT_WORKSPACE);
        
        int index = 0;
        for(Entity root : roots) {
        	_annotationDAO.addEntityToParent(newRoot, root, index++, EntityConstants.ATTRIBUTE_ENTITY, null, false);
        }
        
        _logger.info("Created workspace (id="+newRoot.getId()+") for "+ownerKey+" with "+(index+1)+" top-level roots");
    }
    
    public void addGroupWorkspaceToUserWorkspace(String userKey, String groupKey) throws ComputeException {
        try {
        	Entity groupWorkspace = _annotationDAO.getDefaultWorkspace(groupKey);
        	if (groupWorkspace==null) {
        		_logger.warn("addGroupWorkspaceToUserWorkspace: "+groupKey+" has no default workspace");
        		return;
        	}
        	Entity userWorkspace = _annotationDAO.getDefaultWorkspace(userKey);
        	if (userWorkspace==null) {
        		_logger.warn("addGroupWorkspaceToUserWorkspace: "+userKey+" has no default workspace");
        		return;
        	}
        	
        	List<Long> entityIds = new ArrayList<Long>();
	    	_annotationDAO.populateChildren(groupWorkspace);
        	for(Entity commonRoot : groupWorkspace.getOrderedChildren()) {
        		entityIds.add(commonRoot.getId());
        	}

        	_logger.info("Adding "+entityIds.size()+" "+groupKey+" entities to "+userKey+" workspace");
        	_annotationDAO.addChildren(userKey, userWorkspace.getId(), entityIds, EntityConstants.ATTRIBUTE_ENTITY);
        }
        catch (Exception e) {
            _logger.error("Error adding group workspace "+groupKey+" to user "+userKey, e);
            throw new ComputeException("Error adding group workspace "+groupKey+" to user "+userKey, e);
        }
    }
    
    public void removeGroupWorkspaceFromUserWorkspace(String userKey, String groupKey) throws ComputeException {
        try {
        	Entity userWorkspace = _annotationDAO.getDefaultWorkspace(userKey);
        	if (userWorkspace==null) {
        		_logger.warn("addGroupWorkspaceToUserWorkspace: "+userKey+" has no default workspace");
        		return;
        	}
        	
        	List<EntityData> toRemove = new ArrayList<EntityData>();
	    	_annotationDAO.populateChildren(userWorkspace);
        	for(EntityData ed : userWorkspace.getOrderedEntityData()) {
        		Entity child = ed.getChildEntity();
        		if (child!=null && child.getOwnerKey().equals(groupKey)) {
        			toRemove.add(ed);
        		}
        	}
        	
        	_logger.info("Removing "+toRemove.size()+" "+groupKey+" entities from "+userKey+" workspace");
        	for(EntityData ed : toRemove) {
        		_annotationDAO.deleteEntityData(ed);
        	}
        	
        }
        catch (Exception e) {
            _logger.error("Error adding group workspace "+groupKey+" to user "+userKey, e);
            throw new ComputeException("Error adding group workspace "+groupKey+" to user "+userKey, e);
        }
    }
    
    public void reorderWorkspace(String ownerKey) throws ComputeException {

        try {
	    	// Renumber the remaining workspace children
	    	Entity workspace = _annotationDAO.getDefaultWorkspace(ownerKey);
	    	_annotationDAO.populateChildren(workspace);
	
	        List<EntityData> orderedData = new ArrayList<EntityData>(workspace.getEntityData());
	        Collections.sort(orderedData, new EntityDataRootComparator(ownerKey));
	        
	        int index = 0;
	    	for(EntityData ed : workspace.getOrderedEntityData()) {
	    		if (ed.getChildEntity()==null) continue;
	    		ed.setOrderIndex(index++);
	    		_annotationDAO.saveOrUpdateEntityData(ed);
	    	}
        }
        catch (Exception e) {
            _logger.error("Error ordering workspace for "+ownerKey, e);
            throw new ComputeException("Error ordering workspace for "+ownerKey, e);
        }
    }
    
    private class EntityDataRootComparator implements Comparator<EntityData> {
    	protected String ownerKey;
        public EntityDataRootComparator(String ownerKey) {
			this.ownerKey = ownerKey;
        }
        public int compare(EntityData o1, EntityData o2) {
        	EntityRootComparator comparator = new EntityRootComparator(ownerKey);
            return comparator.compare(o1.getChildEntity(), o2.getChildEntity());
        }
    };
    
    private class EntityRootComparator implements Comparator<Entity> {
    	protected String ownerKey;
        public EntityRootComparator(String ownerKey) {
			this.ownerKey = ownerKey;
        }
		public int compare(Entity o1, Entity o2) {
            return ComparisonChain.start()
                .compareTrueFirst(o1.getOwnerKey().equals(ownerKey), o2.getOwnerKey().equals(ownerKey))
                .compare(o1.getOwnerKey(), o2.getOwnerKey())
                .compareTrueFirst(EntityUtils.isProtected(o1), EntityUtils.isProtected(o2))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_DATA_SETS), o2.getName().equals(EntityConstants.NAME_DATA_SETS))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_RETIRED_DATA), o2.getName().equals(EntityConstants.NAME_RETIRED_DATA))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_BLOCKED_DATA), o2.getName().equals(EntityConstants.NAME_BLOCKED_DATA))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_SHARED_DATA), o2.getName().equals(EntityConstants.NAME_SHARED_DATA))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_SEARCH_RESULTS), o2.getName().equals(EntityConstants.NAME_SEARCH_RESULTS))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_ALIGNMENT_BOARDS), o2.getName().equals(EntityConstants.NAME_ALIGNMENT_BOARDS))
                .compareTrueFirst(o1.getName().equals(EntityConstants.NAME_SPLIT_PICKING), o2.getName().equals(EntityConstants.NAME_SPLIT_PICKING))
                .compare(o1.getId(), o2.getId()).result();
        }
    };
}
