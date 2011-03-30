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

package org.janelia.it.jacs.web.gwt.frv.client.panels.tabs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.PulldownPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentService;
import org.janelia.it.jacs.web.gwt.frv.client.RecruitmentServiceAsync;
import org.janelia.it.jacs.web.gwt.frv.client.popups.QuerySequenceChooserPopup;

/**
 * @author Michael Press
 */
abstract class FrvControlsPanelBaseTab implements FrvControlsPanelTab {
    protected org.janelia.it.jacs.shared.tasks.RecruitableJobInfo _job;
    private QuerySequenceChooserPopup _queryChooserPopup;
    private JobSelectionListener _jobSelectionListener;
    protected static RecruitmentServiceAsync _recruitmentService = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);

    static {
        ((ServiceDefTarget) _recruitmentService).setServiceEntryPoint("recruitment.srv");
    }

    public FrvControlsPanelBaseTab(JobSelectionListener listener) {
        setRecruitableJobSelectionListener(listener);
    }

    public void setRecruitableJobSelectionListener(JobSelectionListener listener) {
        _jobSelectionListener = listener;
    }

    public JobSelectionListener getRecruitableJobSelectionListener() {
        return _jobSelectionListener;
    }

    public org.janelia.it.jacs.shared.tasks.JobInfo getJob() {
        return _job;
    }

    public void setJob(org.janelia.it.jacs.shared.tasks.JobInfo job) {
        _job = (org.janelia.it.jacs.shared.tasks.RecruitableJobInfo) job;
    }

    /*
     * Creates the query selector widget, which is a PulldownPopup (looks like a pulldown but clicking it opens a
     * popup to select the query).  The popup is opened 100 pixels above the pulldown so it has most of the vertical
     * page real estate to show the query selection table.
     */
    protected Widget getQueryWidget(String queryName, int displaySize) {
        if (_queryChooserPopup == null)
            _queryChooserPopup = new QuerySequenceChooserPopup(_jobSelectionListener);

        PulldownPopup widget = new PulldownPopup(_queryChooserPopup);
        widget.setLauncher(new PopupAtRelativePixelLauncher(-100, 0));
        if (queryName != null)
            widget.setText(queryName);

        return widget;
    }

    protected String getConcatenatedSubjectNames(org.janelia.it.jacs.shared.tasks.JobInfo job) {
        StringBuffer buf = new StringBuffer();
        buf.append("&bull;&nbsp;").append(job.getSubjectName()).append("<br/>");
        return buf.toString();
    }

    protected void addPromptValuePair(HTMLTable table, int row, int col, String prompt, String itemValue) {
        TableUtils.addTextRow(table, new RowIndex(row), col, prompt, itemValue);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }

}
