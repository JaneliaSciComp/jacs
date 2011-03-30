/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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