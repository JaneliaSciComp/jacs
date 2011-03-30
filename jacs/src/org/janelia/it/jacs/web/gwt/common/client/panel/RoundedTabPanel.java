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

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.TabBar;

/**
 * @author Michael Press
 */
public class RoundedTabPanel extends org.janelia.it.jacs.web.gwt.common.google.user.client.ui.TabPanel {
    ActionLink[] _actionLink = null;
    private String _actionLinkPanelStyleName;

    public RoundedTabPanel() {
        super();
    }

    /**
     * Constructor takes an array of ActionLinks (one for each tab that will be added).  This is nasty, but the
     * panels can't share one action link, so the caller is responsible for keeping all the action links in sync
     * (state, label displayed, etc.).
     *
     * @param actionLink               array of ActionLinks (one per tab)
     * @param actionLinkPanelStyleName (style to use to locate the action link panel to the right of the tabs)
     */
    public RoundedTabPanel(ActionLink[] actionLink, String actionLinkPanelStyleName) {
        super();
        _actionLink = actionLink;
        _actionLinkPanelStyleName = actionLinkPanelStyleName;
    }

    protected TabBar createTabBar() {
        return new RoundedTabBar();
    }

    /**
     * Override  com.google.user.client.ui.TabPanel to wrap the tab contents in a rounded panel
     */
    public void insert(Widget widget, String tabText, boolean asHTML, int beforeIndex) {
        super.insert(createRoundedTabBody(widget, beforeIndex), tabText, asHTML, beforeIndex);
    }

    protected Panel createRoundedTabBody(Widget widget, int index) {
        // Panel sets background and left/right borders, contains the original widget
        VerticalPanel contentsPanel = new VerticalPanel();
        contentsPanel.setStyleName("roundedTabBody");
        contentsPanel.add(widget);

        HorizontalPanel actionLinkPanel = null;
        if (_actionLink != null) {
            actionLinkPanel = new HorizontalPanel();
            actionLinkPanel.setStyleName(_actionLinkPanelStyleName);
            actionLinkPanel.setCellVerticalAlignment(_actionLink[index], VerticalPanel.ALIGN_MIDDLE);
            actionLinkPanel.add(_actionLink[index]);
        }

        // The tab body is a RoundedPanel2 with contents panel in it
        RoundedPanel2 roundedPanel =
                new RoundedPanel2(contentsPanel, RoundedPanel2.ALL, "#AAAAAA", RoundedPanel2.ROUND_LARGE, actionLinkPanel);
        roundedPanel.setCornerStyleName("roundedTabBodyRounding");

        return roundedPanel;
    }
}
