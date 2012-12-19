
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Projections;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataVO;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.UserDAO;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: Lfoster
 * Date: Aug 10, 2006
 * Time: 1:37:31 PM
 * Implementation of user data access object, using hibernate criteria for query.
 */
public class UserDAOImpl extends DaoBaseImpl implements UserDAO {
    private static Logger _logger = Logger.getLogger(UserDAOImpl.class);

    // DAO's can only come from Spring's Hibernate
    private UserDAOImpl() {
    }

    public User getUserByName(String requestingUser) throws DaoException {
        List result;
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(User.class);
            criteria.add(Expression.eq("userLogin", requestingUser));
            result = getHibernateTemplate().findByCriteria(criteria);
            if (result.size() == 0) {
                return null;
            }
            if (result.size() > 1) {
                throw new Exception("Expecting at most one User by name " + requestingUser);
            }
        }
        catch (HibernateException e) {
            throw handleException(e, "UserDAOImpl - getUserByName");
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "UserDAOImpl - getUserByName");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "UserDAOImpl - getUserByName");
        }
        catch (Exception e) {
            throw handleException(e, "UserDAOImpl - getUserByName");
        }
        return (User) result.get(0);
    }

    /**
     * this method should be used very carefully. It overrides autogeneration of
     * object ID. Use  createUser(String userLogin, String name) instead
     *
     * @param id
     * @param userLogin
     * @param name
     * @return
     * @throws DaoException
     */
    public User createUserWithID(Long id, String userLogin, String name) throws DaoException {
        User user;
        try {
            if (null == userLogin || "".equals(userLogin.trim())) {
                throw new DaoException(new Exception(), "Cannot create a user with an empty or missing login.");
            }
            user = new User(userLogin.trim(), name);
            user.setUserId(id);
            getHibernateTemplate().save(user);
            logger.info("User Login is :" + user.getUserLogin());
        }
        catch (HibernateException e) {
            throw handleException(e, "UserDAOImpl - createUserWithID");
        }
        return user;
    }

    public User createUser(String userLogin, String name) throws DaoException {
        User user;
        try {
            if (null == userLogin || "".equals(userLogin.trim())) {
                throw new DaoException(new Exception(), "Cannot create a user with an empty or missing login.");
            }
            user = new User(userLogin.trim(), name);
            getHibernateTemplate().save(user);
            logger.info("User Login is :" + user.getUserLogin());
        }
        catch (HibernateException e) {
            throw handleException(e, "UserDAOImpl - createUser");
        }
        return user;
    }

    /**
     * this method should be used very carefully. It overrides autogeneration of
     * object ID. Use  createUser(String userLogin, String name) instead
     *
     * @param id
     * @param userLogin
     * @param name
     * @return
     * @throws DaoException
     */
    public User createUserWithID(Long id, String userLogin, String name, String email) throws DaoException {
        User user;
        try {
            if (null == userLogin || "".equals(userLogin.trim())) {
                throw new DaoException(new Exception(), "Cannot create a user with an empty or missing login.");
            }
            user = new User(userLogin.trim(), name);
            user.setUserId(id);
            if (email != null)
                user.setEmail(email);

            getHibernateTemplate().save(user);
            logger.info("User Login is :" + user.getUserLogin());
        }
        catch (HibernateException e) {
            throw handleException(e, "UserDAOImpl - createUserWithID");
        }
        return user;
    }

    public User createUser(String userLogin, String name, String email) throws DaoException {
        User user;
        try {
            if (null == userLogin || "".equals(userLogin.trim())) {
                throw new DaoException(new Exception(), "Cannot create a user with an empty or missing login.");
            }
            user = new User(userLogin.trim(), name);
            if (email != null)
                user.setEmail(email);

            getHibernateTemplate().save(user);
            logger.info("User Login is :" + user.getUserLogin());
        }
        catch (HibernateException e) {
            throw handleException(e, "UserDAOImpl - createUser");
        }
        return user;
    }


    public List<User> findAll() throws DataAccessException, DaoException {
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(User.class);
            return (List<User>) getHibernateTemplate().findByCriteria(criteria);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "UserDAOImpl - findAll");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "UserDAOImpl - findAll");
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
    }

    @Override
    public void saveOrUpdateUser(User user) throws DataAccessException, DaoException {
        saveOrUpdateObject(user, "UserDAOImpl - saveOrUpdateUser");
    }

    /* used as a ping for DB health */
    public long countAllUsers() throws DataAccessException, DaoException {

        List<Long> counts = getHibernateTemplate().find("select count(*) from User ");
        return counts.get(0).longValue();
    }

    /**
     * The method searches the users
     *
     * @param startIndex
     * @param numRows
     * @param sortArgs
     * @return
     * @throws DataAccessException
     * @throws DaoException
     */
    public List<User> getPagedUsers(String searchString, int startIndex, int numRows, SortArgument[] sortArgs) throws DataAccessException, DaoException {
        try {
            StringBuffer orderByFieldsBuffer = new StringBuffer();
            if (sortArgs != null) {
                for (SortArgument sortArg : sortArgs) {
                    String dataSortField = sortArg.getSortArgumentName();
                    if (dataSortField == null || dataSortField.length() == 0) {
                        continue;
                    }
                    if (dataSortField.equals(UserDataVO.SORT_BY_USER_LOGIN)) {
                        dataSortField = UserDataVO.SORT_BY_USER_LOGIN;
                    }
                    else if (dataSortField.equals(UserDataVO.SORT_BY_USER_ID)) {
                        dataSortField = UserDataVO.SORT_BY_USER_ID;
                    }
                    else if (dataSortField.equals(UserDataVO.SORT_BY_FULLNAME)) {
                        dataSortField = UserDataVO.SORT_BY_FULLNAME;
                    }
                    else if (dataSortField.equals(UserDataVO.SORT_BY_EMAIL)) {
                        dataSortField = UserDataVO.SORT_BY_EMAIL;
                    }
                    else {
                        // unknown or unsupported sort field -> therefore set it to null
                        dataSortField = null;
                    }
                    if (dataSortField != null && dataSortField.length() != 0) {
                        if (sortArg.isAsc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField + " asc");
                        }
                        else if (sortArg.isDesc()) {
                            if (orderByFieldsBuffer.length() > 0) {
                                orderByFieldsBuffer.append(',');
                            }
                            orderByFieldsBuffer.append(dataSortField + " desc");
                        }
                    }
                }
            }

            String orderByClause = "";
            if (orderByFieldsBuffer.length() > 0) {
                orderByClause = "order by " + orderByFieldsBuffer.toString();
            }
            String hql =
                    "select cu " +
                            "from User cu ";
            if (searchString != null && searchString.length() > 0) {
                hql += "where lower(cu.userLogin) like lower(:searchString) ";
            }
            hql += orderByClause;

            Query query = getSession().createQuery(hql);
            if (searchString != null && searchString.length() > 0)
                query.setParameter("searchString", searchString);
            if (numRows > 0) {
                query.setFirstResult(startIndex);
                query.setMaxResults(numRows);
            }
            List<User> list = null;
            _logger.debug("User HQL: " + hql);
            list = query.list();
            List<User> userList = new ArrayList<User>();
            for (Iterator resItr = list.iterator(); resItr.hasNext();) {
                userList.add((User) resItr.next());
            }
            return userList;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "UserDAOImpl - getPagedUsers");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "UserDAOImpl - getPagedUsers");
        }
    }


    public int getNumUsers(String searchString) throws DataAccessException, DaoException {
        try {
            _logger.debug("executing getNumUsers");

            // Results
            int totalCount = 0;
            DetachedCriteria criteria = createUserQuery(searchString);
            criteria.setProjection(Projections.rowCount());
            List<Integer> list = (List<Integer>) getHibernateTemplate().findByCriteria(criteria);
            if (list != null && list.size() > 0) {
                totalCount = list.get(0).intValue();
                _logger.debug("Users count=" + totalCount);
            }
            else {
                _logger.debug("Found no users");
            }
            return totalCount;
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, "UserDAOImpl - getNumUsers");
        }
        catch (IllegalStateException e) {
            throw handleException(e, "UserDAOImpl - getNumUsers");
        }
    }

    private DetachedCriteria createUserQuery(String searchString) {
        DetachedCriteria criteria = DetachedCriteria.forClass(User.class);
        if (searchString != null && searchString.length() > 0)
            criteria.add(Expression.like("userLogin", searchString).ignoreCase());

        return criteria;
    }


}
