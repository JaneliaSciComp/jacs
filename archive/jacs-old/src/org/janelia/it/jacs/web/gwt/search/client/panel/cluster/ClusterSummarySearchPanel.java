
package org.janelia.it.jacs.web.gwt.search.client.panel.cluster;

import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class ClusterSummarySearchPanel extends CategorySummarySearchPanel {

    public ClusterSummarySearchPanel(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new ClusterSearchDataBuilder(searchId, searchQuery);
    }

    public void populatePanel() {
        addSearchIcon();
        addLoadingMessage();
        getDataBuilder().getSearchResultCharts();
    }

}
