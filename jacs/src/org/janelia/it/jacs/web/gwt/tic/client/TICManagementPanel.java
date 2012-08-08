package org.janelia.it.jacs.web.gwt.tic.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.tic.BatchTicTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 1/9/12
 * Time: 2:24 PM
 */
public class TICManagementPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.tic.client.TICManagementPanel");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

//    private static final int NAA_DELETE_COLUMN  = 0;
//    private static final int NAA_NAME_COLUMN    = 0;
//    private static final int NAA_PATH_COLUMN    = 1;
//    private static final int NAA_DATE_COLUMN    = 2;
//    private static final int NAA_PROCESSED_COLUMN    = 3;
//    private static final int NAA_ACTION_COLUMN  = 4;

//    private static final String NAA_NAME_HEADING = "Plate Name";
//    private static final String NAA_PATH_HEADING = "Path";
//    private static final String NAA_DATE_HEADING = "Date";
//    private static final String NAA_PROCESSED_HEADING = "Processed";
//    private static final String NAA_ACTION_HEADING = "Action";
//
//    private static final int DEFAULT_NUM_ROWS = 15;
//    private static final String[] DEFAULT_NUM_ROWS_OPTIONS = new String[]{"10", "15", "20"};

    private BatchTicTask _ticTask;
//    private Panel resultsListPanel;
//    private SearchOraclePanel oraclePanel;
//    private TitledBox _discoveryPanel;
    private TextBox _imagePath;

    private TextBox _jobNameTextBox;

    private CheckBox _applyCalibrationCheckBox;
    private CheckBox _illuminationCorrectionCheckBox;
    private CheckBox _fqbatchCheckBox;

    private TextBox _inputFileOrDirTextBox;
    private TitledBox _inputFileOrDirPanel;
    private RoundedButton _validateInputFileOrDirButton;

    private TextBox _transformationFileTextBox;
    private TitledBox _transformationFilePanel;
    private RoundedButton _validateTransformationFileButton;

    private TextBox _avgReadNoiseTextBox;
    private TitledBox _avgReadNoisePanel;

    private TextBox _avgDarkTextBox;
    private TitledBox _avgDarkPanel;

    private TextBox _borderCropPixelsTextBox;
    private TitledBox _borderCropPanel;

    private TextBox _intensityCorrectionMatrixFileTextBox;
    private TitledBox _intensityCorrectionPanel;
    private RoundedButton _validateIntensityCorrectionFileButton;

    private TextBox _microscopeSettingsTextBox;
    private TitledBox _microscopeSettingsPanel;

    private TextBox _positionXMinTextBox;
    private TextBox _positionXMaxTextBox;
    private TextBox _positionYMinTextBox;
    private TextBox _positionYMaxTextBox;
    private TextBox _positionZMinTextBox;
    private TextBox _positionZMaxTextBox;
    private TextBox _sigmaXMinTextBox;
    private TextBox _sigmaXMaxTextBox;
    private TextBox _sigmaYMinTextBox;
    private TextBox _sigmaYMaxTextBox;
    private TextBox _sigmaZMinTextBox;
    private TextBox _sigmaZMaxTextBox;
    private TitledBox _thresholdPanel;
    
    private RoundedButton _validateMicroscopeSettingsFileButton;
    private CheckBox _spotsOnlyCheckBox;

    //    private RoundedButton _scanDirForResultsButton;
    //    private SortableTable naaResultsTable;
    //    private RemotePagingPanel naaResultsPagingPanel;
    //    private RemotingPaginator naaResultsPaginator;
    private String _validationFilePath;
    private boolean haveData = false;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;

    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;
    private HashMap<String, UserDataNodeVO> naaResultMap = new HashMap<String, UserDataNodeVO>();
    private Widget statusMessage;

    public TICManagementPanel(org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        super();
        _listener = listener;
        HorizontalPanel stepPanel = new HorizontalPanel();
        _applyCalibrationCheckBox = new CheckBox("Apply Calibration Step");
        _applyCalibrationCheckBox.setValue(Boolean.TRUE);
        _illuminationCorrectionCheckBox = new CheckBox("Illumination Correction Step");
        _illuminationCorrectionCheckBox.setValue(Boolean.TRUE);
        _fqbatchCheckBox = new CheckBox("FISH QUANT V4 Step");
        _fqbatchCheckBox.setValue(Boolean.TRUE);
        stepPanel.add(_applyCalibrationCheckBox);
        stepPanel.add(_illuminationCorrectionCheckBox);
        stepPanel.add(_fqbatchCheckBox);
        _spotsOnlyCheckBox = new CheckBox("Only persist spots data");

        HorizontalPanel _spotsPanel = new HorizontalPanel();
        _spotsPanel.add(_spotsOnlyCheckBox);

        HorizontalPanel jobPanel = new HorizontalPanel();
        _jobNameTextBox = new TextBox();
        jobPanel.add(HtmlUtils.getHtml("Job Name:", "prompt"));
        jobPanel.add(_jobNameTextBox);

        VerticalPanel settingsPanel = new VerticalPanel();
//        setUpDiscoveryPanel();
//        setupOraclePanel();
//        setupUserListPanel();
        setUpInputFileOrDirectoryPanel();
        setUpBorderCropPanel();
        setUpTransformationFilePanel();
        setUpAvgReadNoisePanel();
        setUpAvgDarkPanel();
        setUpIntensityCorrectionPanel();
        setUpMicroscopeSettingsPanel();
        setUpThresholdPanel();
        createButtons();

        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(jobPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(stepPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(_spotsPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
//        settingsPanel.add(_discoveryPanel);
//        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
//        settingsPanel.add(oraclePanel);
//        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(_inputFileOrDirPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        settingsPanel.add(_borderCropPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        settingsPanel.add(_transformationFilePanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        settingsPanel.add(_intensityCorrectionPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        settingsPanel.add(_avgReadNoisePanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        settingsPanel.add(_avgDarkPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        settingsPanel.add(_microscopeSettingsPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        settingsPanel.add(_thresholdPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
//        settingsPanel.add(resultsListPanel);
        settingsPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        settingsPanel.add(getSubmitButtonPanel());
        settingsPanel.add(getStatusMessage());

        TitledPanel userSelectionPanel = new TitledPanel("Set Parameters ");
        userSelectionPanel.add(settingsPanel);

        add(userSelectionPanel);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        // Prompt the first loading...
//        if (!haveData) {
//            populateOracle();
//            naaResultsPagingPanel.first();
//        }
//        realize();
    }

    private void setUpThresholdPanel() {
        _thresholdPanel= new TitledBox("Thresholds");
        _positionXMinTextBox = new TextBox();
        _positionXMinTextBox.setVisibleLength(10);
        _positionXMinTextBox.setText("-120");
        _positionXMaxTextBox = new TextBox();
        _positionXMaxTextBox.setVisibleLength(10);
        _positionXMaxTextBox.setText("22000");

        _positionYMinTextBox = new TextBox();
        _positionYMinTextBox.setVisibleLength(10);
        _positionYMinTextBox.setText("-120");
        _positionYMaxTextBox = new TextBox();
        _positionYMaxTextBox.setVisibleLength(10);
        _positionYMaxTextBox.setText("22000");

        _positionZMinTextBox = new TextBox();
        _positionZMinTextBox.setVisibleLength(10);
        _positionZMinTextBox.setText("-500");
        _positionZMaxTextBox = new TextBox();
        _positionZMaxTextBox.setVisibleLength(10);
        _positionZMaxTextBox.setText("5000");

        _sigmaXMinTextBox = new TextBox();
        _sigmaXMinTextBox.setVisibleLength(10);
        _sigmaXMinTextBox.setText("30");
        _sigmaXMaxTextBox = new TextBox();
        _sigmaXMaxTextBox.setVisibleLength(10);
        _sigmaXMaxTextBox.setText("600");

        _sigmaYMinTextBox = new TextBox();
        _sigmaYMinTextBox.setVisibleLength(10);
        _sigmaYMinTextBox.setText("30");
        _sigmaYMaxTextBox = new TextBox();
        _sigmaYMaxTextBox.setVisibleLength(10);
        _sigmaYMaxTextBox.setText("600");

        _sigmaZMinTextBox = new TextBox();
        _sigmaZMinTextBox.setVisibleLength(10);
        _sigmaZMinTextBox.setText("50");
        _sigmaZMaxTextBox = new TextBox();
        _sigmaZMaxTextBox.setVisibleLength(10);
        _sigmaZMaxTextBox.setText("800");

        _validateIntensityCorrectionFileButton = getValidationButton(_intensityCorrectionMatrixFileTextBox.getText());

        HorizontalPanel thresholdPanel = new HorizontalPanel();
        Grid thrPosGrid = new Grid(4,3);
        thrPosGrid.setWidget(0,0,HtmlUtils.getHtml("Position","prompt"));
        thrPosGrid.setWidget(0,1,HtmlUtils.getHtml("Min","prompt"));
        thrPosGrid.setWidget(0,2,HtmlUtils.getHtml("Max","prompt"));
        thrPosGrid.setWidget(1,0,HtmlUtils.getHtml("X","prompt"));
        thrPosGrid.setWidget(1,1,_positionXMinTextBox);
        thrPosGrid.setWidget(1,2,_positionXMaxTextBox);
        thrPosGrid.setWidget(2,0,HtmlUtils.getHtml("Y","prompt"));
        thrPosGrid.setWidget(2,1,_positionYMinTextBox);
        thrPosGrid.setWidget(2,2,_positionYMaxTextBox);
        thrPosGrid.setWidget(3,0,HtmlUtils.getHtml("Z","prompt"));
        thrPosGrid.setWidget(3,1,_positionZMinTextBox);
        thrPosGrid.setWidget(3,2,_positionZMaxTextBox);

        Grid thrSigGrid = new Grid(4,3);
        thrSigGrid.setWidget(0,0,HtmlUtils.getHtml("Sigma","prompt"));
        thrSigGrid.setWidget(0,1,HtmlUtils.getHtml("Min","prompt"));
        thrSigGrid.setWidget(0,2,HtmlUtils.getHtml("Max","prompt"));
        thrSigGrid.setWidget(1,0,HtmlUtils.getHtml("X","prompt"));
        thrSigGrid.setWidget(1,1,_sigmaXMinTextBox);
        thrSigGrid.setWidget(1,2,_sigmaXMaxTextBox);
        thrSigGrid.setWidget(2,0,HtmlUtils.getHtml("Y","prompt"));
        thrSigGrid.setWidget(2,1,_sigmaYMinTextBox);
        thrSigGrid.setWidget(2,2,_sigmaYMaxTextBox);
        thrSigGrid.setWidget(3,0,HtmlUtils.getHtml("Z","prompt"));
        thrSigGrid.setWidget(3,1,_sigmaZMinTextBox);
        thrSigGrid.setWidget(3,2,_sigmaZMaxTextBox);

        thresholdPanel.add(thrPosGrid);
        thresholdPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        thresholdPanel.add(thrSigGrid);
        _thresholdPanel.add(thresholdPanel);
    }

    protected void popuplateContentPanel() {
    }

//    private void setUpDiscoveryPanel() {
//        _discoveryPanel = new TitledBox("Scan For New Directories");
//        _imagePath = new TextBox();
//        _imagePath.setVisibleLength(60);
//        _imagePath.setText("");
//        _scanDirForResultsButton = new RoundedButton("Scan Directory", new ClickListener() {
//            @Override
//            public void onClick(Widget sender) {
//                _dataservice.findPotentialResultNodes(_imagePath.getText(), new AsyncCallback() {
//                    public void onFailure(Throwable throwable) {
//                        new PopupCenteredLauncher(new ErrorPopupPanel("There was a problem discovering the image directories."), 250).showPopup(_scanDirForResultsButton);
//                    }
//
//                    public void onSuccess(Object o) {
//                        // If successful, reload from scratch
//                        naaResultsPagingPanel.first();
//                        new PopupCenteredLauncher(new InfoPopupPanel("Scan completed successfully."), 250).showPopup(_scanDirForResultsButton);
//                    }
//                });
//            }
//        });
//        HorizontalPanel sourcePanel = new HorizontalPanel();
//        sourcePanel.add(HtmlUtils.getHtml("Image Directory", "prompt"));
//        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
//        sourcePanel.add(_imagePath);
//        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
//        sourcePanel.add(_scanDirForResultsButton);
//        _discoveryPanel.add(sourcePanel);
//    }

    private void setUpInputFileOrDirectoryPanel() {
        _inputFileOrDirPanel = new TitledBox("Input File Or Directory");
        _inputFileOrDirTextBox = new TextBox();
        _inputFileOrDirTextBox.setVisibleLength(90);
        _inputFileOrDirTextBox.setText("");
        _validateInputFileOrDirButton = getValidationButton(_inputFileOrDirTextBox.getText());
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Path: ", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_inputFileOrDirTextBox);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
//        sourcePanel.add(_validateInputFileOrDirButton);
        _inputFileOrDirPanel.add(sourcePanel);
    }

    private void setUpBorderCropPanel() {
        _borderCropPanel = new TitledBox("Crop Border Size");
        _borderCropPixelsTextBox = new TextBox();
        _borderCropPixelsTextBox.setVisibleLength(10);
        _borderCropPixelsTextBox.setText("");
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Pixels to Crop: ", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_borderCropPixelsTextBox);
        _borderCropPanel.add(sourcePanel);
    }

    private void setUpTransformationFilePanel() {
        _transformationFilePanel = new TitledBox("Transformation File");
        _transformationFileTextBox = new TextBox();
        _transformationFileTextBox.setVisibleLength(90);
        _transformationFileTextBox.setText("");
        _validateTransformationFileButton = getValidationButton(_transformationFileTextBox.getText());
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Path: ", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_transformationFileTextBox);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
//        sourcePanel.add(_validateTransformationFileButton);
        _transformationFilePanel.add(sourcePanel);
    }

    private void setUpIntensityCorrectionPanel() {
        _intensityCorrectionPanel = new TitledBox("Intensity Correction File");
        _intensityCorrectionMatrixFileTextBox = new TextBox();
        _intensityCorrectionMatrixFileTextBox.setVisibleLength(90);
        _intensityCorrectionMatrixFileTextBox.setText("");
        _validateIntensityCorrectionFileButton = getValidationButton(_intensityCorrectionMatrixFileTextBox.getText());
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Path: ", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_intensityCorrectionMatrixFileTextBox);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
//        sourcePanel.add(_validateIntensityCorrectionFileButton);
        _intensityCorrectionPanel.add(sourcePanel);
    }

    private void setUpMicroscopeSettingsPanel() {
        _microscopeSettingsPanel = new TitledBox("Microscope Settings File");
        _microscopeSettingsTextBox = new TextBox();
        _microscopeSettingsTextBox.setVisibleLength(90);
        _microscopeSettingsTextBox.setText("");
        _validateMicroscopeSettingsFileButton = getValidationButton(_microscopeSettingsTextBox.getText());
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Path: ", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_microscopeSettingsTextBox);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
//        sourcePanel.add(_validateMicroscopeSettingsFileButton);
        _microscopeSettingsPanel.add(sourcePanel);
    }

    private void setUpAvgReadNoisePanel() {
        _avgReadNoisePanel = new TitledBox("Average Read Noise Settings");
        _avgReadNoiseTextBox = new TextBox();
        _avgReadNoiseTextBox.setVisibleLength(90);
        _avgReadNoiseTextBox.setText("107.089,107.39,107.395,107.41,107.711,107.739,107.609,107.958,107.932");
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Path: ", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_avgReadNoiseTextBox);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        _avgReadNoisePanel.add(sourcePanel);
    }

    private void setUpAvgDarkPanel() {
        _avgDarkPanel = new TitledBox("Average Dark Settings");
        _avgDarkTextBox = new TextBox();
        _avgDarkTextBox.setVisibleLength(90);
        _avgDarkTextBox.setText("50,50,50,50,50,50,50,50,50");
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Path: ", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_avgDarkTextBox);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        _avgDarkPanel.add(sourcePanel);
    }

    private RoundedButton getValidationButton(String testFilePath) {
        _validationFilePath = testFilePath;
        return new RoundedButton("Validate Target", new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                _dataservice.validateFilePath(_validationFilePath, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("There was a problem validating the path."), 250).showPopup(_clearButton);
                    }

                    public void onSuccess(Object o) {
                        new PopupCenteredLauncher(new InfoPopupPanel("The file exists and is reachable."), 250).showPopup(_clearButton);
                    }
                });
            }
        });
    }

//    private void setupOraclePanel() {
//        // Create the oraclePanel and hook up callbacks that will repopulate the table
//        oraclePanel = new SearchOraclePanel("Plate Name", new SearchOracleListener() {
//            public void onRunSearch(String searchString) {
//                naaResultsPagingPanel.clear();
//                naaResultsPaginator.setSortColumns(new SortableColumn[]{
//                        new SortableColumn(NAA_DATE_COLUMN, NAA_DATE_HEADING, SortableColumn.SORT_ASC)
//                });
//                naaResultsPagingPanel.first();
//            }
//
//            public void onShowAll() {
//                naaResultsPagingPanel.clear();
//                naaResultsPaginator.setSortColumns(new SortableColumn[]{
//                        new SortableColumn(NAA_DATE_COLUMN, NAA_DATE_HEADING, SortableColumn.SORT_ASC)
//                });
//                naaResultsPagingPanel.first();
//            }
//        });
//        oraclePanel.addSuggestBoxStyleName("AdvancedBlastPreviousSequenceSuggestBox");
//    }

//    private void populateOracle() {
//        // Populate the oraclePanel with the node names
//        _dataservice.getNodeNamesForUserByName("org.janelia.it.jacs.model.user_data.tic.TICResultNode", new AsyncCallback() {
//            public void onFailure(Throwable caught) {
//                _logger.error("error retrieving naaResult logins for suggest oracle: " + caught.getMessage());
//            }
//
//            public void onSuccess(Object result) {
//                List<String> names = (List<String>) result;
//                oraclePanel.addAllOracleSuggestions(names);
//                haveData = true;
//                _logger.debug("populating SuggestOracle with " + (names == null ? 0 : names.size()) + " node names");
//            }
//        });
//    }

//    private void setupUserListPanel() {
//        resultsListPanel = new VerticalPanel();
//
//        naaResultsTable = new SortableTable();
//        naaResultsTable.setStyleName("SequenceTable");
////        naaResultsTable.addColumn(new ImageColumn("&nbsp;"));        // node delete icon
//        naaResultsTable.addColumn(new TextColumn(NAA_NAME_HEADING)); // must match column int above
//        naaResultsTable.addColumn(new TextColumn(NAA_PATH_HEADING)); // must match column int above
//        naaResultsTable.addColumn(new DateColumn(NAA_DATE_HEADING)); // must match column int above
//        naaResultsTable.addColumn(new TextColumn(NAA_PROCESSED_HEADING)); // must match column int above
//        naaResultsTable.addColumn(new TextColumn(NAA_ACTION_HEADING)); // must match column int above
//
//        naaResultsTable.addSelectionListener(new UserTableListener(naaResultsTable));
//        naaResultsTable.addDoubleClickSelectionListener(new UserDoubleClickListener(naaResultsTable));
//        naaResultsTable.setHighlightSelect(true);
//
//        // TODO: move to abstract RemotePagingPanelFactory
//        String[][] sortConstants = new String[][]{
//                {"", ""},
//                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING},
//                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING},
//                {UserDataNodeVO.SORT_BY_DATE_CREATED, NAA_DATE_HEADING},
//                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING},
//                {UserDataNodeVO.SORT_BY_NAME, NAA_NAME_HEADING}
//        };
//        naaResultsPaginator = new RemotingPaginator(naaResultsTable, new UserDataRetriever(), sortConstants, "NAABrowser");
//        naaResultsPagingPanel = new RemotePagingPanel(naaResultsTable, naaResultsPaginator, DEFAULT_NUM_ROWS_OPTIONS, DEFAULT_NUM_ROWS,
//                "NAABrowser");
//        naaResultsPaginator.setSortColumns(new SortableColumn[]{
//                new SortableColumn(NAA_NAME_COLUMN, NAA_NAME_HEADING, SortableColumn.SORT_ASC)
//        });
//        naaResultsPagingPanel.setNoDataMessage("No plates found.");
//
//        resultsListPanel.add(naaResultsPagingPanel);
//        resultsListPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
//        resultsListPanel.add(getTableSelectHint());
//    }
//
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
//    public class UserDataRetriever implements PagedDataRetriever {
//        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
//            _dataservice.getNumNodesForUserByName("org.janelia.it.jacs.model.user_data.tic.TICResultNode", new AsyncCallback() {
//                public void onFailure(Throwable caught) {
//                    _logger.error("UserDataRetriever.getNumNodesForUserByName().onFailure(): " + caught.getMessage());
//                    listener.onFailure(caught);
//                }
//
//                // On success, populate the table with the DataNodes received
//                public void onSuccess(Object result) {
//                    _logger.debug("UserDataRetriever.getNumNodesForUserByName().onSuccess() got " + result);
//                    listener.onSuccess(result); // Integer
//                }
//            });
//        }
//
//        public void retrieveDataRows(int startIndex,
//                                     int numRows,
//                                     SortArgument[] sortArgs,
//                                     final DataRetrievedListener listener) {
//            _dataservice.getPagedNodesForUserByName("org.janelia.it.jacs.model.user_data.tic.TICResultNode", startIndex, numRows, sortArgs, new AsyncCallback() {
//                public void onFailure(Throwable caught) {
//                    _logger.error("UserDataRetriever.retrieveDataRows().onFailure(): " + caught.getMessage());
//                    listener.onFailure(caught);
//                }
//
//                // On success, populate the table with the DataNodes received
//                public void onSuccess(Object result) {
//                    UserDataNodeVO[] users = (UserDataNodeVO[]) result;
//                    if (users == null || users.length == 0) {
//                        _logger.debug("UserDataRetriever.getPagedNodesForUserByName().onSuccess() got no data");
//                        listener.onNoData();
//                    } else {
//                        // TODO: move to PagingController
//                        _logger.debug("UserDataRetriever.getPagedNodesForUserByName().onSuccess() got data");
//                        listener.onSuccess(processData((UserDataNodeVO[]) result));
//                    }
//                }
//            });
//        }

//        private List processData(UserDataNodeVO[] naaResults) {
//            _logger.debug("UserDataNodeRetriever processing " + naaResults.length + " nodes");
//            List<TableRow> tableRows = new ArrayList<TableRow>();
//            for (UserDataNodeVO naaResult : naaResults) {
//                if (naaResult == null) // temporary until DAOs return real paged data
//                    continue;
//                TableRow tableRow = new TableRow();
//                String objectId = naaResult.getDatabaseObjectId().toString();
//                tableRow.setRowObject(objectId);
//                _logger.debug("Adding name=" + naaResult.getNodeName() + " for userId=" + objectId);
//                naaResultMap.put(objectId, naaResult);
//
////                tableRow.setValue(NAA_DELETE_COLUMN, new TableCell(null, createRemoveNodeWidget(naaResult, tableRow)));
//                tableRow.setValue(NAA_NAME_COLUMN,   new TableCell(naaResult.getNodeName()));
//                tableRow.setValue(NAA_PATH_COLUMN,   new TableCell(naaResult.getDatabaseObjectId()));
//                tableRow.setValue(NAA_DATE_COLUMN,   new TableCell(naaResult.getDateCreated()));
//                tableRow.setValue(NAA_PROCESSED_COLUMN,   new TableCell(naaResult.isProcessed()));
//                tableRow.setValue(NAA_ACTION_COLUMN,   new TableCell(naaResult.isProcessed()));
////                tableRow.setValue(NAA_ACTION_COLUMN,   new TableCell(null, getActionColumnWidget(naaResult)));
//
//                tableRows.add(tableRow);
//            }
//            haveData = true;
//            return tableRows;
//        }
//    }

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
//    private Widget createRemoveNodeWidget(UserDataNodeVO user, TableRow row) {
//        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
//        Image image = imageBundle.getDeleteImage().createImage();
//        image.addMouseListener(new HoverImageSetter(image, imageBundle.getDeleteImage(), imageBundle.getDeleteHoverImage()));
//        image.addClickListener(new RemovePlateEventHandler(user, row));
//        return image;
//    }

//    private class RemovePlateEventHandler implements ClickListener, RemoveNodeListener, PopupListener {
//        private UserDataNodeVO naaResult;
//        private TableRow row;
//        boolean inProgress;
//        RemoveNodePopup popup;
//
//
//        public RemovePlateEventHandler(UserDataNodeVO naaResult, TableRow row) {
//            this.naaResult = this.naaResult;
//            this.row = row;
//            inProgress = false;
//            popup = null;
//        }
//
//        public void onClick(Widget widget) {
//            startPopup(widget);
//        }
//
//        public void onPopupClosed(PopupPanel popupPanel, boolean b) {
//            inProgress = false;
//        }
//
////        public void removeNode(final String nodeId) {
////            AsyncCallback removeUserCallback = new AsyncCallback() {
////
////                public void onFailure(Throwable caught) {
////                    _logger.error("Remove naaResult" + nodeId + " failed", caught);
////                    finishedPopup();
////                }
////
////                public void onSuccess(Object result) {
////                    naaResultsPagingPanel.removeRow(row);
////                    _logger.debug("Remove naaResult succeded");
////                    SystemWebTracker.trackActivity("DeleteUserNode");
////                    finishedPopup();
////                }
////
////            };
////            _dataservice.deleteNode(nodeId, removeUserCallback);
////        }
//
//        private void startPopup(Widget widget) {
//            if (!inProgress) {
//                inProgress = true;
//                popup = new RemoveNodePopup(naaResult.getNodeName(), naaResult.getDatabaseObjectId(), this, false);
//                popup.addPopupListener(this);
//                PopupLauncher launcher = new PopupAboveLauncher(popup);
//                launcher.showPopup(widget);
//            }
//        }
//
//        private void finishedPopup() {
//            if (popup != null) {
//                popup.hide();
//            }
//            popup = null;
//        }
//
//    }

//    public class UserTableListener implements SelectionListener {
//        private SortableTable targetTable;
//
//        public UserTableListener(SortableTable targetTable) {
//            this.targetTable = targetTable;
//        }
//
//        // This callback method is called by SortableTable within its 'onSelect' method when a row has been selected
//        public void onSelect(String rowString) {
//            onPreviousSelect(rowString, targetTable);
//            _submitButton.setEnabled(true);
//        }
//
//        // This callback method is called by SortableTable within its 'onSelect' method when the selected row has been unselected
//        public void onUnSelect(String rowString) {
//            _logger.info("User unselected row " + rowString);
//            // Clear any hover attribute
//            if (null!=naaResultsTable.getSelectedRow()) {
//                _submitButton.setEnabled(true);
//            }
//            else {
//                _submitButton.setEnabled(false);
//            }
//            naaResultsTable.clearHover();
//        }
//    }

//    public class UserDoubleClickListener implements DoubleClickSelectionListener {
//        private SortableTable targetTable;
//
//        public UserDoubleClickListener(SortableTable targetTable) {
//            this.targetTable = targetTable;
//        }
//
//        public void onSelect(String rowString) {
//            if (onPreviousSelect(rowString, targetTable)) {
//                new PopupCenteredLauncher(new InfoPopupPanel("Got the double click for row "+rowString), 250).showPopup(_scanDirForResultsButton);
//                _submitButton.setEnabled(true);
//            }
//        }
//    }

//    private boolean onPreviousSelect(String rowString, SortableTable targetTable) {
//        int row = Integer.valueOf(rowString);
//        TableRow selectedRow = (targetTable.getTableRows().get(row - 1)); // getTableRows() is data rows only, so we use -1 offset
//        String targetId = (String) selectedRow.getRowObject();
//        if (null == targetId || "".equals(targetId) || targetTable.getText(1, 0).indexOf("No Data") > 0) {
//            return false;
//        }
//        String naaResultName = naaResultMap.get(targetId).getNodeName();
//        if (naaResultName == null) {
//            _logger.error("Could not find naaResult name in naaResultMap, parsing from table instead");
////            userName=targetTable.getText(row, USER_LOGIN_COLUMN);
//        }
//        else {
//            _logger.debug("On select, retrieved userName=" + naaResultName + " for targetId=" + targetId);
//        }
//
//        naaResultsTable.clearHover();
//
//        return true;
//    }
//
//    protected Widget getTableSelectHint() {
//        return HtmlUtils.getHtml("&bull;&nbsp;Click a row to select.&nbsp;&nbsp;&bull;&nbsp;Double click to select and apply.", "hint");
//    }

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
//        _submitButton.setEnabled(false);
    }

    private Widget getSubmitButtonPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_clearButton);
        panel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        panel.add(_submitButton);

        return new CenteredWidgetHorizontalPanel(panel);
    }

    private void submitJob() {
        // Validate Information.  If any of the validations fail then leave
        boolean inputFileGood       = checkValue("Input File Or Directory",_inputFileOrDirTextBox.getText());
        if (!inputFileGood) { return; }

        boolean intensityFileGood   = checkValue("Intensity Correction File", _intensityCorrectionMatrixFileTextBox.getText());
        if (!intensityFileGood) { return; }

        boolean microscopeFileGood  = checkValue("Microscope Settings File", _microscopeSettingsTextBox.getText());
        if (!microscopeFileGood) { return; }

        boolean transformFileGood   = checkValue("Transformation File", _transformationFileTextBox.getText());
        if (!transformFileGood) { return; }

        // submit the job
        _ticTask = new BatchTicTask();
//        String nodeId = "Test";//naaResultsTable.getValue(naaResultsTable.getSelectedRow().getRowIndex(), 0).toString();
        _ticTask.setJobName("TIC Job");
        _ticTask.setParameter(BatchTicTask.PARAM_inputFile, _inputFileOrDirTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_borderValue, _borderCropPixelsTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_transformationFile, _transformationFileTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_intensityCorrectionFactorFile, _intensityCorrectionMatrixFileTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_microscopeSettingsFile, _microscopeSettingsTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_runApplyCalibrationToFrame, _applyCalibrationCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_runIlluminationCorrection, _illuminationCorrectionCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_runFQBatch, _fqbatchCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_spotDataOnly, _spotsOnlyCheckBox.getValue().toString());
        _ticTask.setParameter(BatchTicTask.PARAM_avgReadNoise, _avgReadNoiseTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_avgDark, _avgDarkTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionXMin, _positionXMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionXMax, _positionXMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionYMin, _positionYMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionYMax, _positionYMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionZMin, _positionZMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_positionZMax, _positionZMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaXMin, _sigmaXMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaXMax, _sigmaXMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaYMin, _sigmaYMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaYMax, _sigmaYMaxTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaZMin, _sigmaZMinTextBox.getText());
        _ticTask.setParameter(BatchTicTask.PARAM_sigmaZMax, _sigmaZMaxTextBox.getText());
//        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();

        new SubmitJob(_ticTask, new JobSubmissionListener()).runJob();
    }

    private boolean checkValue(String valueName, String text) {
        if (null==text || "".equals(text)) {
            new PopupCenteredLauncher(new ErrorPopupPanel(valueName+" cannot be empty."), 250).showPopup(_submitButton);
            return false;
        }
        String testTxt = text.trim();
        if (testTxt.contains(" ")) {
            new PopupCenteredLauncher(new ErrorPopupPanel(valueName+" cannot contain spaces.  Please fix."), 250).showPopup(_submitButton);
            return false;
        }
        return true;
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable caught) {
//            _submitButton.setEnabled(true);
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
//            _submitButton.setEnabled(true);
            _statusMessage.showSuccessMessage();
            _listener.onSuccess(jobId);
        }
    }


}
