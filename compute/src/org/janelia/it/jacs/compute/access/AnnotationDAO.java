package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
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
            Annotation tmpAnnotation = (Annotation) c.uniqueResult();
            if (null == tmpAnnotation) {
                // This should never happen
                throw new DaoException("Cannot complete deletion when there are more than one annotation with that identifier.");
            }
            if (!tmpAnnotation.getOwner().equals(owner)) {
                throw new DaoException("Cannot delete the annotation as the requestor doesn't own the annotation.");
            }
            session.delete(tmpAnnotation);
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
            if (l == null || l.size() == 0) return new ArrayList<Annotation>();
            return (ArrayList<Annotation>) l;
        }
        catch (HibernateException e) {
            throw new DaoException("Unable to get Annotations for user " + owner, e);
        }
    }

    public Annotation getAnnotationById(String owner, String uniqueIdentifier) {
        Annotation tmpAnnotation = (Annotation) getCurrentSession().get(Annotation.class, uniqueIdentifier);
        if (null != tmpAnnotation && tmpAnnotation.getOwner().equals(owner)) {
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
        List<EntityData> entityDataList = (List<EntityData>) c.list();
        if (entityDataList == null || entityDataList.size() == 0) return new ArrayList<Entity>(); // no matches
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
        EntityType entityType = (EntityType) c.uniqueResult();
        if (null != entityType) {
            return entityType;
        }
        return null;
    }

    public Entity getEntityById(String targetId) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Entity.class);
        c.add(Expression.eq("id", Long.valueOf(targetId)));
        Entity entity = (Entity) c.uniqueResult();
        if (null != entity) {
            return entity;
        }
        return null;
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
        try {
            Session session = getCurrentSession();
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
            // Child Entity items should be deleted by cascade
            session.delete(entity);
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
            // SHould be using uniqueResult
            return (EntityAttribute) c.uniqueResult();
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public EntityType getEntityTypeByName(String name) throws DaoException {
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

            //========== Status ============
            createEntityStatus(EntityConstants.STATUS_DEPRECATED);

            //========== Attribute ============
            createEntityAttribute(EntityConstants.ATTRIBUTE_FILE_PATH);
            createEntityAttribute(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
            createEntityAttribute(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            createEntityAttribute(EntityConstants.ATTRIBUTE_ENTITY);

            //========== Type ============
            Set<String> lsmAttributeNameSet = new HashSet<String>();
            lsmAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
            createEntityType(EntityConstants.TYPE_LSM_STACK, lsmAttributeNameSet);

            Set<String> ontologyElementAttributeNameSet = new HashSet<String>();
            ontologyElementAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
            createEntityType(EntityConstants.TYPE_ONTOLOGY_ELEMENT, ontologyElementAttributeNameSet);

            Set<String> ontologyRootAttributeNameSet = new HashSet<String>();
            ontologyRootAttributeNameSet.add(EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
            createEntityType(EntityConstants.TYPE_ONTOLOGY_ROOT, ontologyRootAttributeNameSet);

            Set<String> folderAttributeNameSet = new HashSet<String>();
            folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
            folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_COMMON_ROOT);
            folderAttributeNameSet.add(EntityConstants.ATTRIBUTE_ENTITY);
            createEntityType(EntityConstants.TYPE_FOLDER, folderAttributeNameSet);

            Set<String> neuronSeparationAttributeNameSet = new HashSet<String>();
            neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_FILE_PATH);
            neuronSeparationAttributeNameSet.add(EntityConstants.ATTRIBUTE_INPUT);
            createEntityType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, neuronSeparationAttributeNameSet);

        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    protected void createEntityStatus(String name) {
        try {
            Session session = getCurrentSession();
            EntityStatus entityStatus = new EntityStatus();
            entityStatus.setName(name);
            session.save(entityStatus);
        }
        catch (HibernateException e) {
            e.printStackTrace();
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
        session.save(entityType);
    }

    protected void createEntityAttribute(String name) {
        try {
            Session session = getCurrentSession();
            EntityAttribute entityAttribute = new EntityAttribute();
            entityAttribute.setName(name);
            session.save(entityAttribute);
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
            StringBuffer hql = new StringBuffer("select clazz from EntityType clazz");
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getAllEntityTypes");
        }
    }


    public List<Entity> getUserEntitiesByName(String userLogin, long entityTypeId) throws DaoException {
        try {
            StringBuffer hql = new StringBuffer("select clazz from Entity clazz");
            hql.append(" where clazz.entityType.id=" + entityTypeId);
            if (null != userLogin) {
                hql.append(" and clazz.user.userLogin='").append(userLogin).append("'");
            }
            if (_logger.isDebugEnabled()) _logger.debug("hql=" + hql);
            Query query = getCurrentSession().createQuery(hql.toString());
            return query.list();
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, "getUserEntitiesByName");
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

    public boolean deleteEntityById(Long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            Criteria c = session.createCriteria(Entity.class);
            c.add(Expression.eq("id", Long.valueOf(entityId)));
            Entity entity = (Entity) c.uniqueResult();
            session.delete(entity);
            System.out.println("The entity id=" + entityId + " has been deleted.");
            return true;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
    }

    public Set<Entity> getParentEntities(long entityId) throws DaoException {
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select clazz from EntityData clazz where clazz.childEntity.id='").append(entityId).append("'");
            Query query = session.createQuery(hql.toString());
            List<EntityData> edList = (List<EntityData>)query.list();
            Set<Entity> eSet=new HashSet<Entity>();
            for (EntityData ed : edList) {
                eSet.add(ed.getParentEntity());
            }
            return eSet;
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }

}
