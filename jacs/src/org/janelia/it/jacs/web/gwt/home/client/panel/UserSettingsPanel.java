/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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