package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.ontology.Category;
import org.janelia.it.jacs.model.ontology.Interval;
import org.janelia.it.jacs.model.ontology.OntologyTermType;
import org.janelia.it.jacs.model.user_data.User;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.*;

@Stateless(name = "AnnotationEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 1, timeout = 10000)
public class AnnotationBeanImpl implements AnnotationBeanLocal, AnnotationBeanRemote {
	
    private Logger _logger = Logger.getLogger(this.getClass());
    
    public static final String APP_VERSION = "jacs.version";
    public static final String SEARCH_EJB_PROP = "AnnotationEJB.Name";
    public static final String MDB_PROVIDER_URL_PROP = "AsyncMessageInterface.ProviderURL";

    private final AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    private final ComputeDAO _computeDAO = new ComputeDAO(_logger);

    private final Map<String, EntityType> entityByName = new HashMap<String, EntityType>();
    private final Map<String, EntityAttribute> attrByName = new HashMap<String, EntityAttribute>();

    private void preloadData() {

        try {
            if (entityByName.isEmpty()) {
                System.out.println("  Preloading entity types...");
                for(EntityType entityType : _annotationDAO.getAllEntityTypes()) {
                    System.out.println("    Loaded entity type: "+entityType.getName());
                    entityByName.put(entityType.getName(), entityType);
                }
            }

            if (attrByName.isEmpty()) {
                System.out.println("  Preloading attribute types...");
                for(EntityAttribute entityAttr : _annotationDAO.getAllEntityAttributes()) {
                    System.out.println("    Loaded entity attr: "+entityAttr.getName());
                    attrByName.put(entityAttr.getName(), entityAttr);
                }
            }
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying preload models.", e);
        }
    }

    public String addAnnotation(String owner, String namespace, String term, String value, String comment, String conditional){
        Annotation tmpAnnotation = new Annotation(0, owner, namespace, term, value, comment, conditional, owner,
                new Date(System.currentTimeMillis()), null, false);
        try {
            Annotation newAnnotation = _annotationDAO.addAnnotation(tmpAnnotation);
            return Long.toString(newAnnotation.getUniqueIdentifier());
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to add an annotation.", e);
        }
        return null;
    }

    public void deleteAnnotation(String owner, String uniqueIdentifier){
        try {
            boolean deleteSuccessful = _annotationDAO.deleteAnnotation(owner, uniqueIdentifier);
            if (deleteSuccessful) {
                System.out.println("Deleted annotation "+uniqueIdentifier+" for user "+owner);
            }
            else {
                System.out.println("DID NOT delete annotation "+uniqueIdentifier+" for user "+owner);
            }
        }
        catch (Exception e) {
            _logger.error("Unexpected error while trying to delete annotation "+uniqueIdentifier+" for user "+owner);
        }
    }

    public ArrayList<Annotation> getAnnotationsForUser(String owner){
        try {
            return _annotationDAO.getAnnotationsForUser(owner);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for user "+owner, e);
        }
        return null;
    }

    public ArrayList<Annotation> getAnnotationsForUserBySession(String owner, String sessionId){
        try {
            return _annotationDAO.getAnnotationsForUserBySession(owner, sessionId);
        }
        catch (Exception e) {
            _logger.error("Unexpected error occurred while trying to get annotations for user "+owner, e);
        }
        return null;
    }

    public void editAnnotation(String owner, String uniqueIdentifier, String namespace, String term, String value,
                               String comment, String conditional){
        try {
            Annotation targetAnnotation = _annotationDAO.getAnnotationById(owner, uniqueIdentifier);
            targetAnnotation.setNamespace(namespace);
            targetAnnotation.setTerm(term);
            targetAnnotation.setValue(value);
            targetAnnotation.setComment(comment);
            targetAnnotation.setConditional(conditional);
            _annotationDAO.updateAnnotation(targetAnnotation);
        }
        catch (Exception e) {
            _logger.error("Unexopected error while trying to update Annotation "+uniqueIdentifier+" for user "+owner);
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

    /**
     * Ontology Section
     */

    public Entity createOntologyRoot(String userLogin, String rootName) {
        try {
            preloadData();

            User tmpUser = _computeDAO.getUserByName(userLogin);
            EntityType tmpType = entityByName.get(EntityConstants.TYPE_NAME_ONTOLOGY_ROOT);
            Entity newOntologyRoot = new Entity(null, rootName, tmpUser, null, tmpType, new Date(), new Date(), null);
            _annotationDAO.saveOrUpdate(newOntologyRoot);

            // Add the type
            EntityAttribute ontologyTypeAttribute = attrByName.get(EntityConstants.ATTR_NAME_ONTOLOGY_TERM_TYPE);
            EntityData termData = new EntityData(null, ontologyTypeAttribute, newOntologyRoot, null,
                    tmpUser, Category.class.getSimpleName(), new Date(), new Date(), null);
            _annotationDAO.saveOrUpdate(termData);

            return newOntologyRoot;
        }
        catch (Exception e) {
            _logger.error("Error trying to create a new Ontology Root ("+rootName+") for user "+userLogin,e);
        }
        return null;
    }

    public Entity createOntologyTerm(String userLogin, String ontologyTermParentId, String termName, OntologyTermType type, Integer orderIndex) {
        try {
            preloadData();

            User tmpUser = _computeDAO.getUserByName(userLogin);
            EntityType tmpElementType = entityByName.get(EntityConstants.TYPE_NAME_ONTOLOGY_ELEMENT);

            // Create and save the new entity
            Entity newOntologyElement = new Entity(null, termName, tmpUser, null, tmpElementType, new Date(), new Date(), null);
            _annotationDAO.saveOrUpdate(newOntologyElement);

            // Associate the entity to the parent
            EntityAttribute ontologyElementAttribute = attrByName.get(EntityConstants.ATTR_NAME_ONTOLOGY_ELEMENT);
            Entity parentOntologyElement = _annotationDAO.getEntityById(ontologyTermParentId);
            
            // If no order index is given then we add in last place
            if (orderIndex == null) {
            	int max = 0;
            	for(EntityData data : parentOntologyElement.getEntityData()) {
            		if (data.getOrderIndex() != null && data.getOrderIndex() > max) max = data.getOrderIndex(); 
            	}
            	orderIndex = max + 1;
            }
            
            EntityData newData = new EntityData(null, ontologyElementAttribute, parentOntologyElement, newOntologyElement,
                    tmpUser,  null, new Date(), new Date(), orderIndex);
            _annotationDAO.saveOrUpdate(newData);

            // Add the type
            EntityAttribute ontologyTypeAttribute = attrByName.get(EntityConstants.ATTR_NAME_ONTOLOGY_TERM_TYPE);
            EntityData termData = new EntityData(null, ontologyTypeAttribute, newOntologyElement, null,
                    tmpUser,  type.getClass().getSimpleName(), new Date(), new Date(), null);
            _annotationDAO.saveOrUpdate(termData);

            // Add the type-specific parameters
            if (type instanceof Interval) {

                Interval interval = (Interval)type;
                EntityAttribute intervalLowerAttribute = attrByName.get(EntityConstants.ATTR_NAME_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER);
                EntityData lowerData = new EntityData(null, intervalLowerAttribute, newOntologyElement, null,
                        tmpUser, interval.getLowerBound().toString(), new Date(), new Date(), null);
                _annotationDAO.saveOrUpdate(lowerData);

                EntityAttribute intervalUpperAttribute = attrByName.get(EntityConstants.ATTR_NAME_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER);
                EntityData upperData = new EntityData(null, intervalUpperAttribute, newOntologyElement, null,
                        tmpUser, interval.getUpperBound().toString(), new Date(), new Date(), null);
                _annotationDAO.saveOrUpdate(upperData);
            }
            
            return newOntologyElement;
        }
        catch (Exception e) {
            _logger.error("Error trying to create a new Ontology Term ("+termName+") for user "+userLogin,e);
        }
        return null;
    }

    public boolean removeOntologyTerm(String userLogin, String ontologyTermId) {
        try {
            boolean success = _annotationDAO.deleteOntologyTerm(userLogin, ontologyTermId);
            if (success) {
                _logger.debug("Deleted term "+ontologyTermId+" for user "+userLogin);
                return true;
            }
            else {
                _logger.error("Unable to delete term "+ontologyTermId+" for user "+userLogin);
                return false;
            }
        }
        catch (Exception e) {
            _logger.error("Error trying to delete the Ontology Term ("+ontologyTermId+") for user "+userLogin,e);
        }
        return false;
    }
   
    public Entity cloneEntityTree(Entity sourceRoot, String targetUserLogin, String targetRootName) throws DaoException {

        Session session = _annotationDAO.getCurrentSession();
        Transaction tx = session.beginTransaction();

        if (null == sourceRoot) {
            throw new DaoException("Cannot find the source ontology.");
        }

        User targetUser = _annotationDAO.getUserByName(targetUserLogin);

        if (targetUser == null) {
            throw new DaoException("Cannot find the target user.");
        }

        try {
            return cloneEntityTree(tx, sourceRoot, targetUser, targetRootName, true);
        }
        catch (Exception e) {
        	tx.rollback();
            _logger.error("Error trying to clone the Ontology ("+sourceRoot.getId()+")",e);
            throw new DaoException(e);
        }
        finally {
            try {
            	session.flush();
                tx.commit();
            	session.close();
            }
            catch (Exception ex) {
                _logger.error("Error trying to clone the Ontology ("+sourceRoot.getId()+")",ex);
                throw new DaoException(ex);
            }
        }
    }

    private Entity cloneEntityTree(Transaction tx, Entity source, User targetUser, String targetName, boolean isRoot) throws DaoException {

    	// TODO: detect cycles

        // Create new ontology element
        Entity newOntologyElement = new Entity(null, targetName, targetUser, null, source.getEntityType(), new Date(), new Date(), null);
    	_annotationDAO.saveOrUpdate(newOntologyElement);

        // Add the children 
    	for(EntityData ed : source.getEntityData()) {

    		Entity newChildEntity = null;
    		Entity childEntity = ed.getChildEntity();
    		if (childEntity != null) {
    			newChildEntity = cloneEntityTree(tx, childEntity, targetUser, childEntity.getName(), false);	
    		}

            EntityData newEd = new EntityData(null, ed.getEntityAttribute(), newOntologyElement, newChildEntity,
            		targetUser, ed.getValue(), new Date(), new Date(), ed.getOrderIndex());

        	_annotationDAO.saveOrUpdate(newEd);
    	}

    	return newOntologyElement;
    }
    
    public Entity saveOrUpdateEntity(Entity entity) {
        try {
            _annotationDAO.saveOrUpdate(entity);
            return entity;
        } catch (DaoException e) {
            _logger.error("Error trying to save or update Entity");
        }
        return null;
    }

    public EntityType getEntityTypeByName(String name) {
        try {
            return _annotationDAO.getEntityTypeByName(name);
        } catch (DaoException e) {
            _logger.error("Error trying to get EntityType by name = " + name);
        }
        return null;
    }

    public Entity getEntityById(String targetId) {
        try {
            return _annotationDAO.getEntityById(targetId);
        }
        catch (Exception e) {
            _logger.error("Error trying to get the entities of id "+targetId);
        }
        return null;
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

    public java.util.List<Entity> getUserEntitiesByType(String userLogin, long entityTypeId) {
        try {
            List<Entity> returnList = _annotationDAO.getUserEntitiesByName(userLogin, entityTypeId);
            System.out.println("Entities returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeId+" for user "+userLogin);
        }
        return null;
    }

    public List<EntityType> getEntityTypes() {
        try {
            List<EntityType> returnList = _annotationDAO.getAllEntityTypes();
            System.out.println("Entity types returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entity types");
        }
        return null;
    }

    public List<Entity> getCommonRootEntitiesByType(long entityTypeId) {
        try {
            List<Entity> returnList = new ArrayList<Entity>();
            List<Entity> tmpList = _annotationDAO.getUserEntitiesByName(null, entityTypeId);
            // todo This is a little brute-force and could probably have a better query
            for (Entity entity : tmpList) {
                for (EntityData entityData : entity.getEntityData()) {
                    if (EntityConstants.ATTRIBUTE_COMMON_ROOT.equals(entityData.getEntityAttribute().getName())) {
                        returnList.add(entity);
                        break;
                    }
                }
            }

            System.out.println("Entities returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeId);
        }
        return null;
    }

    public List<Entity> getEntitiesByType(long entityTypeId) {
        try {
            List<Entity> returnList = _annotationDAO.getUserEntitiesByName(null, entityTypeId);
            System.out.println("Entities returned:"+returnList.size());
            return returnList;
        }
        catch (DaoException e) {
            _logger.error("Error trying to get the entities of type "+entityTypeId);
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

    public Set<Entity> getParentEntities(long entityId) {
        try {
            return _annotationDAO.getParentEntities(entityId);
        } catch (DaoException e) {
            _logger.error("Error trying to get parent entities for id="+entityId+" message: "+e.getMessage());
        }
        return null;
    }

}
