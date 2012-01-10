
package org.janelia.it.jacs.web.gwt.common.client.submit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * @author Michael Press
 */
public class SubmitJob {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob");

    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    private Task _newTask;
    private JobSubmissionListener _listener;

    public SubmitJob(Task newTask, JobSubmissionListener jobSubmissionListener) {
        _newTask = newTask;
        _listener = jobSubmissionListener;
    }

    public void runJob() {
        try {
            _dataservice.submitJob(_newTask, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    // track the failure of job submission
                    SystemWebTracker.trackActivity("SubmitJobFailure");
                    notifyFailure(throwable);
                }

                public void onSuccess(Object object) {
                    // track the success of job submission
                    SystemWebTracker.trackActivity("SubmitJobSuccessful");
                    String jobNumber = (String) object;
                    _newTask.setObjectId(new Long((String) object));
                    notifySuccess(jobNumber);
                }
            });
        }
        catch (Throwable throwable) {
            _logger.error("Error in runJob():" + throwable.getMessage(), throwable);
            notifyFailure(throwable);
        }
    }

    private void notifyFailure(Throwable throwable) {
        SystemWebTracker.trackActivity("SubmitJobFailure");
        if (_listener != null)
            _listener.onFailure(throwable);
    }

    private void notifySuccess(String jobId) {
        SystemWebTracker.trackActivity("SubmitJobSuccessful");
        if (_listener != null)
            _listener.onSuccess(jobId);
    }

}