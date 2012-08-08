
package org.janelia.it.jacs.compute.drmaa;

import org.janelia.it.jacs.model.status.GridJobStatus;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Sep 9, 2008
 * Time: 12:33:20 PM
 */
public interface JobStatusLogger {
    long getTaskId();

    void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state);

    void updateJobStatus(String jobId, GridJobStatus.JobState state);

    void bulkUpdateJobStatus(Map<String, GridJobStatus.JobState> jobStates);

    void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap);

    void bulkUpdateJobInfo(Map<String, GridJobStatus.JobState> changedJobStateMap, Map<String, Map<String, String>> changedJobResourceMap);

    void cleanUpData();
}
