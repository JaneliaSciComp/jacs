
package org.janelia.it.jacs.web.gwt.search.client.panel.website;

import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class WebsiteSearchDataBuilderBasedOnGoogle extends CategorySearchDataBuilder {
    private static final String DATA_PANEL_TITLE = "All Website Matches";

    public WebsiteSearchDataBuilderBasedOnGoogle(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_WEBSITE;
    }

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }

}
