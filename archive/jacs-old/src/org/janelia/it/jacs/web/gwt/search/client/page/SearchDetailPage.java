
package org.janelia.it.jacs.web.gwt.search.client.page;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HasActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.URLUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.search.client.Search;
import org.janelia.it.jacs.web.gwt.search.client.SearchEntityListener;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchResultsPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchResultsPanelFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:42:00 AM
 */
public class SearchDetailPage extends SearchWizardPage {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.page.SearchDetailPage");
    private VerticalPanel _mainPanel;
    private SearchResultsPanelFactory _resultsPanelFactory;
    private SearchResultsPanel _resultsPanel;
    private HasActionLink _backLink;
    private SearchEntityListener _entityListener;

    public SearchDetailPage(SearchResultsData data, WizardController controller) {
        super(data, controller, false);
        init();
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    /**
     * The returned token is typically used for history
     *
     * @return the page token
     */
    public String getPageToken() {
        String pageToken = "SearchDetailPageToken";
        if (getData().getSearchId() != null) {
            pageToken = URLUtils.addParameter(pageToken, Search.SEARCH_ID_PARAM, getData().getSearchId());
        }
        if (getData().getSelectedCategory() != null) {
            pageToken = URLUtils.addParameter(pageToken, Search.SEARCH_CATEGORY_PARAM, getData().getSelectedCategory());
        }
        return pageToken;
    }

    /**
     * @return the page title used during the page rendering process
     */
    public String getPageTitle() {
        return "Category Details";
    }

    protected void initializeResultsPanel() {
        // recreate the panel every time
        String category = getData().getSelectedCategory();
        _mainPanel.clear();
        _resultsPanel = _resultsPanelFactory.createSearchSummaryPanel(category, getData().getSearchId());
        _resultsPanel.setStyleName("CategorySearchDetail");
        _resultsPanel.setEntityListener(_entityListener);
        _resultsPanel.addActionLink(_backLink);
        _mainPanel.add(_resultsPanel);
        _resultsPanel.populatePanel(getData());
    }

    protected void mainPanelCreated() {
        // recreate the panel every time
        String category = getData().getSelectedCategory();
        String searchId = getData().getSearchId();
        if (_resultsPanel == null ||
                !_resultsPanel.getSearchId().equals(searchId) ||
                !_resultsPanel.getCategory().equals(category)) {
            // initialize the panel if it is a different search or different category
            initializeResultsPanel();
        }
    }

    private void init() {
        _mainPanel = new VerticalPanel();
        _mainPanel.setStyleName("SearchDetail");
        _resultsPanelFactory = new SearchResultsPanelFactory();
        // Create an EntityListener that'll go to the entity detail page if an entity is selected somewhere on the page
        _entityListener = new SearchEntityListener() {
            public void onEntitySelected(String entityId, String category, Object entityData) {
                logger.debug("Accession " + entityId + " + selected, going to next page");
                getData().setEntityDetailAcc(entityId);
                getController().next();
            }

            public void onEntitySelected(String entityId, Object entityData) {
            }
        };
        // Create a back link to the main panel
        _backLink = new BackActionLink("back to all search results", new ClickListener() {
            public void onClick(Widget widget) {
                getController().back();
            }
        });
    }

}
