
package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
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
 */
public class EukAnnotJobResultsPanel extends GeneralJobResultsPanel {
    public static final String TASK_EUK_ANNOT = "EukaryoticAnnotationTask";
    public EukAnnotJobResultsPanel(JobSelectionListener jobSelectionListener, JobSelectionListener reRunJobListener,
                                   String[] rowsPerPageOptions, int defaultRowsPerPage) {
        super(TASK_EUK_ANNOT, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "EukAnnotJobResults");
    }

    protected Widget getJobMenu(final JobInfo job) {
        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBarWithRightAlignedDropdowns(true);

//        MenuItem cumulativeCountItem=new MenuItem("Export Assignment Cumulative Count Text", true, new Command() {
//            public void execute() {
//                String url= getDownloadURL(job, "countcum.txt");
//                Window.open(url, "_self", "");
//            }
//        });
        MenuItem paramItem = new MenuItem("Show Parameters", true, new Command() {
            public void execute() {
                _paramPopup = new org.janelia.it.jacs.web.gwt.common.client.popup.jobs.JobParameterPopup(
                        job.getJobname(),
                        new FormattedDateTime(job.getSubmitted().getTime()).toString(),
                        job.getParamMap(), false);
                _paramPopup.setPopupTitle("Job Parameters");
                new PopupCenteredLauncher(_paramPopup).showPopup(menu);
            }
        });

//        dropDown.addItem(cumulativeCountItem);
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