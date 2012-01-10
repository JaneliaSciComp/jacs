
package org.janelia.it.jacs.web.gwt.reversePsiBlast.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.ReversePsiBlastJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

public class ReversePsiBlast extends BaseEntryPoint {
    public static final String TASK_ID_PARAM = "taskId";
    public static final String DATASET_PARAM = "dataset";

    private ReversePsiBlastJobResultsPanel _blastJobResultsPanel;
    private ReversePsiBlastPanel _reversePsiBlastPanel;

    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.JOBS_REVERSE_PSI_BLAST_LABEL), Constants.ROOT_PANEL_NAME);

        Widget contents = getContents();

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(contents);
        show();
    }

    private Widget getContents() {
        VerticalPanel mainPanel = new VerticalPanel();

        mainPanel.add(getAdvancedBlastPanel());
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        mainPanel.add(getJobResultsPanel());

        return mainPanel;
    }

    private Widget getJobResultsPanel() {
        TitledBox blastBox = new TitledBox("Recent Reverse PSI-BLAST Results");
        //blastBox.removeActionLinks();
        blastBox.setWidth("300px"); // min width when contents hidden
        _blastJobResultsPanel = new ReversePsiBlastJobResultsPanel(new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "20"}, 5);
        _blastJobResultsPanel.setJobCompletedListener(new JobCompletedListener());
        blastBox.add(_blastJobResultsPanel);

        // load recent jobs table when browser's done with initial content load
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _blastJobResultsPanel.first();
            }
        });

        return blastBox;
    }

    public Widget getAdvancedBlastPanel() {
        _reversePsiBlastPanel = new ReversePsiBlastPanel("Reverse PSI-BLAST", getTaskIdParam(), getDatasetParam(), new JobSubmittedListener());
        return _reversePsiBlastPanel;
    }

    private String getTaskIdParam() {
        return Window.Location.getParameter(TASK_ID_PARAM);
    }

    private String getDatasetParam() {
        return Window.Location.getParameter(DATASET_PARAM);
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
            new PopupCenteredLauncher(new InfoPopupPanel("Your job has completed")).showPopup(null);
        }

        public void onUnSelect() {
        }
    }

    private class JobSubmittedListener implements JobSubmissionListener {
        public void onFailure(Throwable throwable) {
        } // **submission** failed, so no need to update results panel

        public void onSuccess(String jobId) {
            _blastJobResultsPanel.refresh();
        }
    }

    private class ReRunJobListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            _reversePsiBlastPanel.setJob(job.getJobId());
        }

        public void onUnSelect() {
        }
    }
}