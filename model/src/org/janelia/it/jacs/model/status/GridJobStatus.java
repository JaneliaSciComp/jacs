
package org.janelia.it.jacs.model.status;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Jul 21, 2008
 * Time: 2:32:41 PM
 */
public class GridJobStatus implements Serializable, IsSerializable {
    private transient Logger logger = Logger.getLogger(this.getClass());

    public enum JobState {
        DONE, RUNNING, FAILED, QUEUED, UNKNOWN, ERROR
    }

    // Fields

    private Long taskID;
    private String jobID;
    private transient Date submitTime;
    private transient Date startTime;
    private transient Date endTime;
    private Integer wallclock;
    private Integer userTime;
    private Integer systemTime;
    private Integer cpuTime;
    private Float memory;
    private Integer vmem;
    private Integer maxVMem;
    private Short exitStatus;
    private String status;
    private String queue;

    // Constructors

    /**
     * default constructor
     */
    public GridJobStatus() {
    }

    public GridJobStatus(long taskId, String jobId, String queue, JobState state) {
        this.taskID = taskId;
        this.jobID = jobId;
        this.queue = queue;
        setJobState(state);
    }

    // Property accessors

    public Long getTaskID() {
        return taskID;
    }

    public void setTaskID(Long taskID) {
        this.taskID = taskID;
    }

    public String getJobID() {
        return jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getWallclock() {
        return wallclock;
    }

    public void setWallclock(Integer wallclock) {
        this.wallclock = wallclock;
    }

    public Integer getUserTime() {
        return userTime;
    }

    public void setUserTime(Integer userTime) {
        this.userTime = userTime;
    }

    public Integer getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(Integer systemTime) {
        this.systemTime = systemTime;
    }

    public Integer getCpuTime() {
        return cpuTime;
    }

    public void setCpuTime(Integer cpuTime) {
        this.cpuTime = cpuTime;
    }

    public Float getMemory() {
        return memory;
    }

    public void setMemory(Float memory) {
        this.memory = memory;
    }

    public Integer getVmem() {
        return vmem;
    }

    public void setVmem(Integer vmem) {
        this.vmem = vmem;
    }

    public Integer getMaxVMem() {
        return maxVMem;
    }

    public void setMaxVMem(Integer maxVMem) {
        this.maxVMem = maxVMem;
    }

    public Short getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(Short exitStatus) {
        this.exitStatus = exitStatus;
    }

    // external interfaces should use public interface.
    // direct access to status is for Hibernate use only
    public JobState getJobState() {
        return JobState.valueOf(status);
    }

    public void setJobState(JobState state) {
        this.status = state.name();
    }

    protected String getStatus() {
        return status;
    }

    protected void setStatus(String status) {
        this.status = status;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    // Update fields from a Map
    public void updateFromMap(Map<String, String> resources) {
        if (null==resources) {
            logger.warn("Null resources for task="+taskID+", job="+jobID);
            return;
        }
        if (logger.isTraceEnabled()) {
            // print our the whole map
            StringBuilder sb = new StringBuilder();
            sb.append("Parsing jobInfo Map:\n     ");
            for (String key : resources.keySet()) {
                sb.append(" ").append(key).append(":").append(resources.get(key)).append(" | ");
            }
            logger.trace(sb.toString());
        }

        DecimalFormat df = new DecimalFormat("0.0000");

        try {
            if (null != resources.get("submission_time")) {
                setSubmitTime(new Date(df.parse(resources.get("submission_time")).longValue() ));
            }
        }
        catch (Throwable t) {
            logger.error("Error parsing submission_time", t);
        }

        try {
            if (null != resources.get("start_time")) {
                setStartTime(new Date(df.parse(resources.get("start_time")).longValue() ));
            }
        }
        catch (Throwable t) {
            logger.error("Error parsing start_time", t);
        }

        try {
            if (null != resources.get("end_time")) {
                setEndTime(new Date(df.parse(resources.get("end_time")).longValue() ));
            }
        }
        catch (Throwable t) {
            logger.error("Error parsing end_time", t);
        }

        try {
            if (null != resources.get("vmem")) {
                setVmem(df.parse(resources.get("vmem")).intValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing vmem");
            setVmem(0);
        }

        try {
            if (null != resources.get("ru_stime")) {
                setSystemTime(df.parse(resources.get("ru_stime")).intValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing ru_stime");
        }

        try {
            if (null != resources.get("ru_utime")) {
                setUserTime(df.parse(resources.get("ru_utime")).intValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing ru_utime");
        }

        try {
            if (null != resources.get("ru_wallclock")) {
                setWallclock(df.parse(resources.get("ru_wallclock")).intValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing ru_wallclock");
        }

        try {
            if (null != resources.get("cpu")) {
                setCpuTime(df.parse(resources.get("cpu")).intValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing cpu");
        }

        try {
            if (null != resources.get("maxvmem")) {
                setMaxVMem(df.parse(resources.get("maxvmem")).intValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing maxvmem");
        }

        try {
            if (null != resources.get("exit_status")) {
                setExitStatus(df.parse(resources.get("exit_status")).shortValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing exit_status");
        }

        try {
            if (null != resources.get("mem")) {
                setMemory(df.parse(resources.get("mem")).floatValue());
            }
        }
        catch (Throwable t) {
            logger.debug("Error parsing mem");
        }

    }

    public String toString() {
        return "Job " + jobID + " of task " + taskID + " resoursce usage: { " +
                "submission time: " + submitTime +
                ", start time: " + startTime +
                ", end time: " + endTime +
                ", wallclock seconds: " + wallclock +
                ", user time (seconds)" + userTime +
                ", system time (seconds)" + systemTime +
                ", cpu time (seconds)" + cpuTime +
                ", integral memory usage (Gbytes/s): " + memory +
                ", virtual memory: " + vmem +
                ", max virtual memory used: " + maxVMem +
                ", exit status: " + exitStatus +
                ", status: " + status +
                ", queue: " + queue +
                '}';
    }
}