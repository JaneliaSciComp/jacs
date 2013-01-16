
package org.janelia.it.jacs.web.gwt.common.client.service.prefs;

import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface PreferenceServiceAsync {
    void getSubjectPreference(String category, String name, AsyncCallback async);

    void setSubjectPreference(SubjectPreference pref, AsyncCallback async);
}