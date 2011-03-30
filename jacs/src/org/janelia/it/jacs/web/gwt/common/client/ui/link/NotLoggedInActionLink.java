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

package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.NotLoggedInPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;

/**
 * This looks like a regular action link but pops up a "You must be logged in to view this data" dialog box when clicked.
 * This link can be used in place of a normal action link to login-protected data when the user is not logged in.
 *
 * @author Michael Press
 */
public class NotLoggedInActionLink extends ActionLink {
    public NotLoggedInActionLink(String linkText, Image image, String popupMessage) {
        super(linkText);
        setShowBrackets(false);
        setImage(image);
        init(popupMessage);
    }

    private void init(final String popupMessage) {
        addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                new PopupAboveLauncher(new NotLoggedInPopupPanel(popupMessage)).showPopup(widget);
            }
        });
        setLinkStyleName("notLoggedInActionLink");
    }
}