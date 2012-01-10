
package org.janelia.it.jacs.web.gwt.common.client.panel.user;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.export.FileNodeExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOracleListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOraclePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotingPaginator;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 18, 2008
 * Time: 11:47:19 AM
 */
public class UploadUserSequencePanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.user.UploadUserSequencePanel");
    //    private static final String RETRIEVE_DATA_URL = "/jacs/ExportSelected/get_data.htm";
    public static final String UPLOAD_SEQUENCE_NAME_PARAM = "uploadSequenceName";

    public static final int NEW_SEQUENCE_TAB = 0;
    public static final int PREVIOUS_SEQUENCE_TAB = 1;
    private int currentTabIndex = NEW_SEQUENCE_TAB;

    private BlastData blastData;
    private RoundedTabPanel tabPanel;
    private VerticalPanel newSequencePanel;
    private Panel userListPanel;
    private SearchOraclePanel oraclePanel;
    private String _searchString;
    private TextBox _networkPathTextBox = new TextBox();

    private TextBox nameTextBox;
    private Label fileUploadResultMessage;
    private TextArea sequenceTextArea;
    private HashMap<String, UserDataNodeVO> queryDatasetNodes = new HashMap<String, UserDataNodeVO>();
    private HashMap<String, String> queryNameMap = new HashMap<String, String>();
    private SortableTable userSequenceTable;
    private RemotePagingPanel userSequencePagingPanel;
    private RemotingPaginator userSequencePaginator;
    private boolean haveData = false;
    private HorizontalPanel uploadBar;
    private String[] numRowOptions;
    final FormPanel uploadForm = new FormPanel();

    // Keep track of that state of all the controls in the "Specify Query Sequence" Panel
    private boolean previousSequenceSelected = false;
    private boolean sequenceNameIsValid = true;
    private boolean uploadWasSuccessful = false;
    private boolean validSequenceInTextBox = false;

    RadioButton textBoxRB;
    RadioButton uploadRB;
    RadioButton localPathRB;
    // Local storage for uploaded sequence info in case user toggles radio buttons
    public String _uploadedFileId;
    public String _uploadedFileSequenceType;
    private DockPanel _topRow;
    private String _sequenceType;

    private RoundedButton startUploadButton;
    private String _uploadMessage = DEFAULT_UPLOAD_MESSAGE;
    private SelectionListener sequenceSelectionListener;
    private DoubleClickSelectionListener sequenceDoubleClickSelectionListener;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    private static final int USER_SEQUENCE_DELETE_NODE_COLUMN = 0;
    private static final int USER_SEQUENCE_NAME_COLUMN = 1;
    private static final int USER_SEQUENCE_ADDED_COLUMN = 2;
    private static final int USER_SEQUENCE_TYPE_COLUMN = 3;
    private static final int USER_SEQUENCE_LENGTH_COLUMN = 4;
    private static final int USER_SEQUENCE_ACTION_COLUMN = 5;

    private static final String USER_SEQUENCE_NAME_HEADING = "Sequence Name";
    private static final String USER_SEQUENCE_ADDED_HEADING = "Created";
    private static final String USER_SEQUENCE_TYPE_HEADING = "Type";
    private static final String USER_SEQUENCE_LENGTH_HEADING = "File size";
    private static final String USER_SEQUENCE_ACTION_HEADING = "Action";

    private static final int NAME_MAX_SIZE = 60;
    private int defaultNumRows;
    private static final String[] DEFAULT_NUM_ROWS_OPTIONS = new String[]{"10", "15", "20"};
    public static final String DEFAULT_UPLOAD_MESSAGE = "File upload successful - click Apply to continue.";
    private FileUpload uploader;

    public UploadUserSequencePanel(SelectionListener sequenceSelectionListener, String sequenceType, int defaultNumRows) {
        this(sequenceSelectionListener, null, DEFAULT_NUM_ROWS_OPTIONS, new BlastData(), sequenceType, defaultNumRows);
    }

    public UploadUserSequencePanel(SelectionListener sequenceSelectionListener,
                                   DoubleClickSelectionListener sequenceDoubleClickSelectionListener, String[] numRowOptions, BlastData blastData,
                                   String sequenceType, int defaultNumRows) {
        super();
        this.sequenceSelectionListener = sequenceSelectionListener;
        this.sequenceDoubleClickSelectionListener = sequenceDoubleClickSelectionListener;
        this.numRowOptions = numRowOptions;
        this.defaultNumRows = defaultNumRows;
        this.setVisible(true);
        setBlastData(blastData);
        setSequenceType(sequenceType);
        init();
    }

    public void addUpperRightPanel(Widget widget) {
        _topRow.add(widget, DockPanel.EAST);
        _topRow.setCellHorizontalAlignment(widget, DockPanel.ALIGN_RIGHT);
    }

    public void setUploadMessage(String uploadMessage) {
        _uploadMessage = uploadMessage;
    }

    private boolean validQuerySequenceFormAndEnableApplyButton() {
        boolean isValid = false;

        // Check on whether page is ready to go
        if (currentTabIndex == NEW_SEQUENCE_TAB) {
            if (sequenceNameIsValid) {
                if (uploadWasSuccessful && uploadRB.getValue()) {
                    isValid = true;
                }
                if (validSequenceInTextBox && textBoxRB.getValue()) {
                    isValid = true;
                }
            }
        }
        else if (currentTabIndex == PREVIOUS_SEQUENCE_TAB) {
            if (previousSequenceSelected) {
                isValid = true;
            }
        }

        // Prompt the first loading...
        if (!haveData) {
            populateOracle();
            userSequencePagingPanel.first();
        }

        if (isValid)
            notifySelectListener("");
        else {
            notifyUnselectListener("");
        }
        return isValid;
    }

    private void notifySelectListener(String value) {
        sequenceSelectionListener.onSelect(value);
    }

    private void notifyUnselectListener(String value) {
        sequenceSelectionListener.onUnSelect(value);
    }

    private void notifyDoubleClickListener(String value) {
        sequenceDoubleClickSelectionListener.onSelect(value);
    }

    public boolean isPastedSequence() {
        return textBoxRB.getValue();
        //return blastData != null &&
        //    !StringUtils.hasValue(blastData.getUserReferenceFASTA()) ||
        //    (StringUtils.hasValue(blastData.getUserReferenceFASTA()) &&
        //        !blastData.getUserReferenceFASTA().equals(Constants.UPLOADED_FILE_NODE_KEY));
    }

    private void updateBlastDataForNameTextBoxChange() {
        blastData.setMostRecentlySpecifiedQuerySequenceName(nameTextBox.getText());
    }

    private void updateBlastDataForUploadedFile(String sequenceType, String nodeKey) {
        // Why is the below a map, anyway!?!?!?!?!
        blastData.getQuerySequenceDataNodeMap().clear();
        blastData.setMostRecentlySpecifiedQuerySequenceName(nameTextBox.getText());
        blastData.setMostRecentlySelectedQuerySequenceType(sequenceType);
        blastData.setUserReferenceFASTA(nodeKey);
    }

    private void updateBlastDataForPreviousSequence(String targetId, String queryName) {
        _logger.debug("User selected query sequence " + targetId);
        // put the node in the set of selected queries
        blastData.setMostRecentlySpecifiedQuerySequenceName(queryName);
        blastData.getQuerySequenceDataNodeMap().put(targetId, queryDatasetNodes.get(targetId));
    }

    private class EditUserNodeNameHandler implements ClickListener, EditUserNodeNameListener {

        private UserDataNodeVO node;
        private TableRow row;

        public EditUserNodeNameHandler(UserDataNodeVO node, TableRow row) {
            this.node = node;
            this.row = row;
        }

        public void onClick(Widget widget) {
            new PopupAboveLauncher(new EditUserNodeNamePopup(node, this, false)).showPopup(widget);
        }

        public void replaceUserNodeName(final String nodeId, final String nodeName) {
            AsyncCallback renameNodeCallback = new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("Renaming user node " + nodeId + " from " + node.getNodeName() + " to " +
                            nodeName + " failed", caught);
                }

                //  On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("EditUserNodeNameHandler: onSuccess()");
                    String newNodeName = (String) result;
                    if (newNodeName != null) {
                        if (!newNodeName.equals(nodeName)) {
                            // the user provided a name longer
                            // than the maximum length allowed for the name column
                            Window.alert("The given name was too long therefore it was truncated to:\n" +
                                    "\"" + newNodeName + "\"");
                        }
                        _logger.debug("Renamed node " + node.getDatabaseObjectId() + " from " + node.getNodeName() +
                                " to " + newNodeName);
                        node.setNodeName(newNodeName);
                        // Here we assume that editing the node name should implicitly select it as the selected node
                        blastData.setMostRecentlySpecifiedQuerySequenceName(newNodeName);
                        queryNameMap.put(node.getDatabaseObjectId(), newNodeName);
                        queryDatasetNodes.put(node.getDatabaseObjectId(), node);
                        updateDisplay(node, row);
                    }
                    else {
                        _logger.debug("EditUserNodeNameHandler: newNodeName is null");
                    }
                }
            };
            _dataservice.replaceNodeName(nodeId, nodeName, renameNodeCallback);
        }

        /**
         * Updates the edit node name widget
         *
         * @param node
         * @return
         */
        private void updateDisplay(UserDataNodeVO node, TableRow row) {
            if (node == null)
                _logger.error("updateDisplay() node is null");
            TableCell nodeNameCell = row.getTableCell(USER_SEQUENCE_NAME_COLUMN);
            if (node != null)
                nodeNameCell.setValue(determineUsefulNodeName(node));
            Widget widget = getNodeNameWidget(node, row);
            if (widget == null)
                _logger.error("widget from getNodeNameWidget() is null");
            nodeNameCell.setWidget(widget);
            userSequencePagingPanel.getSortableTable().refreshCell(nodeNameCell.getRow(), nodeNameCell.getCol());
        }

    }

    public static String determineUsefulNodeName(UserDataNodeVO node) {
        if (node.getNodeName() != null &&
                node.getNodeName().startsWith("upload_") &&
                node.getDescription() != null &&
                node.getDescription().length() > 0) {
            return node.getDescription();
        }
        else {
            return node.getNodeName();
        }
    }

    private class RemoveNodeEventHandler implements ClickListener, RemoveNodeListener, PopupListener {
        private UserDataNodeVO node;
        private TableRow row;
        boolean inProgress;
        RemoveNodePopup popup;


        public RemoveNodeEventHandler(UserDataNodeVO node, TableRow row) {
            this.node = node;
            this.row = row;
            inProgress = false;
            popup = null;
        }

        public void onClick(Widget widget) {
            startPopup(widget);
        }

        public void onPopupClosed(PopupPanel popupPanel, boolean b) {
            inProgress = false;
        }

        public void removeNode(final String nodeId) {
            AsyncCallback removeNodeCallback = new AsyncCallback() {

                public void onFailure(Throwable caught) {
                    _logger.error("Remove node" + nodeId + " failed", caught);
                    finishedPopup();
                }

                public void onSuccess(Object result) {
                    userSequencePagingPanel.removeRow(row);
                    _logger.debug("Remove node succeded");
                    SystemWebTracker.trackActivity("DeleteQueryNode");
                    finishedPopup();
                }

            };
            _dataservice.deleteNode(nodeId, removeNodeCallback);
        }

        private void startPopup(Widget widget) {
            if (!inProgress) {
                inProgress = true;
                popup = new RemoveNodePopup(node.getNodeName(), node.getDatabaseObjectId(), this, false);
                popup.addPopupListener(this);
                PopupLauncher launcher = new PopupAboveLauncher(popup);
                launcher.showPopup(widget);
            }
        }

        private void finishedPopup() {
            if (popup != null) {
                popup.hide();
            }
            popup = null;
        }

    }

    private Widget getNodeNameWidget(UserDataNodeVO node, TableRow row) {
        Grid grid = new Grid(1, 3);
        grid.setCellSpacing(0);
        grid.setCellPadding(0);

        grid.setWidget(0, 0, new FulltextPopperUpperHTML(determineUsefulNodeName(node), NAME_MAX_SIZE));
        grid.setWidget(0, 1, HtmlUtils.getHtml("&nbsp;", "text"));
        grid.setWidget(0, 2, new ActionLink("edit", new EditUserNodeNameHandler(node, row)));

        return grid;
    }

    /**
     * Creates the node remove widget
     *
     * @param node
     * @return
     */
    private Widget createRemoveNodeWidget(UserDataNodeVO node, TableRow row) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemoveNodeEventHandler(node, row));
        return image;
    }

    private Widget createUserSequenceExportWidget(final UserDataNodeVO node) {
        org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar menu = new org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar();
        menu.setAutoOpen(false);

        org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar dropDown = new org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar(true);

        dropDown.addItem("Export previous sequence as FASTA", true, new Command() {
            public void execute() {
                _logger.debug("Execute called for export of previous sequence as FASTA for node=" + node.getDatabaseObjectId());
                FileNodeExportTask exportTask = new FileNodeExportTask(node.getDatabaseObjectId(),
                        ExportWriterConstants.EXPORT_TYPE_CURRENT, null, null);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem export = new org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        export.setStyleName("topLevelMenuItem");
        menu.addItem(export);

        return menu;
    }

    private class UserSequenceTabListener implements TabListener {
        public boolean onBeforeTabSelected(SourcesTabEvents tabEvents, int i) {
            return true; // don't do anything
        }

        public void onTabSelected(SourcesTabEvents tabEvents, int i) {
            _logger.debug("UserSequenceTabListener: changing selected tab index=" + i);
            userSequenceTable.clearSelect();
            currentTabIndex = i;

            if (i == NEW_SEQUENCE_TAB) // restore the name in the text box
                updateBlastDataForNameTextBoxChange();
            validQuerySequenceFormAndEnableApplyButton();
        }
    }

    protected void init() {
        setStyleName("UserSequenceMain");

        HorizontalPanel msgPanel = new HorizontalPanel();
        msgPanel.add(HtmlUtils.getHtml("Specify one or more nucleotide query sequences (in &nbsp;", "text"));
        msgPanel.add(new ExternalLink("multi-FASTA format", "http://en.wikipedia.org/wiki/FASTA_format"));
        msgPanel.add(HtmlUtils.getHtml("):", "text"));

        _topRow = new DockPanel();
        _topRow.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
        _topRow.setStyleName("ChooseMessage");
        _topRow.setWidth("100%");
        _topRow.add(msgPanel, DockPanel.WEST);
        _topRow.setCellHorizontalAlignment(msgPanel, DockPanel.ALIGN_LEFT);

        tabPanel = new RoundedTabPanel();
        tabPanel.setStyleName("UserTabPanel");
        newSequencePanel = new VerticalPanel();
        newSequencePanel.setStyleName("UserNewSeqPanel");
        VerticalPanel previousSequencePanel = new VerticalPanel();
        tabPanel.add(newSequencePanel, "New Sequence");
        tabPanel.add(previousSequencePanel, "Previous Sequences");
        tabPanel.selectTab(NEW_SEQUENCE_TAB);
        currentTabIndex = NEW_SEQUENCE_TAB;
        tabPanel.addTabListener(new UserSequenceTabListener());

        nameTextBox = new TextBox();
        nameTextBox.setStyleName("UserNameBox");
        nameTextBox.addKeyUpHandler(new KeyUpHandler() {
            public void onKeyUp(KeyUpEvent event) {
                sequenceNameIsValid = nameTextBox.getText().length() > 0;
                validQuerySequenceFormAndEnableApplyButton();
                updateBlastDataForNameTextBoxChange();
            }
        });

        addNamePanel(nameTextBox, newSequencePanel);
        initNameTextBox();

        setupNetworkFilePanel();
        setupTextboxPanel();
        setupOraclePanel();
        setupUserListPanel();
        setupUploadPanel();

        previousSequencePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        previousSequencePanel.add(oraclePanel);
        previousSequencePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        previousSequencePanel.add(userListPanel);

        add(_topRow);
        add(tabPanel);
        setVisible(true);
        userListPanel.setVisible(true);

        // Without this command, the table does not get initialized in the proper order
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                userSequencePaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(USER_SEQUENCE_ADDED_COLUMN, USER_SEQUENCE_ADDED_HEADING, SortableColumn.SORT_DESC)
                });
                userSequencePagingPanel.first();
            }
        });

    }

    private void setupNetworkFilePanel() {
        localPathRB = new RadioButton("newSequenceGroup", "Reference network file - Paste full path to nucleotide FASTA");
        localPathRB.setValue(true);
        localPathRB.setStyleName("prompt");
        localPathRB.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                sequenceTextArea.setVisible(false);
//                 startUploadButton.setEnabled(true);
//                 updateBlastDataForUploadedFile(_uploadedFileSequenceType, _uploadedFileId);
//                 validQuerySequenceFormAndEnableApplyButton();
            }
        });
        newSequencePanel.add(localPathRB);
        HorizontalPanel networkPanel = new HorizontalPanel();
        _networkPathTextBox.setVisibleLength(40);
        _networkPathTextBox.setText("");
        RoundedButton networkFinderButton = new RoundedButton("Select File", new ClickListener() {
            @Override
            public void onClick(Widget widget) {
                // todo Change this to use the NetworkFileChooserPopup
//                NetworkFileChooserPopup networkPopup = new NetworkFileChooserPopup();
//                new PopupCenteredLauncher(networkPopup, 250).showPopup(startUploadButton);
                _dataservice.validateFilePath(_networkPathTextBox.getText(), new AsyncCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("Unable to reach or use the file: " + _networkPathTextBox.getText())).showPopup(_networkPathTextBox);
                    }

                    @Override
                    public void onSuccess(Object o) {
                        new PopupCenteredLauncher(new InfoPopupPanel("File accepted.  Please click \"Submit\" below.")).showPopup(_networkPathTextBox);
                    }
                });
            }
        });
        networkPanel.setStyleName("UserNetworkBox");
        networkPanel.add(_networkPathTextBox);
        networkPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        networkPanel.add(networkFinderButton);
        newSequencePanel.add(networkPanel);
    }

    private void setupOraclePanel() {
        // Create the oraclePanel and hook up callbacks that will repopulate the table
        oraclePanel = new SearchOraclePanel("Sequence Name", new SearchOracleListener() {
            public void onRunSearch(String searchString) {
                _searchString = searchString;
                userSequencePagingPanel.clear();
                userSequencePaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(USER_SEQUENCE_ADDED_COLUMN, USER_SEQUENCE_ADDED_HEADING, SortableColumn.SORT_DESC)
                });
                userSequencePagingPanel.first();
            }

            public void onShowAll() {
                _searchString = null;
                userSequencePagingPanel.clear();
                userSequencePaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(USER_SEQUENCE_ADDED_COLUMN, USER_SEQUENCE_ADDED_HEADING, SortableColumn.SORT_DESC)
                });
                userSequencePagingPanel.first();
            }
        });
        oraclePanel.addSuggestBoxStyleName("AdvancedBlastPreviousSequenceSuggestBox");
    }

    private void populateOracle() {
        // Populate the oraclePanel with the node names
        _dataservice.getBlastableNodeNamesForUser(_searchString, SequenceType.NUCLEOTIDE, new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("error retrieving blastable node names for suggest oracle: " + caught.getMessage());
            }

            // On success, populate the oracle with the names retrieved
            public void onSuccess(Object result) {
                List<String> names = (List<String>) result;
                oraclePanel.addAllOracleSuggestions(names);
                _logger.debug("populating SuggestOrcle with " + (names == null ? 0 : names.size()) + " node names");
            }
        });
    }

    private void setupUserListPanel() {
        userListPanel = new VerticalPanel();

        userSequenceTable = new SortableTable();
        userSequenceTable.setStyleName("SequenceTable");
        userSequenceTable.addColumn(new ImageColumn("&nbsp;"));                       // node delete icon
        userSequenceTable.addColumn(new TextColumn(USER_SEQUENCE_NAME_HEADING));      // must match column int above
        userSequenceTable.addColumn(new TextColumn(USER_SEQUENCE_ADDED_HEADING));     // must match column int above
        userSequenceTable.addColumn(new TextColumn(USER_SEQUENCE_TYPE_HEADING));      // must match column int above
        userSequenceTable.addColumn(new NumericColumn(USER_SEQUENCE_LENGTH_HEADING)); // must match column int above
        userSequenceTable.addColumn(new TextColumn(USER_SEQUENCE_ACTION_HEADING));    // must match column int above

        userSequenceTable.addSelectionListener(new UserSequenceTableListener(userSequenceTable));
        userSequenceTable.addDoubleClickSelectionListener(new UserSequenceDoubleClickListener(userSequenceTable));
        userSequenceTable.setHighlightSelect(true);

        // TODO: move to abstract RemotePagingPanelFactory
        String[][] sortConstants = new String[][]{
                {"", ""},
                {UserDataNodeVO.SORT_BY_NAME, USER_SEQUENCE_NAME_HEADING},
                {UserDataNodeVO.SORT_BY_DATE_CREATED, USER_SEQUENCE_ADDED_HEADING},
                {UserDataNodeVO.SORT_BY_TYPE, USER_SEQUENCE_TYPE_HEADING},
                {UserDataNodeVO.SORT_BY_LENGTH, USER_SEQUENCE_LENGTH_HEADING},
        };
        userSequencePaginator = new RemotingPaginator(userSequenceTable, new NodeDataRetriever(), sortConstants,
                "UploadUserSequence");
        userSequencePagingPanel = new RemotePagingPanel(userSequenceTable, userSequencePaginator, numRowOptions,
                defaultNumRows, "UploadUserSequence");
        userSequencePaginator.setSortColumns(new SortableColumn[]{
                new SortableColumn(USER_SEQUENCE_ADDED_COLUMN, USER_SEQUENCE_ADDED_HEADING, SortableColumn.SORT_DESC)
        });
        userSequencePagingPanel.setNoDataMessage("No user sequences found.");

        userListPanel.add(userSequencePagingPanel);
        userListPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        userListPanel.add(getTableSelectHint());
    }

    /**
     * This callback is invoked by the paging panel when the user changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class NodeDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            _dataservice.getNumBlastableNodesForUser(_searchString, getSequenceType(), new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("NodeDataRetriever.getNumDataNodesForUser().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("NodeDataRetriever.getNumDataNodesForUser().onSuccess() got " + result);
                    listener.onSuccess(result); // Integer
                }
            });
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            _dataservice.getPagedBlastableNodesForUser(_searchString, getSequenceType(), startIndex, numRows,
                    sortArgs, new AsyncCallback() {
                        public void onFailure(Throwable caught) {
                            _logger.error("NodeDataRetriever.retrieveDataRows().onFailure(): " + caught.getMessage());
                            listener.onFailure(caught);
                        }

                        // On success, populate the table with the DataNodes received
                        public void onSuccess(Object result) {
                            UserDataNodeVO[] nodes = (UserDataNodeVO[]) result;
                            if (nodes == null || nodes.length == 0) {
                                _logger.debug("NodeDataRetriever.retrieveDataRows().onSuccess() got no data");
                                listener.onNoData();
                            }
                            else {
                                // TODO: move to PagingController
                                _logger.debug("NodeDataRetriever.retrieveDataRows().onSuccess() got data");
                                listener.onSuccess(processData((UserDataNodeVO[]) result));
                            }
                        }
                    });
        }

        private List processData(UserDataNodeVO[] nodes) {
            _logger.debug("NodeDataRetriever processing " + nodes.length + " nodes");
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (UserDataNodeVO node : nodes) {
                if (node == null) // temporary until DAOs return real paged data
                    continue;
                TableRow tableRow = new TableRow();
                String objectId = node.getDatabaseObjectId();
                tableRow.setRowObject(objectId);
                _logger.debug("Adding name=" + node.getNodeName() + " for nodeId=" + objectId);
                queryNameMap.put(objectId, determineUsefulNodeName(node));
                queryDatasetNodes.put(node.getDatabaseObjectId(), node);

                tableRow.setValue(USER_SEQUENCE_DELETE_NODE_COLUMN,
                        new TableCell(null, createRemoveNodeWidget(node, tableRow)));
                tableRow.setValue(USER_SEQUENCE_NAME_COLUMN,
                        new TableCell(determineUsefulNodeName(node), getNodeNameWidget(node, tableRow)));
                tableRow.setValue(USER_SEQUENCE_ADDED_COLUMN,
                        new TableCell(new FormattedDateTime(node.getDateCreated().getTime())));
                tableRow.setValue(USER_SEQUENCE_TYPE_COLUMN,
                        new TableCell(node.getSequenceType()));
                tableRow.setValue(USER_SEQUENCE_LENGTH_COLUMN,
                        new TableCell(node.getLength()));
                tableRow.setValue(USER_SEQUENCE_ACTION_COLUMN,
                        new TableCell(null, createUserSequenceExportWidget(node)));
                tableRow.setValue(USER_SEQUENCE_ACTION_COLUMN,
                        new TableCell(null, createUserSequenceExportWidget(node)));

                tableRows.add(tableRow);
            }
            haveData = true;
            return tableRows;
        }
    }

    private void uploadButtonChecked() {
        validQuerySequenceFormAndEnableApplyButton();
    }

    private void setupUploadPanel() {
        // Create a Form (required for FileUpload widget)
        _logger.debug("Buiding Upload fasta sequence file widgets...");
        uploadForm.setAction("fileUpload.htm");
        uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        uploadForm.setMethod(FormPanel.METHOD_POST);

        uploadBar = new HorizontalPanel();
        uploadBar.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        uploadRB = new RadioButton("newSequenceGroup", "Upload multi-fasta sequences file");
        uploadRB.setValue(false);
        uploadRB.setStyleName("prompt");
        uploadRB.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                sequenceTextArea.setVisible(false);
                startUploadButton.setEnabled(true);
                updateBlastDataForUploadedFile(_uploadedFileSequenceType, _uploadedFileId);
                validQuerySequenceFormAndEnableApplyButton();
            }
        });

        uploader = new FileUpload();
        uploader.setName("uploadFormElement");
        uploadBar.add(uploader);

        uploadForm.setWidget(uploadBar);

        FocusPanel focusPanel = new FocusPanel();
        focusPanel.add(uploadForm);
        focusPanel.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                uploadRB.setValue(true);
                startUploadButton.setEnabled(true);
            }
        });

        newSequencePanel.add(uploadRB);
        newSequencePanel.add(focusPanel);

        // Add a 'submit' button.
        uploadBar.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        startUploadButton = new RoundedButton("Upload", new ClickListener() {
            public void onClick(Widget sender) {
                uploadRB.setValue(true);
                Hidden nameParam = new Hidden();
                nameParam.setName(UPLOAD_SEQUENCE_NAME_PARAM);
                nameParam.setValue(nameTextBox.getText());
                uploadBar.add(nameParam);
                uploadForm.submit();
            }
        });
        startUploadButton.setEnabled(false);
        uploadBar.add(startUploadButton);

        // Add an event handler to the form.
        uploadForm.addFormHandler(new FormHandler() {

            public void onSubmitComplete(FormSubmitCompleteEvent event) {
                // When the form submission is successfully completed, this event is
                // fired. Assuming the service returned a response of type text/plain,
                // we can get the result text here (see the FormPanel documentation for
                // further explanation).
                if (fileUploadResultMessage != null) {
                    uploadBar.remove(fileUploadResultMessage);
                    fileUploadResultMessage = null;
                }
                try {
                    if (event == null) {
                        _logger.error("onSubmitComplete() event returned null");
                    }
                    else {
                        _logger.debug("OnSubmitComplete - processing return message");
                    }
                    String tmpSeqType = null;
                    if (event != null && null != event.getResults() && !"".equals(event.getResults())) {
                        String returnMessage = event.getResults();
                        _logger.info("Return message=" + returnMessage);
                        if (returnMessage.indexOf(Constants.ERROR_TEXT_SEPARATOR) != -1) {
                            handleError(returnMessage);
                            return;
                        }
                        // First get rid of any surrounding html added by the servlet writer
                        String[] outerArr = returnMessage.split(Constants.OUTER_TEXT_SEPARATOR);
                        _logger.info("outerArr member count=" + outerArr.length);
                        String innerMessage = outerArr[1];
                        _logger.info("innerMessage=" + innerMessage);
                        String[] msgArr = innerMessage.split(Constants.INNER_TEXT_SEPARATOR);
                        _logger.info("msgArr member count=" + msgArr.length);
                        String tmpSessionDataNodeKey = msgArr[0]; // See FileUploadController for construction of this Arr
                        _logger.info("tempSessionDataNodeKey=" + tmpSessionDataNodeKey);
                        tmpSeqType = msgArr[1];
                        _logger.info("tmpSeqType=" + tmpSeqType);
                        if (null != tmpSeqType && !tmpSeqType.trim().equals(SequenceType.PEPTIDE)
                                && !tmpSeqType.trim().equals(SequenceType.NUCLEOTIDE)) {
                            ErrorPopupPanel popup = new ErrorPopupPanel("A valid file has not been selected.");
                            new PopupCenteredLauncher(popup, 250).showPopup(startUploadButton);
                            return;
                        }
                        _logger.info("setting message results");
                        _uploadedFileSequenceType = tmpSeqType;
                        _uploadedFileId = tmpSessionDataNodeKey;
                        updateBlastDataForUploadedFile(tmpSeqType, tmpSessionDataNodeKey);
                        addFileUploadResultMessage(_uploadMessage, "FileUploadMessage");
                        uploadWasSuccessful = true;
                        validQuerySequenceFormAndEnableApplyButton();
                    }
                    else {
                        // This might be a 'back' or 'change' transition
                        _logger.info("return message not qualified for processing (back button probably pressed)");
                    }
                    _logger.info("Seqtype: " + tmpSeqType);
                }
                catch (Throwable e) {
                    _logger.error("Error onSubmitComplete(): " + e.getMessage());
                    uploadWasSuccessful = false;
                    validQuerySequenceFormAndEnableApplyButton();
                    if (uploader.getFilename() == null || uploader.getFilename().equals("")) {
                        addFileUploadResultMessage("File upload was not possible.  Filename was not specified.", "FileUploadErrorMessage");
                    }
                    else {
                        addFileUploadResultMessage("Upload of file " + uploader.getFilename() + " was not successful.", "FileUploadErrorMessage");
                    }
                }
            }

            public void onSubmit(FormSubmitEvent event) {
                // This event is fired just before the form is submitted. We can take
                // this opportunity to perform validation.
                _logger.debug("Submitting.....");
                uploadButtonChecked();
            }

            private void handleError(String returnMessage) {
                String[] outerArr = returnMessage.split(Constants.ERROR_TEXT_SEPARATOR);
                String errorMessage = outerArr[1];
                _logger.debug("ErrorMessage: " + errorMessage);
                ErrorPopupPanel popup = new ErrorPopupPanel(errorMessage);
                new PopupCenteredLauncher(popup, 250).showPopup(startUploadButton);
            }

        });
    }

    private void addFileUploadResultMessage(String message, String style) {
        if (fileUploadResultMessage != null) {
            uploadBar.remove(fileUploadResultMessage);
        }
        fileUploadResultMessage = new Label(message);
        fileUploadResultMessage.setStyleName(style);
        uploadBar.add(fileUploadResultMessage);
    }

    private void addNamePanel(TextBox textbox, Panel panel) {

        Label nameLabel = new Label("Sequence Name:");
        nameLabel.setStyleName("prompt");

        textbox.setVisibleLength(40);
        textbox.setMaxLength(100);
        textbox.setStyleName("NameBoxEntry");

        HorizontalPanel namePanel = new HorizontalPanel();
        namePanel.setStyleName("NamePanel");
        namePanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        namePanel.add(nameLabel);
        namePanel.add(textbox);

        panel.add(namePanel);
    }

    private void initNameTextBox() {
        nameTextBox.setText("My sequence " + new FormattedDateTime(new Date().getTime()));
    }

    private void setupTextboxPanel() {
        textBoxRB = new RadioButton("newSequenceGroup", "Enter sequences in multi-fasta format:");
        textBoxRB.setStyleName("prompt");
        textBoxRB.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                // Clear the other panel
                sequenceTextArea.setVisible(true);
                startUploadButton.setEnabled(false);
                if (fileUploadResultMessage != null) {
                    uploadBar.remove(fileUploadResultMessage);
                }
                validQuerySequenceFormAndEnableApplyButton();
            }
        });
        newSequencePanel.add(textBoxRB);

        sequenceTextArea = new TextArea();
        sequenceTextArea.setCharacterWidth(80);
        sequenceTextArea.setVisibleLines(10);
        sequenceTextArea.setStyleName("UserTextBox");
        sequenceTextArea.addKeyUpHandler(new KeyUpHandler() {
            public void onKeyUp(KeyUpEvent event) {
                validSequenceInTextBox = sequenceTextArea.getText().length() > 0;
                textBoxRB.setValue(true);
                startUploadButton.setEnabled(false);
                if (fileUploadResultMessage != null) {
                    uploadBar.remove(fileUploadResultMessage);
                }
                validQuerySequenceFormAndEnableApplyButton();
            }
        });
        sequenceTextArea.setVisible(false);
        newSequencePanel.add(sequenceTextArea);
    }

    /**
     * Method to verify sequence selection is okay and persist necessary information into the selection "session" object
     *
     * @return boolean if selection is okay to act upon
     */
    public boolean validateAndPersistSequenceSelection() {
        _logger.info("UploadUserSequencePanel: validateAndPersistSequenceSelection()");
        if (currentTabIndex == NEW_SEQUENCE_TAB) {
            if (textBoxRB.getValue()) {
                // As with the uploadButton below, we need to clear the QuerySequenceDataNodeMap
                // to prevent a previous selection from the user list from superceding this
                // choice
                if (blastData.getQuerySequenceDataNodeMap() != null)
                    blastData.getQuerySequenceDataNodeMap().clear();
                boolean pastedTextIsGood = processPastedText();
                // If pasted text is not good, then clear the data bucket
                if (!pastedTextIsGood) {
                    blastData.setUserReferenceFASTA("");
                    blastData.setMostRecentlySelectedQuerySequenceType("");
                }
                if (pastedTextIsGood) {
                    // track the use of the text area for query sequences
                    SystemWebTracker.trackActivity("UseTextAreadNewSequence");
                    return true;
                }
            }
            else { // evaluate upload possibilty before returning false
                // If we are considering this case then we should clear the map holding the info from the user table,
                // so the user selection doesn't supercede the upload choice. The FileUploadController
                // has correctly set blastData and placed the node in the session state.
                if (blastData.getQuerySequenceDataNodeMap() != null)
                    blastData.getQuerySequenceDataNodeMap().clear();
                if (blastData.getUserReferenceFASTA().equals(Constants.UPLOADED_FILE_NODE_KEY)) {
                    // track the use of the file upload for query sequences
                    SystemWebTracker.trackActivity("UseFileUploadNewdSequence");
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        else if (currentTabIndex == PREVIOUS_SEQUENCE_TAB) {
            // The selection event on the table places the selected node in the
            // QuerySequenceDataNodeMap of blastData, so we have nothing to do
            // because on the SubmitJobWaitPage this map has precedence over the
            // state added to blastData from either the text or upload.
            if (blastData.getQuerySequenceDataNodeMap() != null &&
                    blastData.getQuerySequenceDataNodeMap().size() > 0) {
                // track the use of previously uploaded sequences
                SystemWebTracker.trackActivity("UsePreviouslyUploadedSequence");
                return true;
            }
            else {
                return false;
            }
        }
        else {
            _logger.error("Undefined state for currentTabIndex of=" + currentTabIndex);
        }
        return false;
    }

    private String validateFastaFormat(String fastaText) {
        boolean deflineFound = false;

        String lines[] = fastaText.trim().split("\n");

        for (String line : lines) {
            if (line.charAt(0) == '>') {
                if (deflineFound) {
                    return "Consecutive deflines found without interspersed sequences.<br><br>" +
                            "Are you sure you have a carriage return between the defline and sequence?<br>" +
                            "(Text autowrap can cause a long line to appear to be on separate lines.)<br>";
                }
                deflineFound = true;

                // Now, check if the line contains any un-printable characters. If does, return an appropriate
                // message.
                /* Pattern unPrintableCharPattern = Pattern.compile("[\t\r\f]");
            Matcher m = unPrintableCharPattern.matcher(lines[i]);
            if (m.find()) {
                // The current line has match for a un-printable character, thow an exception
                return "Invalid Query FASTA file. Unprintable characters found in defline";
            }    */

            }
            else {
                deflineFound = false;
            }
        }

        if (deflineFound) {
            return "Defline found, but no sequence followed. <br><br>" +
                    "Are you sure you have a carriage return between the defline and sequence? <br>" +
                    "(Text autowrap can cause a long line to appear to be on separate lines.)<br>";
        }

        return "";
    }

    private boolean processPastedText() {
        _logger.debug("Text area is selected.");
        String pastedText = sequenceTextArea.getText();
        _logger.debug("Pasted text is : " + pastedText);
        if (null != pastedText && !"".equalsIgnoreCase(pastedText)) {
            if (pastedText.charAt(0) == '>') {
                String message = validateFastaFormat(pastedText);
                if (!message.equals("")) {
                    ErrorPopupPanel popup = new ErrorPopupPanel(message);
                    new PopupCenteredLauncher(popup, 250).showPopup(this);
                    _logger.error("User pasted sequence problem: " + message + "\n");
                    return false;
                }
            }
            String testDefline = pastedText.trim();
            if (0 < pastedText.indexOf("\n")) {
                testDefline = pastedText.substring(0, pastedText.indexOf("\n")).trim();
            }
            // If there is no defline marker, then add one and assume sequence-only paste, not fasta
            if (0 > testDefline.indexOf(">")) {
                pastedText = ">" + "SYSTEM_USER_PASTED_SEQUENCE /length=" + pastedText.length() + " \n" + pastedText;
                _logger.debug("Changed the sequence to: " + pastedText);
            }
            else if (1 == testDefline.length()) {
                ErrorPopupPanel popup = new ErrorPopupPanel("A non-empty defline is required.");
                new PopupCenteredLauncher(popup, 250).showPopup(this);
                _logger.error("User pasted text which does not have tags on the defline.");
                return false;
            }
            blastData.setUserReferenceFASTA(pastedText);
            blastData.setMostRecentlySpecifiedQuerySequenceName(nameTextBox.getText());
            _dataservice.getSequenceTypeForFASTA(pastedText, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    _logger.error("Unable to determine the sequence type.");
                    ErrorPopupPanel popup = new ErrorPopupPanel("Unable to determine the sequence type.<br>Please ensure that the sequence is valid and try again.");
                    new PopupCenteredLauncher(popup, 250).showPopup(tabPanel);
                    blastData.setUserReferenceFASTA("");
                    blastData.setMostRecentlySelectedQuerySequenceType("");
                }

                public void onSuccess(Object object) {
                    try {
                        String seqType = ((String) object);
                        _logger.debug("Got the sequence type=" + seqType);
                        if (null == seqType || SequenceType.NOT_SPECIFIED.equalsIgnoreCase(seqType)) {
                            ErrorPopupPanel popup = new ErrorPopupPanel("Unable to determine the sequence type.<br>Please ensure that the sequence is valid and try again.");
                            new PopupCenteredLauncher(popup, 250).showPopup(tabPanel);
                        }
                        else {
                            blastData.setMostRecentlySelectedQuerySequenceType(seqType);
                        }
                    }
                    catch (Throwable e) {
                        _logger.error("Error when processing the pasted text.\n" + e.getMessage());
                    }
                }
            });
            return true;
        }
        return false;
    }

    public class UserSequenceTableListener implements SelectionListener {
        private SortableTable targetTable;

        public UserSequenceTableListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        // This callback method is called by SortableTable within its 'onSelect' method when a row has been selected
        public void onSelect(String rowString) {
            onPreviousSequenceSelect(rowString, targetTable);
        }

        // This callback method is called by SortableTable within its 'onSelect' method when the selected row has been unselected
        public void onUnSelect(String rowString) {
            _logger.info("User unselected row " + rowString);
            blastData.getQuerySequenceDataNodeMap().clear();
            notifyUnselectListener("");
            previousSequenceSelected = false;
            validQuerySequenceFormAndEnableApplyButton();

            // Clear any hover attribute
            userSequenceTable.clearHover();
        }
    }

    public class UserSequenceDoubleClickListener implements DoubleClickSelectionListener {
        private SortableTable targetTable;

        public UserSequenceDoubleClickListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        public void onSelect(String rowString) {
            if (onPreviousSequenceSelect(rowString, targetTable)) {
                notifyDoubleClickListener("");
            }
        }
    }

    private boolean onPreviousSequenceSelect(String rowString, SortableTable targetTable) {
        int row = Integer.valueOf(rowString);
        TableRow selectedRow = targetTable.getTableRows().get(row - 1); // getTableRows() is data rows only, so we use -1 offset
        String targetId = (String) selectedRow.getRowObject();
        blastData.getQuerySequenceDataNodeMap().clear();
        if (null == targetId || "".equals(targetId) || targetTable.getText(1, 0).indexOf("No Data") > 0) {
            return false;
        }
        blastData.setMostRecentlySelectedQuerySequenceType(targetTable.getText(row, USER_SEQUENCE_TYPE_COLUMN));
        String queryName = queryNameMap.get(targetId);
        if (queryName == null) {
            _logger.error("Could not find query name in queryNameMap, parsing from table instead");
            queryName = targetTable.getText(row, USER_SEQUENCE_NAME_COLUMN);
            int editIndex = queryName.lastIndexOf("[edit]");
            if (editIndex > 0) {
                queryName = queryName.substring(0, editIndex).trim();
            }
        }
        else {
            _logger.debug("On select, retrieved queryName=" + queryName + " for targetId=" + targetId);
        }
        updateBlastDataForPreviousSequence(targetId, queryName);

        previousSequenceSelected = true;
        boolean valid = validQuerySequenceFormAndEnableApplyButton();

        // Clear any hover attribute
        if (valid)
            userSequenceTable.clearHover();

        return valid;
    }

    protected Widget getTableSelectHint() {
        return HtmlUtils.getHtml("&bull;&nbsp;Click a row to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
    }

    public BlastData getBlastData() {
        return blastData;
    }

    public void setBlastData(BlastData blastData) {
        this.blastData = blastData;
    }

    public String getSequenceType() {
        return _sequenceType;
    }

    public void setSequenceType(String sequenceType) {
        _sequenceType = sequenceType;
    }

    public boolean isNetworkFileSelected() {
        return localPathRB.getValue();
    }

    public String getNetworkPath() {
        return _networkPathTextBox.getText();
    }

    public String getSequenceName() {
        return nameTextBox.getText();
    }

    public void clear() {
        // New sequence tab
        initNameTextBox();
        if (fileUploadResultMessage != null)
            uploadBar.remove(fileUploadResultMessage);
        uploader.setName("");

        // Prev sequence tab
        oraclePanel.clear();
        userSequenceTable.clearHover();
        userSequenceTable.clearSelect();
    }
}
