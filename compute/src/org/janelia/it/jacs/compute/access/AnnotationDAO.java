package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.entity.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public EntityAttribute getEntityAttributeByName(String name) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(EntityAttribute.class);
            c.add(Expression.eq("name", name));
            List l = c.list();
            if (l==null || l.size()==0) {
                return null; // no result
            } else if (l.size()>1) {
                throw new DaoException("Unexpectedly found more than one EntityAttribute with name = " + name);
            } else {
                EntityAttribute ea = (EntityAttribute)l.get(0);
                return ea;
            }
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public EntityType getEntityTypeByName(String name) throws DaoException {
         try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(EntityType.class);
            c.add(Expression.eq("name", name));
            List l = c.list();
            if (l==null || l.size()==0) {
                return null; // no result
            } else if (l.size()>1) {
                throw new DaoException("Unexpectedly found more than one EntityType with name = " + name);
            } else {
                EntityType et = (EntityType)l.get(0);
                return et;
            }
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Set<EntityAttribute> getEntityAttributesByEntityType(EntityType entityType) throws DaoException {
         try {
            Session session = getCurrentSession();
            if (entityType.getId()==null || entityType.getId()==0) {
                Criteria c = session.createCriteria(EntityType.class);
                c.add(Expression.eq("name", entityType.getName()));
                List l = c.list();
                if (l==null || l.size()==0) {
                    throw new Exception("Could not find EntityType with name = " + entityType.getName());
                }
                entityType = (EntityType)l.get(0);
            }
            Long entityTypeId = entityType.getId();
            Criteria c2 = session.createCriteria(EntityTypeAttribute.class);
            c2.add(Expression.eq("entityTypeId", entityTypeId));
            List l2 = c2.list();
            if (l2==null || l2.size()==0) {
                _logger.info("Could not find any entries in EntityTypeAttribute for EntityType name = " + entityType.getName() + " id = " + entityTypeId);
                return null; // no matches for EntityTypeId
            }
            Set<EntityAttribute> resultSet = new HashSet<EntityAttribute>();
            for (Object o : l2) {
                EntityTypeAttribute eta = (EntityTypeAttribute)o;
                Criteria c3 = session.createCriteria(EntityAttribute.class);
                c3.add(Expression.eq("id", eta.getEntityAttId()));
                List l3 = c3.list();
                if (l3.size()!=1) {
                    throw new Exception("Expected to find precisely one match in EntityAttribute for EntityAttributeId = " + eta.getEntityAttId());
                }
                EntityAttribute ea = (EntityAttribute)l3.get(0);
                resultSet.add(ea);
            }
            return resultSet;
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }


}
