
package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobParameterPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTime;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 28, 2008
 * Time: 11:38:01 AM
 * Modified by nelson for Profile Comparison job results
 */
public class NeuronalAssayAnalysisJobResultsPanel extends GeneralJobResultsPanel {
    public NeuronalAssayAnalysisJobResultsPanel(String taskType, JobSelectionListener jobSelectionListener, JobSelectionListener reRunJobListener, String[] rowsPerPageOptions, int defaultRowsPerPage) {
        super(taskType, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "NeuronalAssayAnalysisJobResults");
    }

    protected Widget getJobMenu(final JobInfo job) {
        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBarWithRightAlignedDropdowns(true);

        MenuItem paramItem = new MenuItem("Show Parameters", true, new Command() {
            public void execute() {
                _paramPopup = new JobParameterPopup(
                        job.getJobname(),
                        new FormattedDateTime(job.getSubmitted().getTime()).toString(),
                        job.getParamMap(), false);
                _paramPopup.setPopupTitle("Job Parameters");
                new PopupCenteredLauncher(_paramPopup).showPopup(menu);
            }
        });

//        // Add a MenuItem for each output file
//        MenuItem comparisonDendrogramOutput = new MenuItem("Export Profile Comparison Dendrogram as PDF File", true, new Command() {
//            public void execute() {
//                String url = getDownloadURL(job, "comparison_matrix.tsv.dendrogram.pdf");
//                Window.open(url, "_self", "");
//            }
//        });
//        MenuItem comparisonMDSOutput = new MenuItem("Export Profile Comparison MDS as PDF File", true, new Command() {
//            public void execute() {
//                String url = getDownloadURL(job, "comparison_matrix.tsv.mds.pdf");
//                Window.open(url, "_self", "");
//            }
//        });
//        MenuItem comparisonTSVOutput = new MenuItem("Export Profile Comparison Matrix as Tab-Delmited File", true, new Command() {
//            public void execute() {
//                String url = getDownloadURL(job, "comparison_matrix.tsv");
//                Window.open(url, "_self", "");
//            }
//        });
        MenuItem exportAllItem = new MenuItem("Export Archive of All Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "archive");
                Window.open(url, "_self", "");
            }
        });

//        dropDown.addItem(comparisonDendrogramOutput);
//        dropDown.addItem(comparisonMDSOutput);
//        dropDown.addItem(comparisonTSVOutput);
        dropDown.addItem(exportAllItem);
        dropDown.addItem(paramItem);

        MenuItem jobItem = new MenuItem("Job&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        jobItem.setStyleName("tableTopLevelMenuItem");
        menu.addItem(jobItem);

        // Check the status
        menu.setVisible(job.getStatus().equals(Event.COMPLETED_EVENT));
        return menu;
    }

    private String getDownloadURL(JobInfo job, String fileTag) {
        return "/jacs/fileDelivery.htm?nodeTaskId=" + job.getJobId() + "&fileTag=" + fileTag;
    }
}