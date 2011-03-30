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

package org.janelia.it.jacs.web.gwt.common.client.popup.jobs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.jobs.JobNameWidget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;

/**
 * @author Cristian Goina
 */
public class EditJobNameListener implements ClickListener {

    private static Logger _logger = Logger.getLogger("EditJobNameListener");
    private JobInfo _jobStatus;
    private TableRow _row;
    private int _jobNameColumnIndex;
    private PagingPanel _pagingPanel;

    private static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");
    }

    public EditJobNameListener(JobInfo jobStatus, TableRow row, int jobNameColumnIndex, PagingPanel pagingPanel) {
        _jobStatus = jobStatus;
        _row = row;
        _jobNameColumnIndex = jobNameColumnIndex;
        _pagingPanel = pagingPanel;
    }

    public void onClick(Widget widget) {
        new PopupAboveLauncher(new EditJobNamePopup(_jobStatus, this, false)).showPopup(widget);
    }

    public void replaceJobName(String jobId, final String jobName) {
        AsyncCallback renameJobCallback = new AsyncCallback() {
            public void onFailure(Throwable caught) {
                _logger.error("Renaming job " + _jobStatus.getJobname() + " to " + jobName + " failed", caught);
            }

            //  On success, populate the table with the DataNodes received
            public void onSuccess(Object result) {
                String newJobName = (String) result;
                if (newJobName != null) {
                    if (!newJobName.equals(jobName)) {
                        // the user provided a name longer
                        // than the maximum length allowed for the job name column
                        Window.alert("The given name was too long therefore it was truncated to:\n" +
                                "\"" + newJobName + "\"");
                    }
                    _logger.debug("Renamed job " + _jobStatus.getJobname() + " to " + newJobName);
                    _jobStatus.setJobname(newJobName);
                    updateDisplay(_jobStatus, _row);
                }
            }
        };
        _statusservice.replaceTaskJobName(jobId, jobName, renameJobCallback);
    }

    /**
     * Updates the edit job name widget
     *
     * @param job job being edited
     * @param row row related to the job
     */
    private void updateDisplay(JobInfo job, TableRow row) {
        // refresh the table's cell
        TableCell jobNameCell = row.getTableCell(_jobNameColumnIndex);
        jobNameCell.setValue(job.getJobname());
        jobNameCell.setWidget(JobNameWidget.getWidget(job, row, _jobNameColumnIndex, _pagingPanel));
        _pagingPanel.getSortableTable().refreshCell(jobNameCell.getRow(), jobNameCell.getCol());
    }

}
