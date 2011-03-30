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

package org.janelia.it.jacs.web.gwt.common.client.jobs;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.popup.CancelListener;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * Base class to submit a task, monitors its progress, and performs some action when the task is complete.  Concrete
 * implementations decide how to submit the task and what to do on completion.  This controller updates a popup
 * with the status of the task.
 *
 * @author Michael Press
 */
abstract public class AsyncTaskController {
    private static Logger _logger = Logger.getLogger("AsyncTaskController");

    private Task _task;
    private AsyncTaskMonitorPopup _popup;
    private JobStatusTimer _statusTimer;

    private static final int STATUS_POLLING_INTERVAL = 1000;

    public AsyncTaskController(Task task) {
        _task = task;
        setPopup(createPopup(new CancelListener() {
            /** If user cancels popup, turn off the polling timer */
            public void onCancel() {
                if (_statusTimer != null)
                    _statusTimer.cancel();
            }
        }));
    }

    abstract protected AsyncTaskMonitorPopup createPopup(CancelListener cancelListener);

    abstract protected void submitTask(AsyncCallback asyncCallback);

    abstract protected void onComplete(JobInfo job);

    abstract protected String getTaskType(); // Display string for popup messages

    /**
     * Kicks off the task request once GUI is free
     */
    public void start() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                new PopupCenteredLauncher((PopupPanel) _popup).showPopup(null);
                submitTask(new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        _popup.setFailureMessage("An error occurred submitting your " + getTaskType() + ".", caught.getMessage());
                    }

                    public void onSuccess(Object result) {
                        startPolling((String) result); // taskId - DON'T convert to Long or it gets truncated!
                    }
                });
            }
        });
    }

    private void startPolling(String taskId) {
        _statusTimer = new JobStatusTimer(taskId, STATUS_POLLING_INTERVAL, new JobStatusListener() {
            public void onCommunicationError() {
                _popup.setFailureMessage("A communication error occurred while processing your " + getTaskType() + ".");
            }

            public void onJobRunning(JobInfo job) {
                // ignore, timer still running
                _logger.debug("task still running...status=" + job.getJobId());
            }

            public void onJobFinished(JobInfo jobInfo) {
                _logger.debug("task " + jobInfo.getJobId() + " completed, status = " + jobInfo.getStatus());
                if (Event.COMPLETED_EVENT.equals(jobInfo.getStatus())) { // stream file from server
                    _popup.close();
                    onComplete(jobInfo);
                }
                else
                    _popup.setFailureMessage("Your " + getTaskType() + " failed on the server.");
            }
        });
    }

    public void setPopup(AsyncTaskMonitorPopup popup) {
        _popup = popup;
    }

    public Task getTask() {
        return _task;
    }
}