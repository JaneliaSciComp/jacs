
package org.janelia.it.jacs.web.gwt.reversePsiBlast.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.web.gwt.blast.client.BlastService;
import org.janelia.it.jacs.web.gwt.blast.client.BlastServiceAsync;
import org.janelia.it.jacs.web.gwt.blast.client.popup.QuerySequenceInfoPopup;
import org.janelia.it.jacs.web.gwt.blast.client.submit.SubmitBlastJob;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.PulldownPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.reversePsiBlast.client.popups.ReversePsiBlastQuerySequenceChooserPopup;

import java.util.HashMap;
import java.util.Set;

/**
 * @author Michael Press
 */
public class ReversePsiBlastPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.reversePsiBlast.client.ReversePsiBlastPanel");

    private static final String BLAST_HELP_LINK_PROP = "Blast.HelpURL";
    private static final String QUERY_SEQUENCE_HELP_PROPERTY = "BlastSequence.HelpURL";
    private static final String REFERENCE_DATASET_HELP_PROPERTY = "BlastSubject.HelpURL";

    private TitledBox _mainPanel;
    private BlastData _blastData;
    private TextBox _jobNameTextBox;
    private boolean _projectCodeRequired;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();
    private ProjectCodePanel _projectCodePanel;

    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;

    private PulldownPopup _querySequencePulldownPopup;
    private ReversePsiBlastQuerySequenceChooserPopup _querySequenceChooserPopup;

    private ListBox _referenceDatasetsListBox;

    private ReversePsiBlastTaskOptionsPanel _optionsPanel;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;
    private HashMap<String, BlastableNodeVO> datasetMap = new HashMap<String, BlastableNodeVO>();

    private static BlastServiceAsync _blastservice = (BlastServiceAsync) GWT.create(BlastService.class);
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);


    static {
        ((ServiceDefTarget) _blastservice).setServiceEntryPoint("blast.srv");
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public ReversePsiBlastPanel(String title, String taskId, String datasetId, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title, taskId, datasetId);
    }

    private void init(String title, String taskId, String datasetId) {
        _projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);

        _blastData = new BlastData();
        _blastData.setBlastTask(new ReversePsiBlastTask());
        _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(_mainPanel, new HelpActionLink("help"), BLAST_HELP_LINK_PROP);
        initWidget(_mainPanel);

        // Prepopulate if a taskId was supplied
        if (StringUtils.hasValue(taskId)) {
            setJob(taskId);
        } // will populateContentPanel() after loading data
        else if (StringUtils.hasValue(datasetId)) {
            setDataset(datasetId);
        } // will populateContentPanel() after loading data

        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();
        _optionsPanel = new ReversePsiBlastTaskOptionsPanel();
        _optionsPanel.setStyleName("AdvancedBlastProgramOptionsPanel");
        _optionsPanel.displayParams(_blastData.getBlastTask());

        Grid grid = new Grid((_projectCodeRequired) ? 6 : 5, 2);
        grid.setCellSpacing(3);

        grid.setWidget(0, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(0, 1, getJobNameWidget());

        grid.setWidget(1, 0, HtmlUtils.getHtml("Query Sequence:", "prompt"));
        grid.setWidget(1, 1, getQuerySequenceWidget());

        grid.setWidget(2, 0, HtmlUtils.getHtml("Reference Dataset:", "prompt"));
        grid.setWidget(2, 1, getReferenceDatasetWidget());

        createSubmitButton();
        grid.setWidget(3, 0, HtmlUtils.getHtml("Program:", "prompt"));
        grid.setWidget(3, 1, HtmlUtils.getHtml(_blastData.getBlastTask().getTaskName(), "prompt"));

        grid.setWidget(4, 0, HtmlUtils.getHtml("Output Destination:", "prompt"));
        grid.setWidget(4, 1, _finalOutputPanel);

        if (_projectCodeRequired) {
            _projectCodePanel = new ProjectCodePanel(_blastData);
            grid.setWidget(5, 0, HtmlUtils.getHtml("Project code:", "prompt"));
            grid.setWidget(5, 1, _projectCodePanel);
        }

        contentPanel.add(grid);
        contentPanel.add(_optionsPanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getStatusMessage());

        _mainPanel.add(contentPanel);

        // Enable or disable the program menu and options as appropriate
        updateDisabledFields();
    }

    private void addHelpIcon(final Panel panel, final String propertyName) {
        String url = SystemProps.getString(propertyName, null);
        if (url != null)
            panel.add(new HelpActionLink("", url));
        else
            _logger.error("Failed to retrieve " + propertyName + " property; can't get URL");
    }

    private Widget getJobNameWidget() {
        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);
        updateJobNameWidget();

        return _jobNameTextBox;
    }

    private boolean isPrePopulatedFromTaskId() {
        return StringUtils.hasValue(_blastData.getTaskIdFromParam()) && _blastData.getBlastTask() != null;
    }

    private Widget getQuerySequenceWidget() {
        _querySequenceChooserPopup = new ReversePsiBlastQuerySequenceChooserPopup(new SequenceSelectedListener(), _blastData);
        _querySequencePulldownPopup = new PulldownPopup(_querySequenceChooserPopup);
        _querySequencePulldownPopup.setLauncher(new PopupAtRelativePixelLauncher(-100, 0));

        if (isPrePopulatedFromTaskId())
            updateQuerySequenceWidget();

        final HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        panel.add(_querySequencePulldownPopup);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        addHelpIcon(panel, QUERY_SEQUENCE_HELP_PROPERTY);

        return panel;
    }

    private Widget getReferenceDatasetWidget() {
        _referenceDatasetsListBox = new ListBox();
        _referenceDatasetsListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                setSubjectDBInfo();
            }
        });
        _dataservice.getReversePsiBlastDatasets(new AsyncCallback() {
            // Failure is not an option
            public void onFailure(Throwable caught) {
            }

            public void onSuccess(Object result) {
                BlastableNodeVO[] tmpDatasets = (BlastableNodeVO[]) result;
                for (BlastableNodeVO tmpDataset : tmpDatasets) {
                    datasetMap.put(tmpDataset.getNodeName(), tmpDataset);
                }
                for (BlastableNodeVO tmpDataset : tmpDatasets) {
                    _referenceDatasetsListBox.addItem(tmpDataset.getNodeName());
                }
                setSubjectDBInfo();

            }
        });

        if (isPrePopulatedFromTaskId())
            updateReferenceDatasetWidget();

        final HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        panel.add(_referenceDatasetsListBox);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        return panel;
    }

    private void setSubjectDBInfo() {
        String selection = _referenceDatasetsListBox.getItemText(_referenceDatasetsListBox.getSelectedIndex());
        BlastableNodeVO selectedNode = datasetMap.get(selection);
        HashMap<String, BlastableNodeVO> tmpMap = new HashMap<String, BlastableNodeVO>();
        tmpMap.put(selectedNode.getNodeName(), selectedNode);
        _blastData.setSubjectSequenceDataNodeMap(tmpMap);
    }

    private void updateJobNameWidget() {
        if (isPrePopulatedFromTaskId() && _blastData.getBlastTask() != null)
            _jobNameTextBox.setText(_blastData.getBlastTask().getJobName());
        else
            _jobNameTextBox.setText("My Reverse PSI-BLAST job " + new FormattedDate().toString());
    }

    private void updateQuerySequenceWidget() {
        if (StringUtils.hasValue(_blastData.getMostRecentlySpecifiedQuerySequenceName())) {
            _querySequencePulldownPopup.setText(_blastData.getMostRecentlySpecifiedQuerySequenceName());
            _querySequencePulldownPopup.setHoverPopup(new QuerySequenceInfoPopup(_blastData));
        }
        else
            _querySequencePulldownPopup.setDefaultText();
    }

    private void updateReferenceDatasetWidget() {
        if (_blastData.getSubjectSequenceDataNodeMap() != null && _blastData.getSubjectSequenceDataNodeMap().size() > 0) {
            BlastableNodeVO subjNode = _blastData.getSubjectSequenceDataNodeMap().values().iterator().next();
            String targetDB = subjNode.getNodeName();
            for (int i = 0; i < _referenceDatasetsListBox.getItemCount(); i++) {
                if (targetDB.equals(_referenceDatasetsListBox.getItemText(i))) {
                    _referenceDatasetsListBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        else {
            _referenceDatasetsListBox.setSelectedIndex(0);
            setSubjectDBInfo();
        }
    }

    private void createSubmitButton() {
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

    private void clear() {
        _blastData = new BlastData();

        // Clear the Query Sequence PulldownPopup and any selections in the popup
        _querySequencePulldownPopup.setHoverPopup(null);
        _querySequenceChooserPopup.setBlastData(_blastData);
        _querySequenceChooserPopup.clear();

        // Clear the Reference Dataset PulldownPopup and any selections in the popup
        _referenceDatasetsListBox.setSelectedIndex(0);

        // Update all the GUI widgets with the new values
        updateAll();
    }

    private void submitJob() {
        // Validate job name
        if (!StringUtils.hasValue(_jobNameTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A job name is required.")).showPopup(null);
        }
        else { // submit the job
            _blastData.getBlastTask().setJobName(_jobNameTextBox.getText());
            if (_projectCodeRequired) {
                String projectCode = _projectCodePanel.getProjectCode();
                if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                    new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                    return;
                }
                _blastData.getBlastTask().setParameter(Task.PARAM_project, projectCode);
            }
            if (_finalOutputPanel.overrideFinalOutputPath()) {
                _blastData.getBlastTask().setParameter(Task.PARAM_finalOutputDirectory, _finalOutputPanel.getFinalOutputDestination());
            }
            _submitButton.setEnabled(false);
            _statusMessage.showSubmittingMessage();
            new SubmitBlastJob(_blastData, new JobSubmissionListener(), new UploadListener()).runJob();
        }
    }

    /**
     * Notification that a file was uploaded as part of executing a BLAST job.  Put the node (that was created
     * for the file contents) in the BlastData as the query sequence node as if it was a previous sequence (so we
     * don't try to upload it again).
     */
    private class UploadListener implements AsyncCallback {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(Object result) {
            _logger.info("User file was was uploaded, adding info to BlastData");
            UserDataNodeVO node = (UserDataNodeVO) result;
            _blastData.getQuerySequenceDataNodeMap().clear();
            _blastData.getQuerySequenceDataNodeMap().put(node.getDatabaseObjectId(), node);
            _blastData.setMostRecentlySpecifiedQuerySequenceName(node.getNodeName());
        }
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable caught) {
            _statusMessage.showFailureMessage();
            _submitButton.setEnabled(true);
        }

        public void onSuccess(String jobId) {
            _statusMessage.showSuccessMessage();
            _submitButton.setEnabled(true);
            _listener.onSuccess(jobId);
        }
    }

    private Widget getStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");
        //_statusMessage.setVisible(false);

        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

    private class SequenceSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            updateQuerySequenceWidget();
            updateDisabledFields();
        }

        public void onUnSelect(String value) {
        }
    }

    private void updateDisabledFields() {
        if (shouldShowDisabledFields()) {
            _submitButton.setEnabled(true);
            _optionsPanel.setVisible(true);
        }
        else {
            _optionsPanel.setVisible(false);
            _submitButton.setEnabled(false);
        }
    }

    private boolean shouldShowDisabledFields() {
        return StringUtils.hasValue(_blastData.getMostRecentlySelectedQuerySequenceType());
    }

    private void populateBlastDataFromTaskId() {
        String taskId = _blastData.getTaskIdFromParam().trim();
        _logger.error("ReversePsiBlastPanel populating from taskId=" + taskId);

        _blastservice.getPrepopulatedBlastTask(taskId, new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("Error retrieving job " + _blastData.getTaskIdFromParam(), caught);

                //init the GUI (in default mode) and then notify the user of the error
                clear();
                ErrorPopupPanel popup = new ErrorPopupPanel("Error retrieving job " + _blastData.getTaskIdFromParam());
                new PopupCenteredLauncher(popup, 250).showPopup(null);
            }

            public void onSuccess(Object result) {
                BlastTaskVO blastTaskVO = (BlastTaskVO) result;
                _blastData.setBlastTask(blastTaskVO.getBlastTask());
                _blastData.setMostRecentlySelectedQuerySequenceType(blastTaskVO.getQueryType());
                _blastData.setMostRecentlySelectedSubjectSequenceType(blastTaskVO.getSubjectType());

                // Populate query node
                UserDataNodeVO queryNodeVO = blastTaskVO.getQueryNodeVO();
                _blastData.getQuerySequenceDataNodeMap().clear();
                _blastData.getQuerySequenceDataNodeMap().put(queryNodeVO.getDatabaseObjectId(), queryNodeVO);
                _blastData.setMostRecentlySpecifiedQuerySequenceName(queryNodeVO.getNodeName());

                // Populate subject database node
                _blastData.getSubjectSequenceDataNodeMap().clear();
                Set<BlastableNodeVO> subjectNodes = blastTaskVO.getSubjectNodeVOs();
                if (subjectNodes != null && subjectNodes.size() > 0) {
                    BlastableNodeVO subjectNodeVO = subjectNodes.iterator().next();
                    _blastData.getSubjectSequenceDataNodeMap().put(subjectNodeVO.getDatabaseObjectId(), subjectNodeVO);
                }

                // Update all the GUI widgets with the new values
                updateAll();
            }
        });
    }

    private void populateBlastDataFromDataset(String datasetId) {
        _blastservice.getBlastableSubjectSetByNodeId(datasetId, new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("Error retrieving subject database " + _blastData.getDatasetFromParam(), caught);

                //init the GUI (in default mode) and then notify the user of the error
                clear();
                ErrorPopupPanel popup = new ErrorPopupPanel("Error retrieving dataset " + _blastData.getDatasetFromParam());
                new PopupCenteredLauncher(popup, 250).showPopup(null);
            }

            public void onSuccess(Object result) {
                BlastableNodeVO node = (BlastableNodeVO) result;
                if (node == null) {
                    _logger.error("Retrieved null BlastableNodeVO for supposedly prepopulated dataset");
                    return;
                }

                // Populate subject database node and type
                _blastData.getSubjectSequenceDataNodeMap().clear();
                _blastData.getSubjectSequenceDataNodeMap().put(node.getDatabaseObjectId(), node);
                _blastData.setMostRecentlySelectedSubjectSequenceType(node.getSequenceType());

                // Update GUI widget with the new values
                updateReferenceDatasetWidget();
            }
        });
    }

    private void updateAll() {
        updateJobNameWidget();
        updateQuerySequenceWidget();
        updateReferenceDatasetWidget();
        updateDisabledFields();
    }

    public void setJob(String taskId) {
        _blastData.setTaskIdFromParam(taskId);
        populateBlastDataFromTaskId();
    }

    private void setDataset(String datasetId) {
        _blastData.setDatasetFromParam(datasetId);
        populateBlastDataFromDataset(datasetId);
    }
}