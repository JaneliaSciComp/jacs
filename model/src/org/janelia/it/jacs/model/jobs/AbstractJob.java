
package org.janelia.it.jacs.model.jobs;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
public class AbstractJob implements Serializable {

    public static enum Status {
        PENDING,
        IN_PROGRESS,
        SUBMITTED,
        FAILED
    }

    // Fields
    private Long dispatchId;
    private String processDefnName;
    private Status dispatchStatus = Status.PENDING;
    private String dispatchedTaskOwner;
    private Long dispatchedTaskId;
    private String dispatchHost;
    private Date creationDate = new Date();
    private int retries = 0;
    private Date dispatchedDate;

    public Long getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(Long dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getProcessDefnName() {
        return processDefnName;
    }

    public void setProcessDefnName(String processDefnName) {
        this.processDefnName = processDefnName;
    }

    public Status getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(Status dispatchStatus) {
        this.dispatchStatus = dispatchStatus;
    }

    public String getStatus() {
        return dispatchStatus.name();
    }

    public void setStatus(String status) {
        this.dispatchStatus = Status.valueOf(status);
    }

    public Long getDispatchedTaskId() {
        return dispatchedTaskId;
    }

    public void setDispatchedTaskId(Long dispatchedTaskId) {
        this.dispatchedTaskId = dispatchedTaskId;
    }

    public String getDispatchedTaskOwner() {
        return dispatchedTaskOwner;
    }

    public void setDispatchedTaskOwner(String dispatchedTaskOwner) {
        this.dispatchedTaskOwner = dispatchedTaskOwner;
    }

    public String getDispatchHost() {
        return dispatchHost;
    }

    public void setDispatchHost(String dispatchHost) {
        this.dispatchHost = dispatchHost;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public void incRetries() {
        ++retries;
    }

    public Date getDispatchedDate() {
        return dispatchedDate;
    }

    public void setDispatchedDate(Date dispatchedDate) {
        this.dispatchedDate = dispatchedDate;
    }

    public void copyTo(AbstractJob dest) {
        dest.dispatchId = this.dispatchId;
        dest.processDefnName = this.processDefnName;
        dest.dispatchStatus = this.dispatchStatus;
        dest.dispatchedTaskOwner = this.dispatchedTaskOwner;
        dest.dispatchedTaskId = this.dispatchedTaskId;
        dest.dispatchHost = this.dispatchHost;
        dest.creationDate = this.creationDate;
        dest.retries = this.retries;
        dest.dispatchedDate = this.dispatchedDate;
    }
}
