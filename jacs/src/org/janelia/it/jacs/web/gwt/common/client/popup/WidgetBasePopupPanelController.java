
package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SourcesMouseEvents;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;

/**
 * Specialization of WidgetPopupController that controls a BasePopupPanel (our standard rounded-corner popup) instead
 * of a generic GWT PopupPanel.
 *
 * @author Michael Press
 */
public class WidgetBasePopupPanelController extends WidgetPopupController {
    public WidgetBasePopupPanelController(SourcesMouseEvents widget, BasePopupPanel popup) {
        super(widget, popup);
    }

    public WidgetBasePopupPanelController(SourcesMouseEvents widget, BasePopupPanel popup, int msDelay, PopupLauncher launcher) {
        super(widget, popup, msDelay);
        setLauncher(launcher);
    }

    /**
     * The superclasses' wraps the popup with a new PopupPanel in order to get mouse enter and leave events;  in order
     * to support drag, we need to delegate setting the new popup position on drag to the outer popup.
     */
    protected void createObservablePopup(PopupPanel innerPopup) {
        super.createObservablePopup(innerPopup);

        BasePopupPanel popup = (BasePopupPanel) _popup;
        popup.addPositionListener(new PopupPanel.PositionCallback() {
            public void setPosition(int top, int left) {
                _popupWrapper.setPopupPosition(left, top);
            }
        });

        // Don't show the close button since the popup disappears on mouseout anyway
        popup.setShowCloseButton(false);
    }

    /**
     * Make sure the popup is realized before showing (this supports not creating the popup until a request
     * to view it has been received).
     */
    protected void showPopup(Widget sender) {
        ((BasePopupPanel) _popup).realize();
        super.showPopup(sender);
    }
}