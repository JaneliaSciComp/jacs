
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;

/**
 * Launches a popup centered in the browser
 *
 * @author Michael Press
 */
public class PopupCenteredLauncher extends PopupLauncher {

    public PopupCenteredLauncher(PopupPanel popup) {
        super(popup);
    }

    public PopupCenteredLauncher(PopupPanel popup, int msDelay) {
        super(popup, msDelay);
    }

    /**
     * Have to override showPopup in order to implement the delay AFTER the popup is shown (offscreen) so the
     * browser has time to render it such that the height and width can be calculated.
     */
    public void showPopup(UIObject sender) {
        showPopupInternal(sender);
    }

    /**
     * Overides parent implementation to show the popup in the middle of the browser (after any delay)
     *
     * @param sender unused
     */
    protected void showPopupInternal(UIObject sender) {
        /* realize the popup */
        if (getPopup() instanceof BasePopupPanel)
            ((BasePopupPanel) getPopup()).realize();

        // For correct calculations, the thing needs to be visible somewhere, so move it off the screen initially
        // TODO There has to be a better way to do this.
        getPopup().setPopupPosition(0, 2000);
        getPopup().show();

        // NOW create any specified delay so that the browser has time to render the popup and calculate height/width
        if (getDelay() > 0)
            new TempTimer().schedule(getDelay());
        else
            new TempTimer().run(); // no delay
    }

    /**
     * Unused
     */
    protected int getPopupTopPosition(UIObject sender) {
        return 0;
    }

    /**
     * The second half of the showPopupInternal implementation - calculate the new position and show it there
     */
    protected void showPopupInternalFinish() {
        int left = (Window.getClientWidth() - getPopup().getOffsetWidth()) / 2;
        int top = (Window.getClientHeight() - getPopup().getOffsetHeight()) / 2;
        getPopup().setPopupPosition(left, top);
    }

    public class TempTimer extends Timer {
        public void run() {
            showPopupInternalFinish();
        }
    }
}
