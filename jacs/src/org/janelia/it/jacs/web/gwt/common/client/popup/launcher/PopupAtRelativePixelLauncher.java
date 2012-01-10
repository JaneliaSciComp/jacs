
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Launches a popup at a specified pixel location relative to the top a specified widget.
 *
 * @author Michael Press
 */
public class PopupAtRelativePixelLauncher extends PopupLauncher {
    private int _topAjust = 0;
    private int _leftAjust = 0;

    /**
     * For deferred assignment of popup
     */
    public PopupAtRelativePixelLauncher() {
        super();
    }

    public PopupAtRelativePixelLauncher(int topAdjustment, int leftAdjustment) {
        super();
        _topAjust = topAdjustment;
        _leftAjust = leftAdjustment;
    }

    public PopupAtRelativePixelLauncher(int topAdjustment, int leftAdjustment, int msDelay) {
        super(null, msDelay);
        _topAjust = topAdjustment;
        _leftAjust = leftAdjustment;
    }

    public PopupAtRelativePixelLauncher(PopupPanel popup, int topAdjustment, int leftAdjustment) {
        super(popup);
        _topAjust = topAdjustment;
        _leftAjust = leftAdjustment;
    }


    protected int getPopupTopPosition(UIObject sender) {
        return sender.getAbsoluteTop() + _topAjust;
    }

    protected int getPopupLeftPosition(UIObject sender) {
        return sender.getAbsoluteLeft() + _leftAjust;
    }
}
