
package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 11:25:09 AM
 *
 */
public class UserDAOImplTest extends JacswebTestCase {
    private UserDAO userDAO;

    public UserDAOImplTest() {
        super(UserDAOImplTest.class.getName());
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void testUserDAO() {
        assertNotNull(userDAO);
    }

    public void testGetUserByName() {
        User user=null;
        try {
            user=userDAO.getUserByName("smurphy");
            assertNotNull(user);
            assertEquals(user.getUserLogin(), "smurphy");
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

    public void testCountAllUsers() {
        try {
            assertTrue("Number of users less then 0", (userDAO.countAllUsers() >= 0));
        } catch (Exception ex) {
            String message="Exception: "+ex.getMessage();
            logger.warn(message);
            fail(message);
        }
    }

}
