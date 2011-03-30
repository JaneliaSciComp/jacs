package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.model.annotation.Annotation;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Date;

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
}
