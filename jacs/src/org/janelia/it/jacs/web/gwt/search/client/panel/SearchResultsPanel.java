
package org.janelia.it.jacs.web.gwt.search.client.panel;

import com.google.gwt.user.client.ui.HTML;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.search.client.SearchEntityListener;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanelFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchResultsIconPanelFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SearchResultsPanel extends TitledBox {
    private String searchId;
    private String category;
    private CategorySummarySearchPanel currentSummaryPanel;
    protected SearchEntityListener _entityListener;

    protected SearchResultsPanel(String title, String searchId, String category) {
        super(title, true);
        this.searchId = searchId;
        this.category = category;
    }

    public String getSearchId() {
        return searchId;
    }

    public String getCategory() {
        return category;
    }

    public SearchEntityListener getEntityListener() {
        return _entityListener;
    }

    public void setEntityListener(SearchEntityListener entityListener) {
        _entityListener = entityListener;
    }

    public void populatePanel(SearchResultsData searchResult) {
        clear();
        createSummaryPanel(searchResult);
        add(getPanelSpacer());

        // populate the summary panel
        currentSummaryPanel.populatePanel();
        CategorySearchResultTablePanel resultTablePanel =
                createTableResults(searchResult);
        add(resultTablePanel);
        resultTablePanel.populatePanel();
    }

    protected HTML getPanelSpacer() {
        return HtmlUtils.getHtml("&nbsp;", "smallSpacer");
    }

    protected CategorySummarySearchPanel createSummaryPanel(SearchResultsData searchResult) {
        // create the summary panel
        String category = searchResult.getSelectedCategory();
        SearchSummaryPanelFactory summaryPanelFactory = new SearchSummaryPanelFactory();
        currentSummaryPanel = summaryPanelFactory.createSearchSummaryPanel(category,
                searchResult.getSearchId(),
                searchResult.getSearchString());
        // create the corresponding icon
        SearchIconPanelFactory searchIconFactory = new SearchResultsIconPanelFactory();
        SearchIconPanel iconPanel = searchIconFactory.createSearchIconPanel(category);

        // Show the category count.  If its not set (because search hasn't been loaded yet), use the supplied prior hit count.
        Integer count;
        if (searchResult.getSearchHitCountByTopic() != null)
            count = (Integer) searchResult.getSearchHitCountByTopic().get(getCategory());
        else
            count = searchResult.getPriorCategoryHits();

        iconPanel.setNumMatches(count);
        currentSummaryPanel.setCategoryIcon(iconPanel, false, null);
        add(currentSummaryPanel);
        return currentSummaryPanel;
    }

    protected CategorySearchResultTablePanel createTableResults(SearchResultsData searchResult) {
        CategorySearchResultTablePanel resultTablePanel =
                new CategorySearchResultTablePanel(searchResult.getSelectedCategory(),
                        searchResult.getSearchId(),
                        searchResult.getSearchString());
        resultTablePanel.setEntityListener(_entityListener);
        return resultTablePanel;
    }

}
