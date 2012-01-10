
package org.janelia.it.jacs.web.gwt.detail.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.gwtwidgets.client.util.WindowUtils;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * This class builds the Detail Base Entry page.
 *
 * @author Tareq Nabeel
 */
public class DetailPage extends BaseEntryPoint {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.DetailPage");
    DetailPanel detailPanel;
    String acc;
    String currentURL;

    public void onModuleLoad() {
        try {
            // Create and fade in the page contents
            logger.debug("DetailPage.onModuleLoad()");
            clear();
            setBreadcrumb(new Breadcrumb("Detail"), Constants.ROOT_PANEL_NAME);
            acc = WindowUtils.getLocation().getParameter(DetailPanel.ACC_PARAM);
            // Create the main panel and display the loading label
            detailPanel = new DetailPanel(this);
            RootPanel.get(Constants.ROOT_PANEL_NAME).add(detailPanel);
            show();

            // Determine the initial project (via URL param)
            currentURL = WindowUtils.getLocation().getPath() + WindowUtils.getLocation().getQueryString();
            logger.debug("DetailPage using acc parameter=" + acc);
            detailPanel.rebuildPanel(acc, currentURL);
        }
        catch (RuntimeException e) {
            // Log the exception so we know where "null is null or not an object" JavaScript error is coming from
            logger.error("DetailPage onModuleLoad() caught exception", e);
            throw e;
        }
    }

}
