package org.janelia.it.jacs.compute.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.PatternSearchDAO;
import org.janelia.it.jacs.compute.launcher.indexing.IndexingHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

@Stateless(name = "AnnotationEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 500, timeout = 10000)
public class AnnotationBeanImpl implements AnnotationBeanLocal, AnnotationBeanRemote {
	
    private static final Logger _logger = Logger.getLogger(AnnotationBeanImpl.class);

    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);

    private void updateIndex(Long entityId) {
    	IndexingHelper.updateIndex(entityId);
    }

    public Entity createOntologyRoot(String userLogin, String rootName) throws ComputeException {
        try {
            Entity ontologyRoot = _annotationDAO.createOntologyRoot(userLogin, rootName);
        	_logger.info(userLogin+" creating ontology "+ontologyRoot.getId());
        	return ontologyRoot;
        }
        catch (Exception e) {
            _logger.error("Error creating new ontology ("+rootName+") for user "+userLogin,e);
            throw new ComputeException("Error creating new ontology ("+rootName+") for user "+userLogin,e);
        }
    }

    public EntityData createOntologyTerm(String userLogin, Long ontologyTermParentId, String termName, OntologyElementType type, Integer orderIndex) throws ComputeException {
        try {
        	EntityData ed = _annotationDAO.createOntologyTerm(userLogin, ontologyTermParentId, termName, type, orderIndex);
            _logger.info(userLogin+" created ontology term "+ed.getChildEntity().getId());
            return ed;
        }
        catch (Exception e) {
            _logger.error("Error creating new term ("+termName+") for user "+userLogin,e);
            throw new ComputeException("Error creating new term ("+termName+") for user "+userLogin,e);
        }
    }

    public void removeOntologyTerm(String userLogin, Long ontologyTermId) throws ComputeException {
    	try {
    		_annotationDAO.deleteOntologyTerm(userLogin, ontologyTermId.toString());
    		_logger.info(userLogin+" deleted ontology term "+ontologyTermId);
    	}
    	catch (DaoException e) {
    		_logger.error("Could not delete ontology term with id="+ontologyTermId+" for user "+userLogin);
    		throw new ComputeException("Could not delete ontology term", e);
    	}
    }
   
    public Entity cloneEntityTree(String userLogin, Long sourceRootId, String targetRootName) throws ComputeException {
        try {
        	Entity ontologyRoot = _annotationDAO.getEntityById(""+sourceRootId);
            if (ontologyRoot==null) {
                throw new Exception("Ontology not found: "+sourceRootId);
            }
        	if (null==ontologyRoot.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PUBLIC)) {
        		throw new ComputeException("Cannot copy a private ontology:"+ontologyRoot.getId());
        	}
            Entity clonedTree = _annotationDAO.cloneEntityTree(sourceRootId, userLogin, targetRootName);
            _annotationDAO.fixInternalOntologyConsistency(clonedTree.getId());
            _logger.info(userLogin+" cloned ontology tree "+sourceRootId+" as "+targetRootName);
            return clonedTree;
        }
        catch (Exception e) {
            _logger.error("Error cloning ontology ("+sourceRootId+")",e);
            throw new ComputeException("Error cloning ontology",e);
        }
    }

    public Entity publishOntology(String userLogin, Long sourceRootId, String targetRootName) throws ComputeException {
        try {
        	Entity ontologyRoot = _annotationDAO.getEntityById(""+sourceRootId);
            if (ontologyRoot==null) {
                throw new Exception("Ontology not found: "+sourceRootId);
            }
        	if (!"system".equals(ontologyRoot.getUser().getUserLogin()) && !userLogin.equals(ontologyRoot.getUser().getUserLogin())) {
        		throw new ComputeException("User "+userLogin+" cannot publish ontology tree "+sourceRootId);
        	}
            Entity publishedTree = _annotationDAO.publishOntology(sourceRootId, targetRootName);
            _logger.info(userLogin+" publishing ontology tree "+sourceRootId+" as "+targetRootName);
            _annotationDAO.fixInternalOntologyConsistency(publishedTree.getId());
            return publishedTree;
        }
        catch (Exception e) {
            _logger.error("Error publishing ontology ("+sourceRootId+")",e);
            throw new ComputeException("Error publishing ontology",e);
        }
    }

	public Entity createOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException {

        try {
            Entity annotEntity = _annotationDAO.createOntologyAnnotation(userLogin, annotation);
            _logger.info(userLogin+" added annotation "+annotEntity.getId());
            updateIndex(annotation.getTargetEntityId());
            return annotEntity;
        }
        catch (Exception e) {
            _logger.error("Error creating ontology annotation for user "+userLogin,e);
            throw new ComputeException("Error creating ontology annotation for user "+userLogin,e);
        }
	}
	
	public Entity createSilentOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException {

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
            Long targetEntityId = _annotationDAO.removeOntologyAnnotation(userLogin, annotationId);
            _logger.info(userLogin+" removed annotation "+annotationId);
            updateIndex(targetEntityId);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to removeAnnotation");
            throw new ComputeException("Error trying to removeAnnotation",e);
        }
	}
	
	public void removeAllOntologyAnnotationsForSession(String userLogin, long sessionId) throws ComputeException {
        try {
            List<Entity> annotations = _annotationDAO.removeAllOntologyAnnotationsForSession(userLogin, sessionId);
            _logger.info(userLogin+" removed all annotations for session "+sessionId);
            updateIndexForAnnotationTargets(annotations);
        } 
        catch (DaoException e) {
            _logger.error("Error trying to removeAllOntologyAnnotationsForSession");
            throw new ComputeException("Error trying to removeAllOntologyAnnotationsForSession",e);
        }
	}
	
    public Entity getOntologyTree(String userLogin, Long id) throws ComputeException {

        try {
            Entity root = _annotationDAO.getEntityById(id);
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

    public Entity getErrorOntology() throws ComputeException {
        try {
            return _annotationDAO.getErrorOntology();
        }
        catch (Exception e) {
            _logger.error("Error getting error ontology",e);
            throw new ComputeException("Error getting error ontology",e);
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
    
    public List<Task> getAnnotationSessionTasks(String username) throws ComputeException {
        try {
             return _annotationDAO.getAnnotationSessions(username);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for entities "+username, e);
            throw new ComputeException("Coud not get annotations for entities ",e);
        }
    }

    public List<Entity> getAnnotationsForChildren(String username, long entityId) throws ComputeException {
        try {
            return _annotationDAO.getAnnotationsForChildren(username, entityId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for child of "+username, e);
            throw new ComputeException("Coud not get annotations for entities ",e);
        }
    }
    
    public List<Entity> getAnnotationsForEntities(String username, List<Long> entityIds) throws ComputeException {
        try {
            return _annotationDAO.getAnnotationsByEntityId(username, entityIds);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for "+username, e);
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

    public Entity createDataSet(String userLogin, String dataSetName) throws ComputeException {
        try {
            Entity dataSet = _annotationDAO.createDataSet(userLogin, dataSetName);
        	_logger.info("Created data set "+dataSetName+" (id="+dataSet.getId()+") for user "+userLogin);
        	return dataSet;
        }
        catch (Exception e) {
            _logger.error("Error creating new data set ("+dataSetName+") for user "+userLogin,e);
            throw new ComputeException("Error creating new data set ("+dataSetName+") for user "+userLogin,e);
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
    
    public List<Entity> getUserDataSets(String userLogin) throws ComputeException {
        try {
        	return _annotationDAO.getUserEntitiesByTypeName(userLogin, EntityConstants.TYPE_DATA_SET);
        }
        catch (DaoException e) {
            _logger.error("Error getting data sets for user "+userLogin, e);
            throw new ComputeException("Error getting data sets for user "+userLogin,e);
        }
    }
    
    public Entity getUserDataSetByName(String userLogin, String dataSetName) throws ComputeException {
        try {
        	List<Entity> dataSets = _annotationDAO.getUserEntitiesByNameAndTypeName(userLogin, dataSetName, EntityConstants.TYPE_DATA_SET);
        	if (dataSets.size()>1) {
        		_logger.warn("Found more than one data set for name: "+dataSetName);
        	}
            return dataSets.isEmpty() ? null : dataSets.get(0);
        }
        catch (DaoException e) {
            _logger.error("Error getting data set: "+dataSetName, e);
            throw new ComputeException("Error getting data set: "+dataSetName+" for user "+userLogin,e);
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
}
