
package org.janelia.it.jacs.web.gwt.search.client.panel.protein;

import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class ProteinSummarySearchPanel extends CategorySummarySearchPanel {

    public ProteinSummarySearchPanel(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    public void populatePanel() {
        addSearchIcon();
        addLoadingMessage();
        getDataBuilder().getSearchResultCharts();
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new ProteinSearchDataBuilder(searchId, searchQuery);
    }

}
