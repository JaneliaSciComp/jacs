
package org.janelia.it.jacs.web.gwt.common.client.service.prefs;

import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * @author Michael Press
 */
public class Preferences {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences");

    private static PreferenceServiceAsync _preferenceService = (PreferenceServiceAsync) GWT.create(PreferenceService.class);

    static {
        ((ServiceDefTarget) _preferenceService).setServiceEntryPoint("preference.srv");
    }

    /**
     * Synchronous retrieval of a preference - requires that this EntryPoint's jsp has properly included
     * Preferences.jsp (which retrieves the preferences before GWT is invoked).  This method DOES NOT GUARANTEE
     * THAT A VALID UserPreference object is returned - if no preference is found, null is returned.
     */
    public static SubjectPreference getSubjectPreference(String name, String category) {
        // Find the JavaScript array that contains the preferences (put there by the underlying JSP) and extract the
        // requested preference
        Dictionary prefs;
        String value = null;
        try {
            prefs = Dictionary.getDictionary(category);
            _logger.debug("Found preferences for category " + category);

            if (prefs != null)
                value = prefs.get(name);
        }
        catch (Exception e) { // Throws exception if no dictionary found
            _logger.info("No preferences found for category " + category + " for this user.");
        }

        // Extract the preference (if found) from the dictionary, else return null
        return (value == null) ? null : new SubjectPreference(name, category, value);
    }

    /**
     * Synchronous retrieval of a preference - requires that this EntryPoint's jsp has properly included
     * Preferences.jsp (which retrieves the preferences before GWT is invoked).  This method guarantees that a valid
     * UserPreference object is returned - if no preference is found, a new one is created using the default value.
     */
    public static SubjectPreference getSubjectPreference(String name, String category, String defaultValue) {
        SubjectPreference pref = getSubjectPreference(name, category);
        return (pref != null) ? pref : new SubjectPreference(name, category, defaultValue);
    }

    /**
     * Asynchronous retrieval of a UserPreference.  This method guarantees that a valid UserPreference is returned - if
     * none is found on the server, a UserPreference with the default value is returned.
     */
    public static void getSubjectPreference(final String prefName, final String category, final String defaultValue, final PreferenceRetrievedCallback callback) {
        _preferenceService.getSubjectPreference(prefName, category, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error("---------------Error retrieving preference " + category + "/" + prefName, throwable);
                callback.onPreferenceRetrieved(new SubjectPreference(prefName, category, defaultValue));
            }

            public void onSuccess(Object object) {
                SubjectPreference pref = (SubjectPreference) object;
                if (pref == null) {
                    if (_logger.isDebugEnabled())
                        _logger.debug("---------------Null preference retrieved, using default value for preference " + category + "/" + prefName + "=" + defaultValue);
                    pref = new SubjectPreference(prefName, category, defaultValue);
                }
                else if (_logger.isDebugEnabled())
                    _logger.debug("---------------Retrieved preference " + prefName + "/" + category + "=" + pref.getValue());

                callback.onPreferenceRetrieved(pref);
            }
        });
    }

    /*
     * Asynchronously sets a UserPreference in the database.
     */
    public static void setSubjectPreference(final SubjectPreference pref) {
        _preferenceService.setSubjectPreference(pref, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                _logger.error("---------------Failed to set preference " + pref.getName() + "/" + pref.getCategory() + ":", throwable);
            }

            public void onSuccess(Object object) {
                if (_logger.isDebugEnabled())
                    _logger.debug("---------------Successfully set preference " + pref.getName() + "/" + pref.getCategory() + "=" + pref.getValue());
            }
        });
    }
}
