
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Launches a popup at a specified pixel location
 *
 * @author Michael Press
 */
public class PopupAtAbsolutePixelLauncher extends PopupLauncher {
    private int _top;
    private int _left;

    /**
     * For deferred assignment of popup
     */
    public PopupAtAbsolutePixelLauncher() {
        super();
    }

    public PopupAtAbsolutePixelLauncher(PopupPanel popup, int top, int left) {
        super(popup);
        _top = top;
        _left = left;
    }

    protected int getPopupTopPosition(UIObject sender) {
        return _top;
    }

    protected int getPopupLeftPosition(UIObject sender) {
        return _left;
    }
}