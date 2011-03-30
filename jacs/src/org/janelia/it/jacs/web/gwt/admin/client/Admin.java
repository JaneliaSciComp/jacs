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

package org.janelia.it.jacs.web.gwt.admin.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.admin.client.panels.DiskUsagePanel;
import org.janelia.it.jacs.web.gwt.admin.client.panels.UserBrowserPanel;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 18, 2006
 * Time: 3:18:54 PM
 */
public class Admin extends BaseEntryPoint {
    private RoundedTabPanel tabs;
    private Panel mainPanel;

    /**
     * Here is where the module associated with download, is 'activated'.
     */
    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.ADMIN_SECTION_LABEL), Constants.ROOT_PANEL_NAME);

        mainPanel = getContentPanel();
        RootPanel.get(Constants.ROOT_PANEL_NAME).add(mainPanel);
        show();
    }

    /**
     * Content : a large tabbed pane with footer information.
     *
     * @return the content.
     */
    private Panel getContentPanel() {
        VerticalPanel tools = new TitledPanel("Tools");

        // Add tab pane.
        tabs = new RoundedTabPanel();
        tabs.setStyleName("AdminTabs");

        UserBrowserPanel dataPanel = new UserBrowserPanel();
        DiskUsagePanel usagepanel = new DiskUsagePanel();
        tabs.add(dataPanel, "User Browser");
        tabs.add(usagepanel, "Disk Usage");

        if (0 < tabs.getWidgetCount()) {
            tabs.selectTab(0);
        }

        tools.add(tabs);
        return tools;
    }

    private void logError(String message) {
        Window.alert(message);
    }
}
