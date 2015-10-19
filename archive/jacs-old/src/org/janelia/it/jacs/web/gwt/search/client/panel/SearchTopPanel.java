
package org.janelia.it.jacs.web.gwt.search.client.panel;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.SmallLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanelFactory;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 6, 2007
 * Time: 1:44:54 PM
 */
public class SearchTopPanel extends TitledBox {
    private static final String EXAMPLE_1 = "GS041";
    private static final String EXAMPLE_2 = "Sargasso Sea";
    private static final String EXAMPLE_3 = "nitrogen fixation";

    private static final int LONG_LENGTH = 80;
    private static final int MAX_LENGTH = 255;
    private static final String SEARCHING_LABEL = "Searching...";
    private static final String LOADING_LABEL = "Loading...";

    private TextBox _keywordBox;
    private RoundedButton _searchButton;
    private Image _searchBusyIcon;
    LoadingLabel _searchingLabel;
    private SearchPanel _topIconPanel;
    private SelectionCounter _topIconCounter = new SelectionCounter();
    private Map<String, SearchIconPanel> _searchIconPanels = new HashMap<String, SearchIconPanel>();
    private HTML _errorMessage;
    private SearchResultsData _data;
    private SearchIconPanelFactory iconFactory;
    private ListBox _optionsList;
    private SmallLink _optionsLink;

    public SearchTopPanel(ClickListener searchClickListener, KeyboardListener searchKeywordListener, SearchResultsData data) {
        super("Search", true /* show action links */);

        // Replace the [hide] action link with "search help"
        removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(this, new HelpActionLink("help"), "Search.HelpURL");

        _data = data;
        DockPanel mainGrid = new DockPanel();
        mainGrid.setWidth("100%");
        mainGrid.setHorizontalAlignment(DockPanel.ALIGN_CENTER);

        // Horizontal Panel for prompt/text box & examples/search button
        HorizontalPanel horizontalPanel = new HorizontalPanel();

        // centering comnpensation for invisible 100px panel on right for "Searching..." label
        HorizontalPanel leftBufferToCompensateForsearchBusyIconPanelOffCentering = new HorizontalPanel();
        leftBufferToCompensateForsearchBusyIconPanelOffCentering.setWidth("80px"); //TODO: put in CSS?
        leftBufferToCompensateForsearchBusyIconPanelOffCentering.add(new HTML("&nbsp;"));

        // Label
        HTML keywordLabel = HtmlUtils.getHtml("Keywords:", "prompt");
        keywordLabel.addStyleName("SearchKeyword" + (BrowserDetector.isIE() ? "IE" : "FF"));

        // Textbox
        VerticalPanel textboxWithExamplePanel = new VerticalPanel();
        textboxWithExamplePanel.add(createKeywordBox(searchKeywordListener));
        textboxWithExamplePanel.add(createOptionPanel());

        // Search Button
        _searchButton = new RoundedButton("Search", searchClickListener);
        _searchButton.setStyleName("SearchButton" + (BrowserDetector.isIE() ? "IE" : "FF"));

        _searchBusyIcon = ImageBundleFactory.getAnimatedImageBundle().getBusyAnimatedIcon().createImage();
        _searchBusyIcon.setStyleName("SearchBusyIcon");
        _searchBusyIcon.setVisible(false);

        HorizontalPanel searchBusyIconPanel = new HorizontalPanel();
        searchBusyIconPanel.setWidth("100px");
        searchBusyIconPanel.add(_searchBusyIcon);

        _searchingLabel = new LoadingLabel(false);
        if (_data.getPriorSearchId() != null)
            _searchingLabel.setText(LOADING_LABEL);
        else
            _searchingLabel.setText(SEARCHING_LABEL);
        _searchingLabel.addStyleName("SearchingLabel");
        searchBusyIconPanel.add(_searchingLabel);

        // Grouping panel
        horizontalPanel.add(leftBufferToCompensateForsearchBusyIconPanelOffCentering);
        horizontalPanel.add(keywordLabel);
        horizontalPanel.add(textboxWithExamplePanel);
        horizontalPanel.add(_searchButton);
        horizontalPanel.add(searchBusyIconPanel);

        mainGrid.add(horizontalPanel, DockPanel.CENTER);

        // Search Icon Set
        _topIconPanel = new SearchPanel();
        _topIconPanel.setStyleName("SearchIconSet");

        // create the icons
        iconFactory = new SearchIconPanelFactory();
        SearchIconPanel allSip = addIcon(Constants.SEARCH_ALL);
        VerticalPanel allSeparator = new VerticalPanel();
        HTML separatorHtml = HtmlUtils.getHtml("", "hint");
        allSeparator.add(separatorHtml);
        allSeparator.setStyleName("SearchIconSeparator");
        _topIconPanel.add(allSeparator);

        // Create additional icons
        SearchIconPanel accessionSip = addIcon(Constants.SEARCH_ACCESSION);
        SearchIconPanel proteinSip = addIcon(Constants.SEARCH_PROTEINS);
        SearchIconPanel clusterSip = addIcon(Constants.SEARCH_CLUSTERS);
        SearchIconPanel publicationSip = addIcon(Constants.SEARCH_PUBLICATIONS);
        SearchIconPanel projectsSip = addIcon(Constants.SEARCH_PROJECTS);
        SearchIconPanel samplesSip = addIcon(Constants.SEARCH_SAMPLES);
        SearchIconPanel websiteSip = addIcon(Constants.SEARCH_WEBSITE);

        // Add unselects for 'all' case
        allSip.addIconToUnselect(accessionSip);
        allSip.addIconToUnselect(proteinSip);
        allSip.addIconToUnselect(clusterSip);
        allSip.addIconToUnselect(publicationSip);
        allSip.addIconToUnselect(projectsSip);
        allSip.addIconToUnselect(samplesSip);
        allSip.addIconToUnselect(websiteSip);

        // Add 'all' unselect for other cases
        accessionSip.addIconToUnselect(allSip);
        proteinSip.addIconToUnselect(allSip);
        clusterSip.addIconToUnselect(allSip);
        publicationSip.addIconToUnselect(allSip);
        projectsSip.addIconToUnselect(allSip);
        samplesSip.addIconToUnselect(allSip);
        websiteSip.addIconToUnselect(allSip);

        // Pre-select 'all'
        allSip.select();

        // Add components
        add(mainGrid);
        add(_topIconPanel);
    }

    private Widget createKeywordBox(KeyboardListener searchKeywordListener) {
        _keywordBox = new TextBox();
        _keywordBox.ensureDebugId("KeywordTextBox");
        _keywordBox.setVisibleLength(LONG_LENGTH);
        _keywordBox.setMaxLength(MAX_LENGTH);
        _keywordBox.setStyleName("SearchKeywordBox");
        _keywordBox.addStyleName("SearchKeywordBoxHeight" + (BrowserDetector.isIE() ? "IE" : "FF"));
        if (searchKeywordListener != null)
            _keywordBox.addKeyboardListener(searchKeywordListener);

        _optionsList = new ListBox();
        _optionsList.setStyleName("SearchOptionsList");
        _optionsList.addStyleName("SearchOptionsListMargin" + (BrowserDetector.isIE() ? "IE" : "FF"));
        _optionsList.addItem("Find all of these words", String.valueOf(SearchTask.MATCH_ALL));
        _optionsList.addItem("Find any of these words", String.valueOf(SearchTask.MATCH_ANY));
        _optionsList.addItem("Find this exact phrase", String.valueOf(SearchTask.MATCH_PHRASE));
        _optionsList.setVisible(false);
        _optionsList.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                _data.setMatchOption(Integer.parseInt(_optionsList.getValue(_optionsList.getSelectedIndex())));
            }
        });

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_keywordBox);
        panel.add(_optionsList);

        return panel;
    }

    private SearchIconPanel addIcon(String category) {
        final SearchIconPanel sip = iconFactory.createSearchIconPanel(category);
        sip.setSelectionCounter(_topIconCounter);
        _topIconPanel.add(sip);
        _searchIconPanels.put(sip.getHeader(), sip);
        return sip;
    }

    public List getSelectedTypes() {
        List<String> types = new ArrayList<String>();
        for (SearchIconPanel iconPanel : _searchIconPanels.values()) {
            if (iconPanel.isSelected())
                types.add(iconPanel.getHeader());
        }
        return types;
    }

    public void setSelectedTypes(Map<String, Integer> topics) {
        if (null == topics) {
            return;
        }
        if (topics.size() == 7) // select all
            _searchIconPanels.get(Constants.SEARCH_ALL).select();
        else {
            _searchIconPanels.get(Constants.SEARCH_ALL).unSelect();
            for (String topic : topics.keySet())
                // SearchIconPanel topics use different strings than the topics retrieved from the database, so we have
                // no choice but hard-coded comparisons...
                if (topic.equals(SearchTask.TOPIC_ACCESSION))
                    _searchIconPanels.get(Constants.SEARCH_ACCESSION).select();
                else if (topic.equals(SearchTask.TOPIC_PROTEIN))
                    _searchIconPanels.get(Constants.SEARCH_PROTEINS).select();
                else if (topic.equals(SearchTask.TOPIC_CLUSTER))
                    _searchIconPanels.get(Constants.SEARCH_CLUSTERS).select();
                else if (topic.equals(SearchTask.TOPIC_PUBLICATION))
                    _searchIconPanels.get(Constants.SEARCH_PUBLICATIONS).select();
                else if (topic.equals(SearchTask.TOPIC_PROJECT))
                    _searchIconPanels.get(Constants.SEARCH_PROJECTS).select();
                else if (topic.equals(SearchTask.TOPIC_SAMPLE))
                    _searchIconPanels.get(Constants.SEARCH_SAMPLES).select();
                else if (topic.equals(SearchTask.TOPIC_WEBSITE))
                    _searchIconPanels.get(Constants.SEARCH_WEBSITE).select();
        }
    }

    public Collection getSearchIconPanels() {
        return _searchIconPanels.values();
    }

    public String getSearchText() {
        return _keywordBox.getText();
    }

    public void setSearchText(String text) {
        _keywordBox.setText(text);
    }

    public void showErrorMessage(String errorMessage) {
        if (_errorMessage != null) {
            remove(_errorMessage);
        }
        _errorMessage = HtmlUtils.getHtml(errorMessage, "error");
        add(_errorMessage);
    }

    public void clearErrorMessage() {
        if (_errorMessage != null)
            remove(_errorMessage);
    }

    public void setNumMatches(String iconTitle, int numMatches) {
        SearchIconPanel iconPanel = _searchIconPanels.get(iconTitle);
        if (iconPanel != null) {
            iconPanel.setNumMatches(numMatches);
        }
    }

    public void removeNumMatches(String iconTitle) {
        SearchIconPanel iconPanel = _searchIconPanels.get(iconTitle);
        if (iconPanel != null) {
            iconPanel.clearFooter();
        }
    }

    public void clearNumMatches() {
        for (SearchIconPanel searchIconPanel : _searchIconPanels.values())
            searchIconPanel.clearFooter();
    }

    public void setSearchBusy(boolean busyFlag) {
        _searchingLabel.setVisible(busyFlag);
        _searchBusyIcon.setVisible(busyFlag);
        if (_data.getPriorSearchId() != null)
            _searchingLabel.setText(LOADING_LABEL);
        else
            _searchingLabel.setText(SEARCHING_LABEL);

        if (busyFlag)
            _searchButton.setText("Cancel");
        else
            _searchButton.setText("Search");
    }

    public void setSearchCancelled() {
        setSearchBusy(false);
        _searchingLabel.setVisible(true);
        _searchingLabel.setText(" Search cancelled.");
    }

    /**
     * left-aligned "show options" link
     */
    private Widget createOptionPanel() {
        HorizontalPanel optionPanel = new HorizontalPanel();
        optionPanel.addStyleName("SearchExamples");

        Widget exampleMenu = createExampleMenu();
        // Commenting out options widget because Lucene doesn't need it.  Keeping around for future search functionality: metagenomic search, etc.
//        Widget optionsWidget = createOptionsWidget();
        optionPanel.add(exampleMenu);
//        optionPanel.add(HtmlUtils.getHtml("|", "SearchDivider"));
//        optionPanel.add(optionsWidget);

        optionPanel.setCellVerticalAlignment(exampleMenu, VerticalPanel.ALIGN_MIDDLE);
//        optionPanel.setCellVerticalAlignment(optionsWidget, VerticalPanel.ALIGN_MIDDLE);

        return optionPanel;
    }

    private Widget createExampleMenu() {
        MenuBar menu = new MenuBar();

        MenuBar dropDown = new MenuBar(/*vertical*/ true);
        addExampleItem(dropDown, EXAMPLE_1);
        addExampleItem(dropDown, EXAMPLE_2);
        addExampleItem(dropDown, EXAMPLE_3);

        MenuItem exampleItem = new MenuItem("Examples" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /*asHTML*/ true, dropDown);
        exampleItem.setStyleName("ExamplesMenu");
        exampleItem.setSelectionStyleName("ExamplesMenu-selected");
        menu.addItem(exampleItem);

        return menu;
    }

    private void addExampleItem(final org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar dropDown,
                                final String text) {
        dropDown.addItem(text, true, new Command() {
            public void execute() {
                _keywordBox.setText(text);
            }
        });
    }

    public boolean isSearchRunning() {
        return _searchButton.getText().equals("Cancel");
    }

    public void executeSearch() {
        _searchButton.execute();
    }
}


