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

package org.janelia.it.jacs.web.gwt.status.client.wizard;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxActionLinkUtils;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.BlastJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.ExportResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.UserFRVPipelineJobResultsPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.HelpActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.status.client.JobResultsData;

/**
 * First page of the job results wizard; shows a table of all jobs.
 */
public class JobResultsPage extends JobResultsWizardPage {
    //private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.wizard.JobResultsPage");

    public static final String HISTORY_TOKEN = "JobResultsPage";
    public static final String JOB_SUBMIT_HELP_URL_PROP = "JobResults.HelpURL";

    private VerticalPanel _resultsPanel;
    private BlastJobResultsPanel _blastJobResultsPanel;
    private UserFRVPipelineJobResultsPanel _frvResultsPanel;
    private ExportResultsPanel _exportResultsPanel;

    public JobResultsPage(JobResultsData data, WizardController controller) {
        super(data, controller);
        init();
    }

    private void init() {
        TitledBox blastBox = new TitledBox(Constants.JOBS_BLAST_RESULTS_LABEL);
        //blastBox.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(blastBox, new HelpActionLink("help"), JOB_SUBMIT_HELP_URL_PROP);
        blastBox.setWidth("300px"); // min width when contents hidden
        _blastJobResultsPanel = new BlastJobResultsPanel(new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "15"}, 5);
        blastBox.add(_blastJobResultsPanel);

        TitledBox frvBox = new TitledBox("My FRV Pipeline Jobs");
        //frvBox.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(frvBox, new HelpActionLink("help"), JOB_SUBMIT_HELP_URL_PROP);
        frvBox.setWidth("300px"); // min width when contents hidden
        _frvResultsPanel = new UserFRVPipelineJobResultsPanel(new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "15"}, 5);
        frvBox.add(_frvResultsPanel);

        TitledBox exportBox = new TitledBox("My Export Jobs");
        //exportBox.removeActionLinks();
        TitledBoxActionLinkUtils.addHelpActionLink(exportBox, new HelpActionLink("help"), JOB_SUBMIT_HELP_URL_PROP);
        exportBox.setWidth("300px"); // min width when contents hidden
        _exportResultsPanel = new ExportResultsPanel(new JobResultsSelectedListener(),
                new ReRunJobListener(), new String[]{"5", "10", "15"}, 5);
        exportBox.add(_exportResultsPanel);

        // Set the page layout
        _resultsPanel = new VerticalPanel();
        _resultsPanel.add(blastBox);
        _resultsPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "text"));
        _resultsPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "text"));
        _resultsPanel.add(frvBox);
        _resultsPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "text"));
        _resultsPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;", "text"));
        _resultsPanel.add(exportBox);
    }

    public Widget getMainPanel() {
        return _resultsPanel;
    }

    public String getPageToken() {
        return HISTORY_TOKEN;
    }

    public String getPageTitle() {
        return Constants.JOBS_JOBS_LABEL;
    }

    protected void setupButtons() {
        getButtonManager().getBackButton().setVisible(false);
        getButtonManager().getNextButton().setVisible(false);
    }

    /**
     * Load the table with the first page of data
     */
    protected void preProcess(Integer priorPageNumber) {
        _blastJobResultsPanel.first();
        _frvResultsPanel.first();
        _exportResultsPanel.first();
    }

    /**
     * When job results have been requested on the job listing panel, goto the job details wizard page.
     */
    private class JobResultsSelectedListener implements JobSelectionListener {
        private JobResultsSelectedListener() {
        }

        public void onSelect(JobInfo job) {
            getData().setJob(job);
            getController().next();
        }

        public void onUnSelect() {
        }
    }

    private class ReRunJobListener implements JobSelectionListener {
        public void onSelect(JobInfo job) {
            String url = UrlBuilder.getAdvancedBlastUrl() + "?taskId=" + job.getJobId();
            Window.open(url, "_self", "");
        }

        public void onUnSelect() {
        }
    }
}

