
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.server.access.PreferenceDAO;
import org.janelia.it.jacs.server.access.UserDAO;

/**
 * @author Michael Press
 */
public class PreferenceDAOImpl extends DaoBaseImpl implements PreferenceDAO {
    Logger logger = Logger.getLogger(this.getClass().getName());
    UserDAO userDao;

    // DAO's can only come from Spring's Hibernate
    private PreferenceDAOImpl() {
    }

    public void storePreferencesForUser(String userLogin, UserPreference pref) throws DaoException {
        User user = userDao.getUserByName(userLogin);
        UserPreference oldPref = user.getPreference(pref.getCategory(), pref.getName());
        // If prefs are equal, do nothing
        if (null == oldPref || !pref.equals(oldPref)) {
            logger.debug("Saving pref for " + userLogin + ": " + pref.getName());
            user.setPreference(pref);
            saveOrUpdateObject(user, "PreferenceDaoImpl - storePreferencesForUser");
        }
    }

    public UserDAO getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDAO userDao) {
        this.userDao = userDao;
    }
}