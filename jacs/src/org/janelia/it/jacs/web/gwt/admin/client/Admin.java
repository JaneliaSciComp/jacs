
package org.janelia.it.jacs.web.gwt.admin.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.admin.client.panels.DiskUsagePanel;
import org.janelia.it.jacs.web.gwt.admin.client.panels.EntityTypePanel;
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
        EntityTypePanel entityPanel = new EntityTypePanel();
        tabs.add(dataPanel, "User Browser");
        tabs.add(usagepanel, "Disk Usage");
        tabs.add(entityPanel, "Entity Types");

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
