
package org.janelia.it.jacs.web.gwt.search.client.page;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.URLUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.common.shared.callback.PollingCallback;
import org.janelia.it.jacs.web.gwt.search.client.Search;
import org.janelia.it.jacs.web.gwt.search.client.SearchEntityListener;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchSummaryPanelFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchTopPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchResultsIconPanelFactory;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:42:00 AM
 */
public class SearchMainPage extends SearchWizardPage {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.page.SearchMainPage");
    Panel _mainPanel;
    Panel _summaryPanels;
    Grid _summaryPanelsGrid;
    SearchTopPanel _searchTopPanel;
    private HashMap<String, String> _iconTopicMap;
    private HashMap _iconSearchMap;
    private HashMap _summaryPanelsPosMap;
    private Integer NOT_SET = -1;
    private Integer CATEGORY_SEARCH_ERROR = -2;
    private Integer UNKNOWN = -3;
    private boolean debug = false;
    private List _eventList = new ArrayList(); // debug
    private SearchEntityListener _entityListener;
    SearchResultsData _data = getData();
    PollingCallbackManager _pollManager;

    private class KeywordSearchPollingCallback extends PollingCallback {
        String _searchId;
        String _searchQuery;
        String _topic;

        public KeywordSearchPollingCallback(String searchId, String searchQuery, String topic) {
            super();
            _searchId = searchId;
            _searchQuery = searchQuery;
            _topic = topic;
        }

        /**
         * Unique id for this combination of search and topic
         */
        public String getId() {
            return _searchId + "-" + _topic;
        }

        public void start() {
        }

        public void cancel() {
            if (_searchId == null && _topic == null) // ignore cancel() called by Timer's constructor before the timer's started
                return;

            // Remove the callback and run the real Timer cancel() code
            _pollManager.removeCallback(this);
            super.cancel();
        }

        public void getData() {
            _searchService.runSearchData(_searchId, _topic, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    handleError(throwable.getMessage());
                }

                public void onSuccess(Object object) {
                    String status = (String) object;
                    if (status.equals(Event.SUBTASKERROR_EVENT)) {
                        handleError("searchId=" + _searchId + " returned error status from server");
                    }
                    else if (status.equals(Event.SUBTASKRUNNING_EVENT)) {
                        // Check to see if a new  search has been started, in which case we can bail
                        // on this one to free up client-side resources
                        if (!_searchId.equals(_data.getSearchId())) {
                            logger.debug("self-canceling obsolete timer " + getId());
                            cancel();
                        }
                        else {
                            String message = "searchId=" + _searchId + " topic=" + _topic + " still running";
                            if (debug) {
                                logger.info(message);
                                _eventList.add(message);
                            }
                        }
                    }
                    else if (status.equals(Event.SUBTASKCOMPLETED_EVENT)) {
                        logger.debug("Canceling timer for completed subtask" + getId());
                        cancel();
                        String message = "searchId=" + _searchId + " topic=" + _topic + " completed";
                        if (debug) {
                            logger.info(message);
                            _eventList.add(message);
                        }
                        if (!_searchId.equals(_data.getSearchId())) {
                            // if this not the most current search ignore the results
                            logger.debug("_searchId=" + _searchId + " but _data.getSearchId()=" +
                                    _data.getSearchId() + " so returning without continuing");
                            return;
                        }
                        addTopicCountToMap(_searchId, _searchQuery, _topic);
                    }
                }

                private void handleError(String message) {
                    logger.debug("Canceling timer due to retrival error" + getId());
                    cancel();
                    logger.error("Error during runKeywordSearch for searchId=" + _searchId + " :" +
                            message);
                    if (!_searchId.equals(_data.getSearchId())) {
                        // if this not the most current search ignore the results
                        return;
                    }
                    _data.setSearchFailed(true);
                    updateCategorySearchCount(_topic, CATEGORY_SEARCH_ERROR);
                    _searchTopPanel.showErrorMessage("Error during search");
                }
            });
        }

    }

    private void populateIconMap() {
        _iconTopicMap = new HashMap();
        //----------------- ICON STRING ----------------- TOPIC STRING ------------------
        _iconTopicMap.put(Constants.SEARCH_ALL, SearchTask.TOPIC_ALL);
        _iconTopicMap.put(Constants.SEARCH_ACCESSION, SearchTask.TOPIC_ACCESSION);
        _iconTopicMap.put(Constants.SEARCH_PROTEINS, SearchTask.TOPIC_PROTEIN);
        _iconTopicMap.put(Constants.SEARCH_CLUSTERS, SearchTask.TOPIC_CLUSTER);
        _iconTopicMap.put(Constants.SEARCH_PUBLICATIONS, SearchTask.TOPIC_PUBLICATION);
        _iconTopicMap.put(Constants.SEARCH_PROJECTS, SearchTask.TOPIC_PROJECT);
        _iconTopicMap.put(Constants.SEARCH_SAMPLES, SearchTask.TOPIC_SAMPLE);
        _iconTopicMap.put(Constants.SEARCH_WEBSITE, SearchTask.TOPIC_WEBSITE);
        _iconSearchMap = new HashMap();
        for (String search : _iconTopicMap.keySet())
            _iconSearchMap.put(_iconTopicMap.get(search), search);
    }

    private class SearchEventListener extends KeyboardListenerAdapter
            implements ClickListener {

        public void onKeyPress(Widget sender, char keyCode, int modifiers) {
            // Check for enter
            if ((keyCode == 13) && (modifiers == 0)) {
                onClick(sender);
            }
        }

        public void onClick(Widget w) {
            if (_searchTopPanel.isSearchRunning())
                cancelSearch();
            else
                submitSearch();
        }

        private void cancelSearch() {
            logger.info("User cancelled search");
            _pollManager.cancelAllCallbacks();
            _searchTopPanel.setSearchCancelled();
        }

        private void submitSearch() {
            clearSearch();
            String searchQuery = _searchTopPanel.getSearchText();
            if (validQuery(searchQuery)) {
                logger.info("Submitting search");
                getData().setSearchString(searchQuery);
                _searchTopPanel.setSearchBusy(true);
                prepareKeywordSearch(searchQuery);
            }
        }

        private boolean validQuery(String searchQuery) {
            return searchQuery.trim().length() > 0;
        }

    }

    public class PollingCallbackManager {
        private Map<String, KeywordSearchPollingCallback> _callbacks;

        public PollingCallbackManager() {
            _callbacks = new HashMap();
        }

        public void addCallback(String searchId, String searchQuery, String topic) {
            if (!_searchTopPanel.isSearchRunning()) {
                logger.debug("Not adding polling callback because search has been cancelled");
                return;
            }

            // Polling callback starts automatically
            KeywordSearchPollingCallback callback = new KeywordSearchPollingCallback(searchId, searchQuery, topic);
            logger.debug("PollingCallbackManager: adding polling callback " + callback.getId());
            _callbacks.put(callback.getId(), callback);
        }

        public void removeCallback(KeywordSearchPollingCallback callback) {
            logger.debug("PollingCallbackManager: removing timer " + callback.getId());
            _callbacks.remove(callback.getId());
        }

        public void cancelAllCallbacks() {
            if (_callbacks.size() == 0) return;

            logger.debug("PollingCallbackManager: canceling " + _callbacks.size() + " timers");
            for (KeywordSearchPollingCallback callback : _callbacks.values())
                if (callback != null)
                    callback.cancel();
            _callbacks.clear();
        }

        public int getNumCallbacks() {
            return _callbacks.size();
        }
    }

    private void clearSearch() {
        _searchTopPanel.setSearchBusy(false);
        _searchTopPanel.clearNumMatches();
        _searchTopPanel.clearErrorMessage(); // clear any previous error message
        clearSearchData();
        clearSummaryPanels();
    }

    private void clearSearchData() {
        getData().setSearchString(null);
        getData().setSearchHitCountByTopic(null);
        getData().setSearchId(null);
        getData().setPriorSearchId(null);
        getData().setPriorCategoryHits(null);
        getData().setSearchFailed(false);
    }

    private void prepareKeywordSearch(final String searchQuery) {
        logger.debug("starting prepareKeywordSearch()");
        List accessionTopicList = new ArrayList();
        List selectedList = _searchTopPanel.getSelectedTypes();
        Map<String, Integer> searchHitCountByTopic = new HashMap();
        // This NOT_SET below is important because it uses the keySet
        // of the map to keep track of which topics the user as selected,
        // regardless of whether we find any matches.
        Iterator selectionIter;
        if (isTypeOfAllSearch(selectedList)) {
            // We need to add all topics
            selectionIter = _iconTopicMap.keySet().iterator();
        }
        else {
            // Add only selected topics
            selectionIter = selectedList.iterator();
        }
        while (selectionIter.hasNext()) {
            String selection = (String) selectionIter.next();
            // track the search
            SystemWebTracker.trackActivity("Text Search", new String[]{selection, searchQuery});
            if (!selection.equals(Constants.SEARCH_ALL)) {
                // Only add topics which are not labels for types of 'All' searches
                String topic = _iconTopicMap.get(selection);
                accessionTopicList.add(topic);
                searchHitCountByTopic.put(topic, NOT_SET);
            }
        }
        int matchOption = getData().getMatchOption();
        getData().setSearchHitCountByTopic(searchHitCountByTopic);
        if (debug) {
            logger.info("starting _searchService.prepareSearch()");
            _eventList.add("Preparing search");
        }

        if (!_searchTopPanel.isSearchRunning()) { // don't start processing if user cancelled search
            logger.debug("Stopping before getting search ID");
            return;
        }

        _searchService.prepareSearch(_searchTopPanel.getSearchText(), accessionTopicList,
                matchOption, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        logger.error("Error during preparation of search for keyword=" + _searchTopPanel.getSearchText() + " :" +
                                throwable.getMessage());
                        getData().setSearchFailed(true);
                        _searchTopPanel.setSearchBusy(false);
                        _searchTopPanel.showErrorMessage("Error preparing search.");
                    }

                    public void onSuccess(Object object) {
                        if (debug) {
                            _eventList.add("success - preparing search");
                            logger.info("prepareKeywordSearch - success");
                        }

                        if (_searchTopPanel.isSearchRunning()) { // stop processing if user cancelled search during async prep
                            String searchId = (String) object;
                            getData().setSearchId(searchId);
                            doKeywordSearch(searchId, searchQuery);
                        }
                    }
                });
    }

    private boolean isTypeOfAllSearch(List selectedList) {
        return selectedList.size() == 1 && selectedList.get(0).equals(Constants.SEARCH_ALL);
    }

    private void doKeywordSearch(final String searchId, final String searchQuery) {
        if (debug) {
            _eventList.add("doKeywordSearch starting");
            logger.info("starting doKeywordSearch()");
        }
        Map searchHitCountByTopic = getData().getSearchHitCountByTopic();
        final Set<String> topics = searchHitCountByTopic.keySet();
        _searchService.runSearchStart(searchId, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                logger.error("Error during runSearchStart for searchId=" + searchId + " :" +
                        throwable.getMessage());
                if (!searchId.equals(_data.getSearchId())) {
                    // if this not the most current search ignore the results
                    return;
                }
                _data.setSearchFailed(true);
                for (String topic : topics)
                    updateCategorySearchCount(topic, CATEGORY_SEARCH_ERROR);
                _searchTopPanel.setSearchBusy(false);
                _searchTopPanel.showErrorMessage("Error during search");
            }

            public void onSuccess(Object object) {
                String message = "runSearchStart - successful submission for searchId=" + searchId;
                if (debug) {
                    _eventList.add(message);
                    logger.info(message);
                }
                if (_searchTopPanel.isSearchRunning()) // stop processing if user cancelled during async search start
                    for (String topic : topics)
                        waitForSearchResultByTopic(searchId, searchQuery, topic);
            }
        });
    }

    public void retrievePriorSearch(final String searchId) {
        _data.setSearchId(searchId);
        final Map<String, Integer> searchHitCountByTopic = new HashMap();
        _searchService.getAllCategoriesForSearchId(searchId, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                logger.error("Error during getAllCategoriesForSearchId=" + searchId + " :" +
                        throwable.getMessage());
                if (!searchId.equals(_data.getSearchId())) {
                    // if not current search
                    return;
                }
                _data.setSearchFailed(true);
                _searchTopPanel.setSearchBusy(false);
                _searchTopPanel.showErrorMessage("Error during retrieval of prior search");
            }

            public void onSuccess(Object object) {
                String message = "retrievePriorSearch - successful getAllCategoriesForSearchId=" + searchId;
                if (debug) {
                    _eventList.add(message);
                    logger.info(message);
                }
                if (_searchTopPanel.isSearchRunning()) { // only advance if user has not cancelled
                    List<String> topics = (List<String>) object;
                    for (String topic : topics)
                        searchHitCountByTopic.put(topic, NOT_SET);
                    _data.setSearchHitCountByTopic(searchHitCountByTopic);
                    _searchTopPanel.setSelectedTypes(searchHitCountByTopic);  // Select the categories on the GUI
                    for (String topic : topics)
                        waitForSearchResultByTopic(searchId, _data.getSearchString(), topic);
                }
            }
        });
    }

    private void waitForSearchResultByTopic(final String searchId,
                                            final String searchQuery,
                                            final String topic) {
        if (debug) {
            String message = "starting method waitForSearchResultByTopic() where topic=" + topic;
            logger.debug(message);
            _eventList.add(message);
        }
        if (topic.equals(SearchTask.TOPIC_ACCESSION) ||
                topic.equals(SearchTask.TOPIC_CLUSTER) ||
                topic.equals(SearchTask.TOPIC_PROTEIN) ||
                topic.equals(SearchTask.TOPIC_SAMPLE) ||
                topic.equals(SearchTask.TOPIC_PROJECT) ||
                topic.equals(SearchTask.TOPIC_PUBLICATION)) {
            _pollManager.addCallback(searchId, searchQuery, topic); // create the result poller
        }
        else if (topic.equals(SearchTask.TOPIC_WEBSITE)) {
            if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
                if (debug) {
                    String skipPollingCallbackMessage =
                            "Not creating KeywordSearchPollingCallback for topic website search with GOOGLE API";
                    logger.debug(skipPollingCallbackMessage);
                    _eventList.add(skipPollingCallbackMessage);
                }
                // Go straight to next step for website
                addTopicCountToMap(searchId, searchQuery, topic);
            }
            else {
                _pollManager.addCallback(searchId, searchQuery, topic); // create the result poller
            }
        }
        if (debug) {
            String doneWaitMessage = "done submitting waitForSearchResultByTopic for topic=" + topic;
            logger.debug(doneWaitMessage);
            _eventList.add(doneWaitMessage);
        }
    }

    private void addTopicCountToMap(final String searchId,
                                    final String searchQuery,
                                    final String topic) {
        if (debug) logger.info("starting addTopicCountToMap()");
        List topicList = new ArrayList();
        topicList.add(topic);
        _searchService.getNumSearchResultsForSearchId(searchId, topicList, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                logger.error("Error on getNumSearchResultsForSeachId for searchId=" + getData().getSearchId() + " for topic=" + topic + " :" +
                        throwable.getMessage());
                if (!searchId.equals(getData().getSearchId())) {
                    // if this not the most current search ignore the results
                    return;
                }
                getData().setSearchFailed(true);
                updateCategorySearchCount(topic, CATEGORY_SEARCH_ERROR);
                _searchTopPanel.showErrorMessage("Error during search");
            }

            public void onSuccess(Object object) {
                if (!_searchTopPanel.isSearchRunning()) // stop processing if user cancelled search during this async call
                    return;

                String message = "addTopicCountToMap - success for topic=" + topic + " count=" + object + " match=" + getData().getMatchOption();
                if (debug) {
                    _eventList.add(message);
                    logger.info(message);
                }
                if (!searchId.equals(getData().getSearchId())) {
                    // if this not the most current search ignore the results
                    return;
                }
                Integer count = (Integer) object;
                updateCategorySearchCount(topic, count);
                if (!count.equals(UNKNOWN)) {
                    _searchTopPanel.setNumMatches((String) _iconSearchMap.get(topic), count);
                }
                if (count > 0 || count.equals(UNKNOWN)) {
                    if (debug) _eventList.add("calling populateSummaryPanel for topic=" + topic);
                    populateSummaryPanel(searchId, searchQuery, topic);
                }
            }
        });
    }

    public SearchMainPage(SearchResultsData data, WizardController controller) {
        super(data, controller, false);
        init();
    }

    protected void init() {
        SearchEventListener searchEventListener = new SearchEventListener();
        _searchTopPanel = new SearchTopPanel(searchEventListener, searchEventListener, getData());
        _pollManager = new PollingCallbackManager();

        createSummaryPanels();

        _mainPanel = new VerticalPanel();
        _mainPanel.setStyleName("SearchMain");
        _mainPanel.add(_searchTopPanel);
        _mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _mainPanel.add(_summaryPanels);
        // Create an EntityListener that'll go to the entity detail page if an entity is selected somewhere on the page
        _entityListener = new SearchEntityListener() {
            public void onEntitySelected(String entityId, String category, Object entityData) {
                logger.debug("Accession " + entityId + " + selected, going to next page");
                getData().setEntityDetailAcc(entityId);
                getData().setSelectedCategory(category);
                // go to the detail page
                getController().gotoPage(2);
            }

            public void onEntitySelected(String entityId, Object entityData) {
            }
        };
        populateIconMap();
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
        String pageToken = "SearchMainPageToken";
        if (getData().getSearchId() != null) {
            pageToken = URLUtils.addParameter(pageToken, Search.SEARCH_ID_PARAM, getData().getSearchId());
        }
        return pageToken;
    }

    /**
     * @return the page title used during the page rendering process
     */
    public String getPageTitle() {
        return null; // don't need redundant "search" sub-page name since section is search
    }

    private void createSummaryPanels() {
        _summaryPanels = new TitledBox("Search Results", false);
        _summaryPanels.setStyleName("SearchMainSummaries");
        String[] summaryPanelTypes = new String[]{
                SearchTask.TOPIC_ACCESSION,
                SearchTask.TOPIC_PROTEIN,
                SearchTask.TOPIC_CLUSTER,
                SearchTask.TOPIC_PUBLICATION,
                SearchTask.TOPIC_PROJECT,
                SearchTask.TOPIC_SAMPLE,
                SearchTask.TOPIC_WEBSITE
        };
        _summaryPanelsGrid = new Grid(summaryPanelTypes.length, 1);
        _summaryPanelsGrid.setStyleName("SummariesGrid");
        _summaryPanelsPosMap = new HashMap();
        for (int i = 0; i < summaryPanelTypes.length; i++) {
            _summaryPanelsGrid.getCellFormatter().setVisible(i, 0, false);
            _summaryPanelsPosMap.put(summaryPanelTypes[i], i);
        }
        _summaryPanels.add(_summaryPanelsGrid);
        _summaryPanels.setVisible(false);
    }

    private void clearSummaryPanels() {
        if (_summaryPanels.isVisible()) {
            _summaryPanelsGrid.clear();
            _summaryPanels.setVisible(false);
        }
    }

    private class ViewSearchDetailsListener implements ClickListener {
        private String searchCategory;

        private ViewSearchDetailsListener(String searchCategory) {
            this.searchCategory = searchCategory;
        }

        public void onClick(Widget widget) {
            getData().setSelectedCategory(searchCategory);
            getController().next();
        }
    }

    private void populateSummaryPanel(String searchId, String searchQuery, String category) {
        if (debug) logger.info("Beginning populateSummaryPanel() for category=" + category);
        if (!_summaryPanels.isVisible()) {
            _summaryPanels.setVisible(true);
        }
        // start with creating the panel
        SearchSummaryPanelFactory summaryPanelFactory = new SearchSummaryPanelFactory();
        CategorySummarySearchPanel summaryPanel = summaryPanelFactory.createSearchSummaryPanel(category,
                searchId,
                searchQuery);
        summaryPanel.getDataBuilder().setEntityListener(_entityListener);
        SearchIconPanel iconPanel = new SearchResultsIconPanelFactory().createSearchIconPanel(category);

        Integer count = (Integer) getData().getSearchHitCountByTopic().get(category);
        if (!count.equals(UNKNOWN)) {
            iconPanel.setNumMatches(count);
        }
        summaryPanel.setCategoryIcon(iconPanel, true, new ViewSearchDetailsListener(category));
        if (searchId.equals(getData().getSearchId())) {
            Integer summaryPanelPos = (Integer) _summaryPanelsPosMap.get(category);
            _summaryPanelsGrid.setWidget(summaryPanelPos, 0, summaryPanel);
            _summaryPanelsGrid.getCellFormatter().setVisible(summaryPanelPos, 0, true);
            summaryPanel.populatePanel();
        }
    }

    private Object[] updateCategorySearchCount(String category, Integer count) {
        Map<String, Integer> topicCountMap = getData().getSearchHitCountByTopic();
        topicCountMap.put(category, count);
        int setCount = 0;
        int totalResults = 0;
        for (Integer topicCount : topicCountMap.values()) {
            if (!topicCount.equals(NOT_SET)) {
                if (topicCount >= 0)
                    totalResults += topicCount;
                setCount++;
            }
        }

        boolean done = false;
        if (setCount == topicCountMap.keySet().size()) {
            // All results are in
            _searchTopPanel.setSearchBusy(false);
            done = true;
            if (debug) {
                StringBuffer sb = new StringBuffer();
                Iterator eventIter = _eventList.iterator();
                int i = 0;
                while (eventIter.hasNext()) {
                    sb.append(i++);
                    sb.append(" ");
                    sb.append(eventIter.next());
                    sb.append("\n");
                }
                logger.info(sb.toString());
            }
        }
        return new Object[]{done, totalResults};
    }

    protected void preProcess(Integer priorPageNumber) {
        if (_data.isFireSearchFlag()) { // run search immediately if there is pre-given search string or priorId
            _data.setFireSearchFlag(false); // reset the flag
            _searchTopPanel.setSearchText(_data.getSearchString());
            if (_data.getPriorSearchId() != null) {
                logger.debug("Found priorSearchId=" + _data.getPriorSearchId());
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        retrievePriorSearch(_data.getPriorSearchId());
                        _searchTopPanel.setSearchBusy(true);
                    }
                });
            }
            else {
                logger.debug("Found pre-given search string=\'" + _data.getSearchString() + "\' : executing search immediately");
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        _searchTopPanel.executeSearch();
                        _searchTopPanel.setSearchBusy(true);  // has to be after execute() or search will be cancelled
                    }
                });
            }
        }
    }
}
