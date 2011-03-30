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
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;


/**
 * @author Michael Press
 */
public class LoginProtectedMenuBarWithRightAlignedDropdowns extends MenuBarWithRightAlignedDropdowns {
    public static final String DEFAULT_MESSAGE = "You must be logged in to perform this function.";

    public LoginProtectedMenuBarWithRightAlignedDropdowns() {
        this(false);
    }

    public LoginProtectedMenuBarWithRightAlignedDropdowns(boolean vertical) {
        super(vertical);
        init(DEFAULT_MESSAGE);
    }

    public LoginProtectedMenuBarWithRightAlignedDropdowns(boolean vertical, String notLoggedInMessage) {
        super(vertical);
        init(notLoggedInMessage);
    }

    private void init(String msg) {
        if (!ClientSecurityUtils.isAuthenticated()) {
            MenuItem item = new MenuItem(msg, new Command() {
                public void execute() { /* do nothing */ }
            });
            item.addStyleName("gwt-MenuItem-NotLoggedIn");
            super.addItem(item);
        }
    }

    public void addItem(MenuItem item) {
        if (ClientSecurityUtils.isAuthenticated())
            super.addItem(item);
    }

    public MenuItem addItem(String text, boolean asHTML, Command cmd) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, asHTML, cmd);
        else
            return null;
    }

    public MenuItem addItem(String text, boolean asHTML, MenuBar popup) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, asHTML, popup);
        else
            return null;
    }

    public MenuItem addItem(String text, Command cmd) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, cmd);
        else
            return null;
    }

    public MenuItem addItem(String text, MenuBar popup) {
        if (ClientSecurityUtils.isAuthenticated())
            return super.addItem(text, popup);
        else
            return null;
    }
}
