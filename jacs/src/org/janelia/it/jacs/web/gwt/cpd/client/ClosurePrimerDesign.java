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

package org.janelia.it.jacs.web.gwt.cpd.client;

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

public class ClosurePrimerDesign extends BaseEntryPoint {
    public static final String TASK_ID_PARAM = "taskId";
    public static final String DATASET_PARAM = "dataset";

    //    private BarcodeDesignerJobResultsPanel _barcodeDesignerJobResultsPanel;
    private ClosurePrimerDesignPanel _closurePrimerDesignPanel;

    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.JOBS_CPD_LABEL), Constants.ROOT_PANEL_NAME);

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
        _closurePrimerDesignPanel = new ClosurePrimerDesignPanel("Primer Design For Closure", getTaskIdParam(), new JobSubmittedListener());
        return _closurePrimerDesignPanel;
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
//            _closurePrimerDesignPanel.setJob(job.getJobId());
        }

        public void onUnSelect() {
        }
    }
}