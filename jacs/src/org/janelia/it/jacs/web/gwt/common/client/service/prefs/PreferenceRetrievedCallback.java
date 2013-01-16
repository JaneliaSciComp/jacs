
package org.janelia.it.jacs.web.gwt.common.client.service.prefs;

import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;

/**
 * @author Michael Press
 */
public interface PreferenceRetrievedCallback {
    public void onPreferenceRetrieved(SubjectPreference value);
}
