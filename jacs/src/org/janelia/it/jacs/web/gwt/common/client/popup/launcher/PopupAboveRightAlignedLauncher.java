
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.UIObject;

/**
 * Launches a popup at a specified pixel location relative to a widget.
 *
 * @author Michael Press
 */
public class PopupAboveRightAlignedLauncher extends PopupAboveLauncher {
    /**
     * For deferred assignment of popup
     */
    public PopupAboveRightAlignedLauncher() {
        super();
    }

    /**
     * Override normal left position to right-align it to the sender
     */
    protected int getPopupLeftPosition(UIObject sender) {
        return sender.getAbsoluteLeft() + sender.getOffsetWidth() - getPopup().getOffsetWidth() + 5;
    }
}
