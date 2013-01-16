
package org.janelia.it.jacs.web.gwt.common.server.prefs;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
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

    public SubjectPreference getSubjectPreference(String name, String category) {
        return JacsSecurityUtils.getSessionUser(getThreadLocalRequest()).getPreference(category, name);
    }

    public void setSubjectPreference(SubjectPreference pref) {
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