
package org.janelia.it.jacs.web.gwt.header.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.SystemPageHeader;

/**
 * Thin wrapper to add just the header to a page
 */
public class Header extends BaseEntryPoint {

    public void onModuleLoad() {
        RootPanel.get(Constants.LOADING_PANEL_NAME).setVisible(false);
        RootPanel.get(Constants.HEADER_PANEL_NAME).add(SystemPageHeader.getHeader()); // insert header
        RootPanel.get("page_wrapper").setVisible(true); // show content
        RootPanel.get("footer").setVisible(true); // show footer
    }
}