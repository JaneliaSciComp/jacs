package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.hibernate.Query;
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
            _logger.error("Error trying to create a new Ontology Root ("+rootName+") for user "+userLogin,e);
        }
        return null;
    }

    public Entity createOntologyTerm(String userLogin, String ontologyTermParentId, String termName, OntologyTermType type, Integer orderIndex) {
        try {
            User tmpUser = _computeDAO.getUserByName(userLogin);

            // Create and save the new entity
            Entity newOntologyElement = newEntity(EntityConstants.TYPE_ONTOLOGY_ELEMENT, termName, tmpUser);
            _annotationDAO.saveOrUpdate(newOntologyElement);

            Entity parentOntologyElement = _annotationDAO.getEntityById(ontologyTermParentId);
            
            // If no order index is given then we add in last place
            if (orderIndex == null) {
            	int max = 0;
            	for(EntityData data : parentOntologyElement.getEntityData()) {
            		if (data.getOrderIndex() != null && data.getOrderIndex() > max) max = data.getOrderIndex(); 
            	}
            	orderIndex = max + 1;
            }

            // Associate the entity to the parent
            EntityData childData = newData(parentOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT, tmpUser);
            childData.setChildEntity(newOntologyElement);
            childData.setOrderIndex(orderIndex);
            _annotationDAO.saveOrUpdate(childData);

            // Add the type
            EntityData termData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, tmpUser);
            termData.setValue(type.getClass().getSimpleName());
            _annotationDAO.saveOrUpdate(termData);

            // Add the type-specific parameters
            if (type instanceof Interval) {

                Interval interval = (Interval)type;

                EntityData lowerData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_LOWER, tmpUser);
                lowerData.setValue(interval.getLowerBound().toString());
                _annotationDAO.saveOrUpdate(lowerData);

                EntityData upperData = newData(newOntologyElement, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE_INTERVAL_UPPER, tmpUser);
                upperData.setValue(interval.getUpperBound().toString());
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

        User targetUser = _annotationDAO.getUserByName(targetUserLogin);
        if (targetUser == null) {
            throw new DaoException("Cannot find the target user.");
        }

        Session session = null;
        Transaction tx = null;
        
        try {
            session = _annotationDAO.getCurrentSession();
            tx = session.beginTransaction();
            Entity cloned = cloneEntityTree(tx, sourceRoot, targetUser, targetRootName, true);
        	session.flush();
            tx.commit();
            return cloned;
            
        }
        catch (Exception e) {
        	tx.rollback();
            _logger.error("Error trying to clone the Ontology ("+sourceRoot.getId()+")",e);
            throw new DaoException(e);
        }
        finally {
            if (session != null) session.close();
        }
    }

	// TODO: detect cycles
    private Entity cloneEntityTree(Transaction tx, Entity source, User targetUser, String targetName, boolean isRoot) throws DaoException {

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
    			newChildEntity = cloneEntityTree(tx, childEntity, targetUser, childEntity.getName(), false);	
    		}

            EntityData newEd = new EntityData(null, ed.getEntityAttribute(), newOntologyElement, newChildEntity,
            		targetUser, ed.getValue(), new Date(), new Date(), ed.getOrderIndex());

        	_annotationDAO.saveOrUpdate(newEd);
    	}

    	return newOntologyElement;
    }
    
    public Entity publishOntology(Entity sourceRoot, String targetRootName) throws DaoException {

        Session session = null;
        Transaction tx = null;

        try {
            session = _annotationDAO.getCurrentSession();
            tx = session.beginTransaction();
            
            Entity clonedEntity = cloneEntityTree(tx, sourceRoot, sourceRoot.getUser(), targetRootName, true);

            // Add the public tag
            EntityData publicEd = newData(clonedEntity, EntityConstants.ATTRIBUTE_IS_PUBLIC, sourceRoot.getUser());
            publicEd.setValue("true");
            _annotationDAO.saveOrUpdate(publicEd);

        	session.flush();
            tx.commit();
            
            return clonedEntity;
        }
        catch (Exception e) {
        	tx.rollback();
            _logger.error("Error trying to clone the Ontology ("+sourceRoot.getId()+")",e);
            throw new DaoException(e);
        }
        finally {
            if (session != null) session.close();
        }
    }
    
    public List<Entity> getPublicOntologies() throws DaoException {

        try {
            EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
            EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
            
            StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
            hql.append(" where clazz.entityType.id=?");
            hql.append(" and exists (from EntityData as attr where attr.parentEntity = clazz and attr.entityAttribute.id = ?)");
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = _annotationDAO.getCurrentSession().createQuery(hql.toString());
            query.setLong(0, tmpType.getId());
            query.setLong(1, tmpAttr.getId());
            return query.list();
        }
        catch (Exception e) {
            _logger.error("Error trying to get public ontologies",e);
            throw new DaoException(e, "getPrivateOntologies");
        }
    }
    
    public List<Entity> getPrivateOntologies(String userLogin) throws DaoException {
    	
        try {
            EntityType tmpType = getEntityTypeByName(EntityConstants.TYPE_ONTOLOGY_ROOT);
            EntityAttribute tmpAttr = getEntityAttributeByName(EntityConstants.ATTRIBUTE_IS_PUBLIC);
            
            StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
            hql.append(" where clazz.entityType.id=?");
            if (null != userLogin) {
                hql.append(" and clazz.user.userLogin=?");
            }
            hql.append(" and not exists (from EntityData as attr where attr.parentEntity = clazz and attr.entityAttribute.id = ?)");
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = _annotationDAO.getCurrentSession().createQuery(hql.toString());
            query.setLong(0, tmpType.getId());
            query.setString(1, userLogin);
            query.setLong(2, tmpAttr.getId());
            return query.list();
        }
        catch (Exception e) {
            _logger.error("Error trying to get private ontologies for "+userLogin,e);
            throw new DaoException(e, "getPrivateOntologies");
        }
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
