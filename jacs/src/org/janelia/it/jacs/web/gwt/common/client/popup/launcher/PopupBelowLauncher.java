
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Launches a popup below the sending widget.
 *
 * @author Michael Press
 */
public class PopupBelowLauncher extends PopupLauncher {
    /**
     * For deferred assignment of popup
     */
    public PopupBelowLauncher() {
        super();
    }

    public PopupBelowLauncher(PopupPanel popup) {
        super(popup);
    }

    public PopupBelowLauncher(PopupPanel popup, int msDelay) {
        super(popup, msDelay);
    }

    /**
     * Set the top of the popup at the bottom of the sender widget
     */
    protected int getPopupTopPosition(UIObject sender) {
        return sender.getAbsoluteTop() + sender.getOffsetHeight();
    }
}