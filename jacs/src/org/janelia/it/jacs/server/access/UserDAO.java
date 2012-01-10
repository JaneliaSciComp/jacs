
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Aug 10, 2006
 * Time: 1:29:06 PM
 */
public interface UserDAO extends DAO {
    User createUser(String userLogin, String name) throws DaoException;

    User createUserWithID(Long id, String userLogin, String name) throws DaoException;

    User createUser(String userLogin, String name, String email) throws DaoException;

    User createUserWithID(Long id, String userLogin, String name, String email) throws DaoException;

    User getUserByName(String requestingUser) throws DaoException;

    List<User> findAll() throws DataAccessException, DaoException;

    void saveOrUpdateUser(User user) throws DataAccessException, DaoException;

    /* used as a ping for DB health */
    long countAllUsers() throws DataAccessException, DaoException;
}
