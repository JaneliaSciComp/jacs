
package org.janelia.it.jacs.web.gwt.dpd.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

public class DegeneratePrimerDesign extends BaseEntryPoint {
    public static final String TASK_ID_PARAM = "taskId";
    public static final String DATASET_PARAM = "dataset";

    //    private BarCodeDesignerJobResultsPanel _barcodeDesignerJobResultsPanel;
    private DegeneratePrimerDesignPanel _degeneratePrimerDesignPanel;

    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.JOBS_DPD_LABEL), Constants.ROOT_PANEL_NAME);

        Widget contents = getContents();

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(contents);
        show();
    }

    private Widget getContents() {
        VerticalPanel mainPanel = new VerticalPanel();

        mainPanel.add(getPanel());
        mainPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
//        mainPanel.add(getJobResultsPanel());

        return mainPanel;
    }

//    private Widget getJobResultsPanel() {
//        TitledBox resultBox = new TitledBox("Recent Results");
//        //blastBox.removeActionLinks();
//        resultBox.setWidth("300px"); // min width when contents hidden
//        _barcodeDesignerJobResultsPanel = new BarCodeDesignerJobResultsPanel(new JobResultsSelectedListener(),
//            new ReRunJobListener(), new String[] {"5", "10", "20"}, 5);
//        _barcodeDesignerJobResultsPanel.setJobCompletedListener(new JobCompletedListener());
//        resultBox.add(_barcodeDesignerJobResultsPanel);
//
//        // load recent jobs table when browser's done with initial content load
//        DeferredCommand.addCommand(new Command() {
//            public void execute()
//            {
//                _barcodeDesignerJobResultsPanel.first();
//            }
//        });
//
//        return resultBox;
//    }

    public Widget getPanel() {
        _degeneratePrimerDesignPanel = new DegeneratePrimerDesignPanel("Degenerate Primer Design", getTaskIdParam(), new JobSubmittedListener());
        return _degeneratePrimerDesignPanel;
    }

    private String getTaskIdParam() {
        return Window.Location.getParameter(TASK_ID_PARAM);
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
//            _barcodeDesignerJobResultsPanel.refresh();
        }
    }

    private class ReRunJobListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
//            _degeneratePrimerDesignPanel.setJob(job.getJobId());
        }

        public void onUnSelect() {
        }
    }
}