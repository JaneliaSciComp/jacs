package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.server.access.EntityDAO;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.ArrayList;
import java.util.List;

public class EntityDAOImpl extends DaoBaseImpl implements EntityDAO {
    private static Logger _logger = Logger.getLogger(EntityDAOImpl.class);

    // DAO's can only come from Spring's Hibernate
    private EntityDAOImpl() {
    }

    public EntityType getEntityTypeByName(String targetEntityType) throws DaoException {
        List result;
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(EntityType.class);
            criteria.add(Expression.eq("name", targetEntityType));
            result = getHibernateTemplate().findByCriteria(criteria);
            if (result.size() == 0) {
                return null;
            }
            if (result.size() > 1) {
                throw new Exception("Expecting at most one Entity Type with name " + targetEntityType);
            }
        }
        catch (Exception e) {
            throw handleException(e, "EntityDAOImpl - getEntityType");
        }
        return (EntityType) result.get(0);
    }

    public EntityType createEntityType(Long id, Long sequence, String name, String style, String description, String iconurl) throws DaoException {
        EntityType entityType;
        try {
            if (null == name || "".equals(name.trim())) {
                throw new DaoException(new Exception(), "Cannot create an Entity Type with an empty or missing name.");
            }
            entityType = new EntityType(id, sequence, name, style, description, iconurl);
            getHibernateTemplate().save(entityType);
            logger.info("Entity Type is :" + entityType.getName());
        }
        catch (HibernateException e) {
            throw handleException(e, "EntityDAOImpl - createEntityType");
        }
        return entityType;
    }

    public List<EntityType> findAllEntityTypes() throws DataAccessException, DaoException {
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(EntityType.class);
            return (List<EntityType>) getHibernateTemplate().findByCriteria(criteria);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "EntityDAOImpl - findAllEntityTypes");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "EntityDAOImpl - findAllEntityTypes");
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
    }

    @Override
    public void saveOrUpdateEntityType(EntityType entityType) throws DataAccessException, DaoException {
        saveOrUpdateObject(entityType, "EntityDAOImpl - saveOrUpdateEntityType");
    }

    /* used as a ping for DB health */
    public long countAllEntityTypes() throws DataAccessException, DaoException {

        List<Long> counts = getHibernateTemplate().find("select count(*) from EntityType ");
        return counts.get(0).longValue();
    }

    /**
     * The method searches the entity types
     *
     * @param startIndex
     * @param numRows
     * @param sortArgs
     * @return
     * @throws org.springframework.dao.DataAccessException
     * @throws org.janelia.it.jacs.server.access.hibernate.DaoException
     */
    public List<EntityType> getPagedEntityTypes(String searchString, int startIndex, int numRows, SortArgument[] sortArgs) throws DataAccessException, DaoException {
        try {
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
//                    if (dataSortField == null || dataSortField.length() == 0) {
//                        continue;
//                    }
//                    if (dataSortField.equals(UserDataVO.SORT_BY_USER_LOGIN)) {
//                        dataSortField = UserDataVO.SORT_BY_USER_LOGIN;
//                    }
//                    else if (dataSortField.equals(UserDataVO.SORT_BY_USER_ID)) {
//                        dataSortField = UserDataVO.SORT_BY_USER_ID;
//                    }
//                    else if (dataSortField.equals(UserDataVO.SORT_BY_FULLNAME)) {
//                        dataSortField = UserDataVO.SORT_BY_FULLNAME;
//                    }
//                    else if (dataSortField.equals(UserDataVO.SORT_BY_EMAIL)) {
//                        dataSortField = UserDataVO.SORT_BY_EMAIL;
//                    }
//                    else {
//                        // unknown or unsupported sort field -> therefore set it to null
//                        dataSortField = null;
//                    }
                    if (dataSortField != null && dataSortField.length() != 0) {
                        if (sortArg.isAsc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" asc");
                        }
                        else if (sortArg.isDesc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField).append(" desc");
                        }
                    }
                }
            }

            String orderByClause = "";
            if (orderByFieldsBuffer.length() > 0) {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            String hql = "select et from EntityType et ";
            if (searchString != null && searchString.length() > 0) {
                hql += "where lower(et.name) like lower(:searchString) ";
            }
            hql += orderByClause;

            Query query = getSession().createQuery(hql);
            if (searchString != null && searchString.length() > 0)
                query.setParameter("searchString", searchString);
            if (numRows > 0) {
                query.setFirstResult(startIndex);
                query.setMaxResults(numRows);
            }
            List<EntityType> list = null;
            _logger.debug("Entity Type HQL: " + hql);
            list = query.list();
            List<EntityType> entityTypeList = new ArrayList<EntityType>();
            for (EntityType aList : list) {
                entityTypeList.add(aList);
            }
            return entityTypeList;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "EntityDAOImpl - getPagedEntityTypes");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "EntityDAOImpl - getPagedEntityTypes");
        }
    }


    public int getNumEntityTypes(String searchString) throws DataAccessException, DaoException {
        try {
            _logger.debug("executing getNumEntityTypes");

            // Results
            int totalCount = 0;
            DetachedCriteria criteria = createEntityTypeQuery(searchString);
            criteria.setProjection(Projections.rowCount());
            List<Integer> list = (List<Integer>) getHibernateTemplate().findByCriteria(criteria);
            if (list != null && list.size() > 0) {
                totalCount = list.get(0).intValue();
                _logger.debug("EntityType count=" + totalCount);
            }
            else {
                _logger.debug("Found no entity types");
            }
            return totalCount;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "EntityDAOImpl - getNumEntityTypes");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "EntityDAOImpl - getNumEntityTypes");
        }
    }

    private DetachedCriteria createEntityTypeQuery(String searchString) {
        DetachedCriteria criteria = DetachedCriteria.forClass(EntityType.class);
        if (searchString != null && searchString.length() > 0)
            criteria.add(Expression.like("name", searchString).ignoreCase());

        return criteria;
    }


}
