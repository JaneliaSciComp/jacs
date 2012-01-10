
package org.janelia.it.jacs.web.gwt.status.client;

import org.janelia.it.jacs.shared.tasks.JobInfo;

/**
 * Holds data about the currently selected job
 */
public class JobResultsData {
    private JobInfo _job;    // selected job
    private String _jobId;      // we might only have the id from URL param if entry point is loaded on later page
    private String _cameraAcc;

    public void setJob(JobInfo job) {
        _job = job;
    }

    public JobInfo getJob() {
        return _job;
    }

    /**
     * Temporary holding of job id while job is retrieved
     */
    public void setJobId(String jobId) {
        _jobId = jobId;
    }

    public String getJobId() {
        return _jobId;
    }

    public String getDetailAcc() {
        return _cameraAcc;
    }

    public void setDetailAcc(String cameraAcc) {
        _cameraAcc = cameraAcc;
    }

}
