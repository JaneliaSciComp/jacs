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

import com.google.gwt.user.client.ui.PopupPanel;

/**
 * Extends google's MenuBar, sets the drowdown/popup to be right-aligned with the parent menu item instead
 * of the standard left-aligned.  Useful for menus appearing on the right edge of the window, where
 * part of the regular menu would appear offscreen.
 *
 * @author Michael Press
 */
public class MenuBarWithRightAlignedDropdowns extends MenuBar {
    public MenuBarWithRightAlignedDropdowns() {
    }

    public MenuBarWithRightAlignedDropdowns(boolean vertical) {
        super(vertical);
    }

    /**
     * Override normal position and set right-aligned with the parent menu item
     *
     * @param vertical a normal vertical menu has vertical==false for some strange reason
     */
    protected void setPopupPosition(PopupPanel popup, MenuItem item, boolean vertical) {
        if (vertical)
            popup.setPopupPosition(item.getAbsoluteLeft() + item.getOffsetWidth(),
                    item.getAbsoluteTop());
        else
            popup.setPopupPosition(item.getAbsoluteLeft() + item.getOffsetWidth() - popup.getOffsetWidth(),
                    item.getAbsoluteTop() + item.getOffsetHeight());
    }
}
