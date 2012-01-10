
package org.janelia.it.jacs.web.gwt.common.client.jobs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusService;
import org.janelia.it.jacs.web.gwt.common.client.service.StatusServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * A timer that monitors the status of a job, and notifies listeners on job events (running/completed) or if
 * communication fails.
 *
 * @author Michael Press
 */
public class JobStatusTimer extends Timer {
    private static Logger _logger = Logger.getLogger("JobStatusTimer");

    public static final int DEFAULT_INTERVAL = 15000; // default 15s
    private static StatusServiceAsync _statusService = (StatusServiceAsync) GWT.create(StatusService.class);

    static {
        ((ServiceDefTarget) _statusService).setServiceEntryPoint("status.srv");
    }

    private int _repeatInterval;
    private String _jobId;
    private JobStatusListener _listener;


    public JobStatusTimer(String jobNum, JobStatusListener listener) {
        this(jobNum, DEFAULT_INTERVAL, listener);
    }

    public JobStatusTimer(String jobNum, int repeatIntervalInMillis, JobStatusListener listener) {
        _jobId = jobNum;
        _repeatInterval = repeatIntervalInMillis;
        _listener = listener;
        scheduleRepeating(_repeatInterval); // start the timer
    }

    /**
     * The execution each time the timer fires.  Calls the server for the status of the job, and invokes the
     * listener's onJobRunning(), onJobCompleted(), or onJobFailed() method as appropriate.  When the job
     * completes or fails, the timer stops itself.
     */
    public void run() {
        _statusService.getTaskResultForUser(_jobId, new AsyncCallback() {
            public void onFailure(Throwable e) {
                // If the service call failed, notify the caller and cancel the timer
                notifyCommunicationError(e.getMessage());
            }

            public void onSuccess(Object object) {
                JobInfo job = (JobInfo) object;

                if (job == null) { // Null == communication error (not necessarily job error)
                    notifyCommunicationError("null task returned");
                }
                else if (Task.isDone(job.getStatus())) {
                    cancel();
                    _listener.onJobFinished(job);
                }
                else {
                    _listener.onJobRunning(job);
                }
            }
        });
    }

    private void notifyCommunicationError(String message) {
        _logger.debug("JobStatusTimer failed due to:" + message);
        cancel();
        _listener.onCommunicationError();
    }

    public void restart() {
        scheduleRepeating(_repeatInterval);
    }

}
