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
package org.janelia.it.jacs.web.gwt.common.google.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;

/**
 * (This is the stock version of the GWT 1.0.21 com.google.gwt.user.client.ui.DialogBox with minor modifications
 * to support rounding the dialog boxes. All changes are marked "MAP changed or MAP added". --Michael Press)
 * <p/>
 * NOTE: This class is only extended by BasePopupPanel
 * <p/>
 * <p/>
 * A form of popup that has a caption area at the top and can be dragged by the
 * user.
 * <p/>
 * <p/>
 * <img class='gallery' src='DialogBox.png'/>
 * </p>
 * <p/>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DialogBox { the outside of the dialog }</li>
 * <li>.gwt-DialogBox .Caption { the caption }</li>
 * </ul>
 * <p/>
 * <h3>Example</h3>
 */
abstract public class RoundedDialogBox extends PopupPanel implements HasHTML, MouseListener {

    private HTML caption = new HTML();
    private boolean dragging;
    private int dragStartX, dragStartY;
    private DockPanel panel = new DockPanel();
    private RoundedPanel2 _roundedPanel = new RoundedPanel2(panel, RoundedPanel2.ALL, getBorderColor());

    /**
     * Creates an empty dialog box. It should not be shown until its child widget
     * has been added using {@link #add}.
     */
    public RoundedDialogBox() {
        this(/*autohide*/ true);
    }

    public RoundedDialogBox(boolean autohide) {
        this(autohide, /*modal*/ false);
    }

    public RoundedDialogBox(boolean autohide, boolean modal) {
        if (null == panel) {
            panel = new DockPanel();
        }
        if (null == _roundedPanel) {
            _roundedPanel = new RoundedPanel2(panel, RoundedPanel2.ALL, getBorderColor());
        }
        /* <begin MAP changed> */
        //panel.add(caption, DockPanel.NORTH); // subclass will add as desired
        // Round the outer dialog box.  Have to wrap in another panel or else width gets set to 100%
        //TODO: find border color from style dynamically
        DockPanel wrapper = new DockPanel();
        wrapper.add(_roundedPanel, DockPanel.CENTER);
        super.setWidget(wrapper);
        /* <end MAP changed> */

        panel.setStyleName("gwt-DialogBox");
        caption.setStyleName("Caption");
        caption.addMouseListener(this);
        /* <end MAP changed> */
        setAutoHideEnabled(autohide);
        setModal(modal);
    }

    public void add(Widget w) {
        panel.add(w, DockPanel.CENTER);
    }

    public String getHTML() {
        return caption.getHTML();
    }

    public String getText() {
        return caption.getText();
    }

    public void onMouseDown(Widget sender, int x, int y) {
        dragging = true;
        DOM.setCapture(caption.getElement());
        dragStartX = x;
        dragStartY = y;
    }

    public void onMouseEnter(Widget sender) {
    }

    public void onMouseLeave(Widget sender) {
    }

    public void onMouseMove(Widget sender, int x, int y) {
        if (dragging) {
            int absX = x + getAbsoluteLeft();
            int absY = y + getAbsoluteTop();
            setPopupPosition(absX - dragStartX, absY - dragStartY);
        }
    }

    public void onMouseUp(Widget sender, int x, int y) {
        dragging = false;
        DOM.releaseCapture(caption.getElement());
    }

    public boolean remove(Widget w) {
        if (null != w)
            return false;

        panel.remove(w);
        return true;
    }

    public void setHTML(String html) {
        caption.setHTML(html);
    }

    public void setText(String text) {
        caption.setText(text);
    }

    /**
     * MAP changed - override to set style name on rounded panel
     */
    public void setStyleName(String style) {
        if (null == panel) {
            panel = new DockPanel();
        }
        panel.setStyleName(style);
    }

    public void setCornerStyleName(String style) {
        if (null == _roundedPanel) {
            _roundedPanel = new RoundedPanel2(panel, RoundedPanel2.ALL, getBorderColor());
        }
        _roundedPanel.setCornerStyleName(style);
    }

    /**
     * Subclass access to caption, which must be added to the panel for the title to be visible
     */
    protected Widget getCaption() {
        return caption;
    }

    protected String getBorderColor() {
        return "cornflowerblue";  // hack until border color can be retrieved dynamically from CSS
    }
/* <end MAP added> */
}
