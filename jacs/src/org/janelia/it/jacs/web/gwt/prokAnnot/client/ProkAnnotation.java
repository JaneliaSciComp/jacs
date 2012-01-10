
package org.janelia.it.jacs.web.gwt.prokAnnot.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.ProkPipelineJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

public class ProkAnnotation extends BaseEntryPoint {
    public static final String TASK_ID_PARAM = "taskId";
    public static final String DATASET_PARAM = "dataset";
    public static final String TASK_PROK_ANNOT_RUN = "ProkaryoticAnnotationTask";
    public static final String TASK_PROK_ANNOT_LOAD = "ProkaryoticAnnotationLoadGenomeDataTask";

    private ProkPipelineJobResultsPanel _prokAnnotJobRunPanel;
    private ProkPipelineJobResultsPanel _prokAnnotJobLoadPanel;

    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.JOBS_PROK_ANNOT_LABEL), Constants.ROOT_PANEL_NAME);

        Widget contents = getContents();

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(contents);
        show();
    }

    private Widget getContents() {
        VerticalPanel mainPanel = new VerticalPanel();

        mainPanel.add(getProkAnnotationPipelinePanel());
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        HorizontalPanel tmpHorizontalPanel = new HorizontalPanel();
        tmpHorizontalPanel.add(getLoadJobResultsPanel());
        tmpHorizontalPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        tmpHorizontalPanel.add(getRunJobResultsPanel());
        mainPanel.add(tmpHorizontalPanel);

        return mainPanel;
    }

    private Widget getLoadJobResultsPanel() {
        TitledBox resultBox = new TitledBox("Recent Load Jobs");
        resultBox.setWidth("300px"); // min width when contents hidden
        _prokAnnotJobLoadPanel = new ProkPipelineJobResultsPanel(TASK_PROK_ANNOT_LOAD, new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "20"}, 5);
        _prokAnnotJobLoadPanel.setJobCompletedListener(new JobCompletedListener());
        resultBox.add(_prokAnnotJobLoadPanel);

        // load recent jobs table when browser's done with initial content load
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _prokAnnotJobLoadPanel.first();
            }
        });

        return resultBox;
    }

    private Widget getRunJobResultsPanel() {
        TitledBox resultBox = new TitledBox("Recent Annotation Jobs");
        resultBox.setWidth("300px"); // min width when contents hidden
        _prokAnnotJobRunPanel = new ProkPipelineJobResultsPanel(TASK_PROK_ANNOT_RUN, new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "20"}, 5);
        _prokAnnotJobRunPanel.setJobCompletedListener(new JobCompletedListener());
        resultBox.add(_prokAnnotJobRunPanel);

        // load recent jobs table when browser's done with initial content load
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _prokAnnotJobRunPanel.first();
            }
        });

        return resultBox;
    }

    public Widget getProkAnnotationPipelinePanel() {
        return new ProkAnnotationPanel("Prokaryotic Annotation", new JobSubmittedListener());
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
            _prokAnnotJobLoadPanel.refresh();
            _prokAnnotJobRunPanel.refresh();
        }
    }

    private class ReRunJobListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            //_prokAnnotPanel.setJob(job.getJobId());
        }

        public void onUnSelect() {
        }
    }
}