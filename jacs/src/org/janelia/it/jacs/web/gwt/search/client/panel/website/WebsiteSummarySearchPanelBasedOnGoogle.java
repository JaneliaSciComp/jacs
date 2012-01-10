
package org.janelia.it.jacs.web.gwt.search.client.panel.website;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.googlesearch.client.GoogleSearchHandler;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class WebsiteSummarySearchPanelBasedOnGoogle extends CategorySummarySearchPanel {

    public WebsiteSummarySearchPanelBasedOnGoogle(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new WebsiteSearchDataBuilderBasedOnGoogle(searchId, searchQuery);
    }

    public void populatePanel() {
        DockPanel iconPanel = createSearchIconPanel();
        VerticalPanel googleBrandingPanel = new VerticalPanel();
        iconPanel.add(googleBrandingPanel, DockPanel.SOUTH);
        addItem(iconPanel);
        VerticalPanel googleResultsPanel = new VerticalPanel();
        googleResultsPanel.setStyleName("SiteSearchSummaryPanel");
        addItem(googleResultsPanel);
        GoogleSearchHandler googleSearch = new GoogleSearchHandler(googleResultsPanel);
        googleSearch.setBrandingPanel(googleBrandingPanel);
        googleSearch.addSearchableDomains("JaCS", Constants.VICSWEB_DOMAIN);
        googleSearch.executeSearch(
                getDataBuilder().getSearchId(),
                getDataBuilder().getSearchQuery(),
                GoogleSearchHandler.WEBSEARCH);
    }

}
