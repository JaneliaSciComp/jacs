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
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;

/**
 * A widget that serves as an "internal" hyperlink. That is, it is a link to
 * another state of the running application. When clicked, it will create a new
 * history frame using {@link com.google.gwt.user.client.History#newItem}, but
 * without reloading the page.
 * <p/>
 * <p>
 * Being a true hyperlink, it is also possible for the user to "right-click,
 * open link in new window", which will cause the application to be loaded in a
 * new window at the state specified by the hyperlink.
 * </p>
 * <p/>
 * <p>
 * <img class='gallery' src='Hyperlink.png'/>
 * </p>
 * <p/>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Hyperlink { }</li>
 * </ul>
 * <p/>
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.HistoryExample}
 * </p>
 */
public class Hyperlink extends Widget implements HasHTML, SourcesClickEvents {

    protected Element anchorElem;
    protected ClickListenerCollection clickListeners;
    protected String targetHistoryToken;

    /**
     * Creates an empty hyperlink.
     */
    public Hyperlink() {
        setElement(DOM.createDiv());
        DOM.appendChild(getElement(), anchorElem = DOM.createAnchor());
        sinkEvents(Event.ONCLICK);
        setStyleName("gwt-Hyperlink");
    }

    /**
     * Creates a hyperlink with its text and target history token specified.
     *
     * @param text               the hyperlink's text
     * @param asHTML             <code>true</code> to treat the specified text as html
     * @param targetHistoryToken the history token to which it will link
     * @see #setTargetHistoryToken
     */
    public Hyperlink(String text, boolean asHTML, String targetHistoryToken) {
        this();
        if (asHTML) {
            setHTML(text);
        }
        else {
            setText(text);
        }
        setTargetHistoryToken(targetHistoryToken);
    }

    /**
     * Creates a hyperlink with its text and target history token specified.
     *
     * @param text               the hyperlink's text
     * @param targetHistoryToken the history token to which it will link
     */
    public Hyperlink(String text, String targetHistoryToken) {
        this();
        setText(text);
        setTargetHistoryToken(targetHistoryToken);
    }

    public void addClickListener(ClickListener listener) {
        if (clickListeners == null) {
            clickListeners = new ClickListenerCollection();
        }
        clickListeners.add(listener);
    }

    public String getHTML() {
        return DOM.getInnerHTML(anchorElem);
    }

    /**
     * Gets the history token referenced by this hyperlink.
     *
     * @return the target history token
     * @see #setTargetHistoryToken
     */
    public String getTargetHistoryToken() {
        return targetHistoryToken;
    }

    public String getText() {
        return DOM.getInnerText(anchorElem);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONCLICK) {
            if (clickListeners != null) {
                clickListeners.fireClick(this);
            }
            History.newItem(targetHistoryToken);
            DOM.eventPreventDefault(event);
        }
    }

    public void removeClickListener(ClickListener listener) {
        if (clickListeners != null) {
            clickListeners.remove(listener);
        }
    }

    public void setHTML(String html) {
        DOM.setInnerHTML(anchorElem, html);
    }

    /**
     * Sets the history token referenced by this hyperlink. This is the history
     * token that will be passed to {@link History#newItem} when this link is
     * clicked.
     *
     * @param targetHistoryToken the new target history token
     */
    public void setTargetHistoryToken(String targetHistoryToken) {
        this.targetHistoryToken = targetHistoryToken;
        DOM.setElementProperty(anchorElem, "href", "#" + targetHistoryToken);
    }

    public void setText(String text) {
        DOM.setInnerText(anchorElem, text);
    }

    /**
     * <b>Affected Elements:</b>
     * <ul>
     * <li>-wrapper = the div around the link.</li>
     * </ul>
     *
     * @see com.google.gwt.user.client.ui.UIObject#onEnsureDebugId(String)
     */
    @Override
    protected void onEnsureDebugId(String baseID) {
        ensureDebugId(anchorElem, "", baseID);
        ensureDebugId(getElement(), baseID, "wrapper");
    }
}
