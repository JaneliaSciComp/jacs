/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.model.status;

//import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Formatter;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Aug 5, 2008
 * Time: 3:58:17 PM
 */
public class TaskStatus implements Serializable {
    // Fields
    // Constants
    public static final String HEADER_COMMON = "        Task ID        |  Jobs  |        Date submitted        |   Queue   |  Done  |";
    public static final String SEPARATOR_COMMON = "-----------------------|--------|------------------------------|-----------|--------|";
    public static final String HEADER_COMPLETE = " Wallclock | CPU Time | Min Wait | Max Wait";
    public static final String SEPARATOR_COMPLETE = "-----------|----------|----------|---------";
    public static final String HEADER_NOT_COMPLETE = " Queued | Running";
    public static final String SEPARATOR_NOT_COMPLETE = "--------|--------";
    private static final String commonFormat = " %21d | %6d | %28tc | %9s | %6d |";
    private static final String completeFormat = " %9d | %8d | %8d | %8d";
    private static final String nCompleteFormat = " %6d | %6d";

    // Common
    private Long taskId;
    private Boolean isCompleted = false;
    private Boolean isWaiting = false;
    private Boolean isRunning = false;
    private Boolean isFailed = false;
    private Date submitTime = null;
    private String queue = null;
    private Integer totalJobs = 0;


    // CompletedTask
    private Integer wallclock = 0;
    private Integer cpuTime = 0;
    private Integer minWait = Integer.MAX_VALUE;
    private Integer maxWait = 0;
    private Date endTime;

    // NotComlpetedTask
    private Integer jobsWaiting;
    private Integer jobsRunning;
    private Integer jobsFailed;
    private Integer jobsDone;

    Map<GridJobStatus.JobState, Integer> jobStates = new HashMap<GridJobStatus.JobState, Integer>(); // count statuses

    // Constructors
    public TaskStatus() {
    }


    public TaskStatus(Long taskId, List<GridJobStatus> jobs) {
//        Map accountants = computeDAO.getAccountantsByTaskId(taskId);
//        Set<String> jobs = accountants.keySet();
//        TaskStatus taskStatus = new TaskStatus();

        //logger.debug(" Creating new TaskStatus for " + taskId + " with " + jobs.size() + " jobs.");

        // set common items
        this.taskId = taskId;
        this.totalJobs = jobs.size();
        if (this.totalJobs > 0) {
            GridJobStatus firstJob = jobs.get(0);
            this.queue = firstJob.getQueue();
        }
        // calculate stats
        submitTime = new Date();
        Date endTime = new Date(0);

        //Map<GridJobStatus.JobState,Integer> jobStates = new HashMap<GridJobStatus.JobState,Integer>(); // count statuses

        for (GridJobStatus gridJobStatus : jobs) {
            //logger.debug(" Processing GridJob " + gridJobStatus.getJobID() + " with job state " + gridJobStatus.getJobState());

            if (gridJobStatus.getCpuTime() != null && gridJobStatus.getWallclock() != null) {
                wallclock += gridJobStatus.getWallclock();
                cpuTime += gridJobStatus.getCpuTime();
            }
            if (gridJobStatus.getSubmitTime() != null && gridJobStatus.getStartTime() != null) {
                int waitTime = (int) ((gridJobStatus.getStartTime().getTime() - gridJobStatus.getSubmitTime().getTime()) / 1000);
                if (waitTime > maxWait) maxWait = waitTime;
                if (waitTime < minWait) minWait = waitTime;
            }
            if (gridJobStatus.getSubmitTime() != null && submitTime.after(gridJobStatus.getSubmitTime()))
                submitTime = gridJobStatus.getSubmitTime();
            if (gridJobStatus.getEndTime() != null && endTime.before(gridJobStatus.getEndTime()))
                endTime = gridJobStatus.getEndTime();

            GridJobStatus.JobState state = gridJobStatus.getJobState();
            Integer num = jobStates.get(state);

            if (num == null) {
                num = 0;
            }
            //logger.debug(" Number of jobs in state " + state + " are " + num);

            jobStates.put(state, num + 1);
        } // end of looping thorugh the jobs

        //logger.debug("Map with state and the job count " + jobStates.toString());

        this.jobsDone = jobStates.get(GridJobStatus.JobState.DONE);
        //logger.debug("Jobs Done count " + this.jobsDone + " Total Jobs " + this.totalJobs);

        if (this.jobsDone != null) {
            if (this.jobsDone.equals(this.totalJobs)) {
                this.isCompleted = true;
                this.endTime = endTime;
            }
        }
        else {
            this.jobsDone = 0;
        }
        //logger.debug(" IsCompleted variable " + this.isCompleted);

        this.jobsRunning = jobStates.get(GridJobStatus.JobState.RUNNING);
        //logger.debug(" Jobs Running " + this.jobsRunning);
        if (this.jobsRunning == null)
            this.jobsRunning = 0;
        //logger.debug(" Jobs Running " + this.jobsRunning);

        this.jobsWaiting = jobStates.get(GridJobStatus.JobState.QUEUED);
        //logger.debug(" Jobs Waiting " + this.jobsWaiting);
        if (this.jobsWaiting == null)
            this.jobsWaiting = 0;
        //logger.debug(" Jobs Waiting" + this.jobsWaiting);

        this.jobsFailed = jobStates.get(GridJobStatus.JobState.FAILED);
        //logger.debug(" Jobs Failed " + this.jobsFailed);
        if (this.jobsFailed == null)
            this.jobsFailed = 0;
        //logger.debug(" Jobs Failed " + this.jobsFailed);


        //logger.debug(getStatusDetails());

        // Set isRunning
        if (this.jobsRunning > 0) {
            this.isRunning = true;
        }

        if ((this.jobsDone > 1) && (this.jobsWaiting > 1)) {
            this.isRunning = true;
        }

        //logger.debug(" IsRunning variable " + this.isRunning);

        // Set isWaiting
        if (this.jobsWaiting.equals(this.totalJobs)) {
            this.isWaiting = true;
        }

        //logger.debug(" IsWaiting variable " + this.isWaiting);

        // set isFailed
        if ((this.jobsFailed > 1) && (this.jobsRunning == 0) && (this.jobsWaiting == 0)) {
            this.isFailed = true;
            this.endTime = endTime;
        }

        //logger.debug(getStatusDetails());

        /*   Time interval = new Time(endTime.getTime()-submitTime.getTime());
   long year = 31536000; year *= 1000;
   if (interval.getTime() < year) result.put("Total task time:", interval);
   else result.put("Total task time", null); */

    }
    // Property accessors

    public Map getGridJobStatusMap() {
        return jobStates;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(Boolean set) {
        isCompleted = set;
    }

    public Boolean isRunning() {
        return isRunning;
    }

    public void setRunning(Boolean set) {
        isRunning = set;
    }

    public Boolean isWaiting() {
        return isWaiting;
    }

    public void setWaiting(Boolean set) {
        isWaiting = set;
    }

    public Boolean isFailed() {
        return isFailed;
    }

    public void setFailed(Boolean set) {
        isFailed = set;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public Integer getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(Integer totalJobs) {
        this.totalJobs = totalJobs;
    }

    public Integer getJobsDone() {
        return this.jobsDone;
    }

    public void setJobsDone(Integer jobsDone) {
        this.jobsDone = jobsDone;
    }

    public Integer getWallclock() {
        return wallclock;
    }

    public void setWallclock(Integer wallclock) {
        this.wallclock = wallclock;
    }

    public Integer getCpuTime() {
        return cpuTime;
    }

    public void setCpuTime(Integer cpuTime) {
        this.cpuTime = cpuTime;
    }

    public Integer getMinWait() {
        return minWait;
    }

    public void setMinWait(Integer minWait) {
        this.minWait = minWait;
    }

    public Integer getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Integer maxWait) {
        this.maxWait = maxWait;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getJobsWaiting() {
        return this.jobsWaiting;
    }

    public void setJobsWaiting(Integer jobsWaiting) {
        this.jobsWaiting = jobsWaiting;
    }

    public Integer getJobsRunning() {
        return this.jobsRunning;
    }

    public void setJobsRunning(Integer jobsRunning) {
        this.jobsRunning = jobsRunning;
    }

    public Integer getjobsFailed() {
        return jobsFailed;
    }

    public void setJobsFailed(Integer jobsFailed) {
        this.jobsFailed = jobsFailed;
    }

    public static String getCompleteHeader() {
        return HEADER_COMMON + HEADER_COMPLETE + "\n" + SEPARATOR_COMMON + SEPARATOR_COMPLETE + "\n";
    }
    //  public static String getCompleteSeparator() { return E;}

    public static String getNotCompleteHeader() {
        return HEADER_COMMON + HEADER_NOT_COMPLETE + "\n" + SEPARATOR_COMMON + SEPARATOR_NOT_COMPLETE + "\n";
    }
    //  public static String getNotCompleteSeparator() { return ;}


    // TO STRING

    public String toString() {
//        GridJobStatus formatter = new GridJobStatus();
//        Formatter formatter = new Formatter();
//        formatter.format(commonFormat,taskId,totalJobs,submitTime,queue,jobsDone);
//        if (isCompleted) formatter.format(completeFormat + "\n",wallclock,cpuTime,minWait,maxWait);
//        else formatter.format(nCompleteFormat + "\n",jobsWaiting,jobsRunning);

        String result = String.format(commonFormat, taskId, totalJobs, submitTime, queue, jobsDone);
        if (isCompleted)
            result += String.format(completeFormat + "\n", wallclock, cpuTime, minWait, maxWait);
        else
            result += String.format(nCompleteFormat + "\n", jobsWaiting, jobsRunning);
        return result;
    }

    public String getStatusDetails() {

        return " Task Id    = " + taskId + "\n" +
                " Submit Time = " + submitTime + "\n" +
                " Total Jobs = " + totalJobs + "\n" +
                " Jobs Done  = " + jobsDone + "\n" +
                " Jobs Running " + jobsRunning + "\n" +
                " Jobs Waiting " + jobsWaiting + "\n" +
                " Jobs Failed  " + jobsFailed + "\n" +
                " isComplete   " + isCompleted() + "\n" +
                " isRunning    " + isRunning() + "\n" +
                " isWaiting    " + isWaiting() + "\n";
    }

}
