
package org.janelia.it.jacs.web.gwt.home.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.hibernate.search.Search;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.shared.tasks.SearchCategoryInfo;
import org.janelia.it.jacs.shared.tasks.SearchJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

import java.util.List;
import java.util.Map;

//import org.janelia.it.jacs.web.gwt.search.client.Search;
//import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconMouseManager;
//import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel;
//import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchTinyIconPanelFactory;

/**
 * @author Cristian Goina
 */
public class RecentSearchesPanel extends TitledBox {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.home.client.panel.RecentSearchesPanel");

    private static final int DEFAULT_NUM_ROWS = 5;

    private static StatusServiceAsync _statusService = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusService).setServiceEntryPoint("status.srv");
    }

    private LoadingLabel _loadingSearchInfoLabel;

    public RecentSearchesPanel() {
        super("Recent Searches");
        setStyleName("RecentSearchesTitleBox");
    }

    protected void popuplateContentPanel() {
        createLoadingLabel();
        DeferredCommand.addCommand(new GetSearchInfo());
    }

    private void createLoadingLabel() {
        _loadingSearchInfoLabel = new LoadingLabel(true);
        add(_loadingSearchInfoLabel);
    }

    public class GetSearchInfo implements Command {
        public void execute() {
            _statusService.getPagedSearchInfoForUser(0, DEFAULT_NUM_ROWS,
                    new SortArgument[]{new SortArgument(SearchJobInfo.SORT_BY_SUBMITTED, SortArgument.SORT_DESC),},
                    new AsyncCallback() {
                        public void onFailure(Throwable caught) {
                            showError();
                            //add(HtmlUtils.getHtml("Error looking up the most recent searches","error"));
                            logger.error("error retrieving recent searches - may be harmless page transition");
                        }

                        public void onSuccess(Object result) {
                            _loadingSearchInfoLabel.setVisible(false);
                            List<SearchJobInfo> searches = (List<SearchJobInfo>) result;
                            if (searches == null || searches.size() == 0)
                                showNoResults();
                            else {
                                for (SearchJobInfo job : (List<SearchJobInfo>) result)
                                    add(getRecentSearchWidget(job));
                            }
                        }
                    });
        }

        private void showNoResults() {
            _loadingSearchInfoLabel.setText("No searches found.");
            _loadingSearchInfoLabel.setStyleName("text");
            _loadingSearchInfoLabel.setVisible(true);
        }

        private void showError() {
            _loadingSearchInfoLabel.setText("Unable to load recent searches.");
            _loadingSearchInfoLabel.setVisible(true);
        }
    }

    private Widget getRecentSearchWidget(final SearchJobInfo searchInfo) {
        Link searchLink = new Link(searchInfo.getQueryName(), new ClickListener() {
            public void onClick(Widget sender) {
                gotoSearch(searchInfo);
            }
        });
        searchLink.addStyleName("RecentSearchesLink");

        HorizontalPanel searchInfoPanel = new HorizontalPanel();
        searchInfoPanel.setStyleName("RecentSearchesInfoPanel");
        searchInfoPanel.add(ImageBundleFactory.getControlImageBundle().getRoundBulletImage().createImage());
        searchInfoPanel.add(HtmlUtils.getHtml("&nbsp;", "text"));
        searchInfoPanel.add(searchLink);
        searchInfoPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        searchInfoPanel.add(HtmlUtils.getHtml(new FormattedDateTime(searchInfo.getSubmitted().getTime()).toString(), "searchSecondaryText"));

        // Add the categories in proper order
        HorizontalPanel iconPanel = new HorizontalPanel();
        iconPanel.setStyleName("RecentSearchesIconPanel");
        Map<String, SearchCategoryInfo> categories = searchInfo.getCategories();
        iconPanel.add(getIcon(searchInfo, categories, SearchTask.TOPIC_ACCESSION));
        iconPanel.add(getIcon(searchInfo, categories, SearchTask.TOPIC_PROTEIN));
        iconPanel.add(getIcon(searchInfo, categories, SearchTask.TOPIC_CLUSTER));
        iconPanel.add(getIcon(searchInfo, categories, SearchTask.TOPIC_PUBLICATION));
        iconPanel.add(getIcon(searchInfo, categories, SearchTask.TOPIC_PROJECT));
        iconPanel.add(getIcon(searchInfo, categories, SearchTask.TOPIC_SAMPLE));
        iconPanel.add(getIcon(searchInfo, categories, SearchTask.TOPIC_WEBSITE));

        VerticalPanel panel = new VerticalPanel();
        panel.add(searchInfoPanel);
        panel.add(iconPanel);

        return panel;
    }

    private Panel getIcon(final SearchJobInfo search, Map<String, SearchCategoryInfo> categories, final String category) {
        final SearchCategoryInfo categoryInfo = categories.get(category);
//        SearchIconPanel iconPanel = new SearchTinyIconPanelFactory().createSearchIconPanel(category);
//
//        if (categoryInfo == null) { // category not selected for this search
//            iconPanel.setNoMatches();
//            iconPanel.setEnabled(false);
//        }
//        else {
//            // If hits, enable the icon as a link to the search results category
//            final Integer numHits = (categoryInfo.getNumHits() >= 0) ? categoryInfo.getNumHits() : 0;
//            if (numHits > 0) {
//                iconPanel.addClickListener(new ClickListener() {
//                    public void onClick(Widget sender) {
//                        if (category.equals(SearchTask.TOPIC_ACCESSION))
//                            gotoSearchAccessionEntity(search);
//                        else
//                            gotoSearchCategory(search, categoryInfo.getName(), numHits);
//                    }
//                });
//                iconPanel.addIconStyleName("SearchTinyIconImageClickable");
//                iconPanel.addMouseListener(new SearchIconMouseManager(iconPanel));
//            }
//
//            iconPanel.setNumMatches(numHits);
//        }

        VerticalPanel panel = new VerticalPanel();
//        panel.add(iconPanel);
        return panel;
    }

    // Load the search page with prior search results */
    private void gotoSearch(SearchJobInfo search) {
//        gotoSearchImpl(search, "#SearchMainPageToken", null, null);
    }

//    // Load the search page with prior search results */
//    private void gotoSearchCategory(SearchJobInfo search, String category, Integer numHits) {
//        gotoSearchImpl(search, "#SearchDetailPageToken", category, numHits);
//    }
//
//    private void gotoSearchAccessionEntity(SearchJobInfo search) {
//        gotoSearchImpl(search, "#SearchEntityDetailsPage", SearchTask.TOPIC_ACCESSION, 1);
//    }
//
    // Load the search detail page with prior category results */
//    private void gotoSearchImpl(SearchJobInfo search, String pageToken, String category, Integer numHits) {
//        StringBuffer url = new StringBuffer(UrlBuilder.getSearchUrl());
//        url.append("?keyword=").append(search.getQueryName());
//        url.append("&").append(Search.SEARCH_PRIOR_ID_PARAM).append("=").append(search.getJobId());
//        if (category != null)
//            url.append("&").append(Search.SEARCH_CATEGORY_PARAM).append("=").append(category);
//        if (numHits != null)
//            url.append("&").append(Search.SEARCH_PRIOR_CATEGORY_HITS_PARAM).append("=").append(String.valueOf(numHits));
//        url.append(pageToken);
//
//        Window.open(url.toString(), "_self", "");
//    }
//
    //private Widget getNumHitsWidget(SearchJobInfo searchInfo)
    //{
    //    if(searchInfo.getNumHits() == null || searchInfo.getNumHits().intValue() <= 0)
    //        return HtmlUtils.getHtml("0 matches...","searchSecondaryText");
    //    else
    //        return HtmlUtils.getHtml(searchInfo.getNumHitsFormatted() + " matches", "searchSecondaryText");
    //}
}
