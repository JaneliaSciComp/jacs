
package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Simple class that will close (hide) a popup.
 *
 * @author Michael Press
 */
public class PopupCloser implements ClickListener {
    private BasePopupPanel _popup;

    public PopupCloser(BasePopupPanel popup) {
        _popup = popup;
    }

    public void onClick(Widget sender) {
        _popup.hide();
    }
}
