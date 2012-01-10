
package org.janelia.it.jacs.web.gwt.mgAnnot.client;


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
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.MgAnnotationJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.MgOrfJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: jgoll
 * Date: Jul 14, 2009
 * Time: 8:23:38 AM
 * layouts metagenomics annotation pipeline panels
 */

public class MgAnnotation extends BaseEntryPoint {
    public static final String TASK_ID_PARAM = "taskId";
    public static final String DATASET_PARAM = "dataset";

    private MgOrfJobResultsPanel _mgOrfJobResultsPanel;
    private MgAnnotationJobResultsPanel _mgAnnotationJobResultsPanel;
    private MgAnnotationPanel _mgAnnotPanel;


    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.JOBS_MG_ANNOT_LABEL), Constants.ROOT_PANEL_NAME);

        Widget contents = getContents();

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(contents);
        show();
    }

    private Widget getContents() {
        VerticalPanel mainPanel = new VerticalPanel();

        mainPanel.add(getMgAnnotationPipelinePanel());
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        mainPanel.add(getOrfJobResultsPanel());
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        mainPanel.add(getAnnotationJobResultsPanel());

        return mainPanel;
    }

    //displays metagenomics job results
    private Widget getOrfJobResultsPanel() {
        TitledBox resultBox = new TitledBox("Recent Metagenomics Orf-Calling Results");
        resultBox.setWidth("300px"); // min width when contents hidden
        _mgOrfJobResultsPanel = new MgOrfJobResultsPanel(new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "20"}, 5);
        _mgOrfJobResultsPanel.setJobCompletedListener(new JobCompletedListener());
        resultBox.add(_mgOrfJobResultsPanel);

        // load recent jobs table when browser's done with initial content load
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _mgOrfJobResultsPanel.first();
            }
        });

        return resultBox;
    }

    //displays metagenomics job results
    private Widget getAnnotationJobResultsPanel() {
        TitledBox resultBox = new TitledBox("Recent Metagenomics Annotation Results");
        resultBox.setWidth("300px"); // min width when contents hidden
        _mgAnnotationJobResultsPanel = new MgAnnotationJobResultsPanel(new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "20"}, 5);
        _mgAnnotationJobResultsPanel.setJobCompletedListener(new JobCompletedListener());
        resultBox.add(_mgAnnotationJobResultsPanel);

        // load recent jobs table when browser's done with initial content load
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _mgAnnotationJobResultsPanel.first();
            }
        });

        return resultBox;
    }


    public Widget getMgAnnotationPipelinePanel() {
        _mgAnnotPanel = new MgAnnotationPanel("Metagenomics Annotation", new JobSubmittedListener());
        return _mgAnnotPanel;
    }


    //internal classes that handle job result events
    private class JobResultsSelectedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            Window.open(UrlBuilder.getStatusUrl() + "#JobDetailsPage" + "?jobId=" + job.getJobId(), "_self", "");
        }

        public void onUnSelect() {
        }
    }

    //shows pop up if job completed successfully
    private class JobCompletedListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            new PopupCenteredLauncher(new InfoPopupPanel("Your job has completed")).showPopup(null);
        }

        public void onUnSelect() {
        }
    }

    //updated results panel if job has sucessfully been submitted
    private class JobSubmittedListener implements JobSubmissionListener {
        public void onFailure(Throwable throwable) {
        } // **submission** failed, so no need to update results panel

        public void onSuccess(String jobId) {
            _mgOrfJobResultsPanel.refresh();
            _mgAnnotationJobResultsPanel.refresh();
        }
    }

    //non functional
    private class ReRunJobListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
        }

        public void onUnSelect() {
        }
    }
}