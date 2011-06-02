package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

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
                throw new DaoException("Cannot complete deletion when there are more than one annotation with that identifier.");
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

    public List<Entity> getEntitiesWithFilePath(String filePath) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(EntityData.class);
        c.add(Expression.eq("value", filePath));
        List<EntityData> entityDataList = (List<EntityData>)c.list();
        if (entityDataList==null || entityDataList.size()==0)
            return new ArrayList<Entity>(); // no matches
        List<Entity> resultList = new ArrayList<Entity>();
        for (EntityData ed : entityDataList) {
            resultList.add(ed.getParentEntity());
        }
        return resultList;
    }

    public EntityType getEntityType(Long entityTypeConstant) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(EntityType.class);
        c.add(Expression.eq("id", entityTypeConstant));
        List<EntityType> entityTypes = (List<EntityType>) c.list();
        if (entityTypes.size()==1) {
            return entityTypes.get(0);
        }
        return null;
    }

    public Entity getEntityById(String targetId) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Entity.class);
        c.add(Expression.eq("id", targetId));
        List<Entity> entities = (List<Entity>) c.list();
        if (entities.size()==1) {
            return entities.get(0);
        }
        return null;
    }

    public EntityAttribute getEntityAttribute(long entityAttributeConstant) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(EntityAttribute.class);
        c.add(Expression.eq("id", entityAttributeConstant));
        List<EntityAttribute> attributes = (List<EntityAttribute>) c.list();
        if (attributes.size()==1) {
            return attributes.get(0);
        }
        return null;
    }

    public boolean deleteOntologyTerm(String userLogin, String ontologyTermId) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", Long.valueOf(ontologyTermId)));
            List l = c.list();
            if (l.size()>1) {
                // This should never happen
                throw new DaoException("Cannot complete deletion when there are more than one entity with that identifier.");
            }
            if (l.size()==1) {
                Entity tmpEntity = (Entity)l.get(0);
                if (!tmpEntity.getUser().getUserLogin().equals(userLogin)) {
                    throw new DaoException("Cannot delete the entity as the requestor doesn't own the item.");
                }
                // Child Entity items should be deleted by cascade
                session.delete(l.get(0));
            }
            System.out.println("The entity has been deleted.");
            return true;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
}
