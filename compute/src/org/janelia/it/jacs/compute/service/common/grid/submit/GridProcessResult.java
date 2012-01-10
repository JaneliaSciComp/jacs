
package org.janelia.it.jacs.compute.service.common.grid.submit;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Jun 2, 2009
 * Time: 5:50:06 PM
 */
public class GridProcessResult implements Serializable {
    private String gridSubmissionKey;
    private long taskId;
    private boolean completed;
    private String error;

    public GridProcessResult() {
    }

    public GridProcessResult(long actualTaskId, boolean jobCompleted) {
        taskId = actualTaskId;
        completed = jobCompleted;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean result) {
        this.completed = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getGridSubmissionKey() {
        return gridSubmissionKey;
    }

    public void setGridSubmissionKey(String gridSubmissionKey) {
        this.gridSubmissionKey = gridSubmissionKey;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Task ID=").append(taskId).append("; ");
        sb.append("Result=").append((completed ? "completed" : "failed")).append("; ");
        sb.append("Error=").append(error).append("; ");
        return sb.toString();
    }
}
