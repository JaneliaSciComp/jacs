
package org.janelia.it.jacs.web.gwt.search.client.panel.project;

import com.google.gwt.user.client.ui.HorizontalPanel;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchResultTablePanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchResultsPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.StandaloneSearchResultsIconPanelFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class ProjectSearchResultsPanel extends SearchResultsPanel {
    public ProjectSearchResultsPanel(String title, String searchId, String category) {
        super(title, searchId, category);
    }

    public void populatePanel(SearchResultsData searchResult) {
        createTopPanel(searchResult);
        add(getPanelSpacer());
        createResultsPanel(searchResult);
    }

    private void createTopPanel(SearchResultsData searchResult) {
        // create a top panel that has the search icon
        HorizontalPanel topPanel = new HorizontalPanel();
        add(topPanel);
        StandaloneSearchResultsIconPanelFactory iconFactory = new StandaloneSearchResultsIconPanelFactory();
        SearchIconPanel iconPanel = iconFactory.createSearchIconPanel(getCategory(), true);

        // Show the category count.  If its not set (because search hasn't been loaded yet), use the supplied prior hit count.
        Integer count;
        if (searchResult.getSearchHitCountByTopic() != null)
            count = (Integer) searchResult.getSearchHitCountByTopic().get(getCategory());
        else
            count = searchResult.getPriorCategoryHits();

        iconPanel.setNumMatches(count);
        topPanel.add(iconPanel);
        //topPanel.setCellWidth(iconPanel,iconPanel.getIconWidth());
    }

    private void createResultsPanel(SearchResultsData searchResult) {
        // create a search results panel
        CategorySearchResultTablePanel projectResultsPanel =
                new CategorySearchResultTablePanel(searchResult.getSelectedCategory(),
                        searchResult.getSearchId(),
                        searchResult.getSearchString());
        projectResultsPanel.setEntityListener(getEntityListener());
        add(projectResultsPanel);
        projectResultsPanel.populatePanel();
    }

}
