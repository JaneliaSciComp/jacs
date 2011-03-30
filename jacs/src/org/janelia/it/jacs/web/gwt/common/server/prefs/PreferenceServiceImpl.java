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

package org.janelia.it.jacs.web.gwt.common.server.prefs;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.server.access.PreferenceDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.PreferenceService;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.security.JacsSecurityUtils;

/**
 * GWT Service to retrieve user preferences
 *
 * @author Michael Press
 */
public class PreferenceServiceImpl extends JcviGWTSpringController implements PreferenceService {
    static Logger _logger = Logger.getLogger(PreferenceServiceImpl.class.getName());

    private PreferenceDAO _preferenceDAO;

    public PreferenceServiceImpl() {
    }

    public PreferenceDAO getPreferenceDAO() {
        return _preferenceDAO;
    }

    public void setPreferenceDAO(PreferenceDAO preferenceDAO) {
        _preferenceDAO = preferenceDAO;
    }

    public UserPreference getUserPreference(String name, String category) {
        return JacsSecurityUtils.getSessionUser(getThreadLocalRequest()).getPreference(category, name);
    }

    public void setUserPreference(UserPreference pref) {
        // Update User cache and push to database
        User user = JacsSecurityUtils.getSessionUser(getThreadLocalRequest());
        try {
            synchronized (user) {
                user.setPreference(pref);
                _preferenceDAO.storePreferencesForUser(user.getUserLogin(), pref);
            }

            if (_logger.isInfoEnabled())
                _logger.info("Successfully set user preference " + pref.getCategory() + " / " + pref.getName() + " = " + pref.getValue());
        }
        catch (DaoException e) {
            //TODO: rollback update to cache if db update fails?
            _logger.error("Error saving preferences for user " + user.getUserLogin());
        }
    }
}