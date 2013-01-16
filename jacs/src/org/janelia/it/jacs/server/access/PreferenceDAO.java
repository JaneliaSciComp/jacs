
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

/**
 * @author Michael Press
 */
public interface PreferenceDAO extends DAO {
    public void storePreferencesForUser(String userLogin, SubjectPreference pref) throws DaoException;
}