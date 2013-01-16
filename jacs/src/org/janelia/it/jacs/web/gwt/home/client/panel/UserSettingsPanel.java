
package org.janelia.it.jacs.web.gwt.home.client.panel;

import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

public class UserSettingsPanel extends TitledBox {
    private CheckBox userNotificationCheckBox;
    private SubjectPreference emailPref;

    public UserSettingsPanel() {
        super("Settings", true);

        setWidth("300px"); // min width when contents are hidden
        userNotificationCheckBox = new CheckBox("Send Email Notification of Job Completion", true);
        userNotificationCheckBox.setStyleName("text");
        emailPref = Preferences.getSubjectPreference(SubjectPreference.PREF_EMAIL_ON_JOB_COMPLETION,
                SubjectPreference.CAT_NOTIFICATION, "false");
        userNotificationCheckBox.setValue(Boolean.valueOf(emailPref.getValue()));
        userNotificationCheckBox.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                emailPref.setValue(Boolean.toString(userNotificationCheckBox.getValue()));
                Preferences.setSubjectPreference(emailPref);
            }
        });
        // Add all the links to the titlebox.
        add(userNotificationCheckBox);
    }

}