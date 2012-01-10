
package org.janelia.it.jacs.web.gwt.search.client.panel.website;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchResultTablePanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchResultsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class WebsiteSearchResultsPanel extends SearchResultsPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.WebsiteSearchResultsPanel");

    public WebsiteSearchResultsPanel(String title, String searchId, String category) {
        super(title, searchId, category);
    }

    public void populatePanel(SearchResultsData searchResult) {
        createResultsPanel(searchResult);
    }

    private void createResultsPanel(SearchResultsData searchResult) {
        // create a search results panel
        CategorySearchResultTablePanel webSearchResultsPanel =
                new CategorySearchResultTablePanel(searchResult.getSelectedCategory(),
                        searchResult.getSearchId(),
                        searchResult.getSearchString());
        webSearchResultsPanel.setEntityListener(getEntityListener());
        add(webSearchResultsPanel);
        webSearchResultsPanel.populatePanel();
    }

}
