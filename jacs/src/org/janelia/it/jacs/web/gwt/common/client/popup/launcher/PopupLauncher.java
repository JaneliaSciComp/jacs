/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverListener;

/**
 * Base class for popup launchers.
 * <p/>
 * If added to an item as ClickListener, this will launch the popup when the item is clicked (the
 * popup is responsible for closing itself).
 * <p/>
 * If added to an item as a HoverListener, will launch the popup (after a delay if specified) when the mouse hovers
 * over the item, and close the popup when the mouse leaves the item.
 * <p/>
 * Subclasses can implement onBeforeShow() and/or onAfterShow() to construct/destruct info necesary for the popup.
 *
 * @author Michael Press
 */
abstract public class PopupLauncher implements ClickListener, HoverListener {
    private PopupPanel _popup = null;
    private ShowTimer _showTimer = null;
    private int _delay = 0;

    /**
     * For deferred assignment of popup
     */
    public PopupLauncher() {
    }

    public PopupLauncher(PopupPanel popup) {
        super();
        _popup = popup;
    }

    public PopupLauncher(PopupPanel popup, int msDelay) {
        super();
        _popup = popup;
        _delay = msDelay;
    }

    public void onClick(Widget sender) {
        showPopup(sender);
    }

    /**
     * Show the popup when notified of a hover;  delay the start if a delay was specified
     */
    public void onHover(final Widget widget) {
        showPopup(widget);
    }

    public void afterHover(Widget widget) {
        hidePopup();
    }

    /**
     * Shows the popup, after a delay if specified in the constructor.  Public so callers can show as desired.
     * Subclasses should implement showPopupImpl to actually show the popup.
     *
     * @param sender the widget that was clicked on;  the popup will show just below it
     */
    public void showPopup(UIObject sender) {
        _showTimer = new ShowTimer(sender);
        if (_delay > 0) {
            _showTimer.schedule(_delay);
        }
        else {
            _showTimer.run();
        }
    }

    /**
     * Overides parent implementation to show the popup above the calling widget
     */
    protected void showPopupInternal(UIObject sender) {
        if (_popup instanceof BasePopupPanel)
            ((BasePopupPanel) getPopup()).realize();

        // For correct calculations, set visiblity to false, but show() so the size of the popup is calculated
        getPopup().setVisible(false);
        getPopup().show();
        getPopup().setPopupPosition(getPopupLeftPosition(sender), getPopupTopPosition(sender));
        getPopup().setVisible(true);
    }

    abstract protected int getPopupTopPosition(UIObject sender);

    /**
     * Default implementation - even with the left side of the sender.
     */
    protected int getPopupLeftPosition(UIObject sender) {
        return sender.getAbsoluteLeft() - 5;
    }

    public void hidePopup() {
        if (_showTimer != null)
            _showTimer.cancel();

        _popup.hide();
    }

    /**
     * Timer class that calls showPopup() when the timer goes off
     */
    protected class ShowTimer extends Timer {
        UIObject _widget;

        public ShowTimer(UIObject widget) {
            _widget = widget;
        }

        public void run() {
            showPopupInternal(_widget);
        }
    }

    public PopupPanel getPopup() {
        return _popup;
    }

    public void setPopup(PopupPanel popup) {
        _popup = popup;
    }

    public int getDelay() {
        return _delay;
    }

    public void setDelay(int delay) {
        _delay = delay;
    }
}