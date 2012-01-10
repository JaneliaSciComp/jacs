
package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 28, 2008
 * Time: 11:38:01 AM
 */
public class PsiBlastJobResultsPanel extends GeneralJobResultsPanel {
    public static final String TASK_PGP_BLAST = "PsiBlastTask";
    public PsiBlastJobResultsPanel(JobSelectionListener jobSelectionListener, JobSelectionListener reRunJobListener,
                                   String[] rowsPerPageOptions, int defaultRowsPerPage) {
        super(TASK_PGP_BLAST, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "PsiBlastJobResults");
    }

    protected Widget getJobMenu(final JobInfo job) {
        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBarWithRightAlignedDropdowns(true);

//        MenuItem gotoItem=new MenuItem("Go To Fragment Recruitment Viewer", true, new Command() {
//            public void execute() {
//                String url= UrlBuilder.getFrvUrl() + "?taskId=" + job.getJobId();
//                Window.open(url, "_self", "");
//            }
//        });
//        dropDown.addItem(gotoItem);

        MenuItem jobItem = new MenuItem("Job&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        jobItem.setStyleName("tableTopLevelMenuItem");
        menu.addItem(jobItem);

        // Check the status
        menu.setVisible(job.getStatus().equals(Event.COMPLETED_EVENT));
        return menu;
    }

}