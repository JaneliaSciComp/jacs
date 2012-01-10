
package org.janelia.it.jacs.web.gwt.search.client.page;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.BackActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.URLUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
import org.janelia.it.jacs.web.gwt.search.client.Search;
import org.janelia.it.jacs.web.gwt.search.client.model.AccessionResult;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchEntityDetailPanelBuilder;

import java.util.List;

/**
 * @author Michael Press
 */
public class SearchEntityDetailPage extends SearchWizardPage {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.page.SearchEntityDetailsPage");

    public static final String HISTORY_TOKEN = "SearchEntityDetailsPage";
    private Panel _detailPanel;
    private LoadingLabel _loadingLabel;

    public SearchEntityDetailPage(SearchResultsData data, WizardController controller) {
        super(data, controller);
        init();
    }

    public SearchEntityDetailPage(SearchResultsData data, WizardController controller, boolean showButtons) {
        super(data, controller, showButtons);
        init();
    }

    private void init() {
        _loadingLabel = new LoadingLabel("loading data...", /*visible*/ true);
        _detailPanel = new VerticalPanel();
    }

    /**
     * @return a specific page token to be used for browser history
     */
    public String getPageToken() {
        String pageToken = HISTORY_TOKEN;
        if (getData().getSearchId() != null) {
            pageToken = URLUtils.addParameter(pageToken, Search.SEARCH_ID_PARAM, getData().getSearchId());
        }
        if (getData().getSelectedCategory() != null) {
            pageToken = URLUtils.addParameter(pageToken, Search.SEARCH_CATEGORY_PARAM, getData().getSelectedCategory());
        }
        if (getData().getEntityDetailAcc() != null) {
            pageToken = URLUtils.addParameter(pageToken, DetailPanel.ACC_PARAM, getData().getEntityDetailAcc());
        }
        return pageToken;
    }

    public Widget getMainPanel() {
        return _detailPanel;
    }

    public String getPageTitle() {
        return "Search Details";
    }

    public boolean checkPageToken(String pageName) {
        return pageName.startsWith(HISTORY_TOKEN);
    }

    protected void preProcess(Integer priorPageNumber) {
        _logger.debug("SearchEntityDetailsPage.preProcess()");
        _detailPanel.add(_loadingLabel);
        _loadingLabel.setText("loading data..."); // necessary when we retrieve an accession before display
        _loadingLabel.setVisible(true);

        // If we're starting on this page with an accession result, we need to retrieve the entity id
        if (isAccessionStartingPoint())
            retrieveAccessionResult();
        else
            buildPanel();
    }

    /**
     * Detects that the search entry point is directly starting on this page with an accession result rather than a
     * normal search and click on the search results.
     */
    private boolean isAccessionStartingPoint() {
        return getData().getPriorSearchId() != null &&
                SearchTask.TOPIC_ACCESSION.equals(getData().getSelectedCategory());
    }

    private void retrieveAccessionResult() {
        _searchService.getPagedCategoryResults(getData().getPriorSearchId(), SearchTask.TOPIC_ACCESSION, 0, 1, new SortArgument[0],
                new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        _loadingLabel.setText("An error occurred loading the accession.");
                    }

                    public void onSuccess(Object object) {
                        List resultList = (List) object;
                        if (resultList != null && resultList.size() > 0) {
                            AccessionResult result = (AccessionResult) resultList.get(0);
                            getData().setEntityDetailAcc(result.getAccession());
                            buildPanel();
                        }
                    }
                });
    }

    private void buildPanel() {
        SearchEntityDetailPanelBuilder builder = new SearchEntityDetailPanelBuilder((Search) getController());
        _detailPanel.clear();
        _detailPanel.add(builder.createEntityDetailPanel(getData().getEntityDetailAcc(), getData(), createBackLink()));
    }

    private BackActionLink createBackLink() {
        final String linkText;
        final int newPage;

        if (SearchTask.TOPIC_ACCESSION.equals(getData().getSelectedCategory())) {
            newPage = 0;
            linkText = "all search results";
        }
        else {
            newPage = getController().getCurrentPageIndex() - 1;
            linkText = "back to category details";
        }
        BackActionLink backLink = new BackActionLink(linkText, new ClickListener() {
            public void onClick(Widget widget) {
                getController().gotoPage(newPage);
            }
        });
        backLink.setTargetHistoryToken(getController().getPageTokenAt(newPage));

        return backLink;
    }
}