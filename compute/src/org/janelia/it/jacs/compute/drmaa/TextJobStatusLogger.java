
package org.janelia.it.jacs.compute.drmaa;

import org.janelia.it.jacs.model.status.GridJobStatus;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: May 29, 2009
 * Time: 3:25:21 PM
 */
public class TextJobStatusLogger implements JobStatusLogger {
    private PrintWriter writer;

    public TextJobStatusLogger(PrintWriter pw) {
        writer = pw;
    }

    public TextJobStatusLogger(PrintStream ps) {
        writer = new PrintWriter(ps);
    }

    public TextJobStatusLogger(OutputStream os) {
        writer = new PrintWriter(os);
    }

    public long getTaskId() {
        return 0;
    }

    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
        writer.print("Processing new set of jobs on queue '" + queue + "' :");
        for (String id : jobIds)
            writer.print(" " + id);

        writer.println();
    }

    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
        writer.println("Job " + jobId + " status changed to :" + state);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void bulkUpdateJobStatus(Map<String, GridJobStatus.JobState> jobStates) {
        for (String id : jobStates.keySet()) {
            updateJobStatus(id, jobStates.get(id));
        }
    }

    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
        GridJobStatus tmpStatus = new GridJobStatus();
        if (tmpStatus != null) {
            tmpStatus.setJobState(state);
            tmpStatus.updateFromMap(infoMap);
        }
        writer.println("Job's " + jobId + " status changed:");
        writer.println(tmpStatus.toString());

    }

    @Override
    public void bulkUpdateJobInfo(Map<String, GridJobStatus.JobState> changedJobStateMap, Map<String, Map<String, String>> changedJobResourceMap) {
        try {
            for (String jobId : changedJobStateMap.keySet()) {
                updateJobInfo(jobId, changedJobStateMap.get(jobId), changedJobResourceMap.get(jobId));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanUpData() {
        // NOTHING TO DO HERE
    }

}