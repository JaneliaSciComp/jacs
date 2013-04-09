package org.janelia.it.jacs.web.gwt.geci.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.geci.NeuronalAssayAnalysisTask;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.RemoveNodeListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.RemoveNodePopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOracleListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.suggest.SearchOraclePanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.DateColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotingPaginator;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 1/9/12
 * Time: 2:24 PM
 */
public class NAAManagementPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.geci.client.NAAManagementPanel");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

//    private static final int NAA_DELETE_COLUMN  = 0;
    private static final int NAA_NAME_COLUMN    = 0;
    private static final int NAA_PATH_COLUMN    = 1;
    private static final int NAA_DATE_COLUMN    = 2;
    private static final int NAA_PROCESSED_COLUMN    = 3;
    private static final int NAA_ACTION_COLUMN  = 4;

    private static final String NAA_NAME_HEADING = "Plate Name";
    private static final String NAA_PATH_HEADING = "Path";
    private static final String NAA_DATE_HEADING = "Date";
    private static final String NAA_PROCESSED_HEADING = "Processed";
    private static final String NAA_ACTION_HEADING = "Action";

    private static final int DEFAULT_NUM_ROWS = 15;
    private static final String[] DEFAULT_NUM_ROWS_OPTIONS = new String[]{"10", "15", "20"};

    private NeuronalAssayAnalysisTask _naaTask;
    private Panel resultsListPanel;
    private SearchOraclePanel oraclePanel;
    private TitledBox _discoveryPanel;
    private TextBox _geciImagePath;
    private RoundedButton _scanDirForResultsButton;
    private SortableTable naaResultsTable;
    private RemotePagingPanel naaResultsPagingPanel;
    private RemotingPaginator naaResultsPaginator;
    private boolean haveData = false;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;

    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;
    private HashMap<String, UserDataNodeVO> naaResultMap = new HashMap<String, UserDataNodeVO>();
    private Widget statusMessage;

    public NAAManagementPanel(org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        super();
        _listener = listener;
        VerticalPanel previousNAAPanel = new VerticalPanel();
        setUpDiscoveryPanel();
        setupOraclePanel();
        setupUserListPanel();
        createButtons();

        previousNAAPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        previousNAAPanel.add(_discoveryPanel);
        previousNAAPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        previousNAAPanel.add(oraclePanel);
        previousNAAPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        previousNAAPanel.add(resultsListPanel);
        previousNAAPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        previousNAAPanel.add(getSubmitButtonPanel());
        previousNAAPanel.add(getStatusMessage());

        TitledPanel userSelectionPanel = new TitledPanel("Browse Neuronal Assay Analysis Folders");
        userSelectionPanel.add(previousNAAPanel);

        add(userSelectionPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        // Prompt the first loading...
        if (!haveData) {
            populateOracle();
            naaResultsPagingPanel.first();
        }
//        realize();
    }

    protected void popuplateContentPanel() {
    }

    private void setUpDiscoveryPanel() {
        _discoveryPanel = new TitledBox("Scan For New Directories");
        _geciImagePath = new TextBox();
        _geciImagePath.setVisibleLength(60);
        _geciImagePath.setText("need path");
        _scanDirForResultsButton = new RoundedButton("Scan Directory", new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                _dataservice.findPotentialResultNodes(_geciImagePath.getText(), new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("There was a problem discovering the image directories."), 250).showPopup(_scanDirForResultsButton);
                    }

                    public void onSuccess(Object o) {
                        // If successful, reload from scratch
                        naaResultsPagingPanel.first();
                        new PopupCenteredLauncher(new InfoPopupPanel("Scan completed successfully."), 250).showPopup(_scanDirForResultsButton);
                    }
                });
            }
        });
        // Grid(int rows, int columns)
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Image Directory", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_geciImagePath);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_scanDirForResultsButton);
        _discoveryPanel.add(sourcePanel);
    }

    private void setupOraclePanel() {
        // Create the oraclePanel and hook up callbacks that will repopulate the table
        oraclePanel = new SearchOraclePanel("Plate Name", new SearchOracleListener() {
            public void onRunSearch(String searchString) {
                naaResultsPagingPanel.clear();
                naaResultsPaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(NAA_DATE_COLUMN, NAA_DATE_HEADING, SortableColumn.SORT_ASC)
                });
                naaResultsPagingPanel.first();
            }

            public void onShowAll() {
                naaResultsPagingPanel.clear();
                naaResultsPaginator.setSortColumns(new SortableColumn[]{
                        new SortableColumn(NAA_DATE_COLUMN, NAA_DATE_HEADING, SortableColumn.SORT_ASC)
                });
                naaResultsPagingPanel.first();
            }
        });
        oraclePanel.addSuggestBoxStyleName("AdvancedBlastPreviousSequenceSuggestBox");
    }

    private void populateOracle() {
        // Populate the oraclePanel with the node names
        _dataservice.getNodeNamesForUserByName("org.janelia.it.jacs.model.user_data.geci.NeuronalAssayAnalysisResultNode", new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("error retrieving naaResult logins for suggest oracle: " + caught.getMessage());
            }

            public void onSuccess(Object result) {
                List<String> names = (List<String>) result;
                oraclePanel.addAllOracleSuggestions(names);
                haveData = true;
                _logger.debug("populating SuggestOracle with " + (names == null ? 0 : names.size()) + " node names");
            }
        });
    }

    private void setupUserListPanel() {
        resultsListPanel = new VerticalPanel();

        naaResultsTable = new SortableTable();
        naaResultsTable.setStyleName("SequenceTable");
//        naaResultsTable.addColumn(new ImageColumn("&nbsp;"));        // node delete icon
        naaResultsTable.addColumn(new TextColumn(NAA_NAME_HEADING)); // must match column int above
        naaResultsTable.addColumn(new TextColumn(NAA_PATH_HEADING)); // must match column int above
        naaResultsTable.addColumn(new DateColumn(NAA_DATE_HEADING)); // must match column int above
        naaResultsTable.addColumn(new TextColumn(NAA_PROCESSED_HEADING)); // must match column int above
        naaResultsTable.addColumn(new TextColumn(NAA_ACTION_HEADING)); // must match column int above

        naaResultsTable.addSelectionListener(new UserTableListener(naaResultsTable));
        naaResultsTable.addDoubleClickSelectionListener(new UserDoubleClickListener(naaResultsTable));
        naaResultsTable.setHighlightSelect(true);

        // TODO: move to abstract RemotePagingPanelFactory
        String[][] sortConstants = new String[][]{
                {"", ""},
                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING},
                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING},
                {UserDataNodeVO.SORT_BY_DATE_CREATED, NAA_DATE_HEADING},
                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING},
                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING}
        };
        naaResultsPaginator = new RemotingPaginator(naaResultsTable, new UserDataRetriever(), sortConstants, "NAABrowser");
        naaResultsPagingPanel = new RemotePagingPanel(naaResultsTable, naaResultsPaginator, DEFAULT_NUM_ROWS_OPTIONS, DEFAULT_NUM_ROWS,
                "NAABrowser");
        naaResultsPaginator.setSortColumns(new SortableColumn[]{
                new SortableColumn(NAA_NAME_COLUMN, NAA_NAME_HEADING, SortableColumn.SORT_ASC)
        });
        naaResultsPagingPanel.setNoDataMessage("No plates found.");

        resultsListPanel.add(naaResultsPagingPanel);
        resultsListPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        resultsListPanel.add(getTableSelectHint());
    }

    public Widget getStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");

        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

    /**
     * This callback is invoked by the paging panel when the naaResult changes pages.  It retrieves one page of jobs
     * from the server, creates a table model (List of TableRow) and gives it to the paginator to update the table
     */
    public class UserDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            _dataservice.getNumNodesForUserByName("org.janelia.it.jacs.model.user_data.geci.NeuronalAssayAnalysisResultNode", new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("UserDataRetriever.getNumNodesForUserByName().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    _logger.debug("UserDataRetriever.getNumNodesForUserByName().onSuccess() got " + result);
                    listener.onSuccess(result); // Integer
                }
            });
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     final DataRetrievedListener listener) {
            _dataservice.getPagedNodesForUserByName("org.janelia.it.jacs.model.user_data.geci.NeuronalAssayAnalysisResultNode", startIndex, numRows, sortArgs, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    _logger.error("UserDataRetriever.retrieveDataRows().onFailure(): " + caught.getMessage());
                    listener.onFailure(caught);
                }

                // On success, populate the table with the DataNodes received
                public void onSuccess(Object result) {
                    UserDataNodeVO[] users = (UserDataNodeVO[]) result;
                    if (users == null || users.length == 0) {
                        _logger.debug("UserDataRetriever.getPagedNodesForUserByName().onSuccess() got no data");
                        listener.onNoData();
                    } else {
                        // TODO: move to PagingController
                        _logger.debug("UserDataRetriever.getPagedNodesForUserByName().onSuccess() got data");
                        listener.onSuccess(processData((UserDataNodeVO[]) result));
                    }
                }
            });
        }

        private List processData(UserDataNodeVO[] naaResults) {
            _logger.debug("UserDataNodeRetriever processing " + naaResults.length + " nodes");
            List<TableRow> tableRows = new ArrayList<TableRow>();
            for (UserDataNodeVO naaResult : naaResults) {
                if (naaResult == null) // temporary until DAOs return real paged data
                    continue;
                TableRow tableRow = new TableRow();
                String objectId = naaResult.getDatabaseObjectId().toString();
                tableRow.setRowObject(objectId);
                _logger.debug("Adding name=" + naaResult.getNodeName() + " for userId=" + objectId);
                naaResultMap.put(objectId, naaResult);

//                tableRow.setValue(NAA_DELETE_COLUMN, new TableCell(null, createRemoveNodeWidget(naaResult, tableRow)));
                tableRow.setValue(NAA_NAME_COLUMN,   new TableCell(naaResult.getNodeName()));
                tableRow.setValue(NAA_PATH_COLUMN,   new TableCell(naaResult.getDatabaseObjectId()));
                tableRow.setValue(NAA_DATE_COLUMN,   new TableCell(naaResult.getDateCreated()));
                tableRow.setValue(NAA_PROCESSED_COLUMN,   new TableCell(naaResult.isProcessed()));
                tableRow.setValue(NAA_ACTION_COLUMN,   new TableCell(naaResult.isProcessed()));
//                tableRow.setValue(NAA_ACTION_COLUMN,   new TableCell(null, getActionColumnWidget(naaResult)));

                tableRows.add(tableRow);
            }
            haveData = true;
            return tableRows;
        }
    }

//    protected Widget getActionColumnWidget(UserDataNodeVO naaResult) {
//        Grid grid = new Grid(1, 1);
//        grid.setCellSpacing(0);
//        grid.setCellPadding(0);
//
//        Widget resultMenu = getActionMenu(naaResult);
//        grid.setWidget(0, 0, resultMenu);
//        if (null != resultMenu) {
//            grid.setWidget(0, 1, HtmlUtils.getHtml("/", "linkSeparator"));
//        }
//
//        return grid;
//    }
//
//
//    public Widget getActionMenu(final UserDataNodeVO naaResult) {
//        final MenuBar menu = new MenuBar();
//        menu.setAutoOpen(false);
//
//        MenuBar dropDown = new MenuBar(true);
//
//        MenuItem rerunItem = new MenuItem("Re-run This Job With New Parameters", true, new Command() {
//            public void execute() {
//                _logger.debug("Re-running job=" + naaResult.getDatabaseObjectId() + " with new parameters selected from drop-down");
////                if (_reRunJobListener != null)
////                    _reRunJobListener.onSelect(job);
//            }
//        });
//        dropDown.addItem(rerunItem);
//
//        MenuItem paramItem = new MenuItem("Show Parameters", true, new Command() {
//            public void execute() {
//                _logger.debug("Displaying parameters for job=" + naaResult.getNodeName() + " with parameter popup");
////                // Subclasses may scrub the parameters
////                Map<String, String> popupParamMap = job.getParamMap();
////                _paramPopup = new JobParameterPopup(
////                        job.getJobname(),
////                        new FormattedDateTime(job.getSubmitted().getTime()).toString(),
////                        popupParamMap, false);
////                new PopupCenteredLauncher(_paramPopup).showPopup(menu);
//            }
//        });
//        dropDown.addItem(paramItem);
//
//        MenuItem rerun = new MenuItem("Job&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
//                /* asHTML*/ true, dropDown);
//        rerun.setStyleName("tableTopLevelMenuItem");
//        menu.addItem(rerun);
//
//        return menu;
//    }
//
    /**
     * Creates the node remove widget
     */
    private Widget createRemoveNodeWidget(UserDataNodeVO user, TableRow row) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        Image image = imageBundle.getDeleteImage().createImage();
        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
        image.addClickListener(new RemovePlateEventHandler(user, row));
        return image;
    }

    private class RemovePlateEventHandler implements ClickListener, RemoveNodeListener, PopupListener {
        private UserDataNodeVO naaResult;
        private TableRow row;
        boolean inProgress;
        RemoveNodePopup popup;


        public RemovePlateEventHandler(UserDataNodeVO naaResult, TableRow row) {
            this.naaResult = this.naaResult;
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
            AsyncCallback removeUserCallback = new AsyncCallback() {

                public void onFailure(Throwable caught) {
                    _logger.error("Remove naaResult" + nodeId + " failed", caught);
                    finishedPopup();
                }

                public void onSuccess(Object result) {
                    naaResultsPagingPanel.removeRow(row);
                    _logger.debug("Remove naaResult succeded");
                    SystemWebTracker.trackActivity("DeleteUserNode");
                    finishedPopup();
                }

            };
            _dataservice.deleteNode(nodeId, removeUserCallback);
        }

        private void startPopup(Widget widget) {
            if (!inProgress) {
                inProgress = true;
                popup = new RemoveNodePopup(naaResult.getNodeName(), naaResult.getDatabaseObjectId(), this, false);
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

    public class UserTableListener implements SelectionListener {
        private SortableTable targetTable;

        public UserTableListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        // This callback method is called by SortableTable within its 'onSelect' method when a row has been selected
        public void onSelect(String rowString) {
            onPreviousSelect(rowString, targetTable);
            _submitButton.setEnabled(true);
        }

        // This callback method is called by SortableTable within its 'onSelect' method when the selected row has been unselected
        public void onUnSelect(String rowString) {
            _logger.info("User unselected row " + rowString);
            // Clear any hover attribute
            if (null!=naaResultsTable.getSelectedRow()) {
                _submitButton.setEnabled(true);
            }
            else {
                _submitButton.setEnabled(false);
            }
            naaResultsTable.clearHover();
        }
    }

    public class UserDoubleClickListener implements DoubleClickSelectionListener {
        private SortableTable targetTable;

        public UserDoubleClickListener(SortableTable targetTable) {
            this.targetTable = targetTable;
        }

        public void onSelect(String rowString) {
            if (onPreviousSelect(rowString, targetTable)) {
                new PopupCenteredLauncher(new InfoPopupPanel("Got the double click for row "+rowString), 250).showPopup(_scanDirForResultsButton);
                _submitButton.setEnabled(true);
            }
        }
    }

    private boolean onPreviousSelect(String rowString, SortableTable targetTable) {
        int row = Integer.valueOf(rowString);
        TableRow selectedRow = (targetTable.getTableRows().get(row - 1)); // getTableRows() is data rows only, so we use -1 offset
        String targetId = (String) selectedRow.getRowObject();
        if (null == targetId || "".equals(targetId) || targetTable.getText(1, 0).indexOf("No Data") > 0) {
            return false;
        }
        String naaResultName = naaResultMap.get(targetId).getNodeName();
        if (naaResultName == null) {
            _logger.error("Could not find naaResult name in naaResultMap, parsing from table instead");
//            userName=targetTable.getText(row, USER_LOGIN_COLUMN);
        }
        else {
            _logger.debug("On select, retrieved userName=" + naaResultName + " for targetId=" + targetId);
        }

        naaResultsTable.clearHover();

        return true;
    }

    protected Widget getTableSelectHint() {
        return HtmlUtils.getHtml("&bull;&nbsp;Click a row to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
    }

    private void createButtons() {
        _clearButton = new RoundedButton("Clear", new ClickListener() {
            public void onClick(Widget sender) {
                clear();
            }
        });

        _submitButton = new RoundedButton("Submit Job", new ClickListener() {
            public void onClick(Widget sender) {
                submitJob();
            }
        });
        _submitButton.setEnabled(false);
    }

    private Widget getSubmitButtonPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);

        return new CenteredWidgetHorizontalPanel(panel);
    }

    private void submitJob() {
        // Validate job name
        // submit the job
        _naaTask = new NeuronalAssayAnalysisTask();
//        String nodeId = "Test";//naaResultsTable.getValue(naaResultsTable.getSelectedRow().getRowIndex(), 0).toString();
//        _naaTask.setJobName(nodeId);
//        _naaTask.setParameter(NeuronalAssayAnalysisTask.PARAM_inputFile, naaResultMap.get(nodeId).getDatabaseObjectId());
        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();

        new SubmitJob(_naaTask, new JobSubmissionListener()).runJob();
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable caught) {
            _submitButton.setEnabled(true);
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
            _submitButton.setEnabled(true);
            _statusMessage.showSuccessMessage();
            _listener.onSuccess(jobId);
        }
    }


}
