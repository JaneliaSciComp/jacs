
package org.janelia.it.jacs.compute.service.common.grid.submit.sge;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.JobControlBeanRemote;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.model.status.GridJobStatus;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Jun 2, 2009
 * Time: 5:26:35 PM
 */
public class RemoteJobStatusLogger implements JobStatusLogger {
    private Logger logger;

    private long taskId;
    private JobControlBeanRemote jobControlRemoteBean;

    public RemoteJobStatusLogger(long taskId, JobControlBeanRemote bean, Logger l) {
        this.taskId = taskId;
        this.jobControlRemoteBean = bean;
        logger = l;
    }

    public long getTaskId() {
        return this.taskId;
    }

    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
        try {
            jobControlRemoteBean.bulkAddGridJobStatus(this.taskId, queue, jobIds, state);
        }
        catch (RemoteException e) {
            logger.error("Unable to save job state. Error:" + e.getMessage());
        }
    }

    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
        try {
            jobControlRemoteBean.updateJobStatus(this.taskId, jobId, state);
        }
        catch (RemoteException e) {
            logger.error("Unable to save job state. Error:" + e.getMessage());
        }
    }

    public void bulkUpdateJobStatus(Map<String, GridJobStatus.JobState> jobStates) {
        try {
            jobControlRemoteBean.bulkUpdateGridJobStatus(this.taskId, jobStates);
        }
        catch (RemoteException e) {
            logger.error("Unable to save job state. Error:" + e.getMessage());
        }
    }

    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
        if (infoMap == null) {
            updateJobStatus(jobId, state);
        }
        else {
            try {
                jobControlRemoteBean.updateJobInfo(this.taskId, jobId, state, infoMap);
            }
            catch (RemoteException e) {
                logger.error("Unable to save job state. Error:" + e.getMessage());
            }

        }
    }

    @Override
    public void cleanUpData() {
        try {
            jobControlRemoteBean.cleanUpJobStatus(this.taskId);
        }
        catch (RemoteException e) {
            logger.error("Unable to clean up job status. Error:" + e.getMessage());
        }
    }
}
