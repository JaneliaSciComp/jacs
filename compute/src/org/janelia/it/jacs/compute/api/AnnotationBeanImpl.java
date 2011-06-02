package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.User;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.model.annotation.Annotation;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Stateless(name = "AnnotationEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 50, timeout = 10000)
public class AnnotationBeanImpl implements AnnotationBeanLocal, AnnotationBeanRemote {
    private Logger _logger = Logger.getLogger(this.getClass());
    public static final String APP_VERSION = "jacs.version";
    public static final String SEARCH_EJB_PROP = "AnnotationEJB.Name";
    public static final String MDB_PROVIDER_URL_PROP = "AsyncMessageInterface.ProviderURL";

    private AnnotationDAO _annotationDAO = new AnnotationDAO(_logger);
    private ComputeDAO _computeDAO = new ComputeDAO(_logger);

    public AnnotationBeanImpl() {
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

    public void createOntologyRoot(String userLogin, String rootName) {
        try {
            User tmpUser = _computeDAO.getUserByName(userLogin);
            EntityType tmpType = _annotationDAO.getEntityType(EntityConstants.TYPE_ONTOLOGY_ROOT);
            Entity newOntologyRoot = new Entity(null, rootName, tmpUser, null, tmpType, new Date(), new Date(), null);
            _annotationDAO.saveOrUpdate(newOntologyRoot);
        }
        catch (DaoException e) {
            _logger.error("Error trying to create a new Ontology Root ("+rootName+") for user "+userLogin,e);
        }
    }

    public void createOntologyTerm(String userLogin, String ontologyTermParentId, String termName) {
        try {
            User tmpUser = _computeDAO.getUserByName(userLogin);
            EntityType tmpElementType = _annotationDAO.getEntityType(EntityConstants.TYPE_ONTOLOGY_ELEMENT);
            // Create and save the new entity
            Entity newOntologyElement = new Entity(null, termName, tmpUser, null, tmpElementType, new Date(), new Date(), null);
            _annotationDAO.saveOrUpdate(newOntologyElement);
            // Associate the entity to the parent
            EntityAttribute ontologyElementAttribute = _annotationDAO.getEntityAttribute(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT_ID);
            Entity parentOntologyElement = _annotationDAO.getEntityById(ontologyTermParentId);
            EntityData newData = new EntityData(null, ontologyElementAttribute, parentOntologyElement, newOntologyElement,
                    tmpUser,  null, new Date(), new Date(), null);
            _annotationDAO.saveOrUpdate(newData);
        }
        catch (DaoException e) {
            _logger.error("Error trying to create a new Ontology Term ("+termName+") for user "+userLogin,e);
        }
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
        catch (DaoException e) {
            _logger.error("Error trying to delete the Ontology Term ("+ontologyTermId+") for user "+userLogin,e);
        }
        return false;
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

    public EntityAttribute getEntityAttributeByName(String name) {
        try {
            return _annotationDAO.getEntityAttributeByName(name);
        } catch (DaoException e) {
            _logger.error("Error trying to get EntityAttribute by name = " + name);
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

    public Set<EntityAttribute> getEntityAttributesByEntityType(EntityType entityType) {
        try {
            return _annotationDAO.getEntityAttributesByEntityType(entityType);
        } catch (DaoException e) {
            _logger.error("Error trying to get EntityAttributes by EntityType name = " + entityType.getName());
        }
        return null;
    }


}
