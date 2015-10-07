
package org.janelia.it.jacs.web.gwt.eukAnnot.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.eukAnnotation.EukaryoticAnnotationTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
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
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;

/**
 * @author Todd Safford
 */
public class EukAnnotationPanel extends Composite {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.eukAnnot.client.EukAnnotationPanel");
    private TitledBox _mainPanel;
    private TextBox _jobNameTextBox;
    private TextBox _projectCodeTextBox;
    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;

    private PulldownPopup _localDirectoryPulldownPopup;
    private PulldownPopup _ftpDirectoryPulldownPopup;

//    private AP16SFragmentChooserPopup _fragmentChooserPopup;

    //    private AnalysisPipeline16STaskOptionsPanel _optionsPanel;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;
    private EukaryoticAnnotationTask eukAnnotationTask = new EukaryoticAnnotationTask();
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public EukAnnotationPanel(String title, String taskId, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title, taskId);
    }

    private void init(String title, String taskId) {
        _mainPanel = new TitledBox(title);
        _mainPanel.removeActionLinks();
        initWidget(_mainPanel);

        // Prepopulate if a taskId was supplied
        if (StringUtils.hasValue(taskId)) {
            setJob(taskId);
        } // will populateContentPanel() after loading data
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();
//        _optionsPanel = new AnalysisPipeline16STaskOptionsPanel();
//        _optionsPanel.setStyleName("AdvancedBlastProgramOptionsPanel");
//        _optionsPanel.displayParams(eukAnnotationTask);

        _projectCodeTextBox = new TextBox();
        _projectCodeTextBox.setVisibleLength(10);

        Grid grid = new Grid(4, 2);
        grid.setCellSpacing(3);

        grid.setWidget(0, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(0, 1, getJobNameWidget());

        grid.setWidget(1, 0, HtmlUtils.getHtml("Local Organism Directory:", "prompt"));
        grid.setWidget(1, 1, getInitializedPulldownPopup(_localDirectoryPulldownPopup));

        grid.setWidget(2, 0, HtmlUtils.getHtml("NCBI Data Location :", "prompt"));
        grid.setWidget(2, 1, getInitializedPulldownPopup(_ftpDirectoryPulldownPopup));

        grid.setWidget(3, 0, HtmlUtils.getHtml("Project Code:", "prompt"));
        grid.setWidget(3, 1, _projectCodeTextBox);

        createSubmitButton();

        contentPanel.add(HtmlUtils.getHtml("Please define the parameters for the organism.", "prompt"));
        contentPanel.add(grid);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
//        contentPanel.add(_optionsPanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getStatusMessage());

        _mainPanel.add(contentPanel);

        // Enable or disable the program menu and options as appropriate
        updateDisabledFields();
    }

    private Widget getJobNameWidget() {
        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);
        updateJobNameWidget();

        return _jobNameTextBox;
    }

    private Widget getInitializedPulldownPopup(PulldownPopup targetPulldownPopup) {
//        _fragmentChooserPopup = new AP16SFragmentChooserPopup(new SequenceSelectedListener());
        targetPulldownPopup = new PulldownPopup(null);
        targetPulldownPopup.setLauncher(new PopupAtRelativePixelLauncher(-100, 0));

        final HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        panel.add(targetPulldownPopup);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        return panel;
    }

    private void updateJobNameWidget() {
        _jobNameTextBox.setText("My Eukaryotic Pipeline job " + new FormattedDate().toString());
    }

    private void updateQuerySequenceWidget(String value) {
        if (null == value && "".equals(value)) {
//            _fragmentPulldownPopup.setDefaultText();
        }
        else {
//            Window.alert("Setting query to "+value);
//            _fragmentPulldownPopup.setText(value);
//            eukAnnotationTask.setParameter(AnalysisPipeline16sTask.PARAM_fragmentFiles, _fragmentChooserPopup.getSelectedNodeId());
        }
    }

    private void updateReferenceDatasetWidget() {
//        _referenceDatasetsListBox.setSelectedIndex(0);
//        setSubjectDBInfo();
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
        // Clear the Query Sequence PulldownPopup and any selections in the popup
//        _fragmentPulldownPopup.setHoverPopup(null);
//        _fragmentChooserPopup.clear();

        // Clear the Reference Dataset PulldownPopup and any selections in the popup
//        _referenceDatasetsListBox.setSelectedIndex(0);

        // Update all the GUI widgets with the new values
        updateAll();
    }

    private void submitJob() {
        // Validate job name
        if (!StringUtils.hasValue(_jobNameTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A job name is required.")).showPopup(null);
        }
        else { // submit the job
//            eukAnnotationTask.setJobName(_jobNameTextBox.getText());
//            eukAnnotationTask.setParameter(Task.PARAM_project, _projectCodeTextBox.getText());
//            _submitButton.setEnabled(false);
              //showSubmittingMessage();
//            new SubmitJob(eukAnnotationTask, new JobSubmissionListener(), new UploadListener()).runJob();
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
        }
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

    private Widget getStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");
        //_statusMessage.setVisible(false);

        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

    private class SequenceSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            updateQuerySequenceWidget(value);
            updateDisabledFields();
        }

        public void onUnSelect(String value) {
        }
    }

    private void updateDisabledFields() {
        if (shouldShowDisabledFields()) {
            _submitButton.setEnabled(true);
//            _optionsPanel.setVisible(true);
        }
        else {
//            _optionsPanel.setVisible(false);
            _submitButton.setEnabled(false);
        }
    }

    private boolean shouldShowDisabledFields() {
        return true;
    }

    private void updateAll() {
        updateJobNameWidget();
        updateQuerySequenceWidget(null);
        updateReferenceDatasetWidget();
        updateDisabledFields();
    }

    public void setJob(String taskId) {
//        _blastData.setTaskIdFromParam(taskId);
//        populateBlastDataFromTaskId();
    }

}