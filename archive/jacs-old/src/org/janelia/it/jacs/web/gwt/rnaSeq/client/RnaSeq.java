
package org.janelia.it.jacs.web.gwt.rnaSeq.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.rnaSeq.RnaSeqPipelineTask;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UserNodeManagementPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.node.UserNodeManagementFastqNodePopup;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.node.UserNodeManagementRnaSeqReferenceGenomeNodePopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobCompletedPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.StringUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.rnaSeq.client.panel.RnaSeqJobResultsPanel;
import org.janelia.it.jacs.web.gwt.rnaSeq.client.panel.RnaSeqTaskConfigurationPanel;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 22, 2010
 * Time: 4:40:42 PM
 */
public class RnaSeq extends BaseEntryPoint {

    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.rnaSeq.client.RnaSeq");

    RnaSeqJobResultsPanel _rnaSeqJobResultsPanel;
    RnaSeqPipelineTask rnaSeqPipelineTask;
    TextBox _jobNameTextBox;
    TextBox _projectCodeTextBox;
    RoundedButton _submitButton;
    private LoadingLabel _statusMessage;
    private org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener _listener;

    public void onModuleLoad() {
        _logger.info("RnaSeq onModuleLoad() start");
        rnaSeqPipelineTask = new RnaSeqPipelineTask();
        initTask(rnaSeqPipelineTask);

        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.RNA_SEQ_PIPELINE_LABEL), Constants.ROOT_PANEL_NAME);
        Widget contents = getContents();
        RootPanel.get(Constants.ROOT_PANEL_NAME).add(contents);
        show();
    }

    private Widget getContents() {
        VerticalPanel mainPanel = new VerticalPanel();

        TitledBox titleBox = new TitledBox("RNA Seq Pipeline");
        titleBox.add(new HTMLPanel("The VICS RnaSeq Pipeline is currently implemented to use the following sequence of tools/steps:"));
        titleBox.add(new HTMLPanel("1) Read-mapper:             Tophat          <a href=\"http://tophat.cbcb.umd.edu/\" target=\"_blank\">http://tophat.cbcb.umd.edu</a>"));
        titleBox.add(new HTMLPanel("2) Transcript-assembler:    Cufflinks       <a href=\"http://cufflinks.cbcb.umd.edu/\" target=\"_blank\">http://cufflinks.cbcb.umd.edu</a>"));
        titleBox.add(new HTMLPanel("3) Database-loader:         Gtf-to-Pasa"));
        titleBox.add(new HTMLPanel("&nbsp;"));
        titleBox.add(new HTMLPanel("To use the pipeline, use the first panel to upload the Fastq directory containing the results to be analyzed."));
        titleBox.add(new HTMLPanel("Then, check that the appropriate matching genome reference sequence is available. If it is not, then"));
        titleBox.add(new HTMLPanel("it must be uploaded also using the second panel. NOTE that uploading the files may take 5-10 minutes."));
        titleBox.add(new HTMLPanel("To check progress on the upload, the 'Refresh' button can be used on the panel. It will take 2-3 minutes"));
        titleBox.add(new HTMLPanel("before the new row for the Fastq directory appears, and then an additional 5 minutes before its status"));
        titleBox.add(new HTMLPanel("is 'completed'. If the RnaSeq pipeline job is started before this point, an error will occur."));
        titleBox.add(new HTMLPanel("&nbsp;"));
        titleBox.add(new HTMLPanel("Note that the Fastq upload is done on a DIRECTORY basis - not a file basis. This means if you want to process"));
        titleBox.add(new HTMLPanel("a subset of data from a Fastq run, then you must create a new UNIX directory into which you copy only those"));
        titleBox.add(new HTMLPanel("files you want to process, and then upload this new directory for processing. Note that for paired runs, matching"));
        titleBox.add(new HTMLPanel("files must be present or an error will occur."));
        titleBox.add(new HTMLPanel("&nbsp;"));
        titleBox.add(new HTMLPanel("Fastq file names must be s(lane)_(direction).txt for unpaired data example: s3_1.txt or s4_1.txt, etc."));
        titleBox.add(new HTMLPanel("For paired data, file names must match, example: s3_1.txt and s3_2.txt, or s4_1.txt and s4_2.txt, etc."));
        titleBox.add(new HTMLPanel("&nbsp;"));
        titleBox.add(new HTMLPanel("Once the nodes are available in the Fastq and Reference panels, select them and then customize the pipeline"));
        titleBox.add(new HTMLPanel("options as desired. An important field is the 'Pasa Database' name, which must be used to access the pipeline"));
        titleBox.add(new HTMLPanel("results via PASA if this is desired. If the 'Pasa Database' field is left blank, then the results will not be"));
        titleBox.add(new HTMLPanel("loaded to pasa. If the given pasa database name does not end with '_pasa' this extension will automatically"));
        titleBox.add(new HTMLPanel("added. Finally, submit the pipeline job. Its progress can be monitored in the top panel."));
        titleBox.setWidth("80%");
        mainPanel.add(titleBox);

        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        Widget recentActivityPanel = getRecentActivityPanel();
        mainPanel.add(recentActivityPanel);

        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        UserNodeManagementPanel fastqNodeManagementPanel = getFastqNodeManagementPanel();
        mainPanel.add(fastqNodeManagementPanel);

        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        UserNodeManagementPanel refGenomeNodeManagementPanel = getRefGenomeNodeManagementPanel();
        mainPanel.add(refGenomeNodeManagementPanel);
        
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        RnaSeqTaskConfigurationPanel rnaSeqTaskConfigurationPanel = new RnaSeqTaskConfigurationPanel(rnaSeqPipelineTask);
        fastqNodeManagementPanel.registerNodeSelectedAction(rnaSeqTaskConfigurationPanel);
        refGenomeNodeManagementPanel.registerNodeSelectedAction(rnaSeqTaskConfigurationPanel);
        mainPanel.add(rnaSeqTaskConfigurationPanel);

        Grid grid = new Grid(4, 2);
        grid.setCellSpacing(7);
        int tmpIndex = 0;
        grid.setWidget(tmpIndex, 0, new HTMLPanel("<span class='prompt'>Required Information</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Job Name:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getJobNameWidget());
        grid.setWidget(++tmpIndex, 0, new HTMLPanel("<span class='prompt'>Project Code:</span><span class='requiredInformation'>*</span>"));
        grid.setWidget(tmpIndex, 1, getProjectCodeWidget());
        grid.setWidget(++tmpIndex, 0, getSubmitButton());
        mainPanel.add(grid);

        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        mainPanel.add(getStatusMessage());

        mainPanel.setVisible(true);

        return mainPanel;
    }

    private Widget getStatusMessage() {
        _statusMessage = new LoadingLabel();
        _statusMessage.setHTML("&nbsp;");
        _statusMessage.addStyleName("AdvancedBlastStatusLabel");
        return new CenteredWidgetHorizontalPanel(_statusMessage);
    }

    private Widget getRecentActivityPanel() {
        HorizontalPanel recentActivityPanel = new HorizontalPanel();
        TitledBox rnaSeqJobTitleBox = new TitledBox("Recent RnaSeq Results");
        rnaSeqJobTitleBox.setWidth("300px"); // min width when contents hidden
        _rnaSeqJobResultsPanel = new RnaSeqJobResultsPanel(new JobResultsSelectedListener(), new ReRunJobListener(), new String[]{"5", "10", "20"}, 10);
        _rnaSeqJobResultsPanel.setJobCompletedListener(new JobCompletedListener());
        _rnaSeqJobResultsPanel.showActionColumn(false);
        _rnaSeqJobResultsPanel.showDeletionColumn(false);
        rnaSeqJobTitleBox.add(_rnaSeqJobResultsPanel);
        recentActivityPanel.add(rnaSeqJobTitleBox);

        // load recent jobs table when browser's done with initial content load
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _rnaSeqJobResultsPanel.first();
            }
        });

        return recentActivityPanel;
    }

    private class JobResultsSelectedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            Window.open(UrlBuilder.getStatusUrl() + "#JobDetailsPage" + "?jobId=" + job.getJobId(), "_self", "");
        }

        public void onUnSelect() {
        }
    }

    private class JobCompletedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            new PopupCenteredLauncher(new JobCompletedPopup(job)).showPopup(null);
            _rnaSeqJobResultsPanel.refresh();
        }

        public void onUnSelect() {
        }
    }

    private class ReRunJobListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
        }

        public void onUnSelect() {
        }
    }

    UserNodeManagementPanel getFastqNodeManagementPanel() {
        return new UserNodeManagementPanel("org.janelia.it.jacs.model.user_data.FastqDirectoryNode",
                new UserNodeManagementFastqNodePopup("Fastq Upload"), 900, 500);
    }

    UserNodeManagementPanel getRefGenomeNodeManagementPanel() {
        return new UserNodeManagementPanel("org.janelia.it.jacs.model.user_data.rnaSeq.RnaSeqReferenceGenomeNode",
                new UserNodeManagementRnaSeqReferenceGenomeNodePopup("Reference Genome"), 1100, 500);
    }

    private void initTask(RnaSeqPipelineTask task) {
        String dateString=new Date().getTime()+"";
        // This was requested 'off' by default.
        // task.setParameter(RnaSeqPipelineTask.PARAM_pasa_db_name, dateString.substring(dateString.length()-9,dateString.length()-1)+"_pasa");
        task.setParameter(RnaSeqPipelineTask.PARAM_pasa_db_name, "");
    }

    private Widget getJobNameWidget() {
        _jobNameTextBox = new TextBox();
        _jobNameTextBox.setMaxLength(64);
        _jobNameTextBox.setVisibleLength(64);

        if (rnaSeqPipelineTask != null && rnaSeqPipelineTask.getJobName()!=null) {
            _jobNameTextBox.setText(rnaSeqPipelineTask.getJobName());
        } else {
            _jobNameTextBox.setText("My RnaSeq Pipeline job " + new FormattedDate().toString());
        }

        return _jobNameTextBox;
    }

    private Widget getProjectCodeWidget() {
        _projectCodeTextBox = new TextBox();
        _projectCodeTextBox.setMaxLength(64);
        _projectCodeTextBox.setVisibleLength(64);

        if (rnaSeqPipelineTask != null && rnaSeqPipelineTask.getParameter(RnaSeqPipelineTask.PARAM_project)!=null) {
            _projectCodeTextBox.setText(rnaSeqPipelineTask.getParameter(RnaSeqPipelineTask.PARAM_project));
        } else {
            _projectCodeTextBox.setText("");
        }

        return _projectCodeTextBox;
    }

    private Widget getSubmitButton() {
        _submitButton = new RoundedButton("Submit Job", new ClickListener() {
            public void onClick(Widget sender) {
                submitJob();
            }
        });
        _submitButton.setEnabled(true);
        return _submitButton;
    }

    private void submitJob() {
        // Validate job name
        if (!StringUtils.hasValue(_jobNameTextBox.getText())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A job name is required.")).showPopup(null);
        } else { // submit the job
            rnaSeqPipelineTask.setJobName(_jobNameTextBox.getText());
            String projectCode = _projectCodeTextBox.getText();
            rnaSeqPipelineTask.setParameter(Task.PARAM_project, projectCode);
            _submitButton.setEnabled(false);
            _statusMessage.showSubmittingMessage();
            new SubmitRnaSeqPipelineJob(rnaSeqPipelineTask, new JobSubmissionListener()).runJob();
        }
    }

    private class JobSubmissionListener implements org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener {
        public void onFailure(Throwable caught) {
            _submitButton.setEnabled(true);
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
            _statusMessage.showSuccessMessage();
            _submitButton.setEnabled(true);
            _rnaSeqJobResultsPanel.refresh();
           // _rnaSeqJobResultsPanel.refresh();  Try moving this to HideMessageTimer since refresh not working properly
        }
    }



}
