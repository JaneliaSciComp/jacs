
package org.janelia.it.jacs.compute.api;


import org.janelia.it.jacs.model.jobs.DispatcherJob;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.status.TaskStatus;

import javax.ejb.Local;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Jul 30, 2008
 * Time: 11:28:08 AM
 * <p/>
 * An interface for using the job resource usage data
 */
@Local
public interface JobControlBeanLocal {

    public List getGridJobStatusesByTaskId(long taskId);

    public List getJobIdsByTaskId(long taskId);

    public List getJobIdsByTaskId(long taskId, String[] states);

    public GridJobStatus getGridJobStatus(long taskId, String jobId);

    public TaskStatus getTaskStatus(long taskId);

    public List<TaskStatus> getActiveTasks();

    public List<TaskStatus> getWaitingTasks();

    public TreeMap<Long, Long> getOrderedWaitingTasks();

    public Integer getPercentCompleteForATask(long taskId);

    public void updateJobStatus(long taskId, String jobId, GridJobStatus.JobState state);

    public void updateJobInfo(long taskId, String jobId, GridJobStatus.JobState state, Map<String, String> infoMap);

    public void saveGridJobStatus(GridJobStatus gridJobStatus);

    public void bulkAddGridJobStatus(long taskId, String queue, Set<String> jobIds, GridJobStatus.JobState state);

    public void bulkUpdateGridJobStatus(long taskId, Map<String, GridJobStatus.JobState> jobStates);

    public void cleanUpJobStatus(long taskId);

    public void cancelTask(long taskId);

    public void bulkUpdateGridJobInfo(long taskId, Map<String,GridJobStatus.JobState> changedJobStateMap, Map<String,Map<String, String>> changedJobResourceMap);

    public void updateDispatcherJob(DispatcherJob job);
}
