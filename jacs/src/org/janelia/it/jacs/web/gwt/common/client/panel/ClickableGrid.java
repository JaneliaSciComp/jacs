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

package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SourcesClickEvents;

public class ClickableGrid extends Grid implements SourcesClickEvents {
    private ClickListenerCollection listeners = new ClickListenerCollection();

    /**
     * Ensure getting events.
     */
    public ClickableGrid() {
        // Tells 'framework' to send events here, when they happen.
        sinkEvents(Event.ONCLICK);
    }

    public ClickableGrid(int rows, int cols) {
        super(rows, cols);
        sinkEvents(Event.ONCLICK);
    }

    /**
     * Dispatcher for events of interest.  Using this to source mouse events.
     *
     * @param event what to dispatch.
     */
    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONCLICK) {
            listeners.fireClick(this);
        }
    }

    /**
     * Registration method.  Add listener to hear about clicks.
     *
     * @param listener what will be listening.
     */
    public void addClickListener(ClickListener listener) {
        listeners.add(listener);
    }

    /**
     * De-registration method. Take listener out.
     *
     * @param listener what will no longer be hearing about clicks.
     */
    public void removeClickListener(ClickListener listener) {
        listeners.remove(listener);
    }

}
