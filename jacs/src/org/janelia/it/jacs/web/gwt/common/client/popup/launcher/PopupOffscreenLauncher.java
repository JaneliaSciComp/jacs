
package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Launches a popup offscreen (why? you may ask... sometimes IE needs a DOM action to render something correctly)
 *
 * @author Michael Press
 */
public class PopupOffscreenLauncher extends PopupLauncher {
    public PopupOffscreenLauncher(PopupPanel popup) {
        super(popup);
    }

    protected int getPopupTopPosition(UIObject sender) {
        return 3000;
    }

    protected int getPopupLeftPosition(UIObject sender) {
        return 0;
    }
}

