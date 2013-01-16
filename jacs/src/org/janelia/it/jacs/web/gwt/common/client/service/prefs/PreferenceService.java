
package org.janelia.it.jacs.web.gwt.common.client.service.prefs;

import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;

import com.google.gwt.user.client.rpc.RemoteService;

public interface PreferenceService extends RemoteService {
    public SubjectPreference getSubjectPreference(String name, String category);

    public void setSubjectPreference(SubjectPreference pref);
}