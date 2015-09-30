
package org.janelia.it.jacs.web.gwt.psiBlast.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.web.gwt.blast.client.BlastService;
import org.janelia.it.jacs.web.gwt.blast.client.BlastServiceAsync;
import org.janelia.it.jacs.web.gwt.blast.client.popup.BlastableNodeInfoPopup;
import org.janelia.it.jacs.web.gwt.blast.client.popup.QuerySequenceInfoPopup;
import org.janelia.it.jacs.web.gwt.blast.client.submit.SubmitBlastJob;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
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
import org.janelia.it.jacs.web.gwt.psiBlast.client.popup.PsiBlastQuerySequenceChooserPopup;
import org.janelia.it.jacs.web.gwt.psiBlast.client.popup.PsiBlastReferenceDatasetChooserPopup;

import java.util.Set;

/**
 * @author Michael Press
 */
public class PsiBlastPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.advancedblast.client.AdvancedBlastPanel");

    private static final String BLAST_HELP_LINK_PROP = "Blast.HelpURL";
    private static final String QUERY_SEQUENCE_HELP_PROPERTY = "BlastSequence.HelpURL";
    private static final String REFERENCE_DATASET_HELP_PROPERTY = "BlastSubject.HelpURL";
    private static final String BLAST_PROGRAM_HELP_PROPERTY = "BlastOptions.HelpURL";

    private TitledBox _mainPanel;
    private BlastData _blastData;
    private TextBox _jobNameTextBox;
    private boolean _projectCodeRequired;
    private ProjectCodePanel _projectCodePanel;

    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;

    private PulldownPopup _querySequencePulldownPopup;
    private PsiBlastQuerySequenceChooserPopup _querySequenceChooserPopup;

    private PulldownPopup _referenceDatasetPulldownPopup;
    private PsiBlastReferenceDatasetChooserPopup _referenceDatasetChooserPopup;

    private ListBox _programMenu;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();
    private PsiBlastOptionsPanel _optionsPanel;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;

    private static BlastServiceAsync _blastservice = (BlastServiceAsync) GWT.create(BlastService.class);

    static {
        ((ServiceDefTarget) _blastservice).setServiceEntryPoint("blast.srv");
    }

    public PsiBlastPanel(String title, String taskId, String datasetId, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title, taskId, datasetId);
    }

    private void init(String title, String taskId, String datasetId) {
        _projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);

        _blastData = new BlastData();

        _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(_mainPanel, new HelpActionLink("help"), BLAST_HELP_LINK_PROP);
        initWidget(_mainPanel);

        // Prepopulate if a taskId was supplied
        if (StringUtils.hasValue(taskId))
            setJob(taskId); // will populateContentPanel() after loading data
        else if (StringUtils.hasValue(datasetId))
            setDataset(datasetId); // will populateContentPanel() after loading data

        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        Grid grid = new Grid((_projectCodeRequired) ? 6 : 5, 2);
        grid.setCellSpacing(3);

        grid.setWidget(0, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(0, 1, getJobNameWidget());

        grid.setWidget(1, 0, HtmlUtils.getHtml("Query Sequence:", "prompt"));
        grid.setWidget(1, 1, getQuerySequenceWidget());

        grid.setWidget(2, 0, HtmlUtils.getHtml("Reference Dataset:", "prompt"));
        grid.setWidget(2, 1, getReferenceDatasetWidget());

        createSubmitButton();
        createProgramPanel();
        grid.setWidget(3, 0, HtmlUtils.getHtml("Program:", "prompt"));
        grid.setWidget(3, 1, getProgramMenuPanel());

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

    private Widget getProgramMenuPanel() {
        _programMenu = _optionsPanel.getProgramMenu();

        // Have to put help icon in its own panel since its asynchronous and may get added at any time
        final SimplePanel helpIconPanel = new SimplePanel();
        addHelpIcon(helpIconPanel, BLAST_PROGRAM_HELP_PROPERTY);

        HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        panel.add(_programMenu);
        panel.add(HtmlUtils.getHtml("&nbsp;", "text"));
        panel.add(helpIconPanel);
        panel.add(HtmlUtils.getHtml("&nbsp;", "text"));

        // Enable or disable the program menu as appropriate
        updateDisabledFields();

        return panel;
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
        _querySequenceChooserPopup = new PsiBlastQuerySequenceChooserPopup(new SequenceSelectedListener(), _blastData);
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
        _referenceDatasetChooserPopup = new PsiBlastReferenceDatasetChooserPopup(new ReferenceDatasetSelectedListener(), _blastData);
        _referenceDatasetPulldownPopup = new PulldownPopup(_referenceDatasetChooserPopup);
        _referenceDatasetPulldownPopup.setLauncher(new PopupAtRelativePixelLauncher(-130, 0));

        if (isPrePopulatedFromTaskId())
            updateReferenceDatasetWidget();

        final HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        panel.add(_referenceDatasetPulldownPopup);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        addHelpIcon(panel, REFERENCE_DATASET_HELP_PROPERTY);

        return panel;
    }

    private void updateJobNameWidget() {
        if (isPrePopulatedFromTaskId() && _blastData.getBlastTask() != null)
            _jobNameTextBox.setText(_blastData.getBlastTask().getJobName());
        else
            _jobNameTextBox.setText("My PSI-BLAST job " + new FormattedDate().toString());
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
            _referenceDatasetPulldownPopup.setText(subjNode.getNodeName());
            _referenceDatasetPulldownPopup.setHoverPopup(new BlastableNodeInfoPopup(subjNode, /*realize now*/true));
        }
        else
            _referenceDatasetPulldownPopup.setDefaultText();
    }

    private void createProgramPanel() {
        _optionsPanel = new PsiBlastOptionsPanel(_blastData);
        _optionsPanel.setStyleName("AdvancedBlastProgramOptionsPanel");
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
        _referenceDatasetPulldownPopup.setHoverPopup(null);
        _referenceDatasetChooserPopup.setBlastData(_blastData);
        _referenceDatasetChooserPopup.clear();

        // Don't have to clear the options panel since it'll get grayed out and repopulated after user makes choices
        _optionsPanel.setBlastData(_blastData);

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

    private class ReferenceDatasetSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            updateReferenceDatasetWidget();
            updateDisabledFields();
        }

        public void onUnSelect(String value) {
        }
    }

    private void updateDisabledFields() {
        if (shouldShowDisabledFields()) {
            updateProgramMenu();
            _submitButton.setEnabled(true);
        }
        else {
            _programMenu.addItem("PSI-BLAST (prot/prot)"); // fake, but menu is disabled and will be populated for real when enabled
            _programMenu.setEnabled(false);
            _optionsPanel.setVisible(false);
            _submitButton.setEnabled(false);
        }
    }

    private boolean shouldShowDisabledFields() {
        return (StringUtils.hasValue(_blastData.getMostRecentlySelectedQuerySequenceType()) &&
                StringUtils.hasValue(_blastData.getMostRecentlySelectedSubjectSequenceType()));
    }

    /**
     * Enable the program menu and blast options if a query and reference have been chosen.  Have to do this on
     * a deferred command so that the browser finishes processing the popup hide() before showing the new stuff.
     */
    private void updateProgramMenu() {
        _programMenu.clear();
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                // Update the program menu, then clear the loading label
                _optionsPanel.updateBlastPrograms();

                // Enable the program menu, options panel and submit button
                _programMenu.setEnabled(true);
                _optionsPanel.setVisible(true);
            }
        });
    }

    private void populateBlastDataFromTaskId() {
        String taskId = _blastData.getTaskIdFromParam().trim();
        _logger.error("AdvancedBlastPanel populating from taskId=" + taskId);

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

    public void setDataset(String datasetId) {
        _blastData.setDatasetFromParam(datasetId);
        populateBlastDataFromDataset(datasetId);
    }
}