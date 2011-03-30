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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;

/**
 * Toggles styles for a widget when the mouse hovers (enters/exists). Also supports toggling styles on a
 * RoundedPanel2 that decorates the widget.  Supports use as an EventListener or MouseListener
 * (not sure why HTML widget sometimes works with one but not the other, and sometimes the opposite)
 * <p/>
 * If given a ClickListener, this class will invoke ClickListener.onClick() when the widget is clicked.  If given
 * a HoverListener, this class will invoke HoverListener.onHover() when the mouse enters the item and
 * HoverListener.afterHover() when the mouse leaves.
 *
 * @author Michael Press
 *         <p/>
 *         //TODO: can get widget from RoundedPanel?
 */
public class HoverStyleSetter implements EventListener, MouseListener {
    RoundedPanel2 _roundedPanel = null;
    Widget _widget = null;
    String _styleName = null;
    String _hoverStyleName = null;
    String _cornerStyleName = null;
    String _hoverCornerStyleName = null;
    ClickListener _clickListener = null;
    HoverListener _hoverListener = null;

    public HoverStyleSetter(Widget widget, String styleName, String hoverStyleName, HoverListener hoverListener) {
        //TODO: throw IllegalArgumentException if anything's null
        _widget = widget;
        _styleName = styleName;
        _hoverStyleName = hoverStyleName;
        _hoverListener = hoverListener;
    }

    public HoverStyleSetter(Widget widget, String styleName, String hoverStyleName, ClickListener clickListener) {
        //TODO: throw IllegalArgumentException if anything's null (except clickListener)
        _widget = widget;
        _styleName = styleName;
        _hoverStyleName = hoverStyleName;
        _clickListener = clickListener;
    }

    public HoverStyleSetter(Widget widget, String styleName, String hoverStyleName,
                            RoundedPanel2 roundedPanel, String cornerStyleName, String hoverCornerStyleName,
                            ClickListener clickListener) {
        this(widget, styleName, hoverStyleName, clickListener);
        _roundedPanel = roundedPanel;
        _cornerStyleName = cornerStyleName;
        _hoverCornerStyleName = hoverCornerStyleName;
    }

    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONMOUSEOVER)
            onMouseEnter(_widget);
        else if (DOM.eventGetType(event) == Event.ONMOUSEOUT)
            onMouseLeave(_widget);
        else if (DOM.eventGetType(event) == Event.ONMOUSEUP)
            onMouseLeave(_widget);
        else if (DOM.eventGetType(event) == Event.ONMOUSEUP)
            onMouseUp(_widget, -1, -1);
        else if (DOM.eventGetType(event) == Event.ONMOUSEDOWN)
            onMouseDown(_widget, -1, -1);
    }

    private boolean hasRoundedPanel() {
        return _roundedPanel != null && _cornerStyleName != null && _hoverCornerStyleName != null;
    }

    public void onMouseEnter(Widget widget) {
        widget.setStyleName(_hoverStyleName);
        if (hasRoundedPanel())
            _roundedPanel.setCornerStyleName(_hoverCornerStyleName);
        if (_hoverListener != null)
            _hoverListener.onHover(widget);
    }

    public void onMouseLeave(Widget sender) {
        _widget.setStyleName(_styleName);
        if (hasRoundedPanel())
            _roundedPanel.setCornerStyleName(_cornerStyleName);
        if (_hoverListener != null)
            _hoverListener.afterHover(sender);
    }

    public void onMouseUp(Widget sender, int x, int y) {
    }

    public void onMouseDown(Widget sender, int x, int y) {
        onMouseLeave(sender);       // restore style to normal
        if (_clickListener != null) // process click event
            _clickListener.onClick(_widget);
    }

    public void onMouseMove(Widget sender, int x, int y) {
    }

    public void setClickListener(ClickListener clickListener) {
        this._clickListener = clickListener;
    }

    public ClickListener getClickListener() {
        return _clickListener;
    }
}
