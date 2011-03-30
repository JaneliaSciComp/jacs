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

package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.RoundedDialogBox;

/**
 * @author Michael Press
 */
abstract public class BasePopupPanel extends RoundedDialogBox {
    protected VerticalPanel _panel = null;
    protected String _title = null;
    private boolean _isDraggable = true;
    private boolean _showCloseButton = true;
    private Widget _closeIcon;
    private ButtonSet _buttonSet;

    private boolean _showing = false;
    private boolean _realized = false;

    private MouseListener _mouseListener;
    private PositionCallback _positionCallback;

    private static final String BORDER_COLOR = "#333333"; //TODO: get this dynamically from CSS

    protected BasePopupPanel() {
        this(null);
    }

    /**
     * Performs all required initialization (meaning all Widgets are realized and any service invocations
     * by subclass will be invoked at instantiation time.  To perform lazy-initialization, construct with
     * BasePopupPanel(title, false) and call realize() when desired.
     */
    public BasePopupPanel(String title) {
        this(title, /*realize now*/ true);
    }

    /**
     * Performs no initialization.  Caller required to invoke realize() method when initialization is to be performed.
     */
    public BasePopupPanel(String title, boolean realizeNow) {
        this(title, realizeNow, /*autohide*/ true);
    }

    /**
     * Allows autohide as well as the usual parameters.
     */
    public BasePopupPanel(String title, boolean realizeNow, boolean autohide) {
        this(title, realizeNow, autohide, /*modal*/ false);
    }

    public BasePopupPanel(String title, boolean realizeNow, boolean autohide, boolean modal) {
        super(autohide, modal);
        _title = title;
        setStyleName("popupOuterPanel");
        setCornerStyleName("popupOuterPanelRounding");

        if (realizeNow)
            realize();
    }

    /**
     * Initialization - instantiates all widgets, and may result in service calls by subclasses to populate content.
     */
    public void realize() {
        if (!isRealized()) {
            if (_panel == null) // Add our panel to the popup (it only accepts 1 panel)
                createPanel();

            // Add title and sublclass's content
//          setText(title); // draggable title area
            if (_title != null)
                _panel.add(createTitlePanel(_title));

            populateContent(); // Get subclass content

            // Add any _buttonSet specified by the subclass
            _buttonSet = createButtons();
            if (_buttonSet != null && _buttonSet.getWidgetCount() > 0) {
                SimplePanel buttonPanel = new SimplePanel();
                buttonPanel.setStyleName("popupButtonPanel");
                buttonPanel.add(_buttonSet);
                _panel.add(buttonPanel);
            }

            setRealized(true);
        }
    }

    public boolean isRealized() {
        return _realized;
    }

    public void setRealized(boolean realized) {
        _realized = realized;
    }

    public void setPopupTitle(String newTitle) {
        if (null == newTitle) return;
        setRealized(false);
        _title = newTitle;
        realize();
    }

    public void add(Widget w) {
        if (_panel == null) {
            createPanel();
        }
        _panel.add(w);
    }

    public void createPanel() {
        _panel = new VerticalPanel();
        super.add(_panel);
    }

    protected Panel createTitlePanel(String title) {
        HorizontalPanel titlePanel = new HorizontalPanel();
        titlePanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        titlePanel.setStyleName("popupTitlePanel");
        titlePanel.setWidth("100%");

        setText(title); // sets the title text on the superclass's caption
        if (isDraggable()) {
            titlePanel.add(getDragImage());
            titlePanel.add(HtmlUtils.getHtml("&nbsp;", "text"));
        }
        titlePanel.add(getCaption()); // add the superclass's draggable caption to our title area
        titlePanel.setCellWidth(getCaption(), "100%");

        _closeIcon = getCloseIcon();
        _closeIcon.setVisible(showCloseButton());
        titlePanel.add(_closeIcon);

        return titlePanel;
    }

    private Widget getDragImage() {
        Image dragImage = ImageBundleFactory.getControlImageBundle().getDraggableImage().createImage();
        dragImage.setStyleName(".draggableImage");

        return dragImage;
    }

    protected Widget getCloseIcon() {
        Image closeButton = ImageBundleFactory.getControlImageBundle().getCloseImage().createImage();
        closeButton.setStyleName("closeButton");
        closeButton.addMouseListener(new HoverImageSetter(closeButton,
                ImageBundleFactory.getControlImageBundle().getCloseImage(),
                ImageBundleFactory.getControlImageBundle().getCloseHoverImage(),
                new ClickListener() {
                    public void onClick(Widget sender) {
                        hide();
                    }
                }));

        return closeButton;
    }

    //TODO: alow subclasses to add buttons to panel
    /**
     * Allows subclasses to add a ButtonSet to the popup
     */
    protected ButtonSet createButtons() {
        return null;
    }

    public void show() {
        super.show();
        _showing = true;
    }

    public void hide() {
        super.hide();
        _showing = false;
    }

    public boolean isShowing() {
        return _showing;
    }

    protected String getBorderColor() {
        return BORDER_COLOR;
    }

    public boolean isDraggable() {
        return _isDraggable;
    }

    /**
     * Has to be set before popup is realized
     */
    public void setDraggable(boolean draggable) {
        _isDraggable = draggable;
    }

    public boolean showCloseButton() {
        return _showCloseButton;
    }

    public void setShowCloseButton(boolean showCloseButton) {
        _showCloseButton = showCloseButton;
        if (_closeIcon != null)
            _closeIcon.setVisible(_showCloseButton);
    }

    /**
     * Support notification of Mouse events
     */
    public void addMouseListener(MouseListener mouseListener) {
        _mouseListener = mouseListener;
    }

    /**
     * Notifies MouseListener of MouseEnter or MouseLeave events.
     */
    public void onBrowserEvent(Event event) {
        if (_mouseListener != null) {
            if (DOM.eventGetType(event) == Event.ONMOUSEOVER) _mouseListener.onMouseEnter(_panel);
            else if (DOM.eventGetType(event) == Event.ONMOUSEOUT) _mouseListener.onMouseLeave(_panel);
        }
        super.onBrowserEvent(event);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void addPositionListener(PositionCallback callback) {
        _positionCallback = callback;
    }

    /**
     * Overrides setPopupPosition to notify the positionCallback of position change events
     */
    public void setPopupPosition(int left, int top) {
        super.setPopupPosition(left, top);    //To change body of overridden methods use File | Settings | File Templates.
        if (_positionCallback != null)
            _positionCallback.setPosition(top, left);
    }

    public ButtonSet getButtonSet() {
        return _buttonSet;
    }

    /**
     * For subclasses to supply dialog content
     */
    abstract protected void populateContent();
}
