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
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;

/**
 * Represents "area" html element
 */
public class ImageMapArea extends Widget implements SourcesClickEvents, SourcesMouseEvents {

    private static String[] shapePossibleValues = new String[]{
            "circ",
            "circle",
            "poly",
            "polygon",
            "rect",
            "rectangle"
    };

    private ClickListenerCollection clickListeners;
    private MouseListenerCollection mouseListeners;

    private String targetHistoryToken;


    private ImageMapArea() {
        setElement(DOM.createElement("area"));
        sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);
    }

    ImageMapArea(String shape, String coords) {
        this();
        setShape(shape);
        setCoords(coords);
    }

    ImageMapArea(String shape, String coords, String targetHistoryToken) {
        this(shape, coords);
        setTargetHistoryToken(targetHistoryToken);
    }

    public String getShape() {
        return DOM.getElementAttribute(this.getElement(), "shape");
    }

    public String getCoords() {
        return DOM.getElementAttribute(this.getElement(), "coords");
    }

    public void addClickListener(ClickListener listener) {
        if (clickListeners == null) {
            clickListeners = new ClickListenerCollection();
        }
        clickListeners.add(listener);
    }

    public void removeClickListener(ClickListener listener) {
        if (clickListeners != null) {
            clickListeners.remove(listener);
        }
    }

    public void addMouseListener(MouseListener listener) {
        if (mouseListeners == null) {
            mouseListeners = new MouseListenerCollection();
        }
        mouseListeners.add(listener);
    }

    public void removeMouseListener(MouseListener listener) {
        if (mouseListeners != null) {
            mouseListeners.remove(listener);
        }
    }

    /**
     * Gets the history token referenced by this widget.
     *
     * @return the target history token
     * @see #setTargetHistoryToken
     */
    public String getTargetHistoryToken() {
        return targetHistoryToken;
    }

    /**
     * Sets the history token referenced by this widget. This is the history
     * token that will be passed to {@link History#newItem} when this link is
     * clicked.
     *
     * @param targetHistoryToken the new target history token
     */
    public void setTargetHistoryToken(String targetHistoryToken) {
        this.targetHistoryToken = targetHistoryToken;
        DOM.setElementAttribute(this.getElement(), "href", "#" + targetHistoryToken);
    }

    public void onBrowserEvent(Event event) {
        switch (DOM.eventGetType(event)) {
            case Event.ONCLICK: {
                fireClick();
                break;
            }
            case Event.ONMOUSEDOWN:
            case Event.ONMOUSEUP:
            case Event.ONMOUSEMOVE:
            case Event.ONMOUSEOVER:
            case Event.ONMOUSEOUT: {
                if (mouseListeners != null) {
                    mouseListeners.fireMouseEvent(this, event);
                }
                break;
            }
            case Event.ONMOUSEWHEEL:
                break;
            case Event.ONLOAD: {
                break;
            }
            case Event.ONERROR: {
                break;
            }
        }
    }

    // ---------------------------------------------------------------------- Non-public methods

    /**
     * Non-javadoc
     */
    private void fireClick() {
        if (clickListeners != null) {
            clickListeners.fireClick(this);
        }
        if (targetHistoryToken != null) {
            History.newItem(targetHistoryToken);
        }
    }

    protected void setShape(String shape) {
        if (shape == null) {
            throw new IllegalArgumentException(
                    "Parameter 'shape' cannot be a null");
        }

        boolean match = false;
        for (String shapePossibleValue : shapePossibleValues) {
            if (shape.equalsIgnoreCase(shapePossibleValue)) {
                match = true;
                break;
            }
        }
        if (!match) {
            throw new IllegalArgumentException(
                    "Illegal value of 'shape' parameter");
        }

        DOM.setElementAttribute(this.getElement(), "shape", shape);
    }

    protected void setCoords(String coords) {
        if (coords == null) {
            throw new IllegalArgumentException(
                    "Parameter 'coords' cannot be a null");
        }

        DOM.setElementAttribute(this.getElement(), "coords", coords);
    }

}
