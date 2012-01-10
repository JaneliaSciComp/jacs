package org.janelia.it.jacs.web.gwt.geci.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.geci.NeuronalAssayAnalysisTask;
import org.janelia.it.jacs.model.user_data.geci.GeciImageDirectoryVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.FinalOutputDestinationPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.ProjectCodePanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

import java.util.ArrayList;
import java.util.List;

public class NeuronalAssayAnalysisPanel extends Composite {
    private FlexTable _geciResultsTable;
    private TitledBox _mainPanel;
    private TextBox _jobNameTextBox;
    private TextBox _geciImagePath;
    private ListBox _geciFRETList;
    private RoundedButton _scanDirForResultsButton;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;

    // Input file(s)
    //final FormPanel _uploadFileForm = new FormPanel();
    private List<GeciImageDirectoryVO> _geciImageDirs;

    private boolean projectCodeRequired = false;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();
    private ProjectCodePanel _projectCodePanel;
    private NeuronalAssayAnalysisTask _neuronalAssayAnalysisTask = new NeuronalAssayAnalysisTask();
    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);


    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public NeuronalAssayAnalysisPanel(String title, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title);
    }

    private void init(String title) {
        _mainPanel = new TitledBox(title);
        _mainPanel.addActionLink(new HelpActionLink("help", SystemProps.getString("Wiki.Base", "")));
        projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);
        _geciFRETList = new ListBox(false);
        _geciFRETList.addItem("GCaMP");
        _geciResultsTable = new FlexTable();
        _geciResultsTable.setHTML(0, 0, HtmlUtils.getHtml("Directory", "prompt").toString());
        _geciResultsTable.setHTML(0, 1, HtmlUtils.getHtml("Path", "prompt").toString());
        _geciResultsTable.setHTML(0, 2, HtmlUtils.getHtml("Is Processed", "prompt").toString());
        initWidget(_mainPanel);
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        _geciImagePath = new TextBox();
        _geciImagePath.setVisibleLength(30);
        _geciImagePath.setText("/groups/scicomp/jacsData/GECI/20111018");
        _scanDirForResultsButton = new RoundedButton("Scan Directory", new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                _dataservice.getPotentialResultNodes(_geciImagePath.getText(), new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("There was a problem discovering the image directories."), 250).showPopup(_submitButton);
                    }

                    public void onSuccess(Object o) {
                        _geciImageDirs = (List<GeciImageDirectoryVO>)o;
                        clear();
                        reloadTableData();
                        _submitButton.setEnabled(_geciImageDirs.size()>0);
                    }
                });
            }
        });
        VerticalPanel contentPanel = new VerticalPanel();

        // Grid(int rows, int columns)
        HorizontalPanel sourcePanel = new HorizontalPanel();
        sourcePanel.add(HtmlUtils.getHtml("Image Directory", "prompt"));
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_geciImagePath);
        sourcePanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        sourcePanel.add(_scanDirForResultsButton);

        contentPanel.add(sourcePanel);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(_geciResultsTable);
        createButtons();
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getPrimerStatusMessage());

        _mainPanel.add(contentPanel);
        _submitButton.setEnabled(false);                                                                                  
    }

    private void reloadTableData() {
        for (GeciImageDirectoryVO geciImageDir : _geciImageDirs) {
            int numRows = _geciResultsTable.getRowCount();
            _geciResultsTable.setWidget(numRows, 0, HtmlUtils.getHtml(geciImageDir.getLocalDirName(), "nowrapText"));
            _geciResultsTable.setWidget(numRows, 1, HtmlUtils.getHtml(geciImageDir.getTargetDirectoryPath(), "nowrapText"));
            _geciResultsTable.setWidget(numRows, 2, HtmlUtils.getHtml(Boolean.toString(geciImageDir.isProcessed()), "nowrapText"));
        }
    }

    private Widget getPrimerStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");

        return new CenteredWidgetHorizontalPanel(_statusMessage);
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

    private Widget getJobNameWidget() {
        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);
        updateJobNameWidget();

        return _jobNameTextBox;
    }

    private void updateJobNameWidget() {
        _jobNameTextBox.setText("My GECI Image Processing job " + new FormattedDate().toString());
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
        //_uploadFileForm.clear();
        // Clear out all the old data
        for (int i = 0; i < _geciResultsTable.getRowCount(); i++) {
            _geciResultsTable.removeRow(i);
        }
        _neuronalAssayAnalysisTask = new NeuronalAssayAnalysisTask();
//        _optionsPanel.displayParams(_neuronalAssayAnalysisTask);
        _submitButton.setEnabled(false);
        // Update all the GUI widgets with the new values
        updateAll();
    }

    private void submitJob() {
        // Validate job name
        if (!StringUtils.hasValue(_jobNameTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A job name is required.")).showPopup(null);
            return;
        }
        if (projectCodeRequired) {
            if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                return;
            }
        }
        // submit the job
        _neuronalAssayAnalysisTask.setJobName(_jobNameTextBox.getText());
        if (projectCodeRequired) {
            String projectCode = _projectCodePanel.getProjectCode();
            if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                return;
            }
            _neuronalAssayAnalysisTask.setParameter(Task.PARAM_project, projectCode);
        }

        ArrayList<String> notReducedList = new ArrayList<String>();
        for (int i = 1; i < _geciResultsTable.getRowCount(); i++) {
            if (_geciResultsTable.getText(i,2).equalsIgnoreCase(Boolean.FALSE.toString())) {
                notReducedList.add(_geciResultsTable.getText(i,2));
            }
        }
        _neuronalAssayAnalysisTask.setParameter(NeuronalAssayAnalysisTask.PARAM_inputFile, Task.csvStringFromCollection(notReducedList));
        _neuronalAssayAnalysisTask.setParameter(NeuronalAssayAnalysisTask.PARAM_fretType, _geciFRETList.getValue(_geciFRETList.getSelectedIndex()));
        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();
        new SubmitJob(_neuronalAssayAnalysisTask, new JobSubmissionListener()).runJob();
    }

    private void updateAll() {
        updateJobNameWidget();
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