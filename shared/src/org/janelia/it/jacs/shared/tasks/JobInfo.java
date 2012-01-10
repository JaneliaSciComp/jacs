
package org.janelia.it.jacs.shared.tasks;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 30, 2006
 * Time: 2:41:26 PM
 */
public class JobInfo implements IsSerializable, Serializable {
    // todo Why is this here exactly?  There must be a better place for view sorting constants
    public static final String SORT_BY_JOB_ID = "jobid";
    public static final String SORT_BY_JOB_NAME = "jobName";
    public static final String SORT_BY_SUBMITTER = "owner";
    public static final String SORT_BY_STATUS = "status";
    public static final String SORT_BY_SUBMITTED = "submitted";

    private String _jobname;
    private Integer numTaskMessages;

    private String _username;
    private String _jobId; // task id
    private String _jobNote;

    // Leaving these four here since these are common to Recruitment and Blast
    private String _queryName;
    private String _subjectName;
    private Long _numHits;
    private String _numHitsFormatted;

    private String _status;
    private String _statusDescription;
    private Date _submitted;

    // Order of the job among the waiting jobs on the Grid
    private Integer _jobOrder;

    private Boolean isWaitingOnTheGrid = false;
    private Boolean isRunningOnTheGrid = false;

    private Integer _percentComplete;

    /**
     * Required by the Google Web Toolkit compiler - Must be directly before the argument
     */
    private Map<String, String> _paramMap;

    public JobInfo() {
    }

    public String getJobname() {
        return _jobname;
    }

    public void setJobname(String jobname) {
        this._jobname = jobname;
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String username) {
        this._username = username;
    }

    public String getJobId() {
        return _jobId;
    }

    public void setJobId(String jobId) {
        this._jobId = jobId;
    }

    public String getQueryName() {
        return _queryName;
    }

    public void setQueryName(String queryName) {
        this._queryName = queryName;
    }

    public String getSubjectName() {
        return _subjectName;
    }

    public void setSubjectName(String subjectName) {
        _subjectName = subjectName;
    }

    public String getStatus() {
        return _status;
    }

    public void setStatus(String status) {
        this._status = status;
    }

    public String getStatusDescription() {
        return _statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this._statusDescription = statusDescription;
    }

    public Date getSubmitted() {
        return _submitted;
    }

    public void setSubmitted(Date submitted) {
        this._submitted = submitted;
    }

    public Map<String, String> getParamMap() {
        return _paramMap;
    }

    public void setParamMap(Map<String, String> paramMap) {
        this._paramMap = paramMap;
    }

    public void setNumHits(Long numHits) {
        _numHits = numHits;
    }

    public Long getNumHits() {
        return _numHits;
    }

    public String getNumHitsFormatted() {
        return _numHitsFormatted;
    }

    public void setNumHitsFormatted(String numHitsFormatted) {
        _numHitsFormatted = numHitsFormatted;
    }

    public boolean jobCompletedSuccessfully() {
        /** Nasty, but seems to be the only way to determine if the job was successful */
        return getNumTaskMessages() == null || getNumTaskMessages() == 0;
    }

    public Integer getNumTaskMessages() {
        return numTaskMessages;
    }

    public void setNumTaskMessages(Integer numTaskMessages) {
        this.numTaskMessages = numTaskMessages;
    }

    public Integer getJobOrder() {
        return _jobOrder;
    }

    public void setJobOrder(Integer jobOrder) {
        this._jobOrder = jobOrder;
    }

    public Boolean isWaitingOnTheGrid() {
        return this.isWaitingOnTheGrid;
    }

    public void setIsWaitingOnTheGrid(Boolean waiting) {
        isWaitingOnTheGrid = waiting;
    }

    public Boolean isRunningOnTheGrid() {
        return this.isRunningOnTheGrid;
    }

    public void setIsRunningOnTheGrid(Boolean running) {
        isRunningOnTheGrid = running;
    }

    public Integer getPercentComplete() {
        return _percentComplete;
    }

    public void setPercentComplete(Integer percentComplete) {
        this._percentComplete = percentComplete;
    }

    public String getJobNote() {
        return _jobNote;
    }

    public void setJobNote(String _jobNote) {
        this._jobNote = _jobNote;
    }

    public String toString() {
        return "JobInfo{" +
                ", _jobname='" + _jobname + '\'' +
                ", _username='" + _username + '\'' +
                ", _jobId='" + _jobId + '\'' +
                ", _status='" + _status + '\'' +
                ", _note='" + _jobNote + '\'' +
                ", _statusDescription='" + _statusDescription + '\'' +
                ", _submitted=" + _submitted +
                '}';
    }

}
