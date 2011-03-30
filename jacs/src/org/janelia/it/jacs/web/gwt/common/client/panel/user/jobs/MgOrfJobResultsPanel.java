/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
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
 * User: jgoll
 * Date: Jul 14, 2009
 * Time: 8:23:38 AM
 * sepcifies metagenomics orf results panel options
 */
public class MgOrfJobResultsPanel extends GeneralJobResultsPanel {
    public static final String TASK_MG_ORF_CALLING = "MetaGenoOrfCallerTask";
    public MgOrfJobResultsPanel(JobSelectionListener jobSelectionListener, JobSelectionListener reRunJobListener,
                                String[] rowsPerPageOptions, int defaultRowsPerPage) {
        super(TASK_MG_ORF_CALLING, jobSelectionListener, reRunJobListener, rowsPerPageOptions, defaultRowsPerPage,
                "MgAnnotJobResults");
    }

    protected Widget getJobMenu(final JobInfo job) {

        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBarWithRightAlignedDropdowns(true);

        MenuItem expPeptidesItem = new MenuItem("Export Final Peptides", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "metagene_mapped_pep.fasta");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expMetageneItem = new MenuItem("Export Metagene Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "metagene.combined.raw");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expOrfsNtItem = new MenuItem("Export Open Reading Frames (nucleotides)", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "open_reading_frames.combined.fna");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expOrfsAaItem = new MenuItem("Export Open Reading Frames (amino acids)", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "open_reading_frames.combined.faa");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expRnaItem = new MenuItem("Export rRNAs", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "camera_rrna_finder.combined.fasta");
                Window.open(url, "_self", "");
            }
        });
        MenuItem expTrnaItem = new MenuItem("Export tRNAs", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "camera_extract_trna.combined.fasta");
                Window.open(url, "_self", "");
            }
        });

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
        MenuItem exportAllItme = new MenuItem("Export Archive of All Output", true, new Command() {
            public void execute() {
                String url = getDownloadURL(job, "archive");
                Window.open(url, "_self", "");
            }
        });

        dropDown.addItem(expPeptidesItem);
        dropDown.addItem(expMetageneItem);
        dropDown.addItem(expOrfsAaItem);
        dropDown.addItem(expOrfsNtItem);
        dropDown.addItem(expTrnaItem);
        dropDown.addItem(expRnaItem);
        dropDown.addItem(exportAllItme);
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