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

package org.janelia.it.jacs.web.gwt.common.client.ui.menu;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SourcesMouseEvents;
import com.google.gwt.user.client.ui.UIObject;
import org.janelia.it.jacs.web.gwt.common.client.popup.WidgetPopupController;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;

/**
 * A controller to manage the auto-open and auto-close interaction between a menu bar's top-level menu item
 * (like "File") and the dropdown menu that shows on hover.  The controller shows the dropdown menu on hover,
 * and closes it on mouse out of the top-level menu item, UNLESS the mouse moved into the dropdown menu.
 *
 * @author Michael Press
 */
public class SimpleMenuBarController extends WidgetPopupController {
    /**
     * @param widget top level menu item (e.g. HTML, Label, Image)
     * @param popup  the popup to control
     */
    public SimpleMenuBarController(SourcesMouseEvents widget, PopupPanel popup) {
        super(widget, popup);
        popup.sinkEvents(Event.ONCLICK); // make sure click events on menu items are handled
    }

    public PopupLauncher getLauncher() {
        if (_launcher == null)
            setLauncher(new PopupBelowLauncher(getPopup(), getDelay()) {
                protected int getPopupLeftPosition(UIObject sender) {
                    return sender.getAbsoluteLeft();
                }
            });
        return _launcher;
    }

    /**
     * Notification that a menu item has been selected. Closes the dropdown menu.
     */
    public void onMenuItemSelected() {
        if (getTimer() != null)
            getTimer().cancel();
        getPopup().hide();
    }
}
