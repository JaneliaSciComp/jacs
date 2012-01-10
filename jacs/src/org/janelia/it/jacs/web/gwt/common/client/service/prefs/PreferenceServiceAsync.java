
package org.janelia.it.jacs.web.gwt.common.client.service.prefs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;

public interface PreferenceServiceAsync {
    void getUserPreference(String category, String name, AsyncCallback async);

    void setUserPreference(UserPreference pref, AsyncCallback async);
}