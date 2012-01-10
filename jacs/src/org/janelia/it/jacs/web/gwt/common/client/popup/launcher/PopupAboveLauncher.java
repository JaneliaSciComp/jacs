
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Launches a popup above the clicked/hovered widget, unlike its parent which launches the popup below.
 *
 * @author Michael Press
 */
public class PopupAboveLauncher extends PopupLauncher {
    /**
     * For deferred assignment of popup
     */
    public PopupAboveLauncher() {
        super();
    }

    public PopupAboveLauncher(PopupPanel popup) {
        super(popup);
    }

    public PopupAboveLauncher(PopupPanel popup, int msDelay) {
        super(popup, msDelay);
    }

    protected int getPopupTopPosition(UIObject sender) {
        return sender.getAbsoluteTop() - getPopup().getOffsetHeight();
    }
}
