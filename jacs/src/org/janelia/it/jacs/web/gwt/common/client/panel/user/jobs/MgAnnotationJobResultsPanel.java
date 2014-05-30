
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
 * User: jgoll
 * Date: Jul 14, 2009
 * Time: 8:23:38 AM
 * sepcifies metagenomics annotation results panel options
 */
public class MgAnnotationJobResultsPanel extends GeneralJobResultsPanel {
    public static final String TASK_MG_ANNOTATION = "MetaGenoAnnotationTask";
    public MgAnnotationJobResultsPanel(JobSelectionListener jobSelectionListener, JobSelectionListener reRunJobListener,
                                       String[] rowsPerPageOptions, int defaultRowsPerPage) {
        super(TASK_MG_ANNOTATION, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "MgAnnotJobResults");
    }

    protected Widget getJobMenu(final JobInfo job) {


        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBarWithRightAlignedDropdowns(true);

        MenuItem expAnnotationItem = new MenuItem("Export Final Annotation File", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "annotation_rules.combined.out");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expRawHmmItem = new MenuItem("Export HMM Raw Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "ldhmmpfam_full.raw.combined.out");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expBtabHmmItem = new MenuItem("Export HMM Htab Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "ldhmmpfam_full.htab.combined.out");
                Window.open(url, "_self", "");
            }
        });
//        MenuItem expXmlBlastItem=new MenuItem("Export Panda Blast Xml Output", true, new Command() {
//            public void execute() {
//                String url= getDownloadURL(job, MetaGenoAnnotationResultNode.FILENAME_BLAST_XML);
//                Window.open(url, "_self", "");
//            }
//        });

        MenuItem expBtabBlastItem = new MenuItem("Export Panda Blast Btab Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "ncbi_blastp_btab.combined.out");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expRawPriamItem = new MenuItem("Export Priam Raw Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "priam_ec.output.hits");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expTabPriamItem = new MenuItem("Export Priam Tab Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "priam_ec.ectab.combined.out");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expRawTmhmmItem = new MenuItem("Export TmHmm Raw Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "tmhmm.raw.combined.out");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expLipItem = new MenuItem("Export Lipoprotein Motif Bsml Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "lipoprotein_bsml.parsed");
                Window.open(url, "_self", "");
            }
        });
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
        MenuItem exportAllItem = new MenuItem("Export Archive of All Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "archive");
                Window.open(url, "_self", "");
            }
        });

        dropDown.addItem(expAnnotationItem);
        dropDown.addItem(expRawHmmItem);
        dropDown.addItem(expBtabHmmItem);
        //dropDown.addItem(expXmlBlastItem);
        dropDown.addItem(expBtabBlastItem);
        dropDown.addItem(expRawPriamItem);
        dropDown.addItem(expTabPriamItem);
        dropDown.addItem(expRawTmhmmItem);
        dropDown.addItem(expRawTmhmmItem);
        dropDown.addItem(expLipItem);
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