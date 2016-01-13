
package org.janelia.it.jacs.model.jobs;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
public class DispatcherJob implements Serializable {

    public static enum Status {
        PENDING,
        SUBMITTED
    }

    // Fields
    private Long dispatchId;
    private String dispatchDiscriminatorValue;
    private Status dispatchStatus = Status.PENDING;
    private String dispatchedTaskOwner;
    private Long dispatchedTaskId;
    private String dispatchHost;
    private Date creationDate = new Date();
    private Date dispatchedDate;

    public Long getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(Long dispatchId) {
        this.dispatchId = dispatchId;
    }

    public String getDispatchDiscriminatorValue() {
        return dispatchDiscriminatorValue;
    }

    public void setDispatchDiscriminatorValue(String dispatchDiscriminatorValue) {
        this.dispatchDiscriminatorValue = dispatchDiscriminatorValue;
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

    public Date getDispatchedDate() {
        return dispatchedDate;
    }

    public void setDispatchedDate(Date dispatchedDate) {
        this.dispatchedDate = dispatchedDate;
    }
}
