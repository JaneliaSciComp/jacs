
package org.janelia.it.jacs.web.gwt.common.client.security;

import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;

/**
 * @author Michael Press
 */
public class ClientSecurityUtils {
    /**
     * Client-side (GWT) method to determine if the user is logged in.  Requires that Preferences.jsp is
     * included in the entry point's JSP.
     *
     * @return false if the login status cannot be determined (such as if Preference.jsp has not been
     *         included), true if the user is logged in, false if user is not logged in.
     */
    public static boolean isAuthenticated() {
        UserPreference userPreference = Preferences.getUserPreference("isAuthenticated", "security", /* default */ "false");
        return userPreference.getValue().equals("true");
    }

    public static boolean isAdmin() {
        UserPreference userPreference = Preferences.getUserPreference("isAdmin", "security", /* default */ "false");
        return userPreference.getValue().equals("true");
    }
}
