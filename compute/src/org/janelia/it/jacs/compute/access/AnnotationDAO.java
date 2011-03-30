package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.annotation.Annotation;

import java.util.ArrayList;
import java.util.List;

public class AnnotationDAO extends ComputeBaseDAO {
    public AnnotationDAO(Logger logger) {
        super(logger);
    }

    public Annotation addAnnotation(Annotation newAnnotation) throws DaoException {
        try {
            Session session = getCurrentSession();
            session.saveOrUpdate(newAnnotation);
            return newAnnotation;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public boolean deleteAnnotation(String owner, String uniqueIdentifier) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(Annotation.class);
            c.add(Expression.eq("id", Long.valueOf(uniqueIdentifier)));
            List l = c.list();
            if (l.size()>1) {
                // This should never happen
                throw new DaoException("Cannot complete delete when there are more than one annotation with that identifier.");
            }
            if (l.size()==1) {
                Annotation tmpAnnotation = (Annotation)l.get(0);
                if (!tmpAnnotation.getOwner().equals(owner)) {
                    throw new DaoException("Cannot delete the annotation as the requestor doesn't own the annotation.");
                }
                session.delete(l.get(0));
            }
            System.out.println("The annotation has been deleted.");
            return true;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public ArrayList<Annotation> getAnnotationsForUser(String owner) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(Annotation.class);
            c.add(Expression.eq("owner", owner));
            List l = c.list();
            if (l==null || l.size() == 0) return new ArrayList<Annotation>();
            return (ArrayList<Annotation>) l;
        }
        catch (HibernateException e) {
            throw new DaoException("Unable to get Annotations for user "+owner, e);
        }
    }

    public Annotation getAnnotationById(String owner, String uniqueIdentifier){
        Annotation tmpAnnotation = (Annotation) getCurrentSession().get(Annotation.class, uniqueIdentifier);
        if (null!=tmpAnnotation && tmpAnnotation.getOwner().equals(owner)) {
            return tmpAnnotation;
        }
        return null;
    }

    public void updateAnnotation(Annotation targetAnnotation) {
        getSession().saveOrUpdate(targetAnnotation);
    }
}
