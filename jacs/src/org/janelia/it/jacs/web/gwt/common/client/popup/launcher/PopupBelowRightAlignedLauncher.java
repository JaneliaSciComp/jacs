
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * @author Michael Press
 */
public class PopupBelowRightAlignedLauncher extends PopupBelowLauncher {
    /**
     * For deferred assignment of popup
     */
    public PopupBelowRightAlignedLauncher() {
        super();
    }

    public PopupBelowRightAlignedLauncher(PopupPanel popup) {
        super(popup);
    }

    /**
     * Override normal left position to right-align it to the sender
     */
    protected int getPopupLeftPosition(UIObject sender) {
        return sender.getAbsoluteLeft() + sender.getOffsetWidth() - getPopup().getOffsetWidth() + 5;
    }
}