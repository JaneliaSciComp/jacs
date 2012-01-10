
package org.janelia.it.jacs.web.gwt.home.client.panel;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;

public class UserSettingsPanel extends TitledBox {
    private CheckBox userNotificationCheckBox;
    private UserPreference emailPref;

    public UserSettingsPanel() {
        super("Settings", true);

        setWidth("300px"); // min width when contents are hidden
        userNotificationCheckBox = new CheckBox("Send Email Notification of Job Completion", true);
        userNotificationCheckBox.setStyleName("text");
        emailPref = Preferences.getUserPreference(UserPreference.PREF_EMAIL_ON_JOB_COMPLETION,
                UserPreference.CAT_NOTIFICATION, "false");
        userNotificationCheckBox.setValue(Boolean.valueOf(emailPref.getValue()));
        userNotificationCheckBox.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                emailPref.setValue(Boolean.toString(userNotificationCheckBox.getValue()));
                Preferences.setUserPreference(emailPref);
            }
        });
        // Add all the links to the titlebox.
        add(userNotificationCheckBox);
    }

}