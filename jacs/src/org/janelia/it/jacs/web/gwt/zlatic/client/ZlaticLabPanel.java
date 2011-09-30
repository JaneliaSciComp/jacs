package org.janelia.it.jacs.web.gwt.zlatic.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.zlatic.ZlaticLabTask;
import org.janelia.it.jacs.web.gwt.common.client.panel.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;

import java.util.ArrayList;

public class ZlaticLabPanel extends Composite {
    public static final String DEFAULT_UPLOAD_MESSAGE = "File upload successful - click Apply to continue.";
    public static final String UPLOAD_SEQUENCE_NAME_PARAM = "uploadSequenceName";

    private TitledBox _mainPanel;
    private TextBox _jobNameTextBox;
    private RoundedButton _submitButton;
    private RoundedButton _clearButton;
    private LoadingLabel _statusMessage;

    // Input file(s)
    //final FormPanel _uploadFileForm = new FormPanel();
    private FileChooserPanel _fileChooserPanel;
    public String _filePath1;
    public String _filePath2;
    public String _filePath3;
    public String _filePath4;
    public String _filePath5;
    public String _filePath6;

    private boolean projectCodeRequired = false;
    private FinalOutputDestinationPanel _finalOutputPanel = new FinalOutputDestinationPanel();
    private ProjectCodePanel _projectCodePanel;
    private ZlaticLabTask _zlaticLabTask = new ZlaticLabTask();
    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);


    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public ZlaticLabPanel(String title, org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener listener) {
        _listener = listener;
        init(title);
    }

    private void init(String title) {
        _mainPanel = new TitledBox(title);
        _mainPanel.addActionLink(new HelpActionLink("help", SystemProps.getString("Wiki.Base", "")));
        projectCodeRequired = SystemProps.getBoolean("Grid.RequiresProjectCode", false);
        initWidget(_mainPanel);
        popuplateContentPanel();
    }

    private void popuplateContentPanel() {
        VerticalPanel contentPanel = new VerticalPanel();

        // Grid(int rows, int columns)
        Grid grid = new Grid((projectCodeRequired) ? 10 : 9, 2);
        grid.setCellSpacing(3);
        int tmpIndex = 0;
        grid.setWidget(tmpIndex, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getJobNameWidget());

        grid.getCellFormatter().setVerticalAlignment(3, 0, VerticalPanel.ALIGN_TOP);
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Input:</span>"));
        grid.setWidget(tmpIndex, 1, new HTMLPanel("<span class='prompt'>A set of FASTA, alignment or profile files.</span>"));

        // Yes, this feels like a great big hack
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Input File (1):</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getFastaUploadPanel1());
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Input File (2):</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getFastaUploadPanel2());
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Input File (3):</span>"));
        grid.setWidget(tmpIndex, 1, getFastaUploadPanel3());
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Input File (4):</span>"));
        grid.setWidget(tmpIndex, 1, getFastaUploadPanel4());
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Input File (5):</span>"));
        grid.setWidget(tmpIndex, 1, getFastaUploadPanel5());
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Input File (6):</span>"));
        grid.setWidget(tmpIndex, 1, getFastaUploadPanel6());

        grid.setWidget(++tmpIndex, 0, HtmlUtils.getHtml("Output Destination:", "prompt"));
        grid.setWidget(tmpIndex, 1, _finalOutputPanel);

        if (projectCodeRequired) {
            _projectCodePanel = new ProjectCodePanel();
            grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Project Code:</span><span class='requiredInformation'>*</span>"));
            grid.setWidget(tmpIndex, 1, _projectCodePanel);
        }

        createButtons();
        contentPanel.add(grid);
        contentPanel.add(HtmlUtils.getHtml("&nbsp", "spacer"));
        contentPanel.add(getSubmitButtonPanel());
        contentPanel.add(getPrimerStatusMessage());

        _mainPanel.add(contentPanel);
        _submitButton.setEnabled(false);
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

        _zlaticLabTask = new ZlaticLabTask();
//        _optionsPanel.displayParams(_zlaticLabTask);
        _fileChooserPanel.clear();
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
        _zlaticLabTask.setJobName(_jobNameTextBox.getText());
        if (projectCodeRequired) {
            String projectCode = _projectCodePanel.getProjectCode();
            if (!_projectCodePanel.isCurrentProjectCodeValid()) {
                new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
                return;
            }
            _zlaticLabTask.setParameter(Task.PARAM_project, projectCode);
        }

        // Set our input files parameter
        String _inputFiles = "";
        _inputFiles += _filePath1 + ",";
        _inputFiles += _filePath2 + ",";
        _inputFiles += _filePath3 + ",";
        _inputFiles += _filePath4 + ",";
        _inputFiles += _filePath5 + ",";
        _inputFiles += _filePath6 + ",";
        // Remove the trailing comma
        _inputFiles = _inputFiles.substring(0, _inputFiles.length() - 1);

        _zlaticLabTask.setParameter(ZlaticLabTask.PARAM_inputFile, _inputFiles);
        _submitButton.setEnabled(false);
        _statusMessage.showSubmittingMessage();
        new SubmitJob(_zlaticLabTask, new JobSubmissionListener()).runJob();
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

    private void addFileTypes(ArrayList<FileChooserPanel.FILE_TYPE> types) {
        types.add(FileChooserPanel.FILE_TYPE.mpfa);
        types.add(FileChooserPanel.FILE_TYPE.seq);
        types.add(FileChooserPanel.FILE_TYPE.ffn);
        types.add(FileChooserPanel.FILE_TYPE.fa);
        types.add(FileChooserPanel.FILE_TYPE.faa);
        types.add(FileChooserPanel.FILE_TYPE.fna);
        types.add(FileChooserPanel.FILE_TYPE.fsa);
        types.add(FileChooserPanel.FILE_TYPE.fasta);
        types.add(FileChooserPanel.FILE_TYPE.aln);
        types.add(FileChooserPanel.FILE_TYPE.alignment);
        types.add(FileChooserPanel.FILE_TYPE.clustalw);
        types.add(FileChooserPanel.FILE_TYPE.clw);
        types.add(FileChooserPanel.FILE_TYPE.prof);
        types.add(FileChooserPanel.FILE_TYPE.profile);
    }

    private Panel getFastaUploadPanel1() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        this.addFileTypes(types);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _filePath1 = value;
                _submitButton.setEnabled(true);
            }

            public void onUnSelect(String value) {
                _filePath1 = null;
            }
        }, types);
        return _fileChooserPanel;
    }

    private Panel getFastaUploadPanel2() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        this.addFileTypes(types);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _filePath2 = value;
            }

            public void onUnSelect(String value) {
                _filePath2 = null;
            }
        }, types);
        return _fileChooserPanel;
    }

    private Panel getFastaUploadPanel3() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        this.addFileTypes(types);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _filePath3 = value;
            }

            public void onUnSelect(String value) {
                _filePath3 = null;
            }
        }, types);
        return _fileChooserPanel;
    }

    private Panel getFastaUploadPanel4() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        this.addFileTypes(types);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _filePath4 = value;
            }

            public void onUnSelect(String value) {
                _filePath4 = null;
            }
        }, types);
        return _fileChooserPanel;
    }

    private Panel getFastaUploadPanel5() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        this.addFileTypes(types);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _filePath5 = value;
            }

            public void onUnSelect(String value) {
                _filePath5 = null;
            }
        }, types);
        return _fileChooserPanel;
    }

    private Panel getFastaUploadPanel6() {
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        this.addFileTypes(types);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                _filePath6 = value;
            }

            public void onUnSelect(String value) {
                _filePath6 = null;
            }
        }, types);
        return _fileChooserPanel;
    }

}