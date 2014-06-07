
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.core.BrowserDetector;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LinkGroup;
import org.janelia.it.jacs.web.gwt.common.client.ui.LinkGroupSelectionModel;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.AdvancedSortActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.SmallLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortClickListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;

import java.util.ArrayList;
import java.util.List;

/**
 * This class wraps a SortableTable and uses an instance of Paginator and various widgets to provide
 * paging functionality
 *
 * @author Tareq Nabeel
 */

public class PagingPanel extends VerticalPanel {

    public static final int ADVANCEDSORTLINK_IN_THE_HEADER = 1;
    public static final int ADVANCEDSORTLINK_IN_THE_FOOTER = 2;
    public static final int NO_ADVNACED_SORT_LINK_ANYWHERE_EVER_FORGET_IT_DUDE = 3;

    private static final Logger logger = Logger.getLogger("PagingPanel");

    // List of options for changing number of rows on the page
    protected static final String[] DEFAULT_ROWS_PER_PAGE_OPTIONS = new String[]{"10", "20", "50"};
    public static final int DEFAULT_VISIBLE_ROWS = 20;
    // default option for creating the page loading label
    protected static final boolean DEFAULT_CREATE_PAGE_LOADING_LABEL = true;

    private static final ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
    private AbstractImagePrototype left_arrow_template = imageBundle.getArrowLeftEnabledImage();
    private AbstractImagePrototype right_arrow_template = imageBundle.getArrowRightEnabledImage();

    /**
     * The wrapped data structure containing the data and sorting functionality
     */
    private SortableTable sortableTable;

    /**
     * Provides the actual paging functionality
     */
    private Paginator paginator;

    /**
     * Runtime user-manipulated value on the UI for changing number of rows on the page
     */
    private String[] rowsPerPageOptions;
    private int rowsPerPage;
    private LinkGroupSelectionModel linkGroupSelectionModel;
    private boolean rowsPerPageInitialized = false;

    /**
     * Container for <code>rowsPerPageOptions</code> which gives user different options
     * for number of rows per page (e.g. 10 20 50)
     */
    private HorizontalPanel sizerLinkPanel;


    /**
     * If true, contents of SortableTable will be wrapped in a ScrollPanel
     */
    private boolean includeScrolling;

    /**
     * If true, we add a footer panel to the sortable table
     */
    private boolean includeTableFooter;
    /**
     * if set to ADVANCEDSORTLINK_IN_THE_HEADER or ADVANCEDSORTLINK_IN_THE_FOOTER the initializer
     * adds an advanced sort link in the specified panel
     */
    private int advancedSortLinkPosition;
    private ActionLink advancedSortLink;


    /**
     * Contains the text HTML widgets (e.g. "next 10") that user would click to move to next page.  Two are needed; one for
     * above and one for below SortableTable
     */
    private List<SmallLink> nextHtmls = new ArrayList<SmallLink>();

    /**
     * Contains the text HTML widgets (e.g. "prev 10") that user would click to move to previous page.  Two are needed; one for
     * above and one for below SortableTable
     */
    private List<SmallLink> prevHtmls = new ArrayList<SmallLink>();

    /**
     * Contains the image arrow widgets that user would click to move to next page.  Two are needed; one for
     * above and one for below SortableTable
     */
    private List<Image> nextArrows = new ArrayList<Image>();

    /**
     * Contains the image HTML widgets (e.g. "<") that user would click to move to previous page.  Two are needed; one for
     * above and one for below SortableTable
     */
    private List<Image> prevArrows = new ArrayList<Image>();

    /**
     * Range indicator would be e.g. "1-10 of 56".  It indicates the current range of rows on the page.
     * Two are needed, for above and below SortableTable
     */
    private List<HTML> rangeIndicators = new ArrayList<HTML>();

    /**
     * Contains a range indicator, possibly a sizer panel, and the next/goto/prev paging controls
     */
    private List<DockPanel> pagingControlsPanels = new ArrayList<DockPanel>();

    /**
     * Used to display a message while moving to prev/next/first/last page
     */
    private LoadingLabel pagingInProgressLabel;

    /**
     * If true, then loading label will be displayed when user clicks next/prev/goto paging control panels
     */
    private boolean createLoadingPageLabel;
    private Panel dataPanel;
    private DockPanel pagingControlsPanelTop;
    private DockPanel pagingControlsPanelBottom;
    private DockPanel tableFooterPanel;
    private String _noDataMessage = "No data";
    private String rowsPerPagePreferenceKey;
    private Panel _topCustomContentPanel;
    private Panel _bottomCustomContentPanel;

    public PagingPanel() {
    }

    /**
     * Full paging panel constructor
     *
     * @param sortableTable            - The SortableTable instance to wrap
     * @param rowsPerPageOptions       - caller controlled value for changing the number of rows per page
     * @param rowsPerPage              - current rows per page selection - this value and the rowsPerPageOptions must be correlated
     * @param includeScrolling         - if true wrap the SortableTable in a ScrollPanel
     * @param createLoadingPageLabel   If true, then a loading label will be displayed when user clicks on prev / goto / next
     * @param paginator
     * @param includeTableFooter
     * @param advancedSortLinkPosition
     */
    public PagingPanel(SortableTable sortableTable,
                       String[] rowsPerPageOptions,
                       int rowsPerPage,
                       boolean includeScrolling,
                       boolean createLoadingPageLabel,
                       Paginator paginator,
                       boolean includeTableFooter,
                       int advancedSortLinkPosition,
                       String rowsPerPagePreferenceKey) {
        try {
            this.sortableTable = sortableTable;
            this.rowsPerPageOptions = rowsPerPageOptions;
            this.rowsPerPage = rowsPerPage > 0 ? rowsPerPage : DEFAULT_VISIBLE_ROWS;
            this.includeScrolling = includeScrolling;
            this.createLoadingPageLabel = createLoadingPageLabel;
            this.paginator = paginator;
            this.includeTableFooter = includeTableFooter;
            this.advancedSortLinkPosition = advancedSortLinkPosition;
            this.rowsPerPagePreferenceKey = rowsPerPagePreferenceKey;
            init();
        }
        catch (RuntimeException e) {
            // Log the exception so we know where "null is null or not an object" JavaScript error is coming from
            logger.error("PagingPanel constructor caught exception " + e.getMessage());
            throw e;
        }
    }

    /**
     * Paging panel constructor with default value for current rows per page selection
     *
     * @param sortableTable
     * @param rowsPerPageOptions
     * @param includeScrolling
     * @param createLoadingPageLabel
     * @param paginator
     * @param includeTableFooter
     * @param advancedSortLinkPosition
     * @param rowsPerPagePreferenceKey
     */
    public PagingPanel(SortableTable sortableTable,
                       String[] rowsPerPageOptions,
                       boolean includeScrolling,
                       boolean createLoadingPageLabel,
                       Paginator paginator,
                       boolean includeTableFooter,
                       int advancedSortLinkPosition,
                       String rowsPerPagePreferenceKey) {
        this(sortableTable,
                rowsPerPageOptions,
                DEFAULT_VISIBLE_ROWS,
                includeScrolling,
                createLoadingPageLabel,
                paginator,
                includeTableFooter,
                advancedSortLinkPosition,
                rowsPerPagePreferenceKey);
    }

    /**
     * Creates and initializes a PagingPanel
     *
     * @param sortableTable            The SortableTable instance to wrap
     * @param includeScrolling         If true, contents of SortableTable will be wrapped in a ScrollPanel
     * @param advancedSortLinkPosition specifies whether there is an advanced sort link
     * @param rowsPerPagePreferenceKey
     */
    public PagingPanel(SortableTable sortableTable, boolean includeScrolling, int advancedSortLinkPosition,
                       String rowsPerPagePreferenceKey) {
        this(sortableTable,
                DEFAULT_ROWS_PER_PAGE_OPTIONS,
                includeScrolling,
                DEFAULT_CREATE_PAGE_LOADING_LABEL,
                null,
                false,
                advancedSortLinkPosition,
                rowsPerPagePreferenceKey);
    }

    public PagingPanel(SortableTable sortableTable, boolean includeScrolling, boolean includeTableFooter,
                       String rowsPerPagePreferenceKey) {
        this(sortableTable, DEFAULT_ROWS_PER_PAGE_OPTIONS, includeScrolling, DEFAULT_CREATE_PAGE_LOADING_LABEL,
                null, includeTableFooter, 0, rowsPerPagePreferenceKey);
    }

    public PagingPanel(SortableTable table, Paginator paginator, String rowsPerPagePreferenceKey) {
        this(table, DEFAULT_ROWS_PER_PAGE_OPTIONS, false, DEFAULT_CREATE_PAGE_LOADING_LABEL, paginator, false, 0,
                rowsPerPagePreferenceKey);
    }

    public PagingPanel(SortableTable table, Paginator paginator, boolean includeTableFooter, String rowsPerPagePreferenceKey) {
        this(table, DEFAULT_ROWS_PER_PAGE_OPTIONS, false, DEFAULT_CREATE_PAGE_LOADING_LABEL, paginator, includeTableFooter, 0,
                rowsPerPagePreferenceKey);
    }

    public PagingPanel(SortableTable sortableTable, String rowsPerPagePreferenceKey) {
        this(sortableTable, DEFAULT_ROWS_PER_PAGE_OPTIONS, false, DEFAULT_CREATE_PAGE_LOADING_LABEL, null, false, 0,
                rowsPerPagePreferenceKey);
    }

    public PagingPanel(SortableTable sortableTable, String rowsPerPagePreferenceKey, int defaultRowsPerPage) {
        this(sortableTable, DEFAULT_ROWS_PER_PAGE_OPTIONS, defaultRowsPerPage, false,
                DEFAULT_CREATE_PAGE_LOADING_LABEL, null, false, 0, rowsPerPagePreferenceKey);
    }

    public DockPanel getPagingControlsPanelTop() {
        return pagingControlsPanelTop;
    }

    public DockPanel getPagingControlsPanelBottom() {
        return pagingControlsPanelBottom;
    }

    public DockPanel getTableFooterPanel() {
        return tableFooterPanel;
    }

    public Panel getDataPanel() {
        return dataPanel;
    }

    public void showTableFooter() {
        tableFooterPanel.setVisible(true);
    }

    /**
     * Initializes the paging panel
     */
    private void init() {
        sortableTable.setTableController(new PaginatorTableController(sortableTable));
        sortableTable.addStyleName("pagingPanelTable");

        _topCustomContentPanel = new SimplePanel();
        _bottomCustomContentPanel = new SimplePanel();

        createPaginator();

        // Single reference of paging control panel won't work.  Separate instances are needed for diff DOM elements !!!!
        pagingControlsPanelTop = createPagingControlsPanel(false);
        addCenterWidgets(pagingControlsPanelTop,
                createLoadingPageLabel,
                advancedSortLinkPosition == ADVANCEDSORTLINK_IN_THE_HEADER,
                _topCustomContentPanel);
        add(pagingControlsPanelTop);
        logger.debug("PagingPanel.init() setting rowsPerPage=" + rowsPerPage);
        paginator.initRowsPerPage(rowsPerPage); // caller can override
        logger.debug("PagingPanel.init() getting rowsPerPage from paginator=" + paginator.getRowsPerPage());
        rowsPerPage = paginator.getRowsPerPage(); // make sure these are sync'd
        add(createDataPanel());
        // create the table footer
        createTableFooterPanel();
        addCenterWidgets(tableFooterPanel,
                false,
                advancedSortLinkPosition == ADVANCEDSORTLINK_IN_THE_FOOTER,
                _bottomCustomContentPanel);

        add(tableFooterPanel);
        if (!includeTableFooter) {
            // but if the flag is not set do not display it
            tableFooterPanel.setVisible(false);
        }

        pagingControlsPanelBottom = createPagingControlsPanel(true);
        logger.debug("setting linkGroupSelectionModel rowsPerPage=" + rowsPerPage);
        linkGroupSelectionModel.setSelectedValue(String.valueOf(rowsPerPage)); // sync this up too
        logger.debug("Check1");
        add(pagingControlsPanelBottom);

        setStyleName("pagingPanel");
    }

    public void addPageSizeChangedListener(SelectionListener pageSizeChangedListener) {
        linkGroupSelectionModel.addSelectionListener(pageSizeChangedListener);
    }

    public void removePageSizeChangedListener(SelectionListener pageSizeChangedListener) {
        linkGroupSelectionModel.removeSelectionListener(pageSizeChangedListener);
    }

    /**
     * Creates a range indicator. Range indicator would be e.g. "1-10 of 56".
     * It indicates the current range of rows on the page. Two are needed, for above and below SortableTable
     *
     * @return Range indicator would be e.g. "1-10 of 56".  It indicates the current range of rows on the page.
     *         Two are needed, for above and below SortableTable
     */
    public HTML createRangeIndicator() {
        HTML rangeIndicator = new HTML();
        rangeIndicator.setStyleName("infoText");
        rangeIndicator.addStyleName("pagingControlRange");
        rangeIndicators.add(rangeIndicator);
        return rangeIndicator;
    }

    /**
     * Paginator is responsible for pagination.
     */
    private void createPaginator() {
        if (paginator == null) {
            paginator = new LocalPaginator(sortableTable, pagingInProgressLabel, rowsPerPagePreferenceKey);
        }
        // SortableTable's sort listener should be invoked after paginator's sort listener is invoke
        sortableTable.addClearListener(new SortableTableClearListener());
        sortableTable.addSortListener(new SortableTableSortListener());
    }

    /**
     * Creates an instance of paging control panel. Two are needed; one for
     * above and one for below SortableTable
     *
     * @param createSizerPanel If true, sizer panel is created and added to paging controls panel
     * @return a paging controls panel
     */
    private DockPanel createPagingControlsPanel(boolean createSizerPanel) {
        DockPanel pagingControlsPanel = new DockPanel();
        if (BrowserDetector.isIE()) {
            pagingControlsPanel.setStyleName("pagingPanelControlsPanelIE");
        }
        else {
            pagingControlsPanel.setStyleName("pagingPanelControlsPanel");
        }
        addRangeIndicator(pagingControlsPanel);
        addSizerPanel(pagingControlsPanel, createSizerPanel);
        addNextGoToPrevControls(pagingControlsPanel);
        pagingControlsPanels.add(pagingControlsPanel);
        return pagingControlsPanel;
    }

    /**
     * Adds the range indicator to the paging controls panel
     *
     * @param panel the panel to add range indicator to
     */
    private void addRangeIndicator(DockPanel panel) {
        HTML rangeIndicator = createRangeIndicator();
        panel.add(rangeIndicator, DockPanel.WEST);
        panel.setCellHorizontalAlignment(rangeIndicator, HorizontalPanel.ALIGN_LEFT);
        panel.setCellVerticalAlignment(rangeIndicator, VerticalPanel.ALIGN_MIDDLE);
    }

    /**
     * Adds loading label
     *
     * @param panel                  the panel to add range indicator to
     * @param createLoadingPageLabel If true, then loading label will be displayed
     *                               when user clicks next/prev/goto paging control panels
     * @param createAdvancedSortLink if true it adds an advanced sort link to the panel
     */
    private void addCenterWidgets(DockPanel panel, boolean createLoadingPageLabel, boolean createAdvancedSortLink,
                                  Panel customPanel) {
        if (createLoadingPageLabel) {
            pagingInProgressLabel = new LoadingLabel("Loading...", true);
            if (paginator.getPagingInProgressLabel() == null) {
                paginator.setPagingInProgressLabel(pagingInProgressLabel);
            }
        }
        if (createAdvancedSortLink) {
            advancedSortLink = new AdvancedSortActionLink();
        }

        HorizontalPanel centerPanel = new HorizontalPanel();
        if (createAdvancedSortLink) {
            centerPanel.add(advancedSortLink);
            centerPanel.add(HtmlUtils.getHtml("&nbsp;", "pagingPanelLoadingLabelSeparator"));
        }
        centerPanel.add(customPanel);
        if (createLoadingPageLabel) {
            centerPanel.add(pagingInProgressLabel);
        }
        panel.add(centerPanel, DockPanel.CENTER);
        panel.setCellHorizontalAlignment(centerPanel, HorizontalPanel.ALIGN_CENTER);
        panel.setCellVerticalAlignment(centerPanel, VerticalPanel.ALIGN_MIDDLE);
    }

    public void addCustomContentTop(Widget content) {
        _topCustomContentPanel.clear();
        _topCustomContentPanel.add(content);
    }

    public void addCustomContentBottom(Widget content) {
        _bottomCustomContentPanel.clear();
        _bottomCustomContentPanel.add(content);
    }

    /**
     * Creates and adds the sizer panel to the paging controls panel (e.g. 10 20 50)
     * Thist gives user different options for number of rows per page
     * Creates the sizer panel
     *
     * @param panel            the panel to add sizer panel to
     * @param createSizerPanel If true, sizer panel is created and added to paging controls panel
     */
    private void addSizerPanel(DockPanel panel, boolean createSizerPanel) {
        if (createSizerPanel) {
            HorizontalPanel sizerPanel = createSizerLinkPanel();
            panel.add(sizerPanel, DockPanel.CENTER);
            panel.setCellHorizontalAlignment(sizerPanel, HorizontalPanel.ALIGN_CENTER);
            panel.setCellVerticalAlignment(sizerPanel, VerticalPanel.ALIGN_MIDDLE);
        }
    }

    /**
     * Creates the sizer panel (e.g. 10 20 50) that gives user different options for number
     * of rows per page
     *
     * @return an instance of sizer panel
     */
    private HorizontalPanel createSizerLinkPanel() {
        sizerLinkPanel = new HorizontalPanel();
        sizerLinkPanel.setStyleName("PagingPanelShowControlPanel");
        linkGroupSelectionModel = new LinkGroupSelectionModel();
        linkGroupSelectionModel.setSelectedValue(String.valueOf(rowsPerPage));
        VisibleRowsSelectionListener visibleRowsListener = new VisibleRowsSelectionListener();
        linkGroupSelectionModel.addSelectionListener(visibleRowsListener);

        // The linkGroup (GUI part of the multi-selector thingy) needs to be notified of a change to update the GUI
        LinkGroup linkGroup = new LinkGroup(rowsPerPageOptions, linkGroupSelectionModel);
        linkGroupSelectionModel.addSelectionListener(linkGroup);

        HTML[] rowNumberHTMLs = linkGroup.getGroupMembers();
        HTML promptText = new HTML("&nbsp;Show:&nbsp;&nbsp;");
        promptText.setStyleName("infoPrompt");

        sizerLinkPanel.add(ImageBundleFactory.getControlImageBundle().getShowRowsImage().createImage());
        sizerLinkPanel.add(promptText);
        for (HTML rowNumberHTML : rowNumberHTMLs) {
            sizerLinkPanel.add(rowNumberHTML);
            sizerLinkPanel.add(new HTML("&nbsp;"));
        }
        //logger.debug("sizerLinkPanel=" + sizerLinkPanel.toString());
        sizerLinkPanel.addStyleName("pagingControlSizer");
        return sizerLinkPanel;
    }

    protected void setDefaultNumVisibleRows(int numRows) {
        linkGroupSelectionModel.setSelectedValue(numRows + "");
        updateNextControls(paginator.hasNext());
        updatePrevControls(paginator.hasPrevious());
    }

    /**
     * Adds prev/goto/next paging controls to the paging controls panel
     *
     * @param panel the panel to add prev/goto/next paging controls to
     */
    private void addNextGoToPrevControls(DockPanel panel) {
        HorizontalPanel nextGotoPreviousPanel = new HorizontalPanel();
        nextGotoPreviousPanel.setStyleName(getNextGoToPrevPanelStyleName());

        Widget leftArrow = createLeftArrow();
        HTML prevHtml = createPrevHtml();
        Widget goToMenu = createGoToMenu();
        HTML nextHtml = createNextHtml();
        Widget rightArrow = createRightArrow();

        // Ugly but needs to be done for proper alignment
        nextGotoPreviousPanel.add(leftArrow);
        nextGotoPreviousPanel.add(HtmlUtils.getHtml("&nbsp;", "smallText"));
        nextGotoPreviousPanel.add(prevHtml);
        //nextGotoPreviousPanel.add(verticalBar1);
        nextGotoPreviousPanel.add(goToMenu);
        //nextGotoPreviousPanel.add(verticalBar2);
        nextGotoPreviousPanel.add(nextHtml);
        nextGotoPreviousPanel.add(HtmlUtils.getHtml("&nbsp;", "smallText"));
        nextGotoPreviousPanel.add(rightArrow);

        // Ugly but needs to be done for proper alignment
        nextGotoPreviousPanel.setCellVerticalAlignment(leftArrow, VerticalPanel.ALIGN_MIDDLE);
        nextGotoPreviousPanel.setCellVerticalAlignment(prevHtml, VerticalPanel.ALIGN_MIDDLE);
        //nextGotoPreviousPanel.setCellVerticalAlignment(verticalBar1, VerticalPanel.ALIGN_MIDDLE);
        nextGotoPreviousPanel.setCellVerticalAlignment(goToMenu, VerticalPanel.ALIGN_MIDDLE);
        //nextGotoPreviousPanel.setCellVerticalAlignment(verticalBar2, VerticalPanel.ALIGN_MIDDLE);
        nextGotoPreviousPanel.setCellVerticalAlignment(nextHtml, VerticalPanel.ALIGN_MIDDLE);
        nextGotoPreviousPanel.setCellVerticalAlignment(rightArrow, VerticalPanel.ALIGN_MIDDLE);

        panel.add(nextGotoPreviousPanel, DockPanel.EAST);
        panel.setCellHorizontalAlignment(nextGotoPreviousPanel, HorizontalPanel.ALIGN_RIGHT);
        panel.setCellVerticalAlignment(nextGotoPreviousPanel, VerticalPanel.ALIGN_MIDDLE);
    }

    /**
     * Creates the vertical bar between Next / GoTo / Prev paging controls
     *
     * @return a vertical bar html widget
     */
    protected HTML createVerticalBarSeparator() {
        return HtmlUtils.getHtml("&nbsp;&nbsp;|&nbsp;&nbsp;", "hint");
    }

    /**
     * Creates the Prev paging control text
     *
     * @return text HTML widget (e.g. "prev 10") that user would click to move to previous page.
     */
    private HTML createPrevHtml() {
        SmallLink html = new SmallLink("prev " + rowsPerPage);
        html.addClickListener(new PrevClickListener());
        prevHtmls.add(html);
        return html;
    }

    /**
     * Creates the Prev paging control image
     *
     * @return image HTML widget (e.g. "<") that user would click to move to previous page.
     */
    private Widget createLeftArrow() {
        Image left_arrow = left_arrow_template.createImage();
        left_arrow.setStyleName("pagingPanelPrevImage");
        left_arrow.addClickListener(new PrevClickListener());
        prevArrows.add(left_arrow);
        return left_arrow;
    }

    /**
     * Creates the Next paging control text
     *
     * @return text HTML widget (e.g. "next 10") that user would click to move to next page.
     */
    private HTML createNextHtml() {
        SmallLink html = new SmallLink("next " + rowsPerPage);
        html.addClickListener(new NextClickListener());
        nextHtmls.add(html);
        return html;
    }

    /**
     * Creates the Next paging control image
     *
     * @return image HTML widget (e.g. ">") that user would click to move to next page.
     */
    private Widget createRightArrow() {
        Image right_arrow = right_arrow_template.createImage();
        right_arrow.setStyleName("pagingPanelNextImage");
        right_arrow.addClickListener(new NextClickListener());
        nextArrows.add(right_arrow);
        return right_arrow;
    }

    /**
     * Creates the GoTo drop-down menu for pushing sortable table to first and last pages.
     *
     * @return HTML widget that user would click to move to first and last pages
     */
    protected Widget createGoToMenu() {
        MenuBar menu = new MenuBar();
        menu.setStyleName("pagingPanelGotoMenu");
        MenuBar dropDown = new MenuBar(true);

        dropDown.addItem("First Row", true, new Command() {
            public void execute() {
                first();
            }
        });
        dropDown.addItem("Last Row", true, new Command() {
            public void execute() {
                last();
            }
        });

        MenuItem goToItem = new MenuItem("goto&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        goToItem.setStyleName("topLevelMenuItem");
        menu.addItem(goToItem);

        return menu;
    }

    /**
     * Creates the panel containing the data
     *
     * @return panel containing the SortableTable
     */
    private Panel createDataPanel() {
        dataPanel = null;
        if (includeScrolling) {
            dataPanel = createScrollingPanel();
        }
        else {
            dataPanel = new VerticalPanel();
            dataPanel.add(sortableTable);
        }
        return dataPanel;
    }

    private DockPanel createTableFooterPanel() {
        tableFooterPanel = new DockPanel();
        tableFooterPanel.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
        tableFooterPanel.setStyleName("tableFooter");
        tableFooterPanel.setWidth("100%");
        return tableFooterPanel;
    }

    /**
     * Wraps the SortableTable with a scrolling panel
     *
     * @return ScrollPanel instance wrapping contents of SortableTable
     */
    private ScrollPanel createScrollingPanel() {
        ScrollPanel scrollPanel = new ScrollPanel(sortableTable);
        // We need to set a hard-coded width on IE, otherwise the parent panel
        // will stretch to the width of the sortable table causing next/prev paging
        // controls to hide
        if (BrowserDetector.isIE()) {
            scrollPanel.setStyleName("pagingPanelScrollPanelIE");
        }
        else {
            scrollPanel.setStyleName("pagingPanelScrollPanel");
        }
        return scrollPanel;
    }

    public void setScrollPanelStyleName(String styleName) {
        if (includeScrolling)
            dataPanel.setStyleName(styleName);
    }

    public void setTopControlsPanelStyleName(String styleName) {
        pagingControlsPanelTop.setStyleName(styleName);
    }

    public void setBottomControlsPanelStyleName(String styleName) {
        pagingControlsPanelBottom.setStyleName(styleName);
    }

    protected String getNextGoToPrevPanelStyleName() {
        return "";
    }

    /**
     * Listener to push SortableTable to next page.
     */
    private class NextClickListener implements ClickListener {
        public void onClick(Widget w) {
            next();
        }
    }

    /**
     * Listener to push SortableTable to previous page.
     */
    private class PrevClickListener implements ClickListener {
        public void onClick(Widget w) {
            previous();
        }
    }

    public void first() {
        showLoadingLabel();
        paginator.first();
        updateControls();

        // Show a no data message
        if (!paginator.hasData())
            getSortableTable().setNoData(getNoDataMessage());
    }

    public void removeRow(TableRow tableRow) {
        showLoadingLabel();
        paginator.removeRow(tableRow);
        updateControls();

        // Show a no data message
        if (!paginator.hasData())
            getSortableTable().setNoData(getNoDataMessage());
    }

    protected void last() {
        showLoadingLabel();
        paginator.last();
        updateControls();
    }

    protected void next() {
        if (paginator.hasNext()) {
            showLoadingLabel();
            paginator.next();
            updateControls();
        }
    }

    protected void previous() {
        if (paginator.hasPrevious()) {
            showLoadingLabel();
            paginator.previous();
            updateControls();
        }
    }

    protected void modifyRowsPerPage(int size) {
        showLoadingLabel();
        paginator.modifyRowsPerPage(size);
        paginator.first(); // force back to the first page since the offset is likely wrong for the new rows per page
        updateControls();
    }

    protected void initRowsPerPage(int size) {
        showLoadingLabel();
        paginator.initRowsPerPage(size);
        paginator.refresh();
        updateControls();
    }

    /**
     * Listener to change the page size and the text on next/prev links to reflect new page size.
     */
    private class VisibleRowsSelectionListener implements SelectionListener {
        public void onSelect(String value) {
            if (!rowsPerPageInitialized) {
                initRowsPerPage(Integer.parseInt(value));
                rowsPerPageInitialized = true;
            }
            else {
                modifyRowsPerPage(Integer.parseInt(value));
            }
        }

        public void onUnSelect(String value) {
        }
    }


    /**
     * Listener to make the paging controls visible once sorting is done on the SortableTable
     */
    private class SortableTableSortListener implements TableSortListener {
        public void onBusy(Widget widget) {
            showLoadingLabel();
        }

        public void onBusyDone(Widget widget) {
            updatePagingControlsPanel();
            hideLoadingLabel();
        }
    }

    /**
     * Listener that clears the paging controls when the data is cleared
     */
    private class SortableTableClearListener implements TableClearListener {
        public void onBusy(Widget widget) {
            showLoadingLabel();
        }

        public void onBusyDone(Widget widget) {
        }
    }

    /**
     * Updates the paging controls based on current row in pagintor and whether or not there is data
     */
    private void updatePagingControlsPanel() {
        //logger.debug("PagingPanel updatePagingControlsPanel paginator.hasData()="+paginator.hasData());
        if (paginator.hasData()) {
            showPagingControlsPanel();
            updateControls();
        }
        else {
            hidePagingControlsPanel();
        }
    }

    /**
     * Displays the paging control panels
     */
    private void showPagingControlsPanel() {
        if (pagingControlsPanels != null) {
            for (Object pagingControlsPanel : pagingControlsPanels) {
                ((DockPanel) pagingControlsPanel).setVisible(true);
            }
        }
    }

    /**
     * Hides the paging control panels
     */
    private void hidePagingControlsPanel() {
        if (pagingControlsPanels != null) {
            for (Object pagingControlsPanel : pagingControlsPanels) {
                ((DockPanel) pagingControlsPanel).setVisible(false);
            }
        }
    }

    /**
     * Updates the paging controls based on current row in pagintor and whether or not there is data
     */
    protected void updateControls() {
        updateRangeIndicator();
        updateRowsPerPageOptions();
        updatePrevGotoNextControls();
    }

    /**
     * Updates the paging controls based on current row in pagintor and whether or not there is data
     */
    protected void updatePrevGotoNextControls() {
        if (paginator == null) {
            updateNextControls(false);
            updatePrevControls(false);
        }
        updateNextControls(paginator.hasNext());
        updatePrevControls(paginator.hasPrevious());
    }

    /**
     * Updates the paging controls based on current row in pagintor and whether or not there is data
     *
     * @param enable whether or not the control should be enabled
     */
    private void updateNextControls(boolean enable) {
        if (nextHtmls == null || nextArrows == null) {
            return;
        }
        updateNextPrevTextControls(nextHtmls, "next ", enable);
        for (Object nextArrow : nextArrows) {
            Image arrow = (Image) nextArrow;
            if (enable) {
                ImageBundleFactory.getControlImageBundle().getArrowRightEnabledImage().applyTo(arrow);
            }
            else {
                ImageBundleFactory.getControlImageBundle().getArrowRightDisabledImage().applyTo(arrow);
            }
        }
    }

    /**
     * Updates the paging controls based on current row in pagintor and whether or not there is data
     *
     * @param enable whether or not the control should be enabled
     */
    private void updatePrevControls(boolean enable) {
        if (prevHtmls == null || prevArrows == null) {
            return;
        }
        updateNextPrevTextControls(prevHtmls, "prev ", enable);
        for (Object prevArrow : prevArrows) {
            Image arrow = (Image) prevArrow;
            if (enable) {
                ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().applyTo(arrow);
            }
            else {
                ImageBundleFactory.getControlImageBundle().getArrowLeftDisabledImage().applyTo(arrow);
            }
        }
    }

    /**
     * Updates the paging controls based on current row in pagintor and whether or not there is data
     *
     * @param htmls  the list of htmls to update
     * @param text   the text to set the html to
     * @param enable whether or not the control should be enabled
     */
    private void updateNextPrevTextControls(List<SmallLink> htmls, String text, boolean enable) {
        for (SmallLink link : htmls) {
            link.setLinkText(text + paginator.getRowsPerPage());
            link.setStyleName((enable) ? "smallTextLink" : "disabledSmallTextLink");
        }
    }

    /**
     * Updates the range indicator text in the rangeIndicator widget e.g. 11-20 of 56
     */
    protected void updateRangeIndicator() {
        // Update the indicator of what rows are shown within the current page of data.
        if (rangeIndicators == null || paginator == null) {
            return;
        }

        for (Object rangeIndicator : rangeIndicators) {
            HTML html = (HTML) rangeIndicator;
            if (paginator.hasData()) {
                html.setText(paginator.getCurrentOffset() + " - " + paginator.getLastRow() + " of " + paginator.getTotalRowCount());
                html.setStyleName("infoText");
            }
            else {
                html.setText("0 - 0 of 0");
                html.setStyleName("infoTextDisabled");
            }
            html.addStyleName("pagingControlRange");
        }
    }

    /**
     * Updates the rows-per-page options control based on current row in pagintor and whether or not there is data
     */
    protected void updateRowsPerPageOptions() {
        if (sizerLinkPanel == null || paginator == null) {
            return;
        }
        if (paginator.hasData())
            sizerLinkPanel.setVisible(true);
    }

    public void clear() {
        paginator.clear();
        updateControls();
        getSortableTable().clearSort();
    }

    public void displayErrorMessage(String message) {
        hideLoadingLabel();
        if (message != null) {
            getSortableTable().setError(message);
        }
    }

    public void displayNoDataMessage(String message) {
        hideLoadingLabel();
        if (message != null) {
            getSortableTable().setNoData(message);
        }
        else {
            getSortableTable().setNoData(getNoDataMessage());
        }
    }

    public Paginator getPaginator() {
        return paginator;
    }

    public SortableTable getSortableTable() {
        return sortableTable;
    }

    protected void showLoadingLabel() {
        if (pagingInProgressLabel != null)
            pagingInProgressLabel.setVisible(true);

    }

    protected void hideLoadingLabel() {
        if (pagingInProgressLabel != null)
            pagingInProgressLabel.setVisible(false);
    }

    public void addAdvancedSortClickListener(AdvancedSortClickListener advancedClickListener) {
        if (advancedSortLink != null) {
            advancedSortLink.addClickListener(advancedClickListener);
        }
    }

    /**
     * sortDirection one of SortableTable.SORT_ASC or SortableTable.SORT_DESC
     */
    public void setSortColumns(SortableColumn[] sortColumns) {
        getSortableTable().setSortColumns(sortColumns);
    }

    public void setNoDataMessage(String noDataMessage) {
        _noDataMessage = noDataMessage;
    }

    public String getNoDataMessage() {
        return _noDataMessage;
    }

    public Panel getSizerLinkPanel() {
        return sizerLinkPanel;
    }
}
