
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
 */
public class BarcodeDesignerJobResultsPanel extends GeneralJobResultsPanel {
    public BarcodeDesignerJobResultsPanel(String taskType, JobSelectionListener jobSelectionListener, JobSelectionListener reRunJobListener,
                                          String[] rowsPerPageOptions, int defaultRowsPerPage) {
        super(taskType, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "BarcodeDesignerJobResults");
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
        MenuItem barcodeOutput = new MenuItem("Export Barcode Tab File", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "Barcodes.tsv");
                Window.open(url, "_self", "");
            }
        });
        MenuItem generationProfileOutput = new MenuItem("Export Generation Profile Tab File", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "GenerationProfile.tsv");
                Window.open(url, "_self", "");
            }
        });
        MenuItem barcodeWithPrimerOutput = new MenuItem("Export Barcode With Primer Tab File", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "Barcode_wPrimer.tsv");
                Window.open(url, "_self", "");
            }
        });
        MenuItem exportAllItem = new MenuItem("Export Archive of All Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "archive");
                Window.open(url, "_self", "");
            }
        });

        dropDown.addItem(barcodeOutput);
        dropDown.addItem(generationProfileOutput);
        dropDown.addItem(barcodeWithPrimerOutput);
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


    /**
     * The pipeline uses the error stream for debugging so we want to ignore the messages returned.
     *
     * @return returns boolean whether we should ignore the messages from the error files
     */
    protected boolean ignoreStatusMessages() {
        return true;
    }

    private String getDownloadURL(JobInfo job, String fileTag) {
        return "/jacs/fileDelivery.htm?nodeTaskId=" + job.getJobId() + "&fileTag=" + fileTag;
    }
}