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

package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.export.ClusterProteinAnnotationExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.popup.download.DownloadPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;

/**
 * @author Cristian Goina
 */
public class DownloadClusterPopup extends DownloadPopup {
    private static final String TXT_FORMAT = ".txt";

    private class DownloadClusterClickListener implements ClickListener {
        private String outputType;

        private DownloadClusterClickListener(String outputType) {
            this.outputType = outputType;
        }

        public void onClick(Widget w) {
            SystemWebTracker.trackActivity("DownloadCluster",
                    new String[]{
                            outputType
                    });
            ArrayList<String> accessionList = new ArrayList<String>();
            accessionList.add(clusterAcc);
            ClusterProteinAnnotationExportTask exportTask = new ClusterProteinAnnotationExportTask(
                    null, onlyNRSeqFlag, ExportWriterConstants.EXPORT_TYPE_FASTA, accessionList, null);
            if (ExportWriterConstants.COMPRESSION_ZIP.equals(outputType) ||
                    ExportWriterConstants.COMPRESSION_GZ.equals(outputType)) {
                exportTask.setSuggestedCompressionType(outputType);
            }
            new AsyncExportTaskController(exportTask).start();
        }
    }

    private String clusterAcc;
    private boolean onlyNRSeqFlag;
    private String description;

    public DownloadClusterPopup(String clusterAcc,
                                boolean onlyNRSeqFlag,
                                String description) {
        this(clusterAcc, onlyNRSeqFlag, description, false);
    }

    public DownloadClusterPopup(String clusterAcc,
                                boolean onlyNRSeqFlag,
                                String description,
                                boolean realizeNow) {
        super("Download Cluster Sequences", realizeNow);
        this.clusterAcc = clusterAcc;
        this.onlyNRSeqFlag = onlyNRSeqFlag;
        this.description = description;
    }

    protected void populateContent() {
        if (clusterAcc == null) {
            add(HtmlUtils.getHtml("No cluster accession has been provided", "error"));
        }
        else {
            addDescription(null, description + " for " + clusterAcc);
            add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
            add(createDownloadLink("Download as",
                    new Link(TXT_FORMAT, new DownloadClusterClickListener(ExportWriterConstants.EXPORT_TYPE_TEXT))));
            add(createDownloadLink("Download as",
                    new Link(ExportWriterConstants.COMPRESSION_ZIP, new DownloadClusterClickListener(ExportWriterConstants.COMPRESSION_ZIP))));
            add(createDownloadLink("Download as",
                    new Link(ExportWriterConstants.COMPRESSION_GZ, new DownloadClusterClickListener(ExportWriterConstants.COMPRESSION_GZ))));
        }
        // Here's how to get rid of that pesky popup!
        addCloseLink();
    }

}
