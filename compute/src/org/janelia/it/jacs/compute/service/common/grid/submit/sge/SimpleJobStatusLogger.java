
package org.janelia.it.jacs.compute.service.common.grid.submit.sge;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.model.status.GridJobStatus;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Sep 9, 2008
 * Time: 1:39:59 PM
 */
public class SimpleJobStatusLogger implements JobStatusLogger {
    private long taskId;

    public SimpleJobStatusLogger(long taskId) {
        this.taskId = taskId;
    }

    public long getTaskId() {
        return this.taskId;
    }

    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
        EJBFactory.getLocalJobControlBean().bulkAddGridJobStatus(this.taskId, queue, jobIds, state);
    }

    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
        EJBFactory.getLocalJobControlBean().updateJobStatus(this.taskId, jobId, state);
    }

    public void bulkUpdateJobStatus(Map<String, GridJobStatus.JobState> jobStates) {
        EJBFactory.getLocalJobControlBean().bulkUpdateGridJobStatus(this.taskId, jobStates);
    }

    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
        if (infoMap == null)
            updateJobStatus(jobId, state);
        else
            EJBFactory.getLocalJobControlBean().updateJobInfo(this.taskId, jobId, state, infoMap);
    }

    @Override
    public void bulkUpdateJobInfo(Map<String, GridJobStatus.JobState> changedJobStateMap, Map<String, Map<String, String>> changedJobResourceMap) {
        EJBFactory.getLocalJobControlBean().bulkUpdateGridJobInfo(this.taskId, changedJobStateMap, changedJobResourceMap);
    }

    public void cleanUpData() {
        EJBFactory.getLocalJobControlBean().cleanUpJobStatus(this.taskId);
    }

//    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
//    try {
//        EJBFactory.getRemoteJobControlBean().bulkAddGridJobStatus(this.taskId, queue, jobIds, state);
//    } catch (RemoteException e) {
//        logger.error(e);  //To change body of catch statement use File | Settings | File Templates.
//    }
//}
//
//    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
//        try {
//        EJBFactory.getRemoteJobControlBean().updateJobStatus(this.taskId, jobId, state);
//    } catch (RemoteException e) {
//        logger.error(e);  //To change body of catch statement use File | Settings | File Templates.
//    }
//    }
//
//    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
//        if (infoMap == null)
//            updateJobStatus(jobId, state);
//        else
//        {
//            try {
//            EJBFactory.getRemoteJobControlBean().updateJobInfo(this.taskId, jobId, state, infoMap);
//            } catch (RemoteException e) {
//                logger.error(e);  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//    }

}
