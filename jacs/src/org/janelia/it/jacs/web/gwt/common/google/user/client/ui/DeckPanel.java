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
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * (mpress: stock GWT 1.2 com.google.gwt.user.client.ui.DeckPanel - extended to allow subclasses to access visibleWidget)
 * <p/>
 * A panel that displays all of its child widgets in a 'deck', where only one
 * can be visible at a time. It is used by
 * {@link com.google.gwt.user.client.ui.TabPanel}.
 */
public class DeckPanel extends ComplexPanel implements IndexedPanel {

    private Widget visibleWidget;

    /**
     * Creates an empty deck panel.
     */
    public DeckPanel() {
        setElement(DOM.createDiv());
    }

    /**
     * Adds the specified widget to the deck.
     *
     * @param w the widget to be added
     */
    public void add(Widget w) {
        insert(w, getWidgetCount());
    }

    /**
     * Gets the index of the currently-visible widget.
     *
     * @return the visible widget's index
     */
    public int getVisibleWidget() {
        return getWidgetIndex(visibleWidget);
    }

    public Widget getWidget(int index) {
        return getChildren().get(index);
    }

    public int getWidgetCount() {
        return getChildren().size();
    }

    public int getWidgetIndex(Widget child) {
        return getChildren().indexOf(child);
    }

    /**
     * Inserts a widget before the specified index.
     *
     * @param w           the widget to be inserted
     * @param beforeIndex the index before which it will be inserted
     * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
     *                                   range
     */
    public void insert(Widget w, int beforeIndex) {
        if ((beforeIndex < 0) || (beforeIndex > getWidgetCount()))
            throw new IndexOutOfBoundsException();

        super.insert(w, getElement(), beforeIndex, false);

        Element child = w.getElement();
        DOM.setStyleAttribute(child, "width", "100%");
        DOM.setStyleAttribute(child, "height", "100%");
        w.setVisible(false);
    }

    public boolean remove(int index) {
        return remove(getWidget(index));
    }

    public boolean remove(Widget w) {
        if (!super.remove(w))
            return false;

        if (visibleWidget == w)
            visibleWidget = null;

        return true;
    }

    /**
     * Shows the widget at the specified index. This causes the currently- visible
     * widget to be hidden.
     *
     * @param index the index of the widget to be shown
     */
    public void showWidget(int index) {
        checkIndex(index);

        if (visibleWidget != null) {
            visibleWidget.setVisible(false);
        }
        visibleWidget = getWidget(index);
        visibleWidget.setVisible(true);
    }

    /**
     * MAP changed to protected
     */
    protected void checkIndex(int index) {
        if ((index < 0) || (index >= getWidgetCount()))
            throw new IndexOutOfBoundsException();
    }

    /**
     * MAP added
     */
    protected Widget getVisibleWidgetAsWidget() {
        return visibleWidget;
    }

    /**
     * MAP added
     */
    protected void setVisibleWidget(Widget widget) {
        visibleWidget = widget;
    }
}
