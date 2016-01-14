
package org.janelia.it.jacs.compute.api;


import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.status.TaskStatus;

import javax.ejb.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Jul 31, 2008
 * Time: 3:36:17 PM
 */
@Remote
public interface JobControlBeanRemote {
    public List getGridJobStatusesByTaskId(long taskId) throws RemoteException;

    public List getJobIdsByTaskId(long taskId) throws RemoteException;

    public List getJobIdsByTaskId(long taskId, String[] states) throws RemoteException;

    public GridJobStatus getGridJobStatus(long taskId, String jobId) throws RemoteException;

    public TaskStatus getTaskStatus(long taskId) throws RemoteException;

    public List<TaskStatus> getActiveTasks() throws RemoteException;

    public List<TaskStatus> getWaitingTasks() throws RemoteException;

    public TreeMap<Long, Long> getOrderedWaitingTasks() throws RemoteException;

    public void cancelTask(long taskId) throws RemoteException;

    public void cancelTask(Long taskId) throws RemoteException;

    public Integer getPercentCompleteForATask(long taskId) throws RemoteException;

    public void updateJobStatus(long taskId, String jobId, GridJobStatus.JobState state) throws RemoteException;

    public void updateJobInfo(long taskId, String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) throws RemoteException;

    public void saveGridJobStatus(GridJobStatus gridJobStatus) throws RemoteException;

    public void bulkAddGridJobStatus(long taskId, String queue, Set<String> jobIds, GridJobStatus.JobState state) throws RemoteException;

    public void bulkUpdateGridJobStatus(long taskId, Map<String, GridJobStatus.JobState> jobStates) throws RemoteException;

    public void bulkUpdateGridJobInfo(long taskId, Map<String,GridJobStatus.JobState> changedJobStateMap, Map<String, Map<String, String>> changedJobResourceMap) throws RemoteException;

    public void cleanUpJobStatus(long taskId) throws RemoteException;

    public void updateDispatcherJob(DispatcherJob job) throws RemoteException;
}
