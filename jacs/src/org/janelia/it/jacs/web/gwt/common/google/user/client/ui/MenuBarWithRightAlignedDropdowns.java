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
