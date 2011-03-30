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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.popup.RemoveJobPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 19, 2007
 * Time: 1:49:11 PM
 */
public class RemoveJobEventHandler implements ClickListener, PopupListener, org.janelia.it.jacs.web.gwt.common.client.ui.RemoveJobListener {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.RemoveJobEventHandler");

    private JobInfo _jobStatus;
    private TableRow _row;
    private RemotePagingPanel _pagingPanel;
    private boolean _inProgress;
    private RemoveJobPopup _removeJobDialog;

    private static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");
    }

    public RemoveJobEventHandler(org.janelia.it.jacs.shared.tasks.JobInfo jobStatus, TableRow row, RemotePagingPanel pagingPanel) {
        _jobStatus = jobStatus;
        _row = row;
        _inProgress = false;
        _removeJobDialog = null;
        _pagingPanel = pagingPanel;
    }

    public void onClick(Widget widget) {
        startJob(widget);
    }

    public void onPopupClosed(PopupPanel popupPanel, boolean b) {
        _inProgress = false;
    }

    public void removeJob(String jobId) {
        AsyncCallback removeJobCallback = new AsyncCallback() {

            public void onFailure(Throwable caught) {
                _logger.error("Remove job failed", caught);
                finishedRemoveJob();
            }

            //  On success, populate the table with the DataNodes received
            public void onSuccess(Object result) {
                _pagingPanel.removeRow(_row);
                _logger.debug("Remove job succeded");
                SystemWebTracker.trackActivity("DeleteJob");
                finishedRemoveJob();
            }

        };
        _statusservice.markTaskForDeletion(jobId, removeJobCallback);
    }

    private void
    startJob(Widget widget) {
        if (!_inProgress) {
            _inProgress = true;
            _removeJobDialog = new RemoveJobPopup(_jobStatus, this, false);
            _removeJobDialog.addPopupListener(this);
            PopupLauncher removeJobLauncher = new PopupAboveLauncher(_removeJobDialog);
            removeJobLauncher.showPopup(widget);
        }
    }

    private void
    finishedRemoveJob() {
        if (_removeJobDialog != null) {
            _removeJobDialog.hide();
        }
        _removeJobDialog = null;
    }

}

