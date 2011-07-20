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

package org.janelia.it.jacs.compute.drmaa;

import org.apache.log4j.Logger;
import org.ggf.drmaa.*;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.IOUtils;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Feb 17, 2007
 * Time: 9:47:41 AM
 */
public class DrmaaHelper {
    private Logger logger;

    private Session sunSession;                 // DRMAA Session
    private static boolean initialized = false;

    private static final String MAX_STATUS_CHECK_INTERVAL_PROP = "BlastServer.DrmaaMaxStatusCheckInterval";
    private static final int MAX_STATUS_CHECK_INTERVAL = SystemConfigurationProperties.getInt(MAX_STATUS_CHECK_INTERVAL_PROP, 30000);
    private static final int MIN_STATUS_CHECK_INTERVAL = 1000; // one second
    private static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    private static final String DRMAA_SUBMITTER_JAR_PATH = SystemConfigurationProperties.getFilePath("Local.Lib.Path", "Drmaa.Submitter.Jar.Name");
    private static final String DRMAA_SUBMITTER_SCRIPT_PATH = SystemConfigurationProperties.getFilePath("Local.Lib.Path", "Drmaa.Submitter.Script.Name");
    public static final String ROOT_SERVER_DIR_PROP = "ServerRoot.Dir";
    private static HashSet<String> _projectCodes = new HashSet<String>();
    //Map<String, Integer> currentStatusMap = new HashMap<String, Integer>();
    private Map<String, Integer> currentStatusMap = new HashMap<String, Integer>();


    private String errorText = "";
    private String shellReturnMethod = DrmaaSubmitter.OPT_RETURN_VIA_QUEUE_VAL;

    private String submissionKey = "";
    private int statusPoolPeriod = -1;

    public DrmaaHelper(Logger logger) throws DrmaaException {
        this.logger = logger;
        SessionFactory factory = SessionFactory.getFactory();   // DRMAA Session Factory - not hibernate
        sunSession = factory.getSession();                 // DRMAA Session
        synchronized (sunSession) {
            if (!initialized) {
                sunSession.init(null);
                initialized = true;
            }
        }
    }


    public static GridJobStatus.JobState translateStatusCode(int drmaaStatus) {
        GridJobStatus.JobState newState;

        if (drmaaStatus == Session.DONE) {
            newState = GridJobStatus.JobState.DONE;
        }
        else if (drmaaStatus == Session.QUEUED_ACTIVE) {
            newState = GridJobStatus.JobState.QUEUED;
        }
        else if (drmaaStatus == Session.RUNNING) {
            newState = GridJobStatus.JobState.RUNNING;
        }
        else if (drmaaStatus == Session.FAILED) {
            newState = GridJobStatus.JobState.FAILED;
        }
        else {
            newState = GridJobStatus.JobState.UNKNOWN;
        }

        return newState;
    }

    /**
     * this method should only be used on shutdown!
     *
     * @throws DrmaaException error with something inside the DRMAA submission library
     */
    public void exit() throws DrmaaException {
        //To change body of implemented methods use File | Settings | File Templates.
        synchronized (sunSession) {
            //TODO: check if all jobs are completed
            if (initialized) {
                sunSession.exit();
                initialized = false;
            }
        }

    }

    public SerializableJobTemplate createJobTemplate() throws DrmaaException {
        return createJobTemplate(new SerializableJobTemplate());
    }

    public SerializableJobTemplate createJobTemplate(SerializableJobTemplate existingTemplate) throws DrmaaException {
        synchronized (sunSession) {
            JobTemplate jt = sunSession.createJobTemplate();
            existingTemplate.setDrmaaJobTemplate(jt);
            return existingTemplate;
        }
    }

    public SerializableJobTemplate loadTemplate(String jtFileName) throws DrmaaException, IOException, ClassNotFoundException {
        File jtFile = new File(jtFileName);
        // Read object using ObjectInputStream
        ObjectInputStream objStream = new ObjectInputStream(new FileInputStream(jtFile));

        // Read an object
        Object obj = objStream.readObject();
        SerializableJobTemplate sjt = (SerializableJobTemplate) obj;
        objStream.close();

        // inject DRMAA template and transfer data to it
        JobTemplate jt = sunSession.createJobTemplate();
        sjt.setDrmaaJobTemplate(jt);
        sjt.flush();
        return sjt;
    }

    public void deleteJobTemplate(SerializableJobTemplate jobTemplate) throws DrmaaException {
        synchronized (sunSession) {
            sunSession.deleteJobTemplate(jobTemplate.getDrmaaJobTemplate());
        }
    }

    public Process runJobThroughShell(long taskId, String userId, String workingDir, SerializableJobTemplate jobTemplate) throws IOException, InterruptedException, DrmaaException {
        return runBulkJobsThroughShell(taskId, userId, workingDir, jobTemplate, -1, -1, -1, -1);
    }

    public String runJob(SerializableJobTemplate jobTemplate) throws DrmaaException {
        synchronized (sunSession) {
            jobTemplate.flush();
            return sunSession.runJob(jobTemplate.getDrmaaJobTemplate());
        }
    }

    public Process runBulkJobsThroughShell(long taskId, String userId, String workingDir,
                                           SerializableJobTemplate jobTemplate, int start, int end, int incr, int timeoutInSeconds) throws IOException, InterruptedException, DrmaaException {
        return runJobThroughShell(taskId, workingDir, jobTemplate, start, end, incr, timeoutInSeconds);
    }

    public Set<String> runBulkJobs(SerializableJobTemplate jt, int start, int end, int incr) throws DrmaaException {
        synchronized (sunSession) {
            jt.flush();
            return new HashSet<String>(sunSession.runBulkJobs(jt.getDrmaaJobTemplate(), start, end, incr));
        }
    }

    public void control(String jobId, int action) throws DrmaaException {
        synchronized (sunSession) {
            sunSession.control(jobId, action);
        }
    }

    public boolean waitForJob(String jobId, String logPrefix) throws Exception {
        return waitForJob(jobId, logPrefix, null, -1);
    }

    private boolean waitForJob(String jobId, String logPrefix, JobStatusLogger statusLogger, int statusPoolPeriod) throws Exception {
        Set<String> jobSet = new HashSet<String>();
        jobSet.add(jobId);
        return waitForJobs(jobSet, logPrefix, statusLogger, statusPoolPeriod, -1);
    }
                                                    
//    public boolean waitForJobs(Set<String> jobSet, String logPrefix) throws InterruptedException {
//        return waitForJobs(jobSet, logPrefix, null, -1);
//    }

    /**
     * Method to wait for the jobs to finish on the Sun Grid
     *
     *
     * @param jobSet           set of jobs to watch on the grid
     * @param logPrefix        prefix string for the log file
     * @param statusLogger     logger of the status of the grid job
     * @param statusPoolPeriod - milliseconds. if < 0 - use default increasing pooling period
     * @param timeoutInSeconds the amount of time to wait for this job before killing it. -1 waits forever
     * @return boolean boolean whether the job failed or succeeded
     * @throws InterruptedException interrupt for the waiting action
     */
    public boolean waitForJobs(Set<String> jobSet, String logPrefix, JobStatusLogger statusLogger, int statusPoolPeriod,
                               int timeoutInSeconds) throws Exception {
        //String logPrefix = "Computing results for " +resultNode.getObjectId() + " - Grid status: ";
        Date startTime=null;
        int jobStatus;
        String mainJobID = getMainJobId(jobSet);
        // changedJobs - map of jobs that changed it's status since last check
        Map<String, GridJobStatus.JobState> changedJobs = null;
        int dynamicCheckInterval = MIN_STATUS_CHECK_INTERVAL; // start with 2 seconds

        // prepopulate current status map
        for (String id : jobSet)
            currentStatusMap.put(id, Session.QUEUED_ACTIVE);

        while (currentStatusMap.size() > 0) {
            // If timeout is set, check for expiration
            try {
                if (0<timeoutInSeconds) {
                    Date elapsedDate = new Date();
                    if (null!=startTime && ((elapsedDate.getTime()-startTime.getTime())/1000)>timeoutInSeconds) {
                        sunSession.control(mainJobID, Session.TERMINATE);
                        throw new Exception("The grid job ("+mainJobID+") exceeded the timeout specified: "+timeoutInSeconds+" seconds.");
                    }
                }
            }
            catch (DrmaaException e) {
                logger.error("Unable to verify the timeout mechanism.  Continuing...");
            }
            // sleep for a short while then restart loop
            int nappingTime = (statusPoolPeriod > 0) ? statusPoolPeriod : dynamicCheckInterval;
            if (logger.isDebugEnabled())
                logger.debug(logPrefix + " Going to sleep for " + nappingTime + " milliseconds");
            Thread.sleep(nappingTime);

            // get status changes and update current map
            // retry loop is an attempt to circumvent occasional SGE connection problems
            int retry = 0;
            int maxRetries = 3;
            while (retry < maxRetries) {
                try {
                    changedJobs = getBulkJobStatusChanges(currentStatusMap);
                    break;
                }
                catch (DrmaaException e) {
                    logger.error("Caught DrmaaException while checking job status: " + e.toString() + ". Retry=" + retry + " of " + maxRetries);
                    errorText = e.getMessage();
                    if (++retry >= maxRetries)
                        return false;
                    else
                        Thread.sleep(60000); // one minute
                }
            }

            // update status log
            if (statusLogger != null) {
                statusLogger.bulkUpdateJobStatus(changedJobs);
            }
            // reset doneList
//            doneList.clear();
            for (String jobId : changedJobs.keySet()) {
                if (logger.isDebugEnabled()) {
                    String newState;
                    switch (currentStatusMap.get(jobId)) {
                        case Session.DONE:
                            newState = " is DONE";
                            break;
                        case Session.QUEUED_ACTIVE:
                            newState = " has been QUEUED";
                            break;
                        case Session.RUNNING:
                            // Initialize the startTime, once, when the first element goes Running
                            if (null==startTime) {
                                startTime = new Date();
                            }
                            newState = " has been sent to GRID";
                            break;
                        case Session.FAILED:
                            newState = " has finished with ERROR";
                            break;
                        default:
                            newState = " is now in the UNKNOWN state (" + currentStatusMap.get(jobId) + ")";
                    }
                    logger.debug(logPrefix + " Job " + jobId + newState);
                }
                jobStatus = currentStatusMap.get(jobId);
                JobInfo jobInfo = null;
                Map<String, String> infoMap = null;
                if (jobStatus == Session.DONE || jobStatus == Session.FAILED) {
                    try {
                        jobInfo = wait(jobId, Session.TIMEOUT_NO_WAIT);
                        infoMap = (Map<String, String>)jobInfo.getResourceUsage();
                    }
                    catch (ExitTimeoutException e) {
                        // this is a valid error when job is done
                    }
                    catch (DrmaaException e) {
                        logger.error("Caught DrmaaException while checking job status: " + e.toString());
                        errorText = e.getMessage();
                        return false;
                    }
                }

                // update job info data
                if (statusLogger != null && infoMap != null) {
                    statusLogger.updateJobInfo(jobId, changedJobs.get(jobId), infoMap);
                }

                // if done - remove from set and continue
                // if failed - return error
                // if not done - anything else - wait it out
                if (jobStatus == Session.DONE) {
                    // no need to check on this one anymore
                    currentStatusMap.remove(jobId);
                }
                else if (jobStatus == Session.FAILED) {
                    logJobError(jobInfo, jobId);
                    errorText = "Job " + jobId + " failed on compute grid";
                    return false;
                }
            } // end of for loop
            // if there where no changes - increase wait time
            if (changedJobs.size() == 0) {
                // - at least one is not done - break inner loop, and increase wait time
                if (dynamicCheckInterval < MAX_STATUS_CHECK_INTERVAL)
                    dynamicCheckInterval *= 2;
            }
            else // reset interval
            {
                // reset check interval to min - jobs are coming back now
                dynamicCheckInterval = MIN_STATUS_CHECK_INTERVAL;
            }
        } // end of while loop
        if (logger.isDebugEnabled())
            logger.debug(logPrefix + " - all parts are done");
        return true;
    }

    public static String getMainJobId(Set<String> jobSet) {
        String mainJobID = jobSet.iterator().next();
        if (jobSet.size() > 1) {
            mainJobID = mainJobID.substring(0, mainJobID.lastIndexOf('.')) + ".N";
        }
        return mainJobID;
    }

    public String getError() {
        return errorText;
    }

    /**
     * Unlike original Session this one will disallow unlimited wait
     *
     * @param jobId   the job to look for
     * @param timeout wait limit
     * @return returns a DRMAA JobInfo object
     * @throws DrmaaException error inside DRMAA submission library
     */
    public JobInfo wait(String jobId, long timeout) throws DrmaaException {
        assert (timeout != Session.TIMEOUT_WAIT_FOREVER) : "Invalid timeout specified";
        synchronized (sunSession) {
            return sunSession.wait(jobId, timeout);
        }
    }

    public int getJobProgramStatus(String jobId) throws DrmaaException {
        synchronized (sunSession) {
            return sunSession.getJobProgramStatus(jobId);
        }
    }

    public Map<String, GridJobStatus.JobState> getBulkJobStatusChanges(Map<String, Integer> jobStatusMap) throws DrmaaException {
        synchronized (sunSession) {
            Map<String, GridJobStatus.JobState> statusMap = new HashMap<String, GridJobStatus.JobState>();
            for (String jobId : jobStatusMap.keySet()) {
                int status = sunSession.getJobProgramStatus(jobId);
                if (status != jobStatusMap.get(jobId)) {
                    statusMap.put(jobId, translateStatusCode(status));
                    jobStatusMap.put(jobId, status); // reset original
                }
            }
            return statusMap;
        }
    }

    public Map<String, GridJobStatus.JobState> getBulkJobStatus(Map<String, Integer> jobStatusMap) throws DrmaaException {
        synchronized (sunSession) {
            Map<String, GridJobStatus.JobState> statusMap = new HashMap<String, GridJobStatus.JobState>();
            for (String jobId : jobStatusMap.keySet()) {
                int status = sunSession.getJobProgramStatus(jobId);
                statusMap.put(jobId, translateStatusCode(status));
                jobStatusMap.put(jobId, status); // reset original
            }
            return statusMap;
        }
    }


    public String getContact() {
        synchronized (sunSession) {
            return sunSession.getContact();
        }
    }

    public Version getVersion() {
        synchronized (sunSession) {
            return sunSession.getVersion();
        }
    }

    public String getDrmSystem() {
        synchronized (sunSession) {
            return sunSession.getDrmSystem();
        }
    }

    public String getDrmaaImplementation() {
        synchronized (sunSession) {
            return sunSession.getDrmaaImplementation();
        }
    }

    private void logJobError(JobInfo jobInfo, String jobId) {
        if (jobInfo != null && logger.isDebugEnabled()) {
            try {
                StringBuffer jobstats = new StringBuffer();
                Map<String, String> infoMap = jobInfo.getResourceUsage();
                for (String key : infoMap.keySet()) {
                    jobstats.append("\t").append(key).append(": ").append(infoMap.get(key)).append("\n");
                }
                logger.error("DRMAA job " + jobId + " failed. Job statistics are:\n" + jobstats);
            }
            catch (DrmaaException e) {
                logger.error("DRMAA job " + jobId + " failed. DRMAA error: " + e.getMessage());
            }
        }
        else {
            logger.error("DRMAA job " + jobId + " failed.");
        }
    }

    public void setShellReturnMethod(String shellReturnMethod) {
        this.shellReturnMethod = shellReturnMethod;
    }

    public void setStatusPoolPeriod(int statusPoolPeriod) {
        this.statusPoolPeriod = statusPoolPeriod;
    }

    public void setSubmissionKey(String submissionKey) {
        this.submissionKey = submissionKey;
    }


    private Process runJobThroughShell(long taskId, String workingDir, SerializableJobTemplate jobTemplate, int start, int end, int incr, int timeoutInSeconds) throws IOException, InterruptedException, DrmaaException {
        // serialize template
        File jtFile = File.createTempFile("DrmaaTemplate", ".oos", new File(workingDir));
//        jtFile.deleteOnExit();
        ObjectOutputStream objStream = new ObjectOutputStream(new FileOutputStream(jtFile));
        // Write object out to disk
        objStream.writeObject(jobTemplate);
        objStream.close();

        // build command line params
        List<String> clParams = new LinkedList<String>();
        clParams.add(DrmaaSubmitter.OPT_TEMPLATE_FILE + DrmaaSubmitter.DELIMETER + jtFile.getAbsolutePath());
        clParams.add(DrmaaSubmitter.OPT_TASK_ID + DrmaaSubmitter.DELIMETER + taskId);
        clParams.add(DrmaaSubmitter.OPT_RETURN_BY + DrmaaSubmitter.DELIMETER + shellReturnMethod);
        if (submissionKey != null && submissionKey.length() > 0)
            clParams.add(DrmaaSubmitter.OPT_SUBMISSION_KEY + DrmaaSubmitter.DELIMETER + submissionKey);
        if (statusPoolPeriod > 0)
            clParams.add(DrmaaSubmitter.OPT_LOOP_SLEEP_PERIOD + DrmaaSubmitter.DELIMETER + statusPoolPeriod);
        if (start >= 0 && end > 0 && incr > 0) {
            clParams.add(DrmaaSubmitter.OPT_JOB_ARRAY_PARAMS + DrmaaSubmitter.DELIMETER + start + "," + end + "," + incr);
        }
        if (0<timeoutInSeconds) {
            clParams.add(DrmaaSubmitter.OPT_TIMEOUT_SECONDS+DrmaaSubmitter.DELIMETER+timeoutInSeconds);
        }

//        scriptWriter.write(JAVA_PATH + " -cp " + DRMAA_SUBMITTER_JAR_PATH +
//                " " + DRMAA_SUBMITTER + " " + jtFile.getAbsolutePath() );
//        for (String o: options)
//            scriptWriter.write(" " + o);
//        scriptWriter.write("\n");
//        scriptWriter.close();


        List<String> cmdList = new LinkedList<String>();
//        cmdList.add("bash");
        cmdList.add(DRMAA_SUBMITTER_SCRIPT_PATH);
        cmdList.add(JAVA_PATH);
        cmdList.add(DRMAA_SUBMITTER_JAR_PATH);
        cmdList.add("org.janelia.it.jacs.compute.drmaa.DrmaaSubmitter");


        cmdList.addAll(clParams);

        File script = new File(DRMAA_SUBMITTER_SCRIPT_PATH);
        if (!script.canExecute())
            logger.error("ERROR:" + DRMAA_SUBMITTER_SCRIPT_PATH + " is not executable");
        // run process
        ProcessBuilder pb = new ProcessBuilder(cmdList);
        Process shell = pb.start();

        // if user requested syncronous return - wait and here
        if (DrmaaSubmitter.OPT_RETURN_VIA_SYSTEM_VAL.equals(shellReturnMethod)) {
            int shellExitStatus = shell.waitFor();
            // record error in case of error
            if (shellExitStatus != 0) {
                InputStream shellErr = shell.getErrorStream(); // this is actual process output!
                errorText = IOUtils.readInputStream(shellErr);
                logger.error("Shell Drmaa Submitter returned ERROR:" + errorText);
            }
        }

        return shell;
    }

    public boolean isProjectCodeValid(String targetCode) throws Exception {
        if (_projectCodes.contains(targetCode)) {
            return true;
        }
        else {
            // try once to update
            updateProjectCodes();
            return _projectCodes.contains(targetCode);
        }
    }

    public HashSet<String> getProjectCodes() throws Exception {
        updateProjectCodes();
        return _projectCodes;
    }

    private synchronized void updateProjectCodes() throws Exception {
        // Ask the grid about the project code passed
        SystemCall call = new SystemCall(logger);
        String projectCodeFile = SystemConfigurationProperties.getString(ROOT_SERVER_DIR_PROP) +
                File.separator + "projectCodes.txt."+System.currentTimeMillis();
        int success = call.emulateCommandLine("qconf -sprjl > " + projectCodeFile, true);
        if (success != 0) {
            String errorMsg = "Unable to generate the project code file.";
            logger.error(errorMsg);
            throw new IOException(errorMsg);
        }
        File projectFile = new File(projectCodeFile);
        FileUtil.waitForFile(projectCodeFile);
        // Got the file
        logger.debug("Project codes size (initial): " + _projectCodes.size());
        Scanner scanner = new Scanner(projectFile);
        while (scanner.hasNextLine()) {
            String tmpCode = scanner.nextLine().trim();
            if (!_projectCodes.contains(tmpCode)) {
                _projectCodes.add(tmpCode);
            }
        }
        scanner.close();
        logger.debug("Project codes size (final): " + _projectCodes.size());
        boolean deleteSuccess = projectFile.delete();
        if (!deleteSuccess) {
            logger.error("Was not able to delete the project code temporary file("+projectCodeFile+").  Continuing anyway...");
        }
    }


}