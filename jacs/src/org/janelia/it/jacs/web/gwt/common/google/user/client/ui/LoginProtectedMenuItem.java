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

package org.janelia.it.jacs.web.gwt.common.google.user.client.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.NotLoggedInPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;

/**
 * Extends MenuItem but replaces the specified Command action with a NotLoggedInPopupPanel if the user
 * is not logged in.  Also sets the menu item style name to gwt-MenuItem-NotLoggedIn.
 *
 * @author Michael Press
 */
public class LoginProtectedMenuItem extends MenuItem {
    public LoginProtectedMenuItem(String text, boolean asHTML, Command cmd) {
        super(text, asHTML, cmd);
        init();
    }

    public LoginProtectedMenuItem(String text, boolean asHTML, MenuBar subMenu) {
        super(text, asHTML, subMenu);
        init();
    }

    public LoginProtectedMenuItem(String text, Command cmd) {
        super(text, cmd);
        init();
    }

    public LoginProtectedMenuItem(String text, MenuBar subMenu) {
        super(text, subMenu);
        init();
    }

    private void init() {
        setStyleName("gwt-MenuItem-NotLoggedIn");
    }

    public Command getCommand() {
        if (!ClientSecurityUtils.isAuthenticated()) {
            return new Command() {
                public void execute() {
                    Widget parent = getParentMenu().getParent();
                    new PopupAboveLauncher(new NotLoggedInPopupPanel("You must be logged in to export.")).showPopup(parent);
                }
            };
        }
        else return super.getCommand();
    }

    void setSelectionStyle(boolean selected) {
    }
}
