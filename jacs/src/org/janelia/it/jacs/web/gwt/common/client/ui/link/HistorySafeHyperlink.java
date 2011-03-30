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

package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.Hyperlink;

/**
 * This is an extension of the GWT Hyperlink that doesn't cause history events to be stored/fired if the
 * targetHistoryToken is null, which it is by default, so use of hyperlinks doesn't add to the browser's
 * history stack unless targetHistoryToken is explicitly set.
 *
 * @author Michael Press
 */
public class HistorySafeHyperlink extends Hyperlink {
    /**
     * Replaces the Hyperlink implementation with an identical one except that the targetHistoryToken
     * is not added to the history stack if null.
     */
    @Override
    public void onBrowserEvent(Event event) {
        if (DOM.eventGetType(event) == Event.ONCLICK) {
            if (clickListeners != null) {
                clickListeners.fireClick(this);
            }
            if (getTargetHistoryToken() != null) {  // new restriction
                History.newItem(targetHistoryToken);
            }
            DOM.eventPreventDefault(event);
        }
    }

    /**
     * Sets the history token referenced by this hyperlink. This is the history
     * token that will be passed to {@link History#newItem} when this link is
     * clicked.  Ignored if null.
     *
     * @param targetHistoryToken the new target history token
     */
    @Override
    public void setTargetHistoryToken(String targetHistoryToken) {
        this.targetHistoryToken = targetHistoryToken;
        if (targetHistoryToken != null) // new restriction
            DOM.setElementProperty(anchorElem, "href", "#" + targetHistoryToken);
    }
}
