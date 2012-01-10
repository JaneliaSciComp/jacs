
package org.janelia.it.jacs.web.gwt.common.client.service.prefs;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;

public interface PreferenceService extends RemoteService {
    public UserPreference getUserPreference(String name, String category);

    public void setUserPreference(UserPreference pref);
}