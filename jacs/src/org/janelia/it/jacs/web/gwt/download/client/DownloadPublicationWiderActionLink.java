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

package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.WiderActionLink;

import java.util.List;

/**
 * ActionLink that toggles between "wider" (primary state) and "narrower" (secondary state).  On a change
 * of width, also updates all the other DownloadPublicationWiderActionLinks for all publications to be in the new state.
 *
 * @author Michael Press
 */
public class DownloadPublicationWiderActionLink extends WiderActionLink {
    private List _titles;
    private List _actionLinks;

    /**
     * @param narrowWidth Width to set publication titles when in narrow mode
     * @param wideWidth   Width to set publication titles when in wide mode
     * @param titles      List of the publication title HTML Widgets
     * @param actionLinks all of the other DPWAL action links that must be synchronized when one changes state
     */
    public DownloadPublicationWiderActionLink(String narrowWidth, String wideWidth, List titles, List actionLinks) {
        super(narrowWidth, wideWidth);
        _titles = titles;
        _actionLinks = actionLinks;
    }

    /**
     * Override ToggleActionLink implementation to additionally toggle all the other wider action links
     */
    public void toggleToPrimaryState() // make wide
    {
        toggleToPrimaryState(true);
    }

    /**
     * Override ToggleActionLink implementation to additionally toggle all the other wider action links
     */
    public void toggleToSecondaryState() // make narrow
    {
        toggleToSecondaryState(true);
    }

    /**
     * Toggles this action link to primary state, and if cascade==true toggles all other action links too
     */
    public void toggleToPrimaryState(boolean cascade) {
        super.toggleToPrimaryState();
        if (cascade) {
            reSizeAll(getWideWidth());
            toggleAllToPrimaryState();
        }
    }

    /**
     * Toggles this action link to secondary state, and if cascade==true toggles all other action links too
     */
    public void toggleToSecondaryState(boolean cascade) {
        super.toggleToSecondaryState();
        if (cascade) {
            reSizeAll(getNarrowWidth());
            toggleAllToSecondaryState();
        }
    }

    /**
     * Resize all of the publication titles to the new width
     */
    private void reSizeAll(String width) {
        for (int i = 0; i < _titles.size(); i++)
            ((Widget) _titles.get(i)).setWidth(width);
    }

    /**
     * Toggles all action links to primary state
     */
    private void toggleAllToPrimaryState() {
        for (int i = 0; i < _actionLinks.size(); i++)
            ((DownloadPublicationWiderActionLink) _actionLinks.get(i)).toggleToPrimaryState(false);
    }

    /**
     * Toggles all action links to primary state
     */
    private void toggleAllToSecondaryState() {
        for (int i = 0; i < _actionLinks.size(); i++)
            ((DownloadPublicationWiderActionLink) _actionLinks.get(i)).toggleToSecondaryState(false);
    }
}
