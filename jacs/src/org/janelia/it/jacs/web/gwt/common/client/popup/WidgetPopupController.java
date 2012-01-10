
package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SourcesMouseEvents;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;

/**
 * A generic controller that handles the interaction between any widget that shows a popup on hover, and the popup.
 * This controller shows the popup on hover over the widget, and closes the popup on mouse out, UNLESS the mouse
 * moves into the popup, in which case the popup is not closed. This allows the user to interact with the popup (such
 * as selecting text in the popup) instead of having the popup disappear when the mouse moves off the widget.
 *
 * @author Michael Press
 */
public class WidgetPopupController {
    protected static final int DEFAULT_DELAY = 0;

    private SourcesMouseEvents _widget;
    protected PopupPanel _popup; // the popup passed by the caller
    protected PopupPanel _popupWrapper; // wrapper around the popup passed by the caller
    private boolean _inPopup = false;
    private HidePopupTimer _timer;
    protected PopupLauncher _launcher;
    private int _msDelay = DEFAULT_DELAY;

    /**
     * @param widget any widget implementing SourcesMouseEvents (e.g. HTML, Label, Image)
     * @param popup  the popup to control
     */
    public WidgetPopupController(SourcesMouseEvents widget, PopupPanel popup) {
        this(widget, popup, DEFAULT_DELAY);
    }

    public WidgetPopupController(SourcesMouseEvents widget, PopupPanel popup, int msDelay) {
        _msDelay = msDelay;
        setWidget(widget);
        createObservablePopup(popup);
    }

    /**
     * We need to get mouseover and mouseout events from the popup;  since there's no listener we can add to PopupPanel,
     * we have to wrap the provided popup in a new PopupPanel in which we override onBrowserEvent to subscribe to the
     * events we want.
     */
    protected void createObservablePopup(PopupPanel innerPopup) {
        _popup = innerPopup;
        _popupWrapper = new PopupPanel() {
            public void onBrowserEvent(Event event) {
                if (DOM.eventGetType(event) == Event.ONMOUSEOVER)
                    onPopupEnter();
                else if (DOM.eventGetType(event) == Event.ONMOUSEOUT)
                    onPopupLeave();
                super.onBrowserEvent(event); // let the real popup also process the event
            }
        };
        _popupWrapper.sinkEvents(Event.MOUSEEVENTS);
        _popupWrapper.add(innerPopup);
    }

    /**
     * On hover, show the popup under the widget.
     */
    public void onWidgetEnter(Widget sender) {
        _inPopup = true;
        showPopup(sender);
    }

    protected void showPopup(Widget sender) {
        getLauncher().showPopup(sender);
    }

    /**
     * On un-hover, start a timer;  when the timer fires, the popup will be closed unless the mouse has moved over
     * the popup in the intervening time.
     */
    public void onWidgetLeave() {
        _inPopup = false;
        startTimer();
    }

    public void onPopupEnter() {
        _inPopup = true;
    }

    /**
     * On mouse-out of the popup, start a timer;  when the timer fires, the popup will be closed unless the mouse has
     * moved back into the popup or over the widget in the intervening time.
     */
    public void onPopupLeave() {
        _inPopup = false;
        startTimer();
    }

    private void startTimer() {
        if (_timer == null) {
            _timer = new HidePopupTimer();
            _timer.schedule(100);
        }
    }

    public PopupLauncher getLauncher() {
        if (_launcher == null)
            setLauncher(new PopupAboveLauncher(_popupWrapper, _msDelay));
        return _launcher;
    }

    private class HidePopupTimer extends Timer {
        public void run() {
            if (!_inPopup)
                _launcher.hidePopup();
            _inPopup = false;
            _timer = null;
        }
    }

    protected Timer getTimer() {
        return _timer;
    }

    protected PopupPanel getPopup() {
        return _popupWrapper;
    }

    private class WidgetMouseListener extends MouseListenerAdapter {
        public void onMouseEnter(Widget sender) {
            onWidgetEnter(sender);
        }

        public void onMouseLeave(Widget sender) {
            onWidgetLeave();
        }
    }

    public void setLauncher(PopupLauncher launcher) {
        _launcher = launcher;
        _launcher.setPopup(_popupWrapper);
    }

    public void setWidget(SourcesMouseEvents widget) {
        _widget = widget;
        _widget.addMouseListener(new WidgetMouseListener());
    }

    public SourcesMouseEvents getWidget() {
        return _widget;
    }

    public int getDelay() {
        return _msDelay;
    }
}
