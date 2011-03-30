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

package org.janelia.it.jacs.web.gwt.prokAnnot.client.panel;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 16, 2009
 * Time: 4:35:38 PM
 */
public class SybaseInfoPanel extends HorizontalPanel {
    private TextBox _usernameTextBox;
    private TextBox _sybasePasswordTextBox;

    public SybaseInfoPanel() {
        super();
        _usernameTextBox = new TextBox();
        _usernameTextBox.setVisibleLength(15);
        _sybasePasswordTextBox = new TextBox();
        _sybasePasswordTextBox.setVisibleLength(15);

        UserPreference namePref = Preferences.getUserPreference("sbLogin", "ProkPipeline");
        if (null != namePref) {
            _usernameTextBox.setText(namePref.getValue());
        }
        UserPreference passPref = Preferences.getUserPreference("sbPass", "ProkPipeline");
        if (null != passPref) {
            _sybasePasswordTextBox.setText(passPref.getValue());
        }

        this.add(HtmlUtils.getHtml("Username :", "nowrapprompt"));
        this.add(_usernameTextBox);
        this.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "nowrapprompt"));
        this.add(HtmlUtils.getHtml("Sybase Password :", "nowrapprompt"));
        this.add(_sybasePasswordTextBox);
    }

    public String getUsername() {
        return _usernameTextBox.getText();
    }

    public String getSybasePassword() {
        return _sybasePasswordTextBox.getText();
    }
}
