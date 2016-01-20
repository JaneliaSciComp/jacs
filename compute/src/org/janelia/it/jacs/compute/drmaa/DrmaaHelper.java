
package org.janelia.it.jacs.compute.drmaa;

import org.apache.log4j.Logger;
import org.ggf.drmaa.*;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.shared.utils.IOUtils;

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

    private static final int MAX_ERRORS = 1000;
    private int numErrors = 0;
    
    private final Session sunSession;                 // DRMAA Session
    private static boolean initialized = false;

    private static final String MAX_STATUS_CHECK_INTERVAL_PROP = "Grid.DrmaaMaxStatusCheckInterval";
    private static final int MAX_STATUS_CHECK_INTERVAL = SystemConfigurationProperties.getInt(MAX_STATUS_CHECK_INTERVAL_PROP, 30000);
    private static final int MIN_STATUS_CHECK_INTERVAL = 1000; // one second
    private static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    private static final String DRMAA_SUBMITTER_JAR_PATH = SystemConfigurationProperties.getFilePath("Local.Lib.Path", "Drmaa.Submitter.Jar.Name");
    private static final String DRMAA_SUBMITTER_SCRIPT_PATH = SystemConfigurationProperties.getFilePath("Local.Lib.Path", "Drmaa.Submitter.Script.Name");
    private Map<String, Integer> currentStatusMap = new HashMap<>();
    private String mainJobID;

    private String errorText = "";
    private String shellReturnMethod = DrmaaSubmitter.OPT_RETURN_VIA_QUEUE_VAL;

    private String submissionKey = "";

    public DrmaaHelper(Logger logger) throws DrmaaException {
        this.logger = logger;
        SessionFactory factory = SessionFactory.getFactory();   // DRMAA Session Factory - not hibernate
        sunSession = factory.getSession();                 // DRMAA Session
        synchronized (sunSession) {
            if (!initialized) {
                boolean succeeded=false;
                DrmaaException mostRecentException=null;
                for (int i=0; i < 10; i++) {
                    try {
                        sunSession.init(null);
                        succeeded=true;
                    }
                    catch (DrmaaException e) {
                        mostRecentException = e;
                        logger.debug(e.getMessage());
                        i++;
                        try {
                            logger.debug("Retrying DRMAA initialization attempt: "+i);
                            Thread.sleep(5000);
                        }
                        catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        succeeded=false;
                    }
                    if (succeeded) {break;}
                }
                if (succeeded) {initialized = true;}
                else {
                    throw mostRecentException;
                }
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
        synchronized (sunSession) {
            try {
                if (null!=mainJobID && currentStatusMap.size()>0) {
                    sunSession.control(mainJobID, Session.TERMINATE);
                }
            }
            catch (DrmaaException e) {
                logger.error("Unable to terminate the grid jobs after error and terminate call.");
            }
            if (initialized) {
                sunSession.exit();
                initialized = false;
            }
        }

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
            return new HashSet<>(sunSession.runBulkJobs(jt.getDrmaaJobTemplate(), start, end, incr));
        }
    }

    public void control(String jobId, int action) throws DrmaaException {
        synchronized (sunSession) {
            sunSession.control(jobId, action);
        }
    }

    public boolean waitForJob(String jobId, String logPrefix, JobStatusLogger statusLogger, int statusPoolPeriod) throws Exception {
        Set<String> jobSet = new HashSet<>();
        jobSet.add(jobId);
        return waitForJobs(jobSet, logPrefix, statusLogger, statusPoolPeriod, -1);
    }
                                                    
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
        
    	mainJobID = getMainJobId(jobSet);
    	Date startTime = null;
        int dynamicCheckInterval = MIN_STATUS_CHECK_INTERVAL; // start with 2 seconds
        boolean parentJobDone = false;

        // prepopulate current status map
        for (String id : jobSet) {
            currentStatusMap.put(id, Session.QUEUED_ACTIVE);
        }

        while (currentStatusMap.size() > 0 && !parentJobDone) {
            logger.info("Inquiring about jobId "+mainJobID+" ("+currentStatusMap.size()+" items)");
            // If timeout is set, check for expiration
            try {
                if (0<timeoutInSeconds && null!=startTime) {
                    Date now = new Date();
                    long elapsedSeconds = (now.getTime()-startTime.getTime())/1000;
                    logger.info("Timeout is set to "+timeoutInSeconds+" seconds. Elapsed: "+elapsedSeconds+" seconds.");
                    if (elapsedSeconds>timeoutInSeconds) {
                        logger.error("The grid job ("+mainJobID+") exceeded the timeout specified: "+timeoutInSeconds+" seconds.");
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
            logger.info(logPrefix + " Going to sleep for " + nappingTime + " milliseconds");
            Thread.sleep(nappingTime);


            // Check for completed jobs in the currentStatusMap. This should not happen, but it does, probably because of the delay in getting to this point?
            for (String jobId : currentStatusMap.keySet()) {
                if (currentStatusMap.get(jobId) == Session.DONE) {
                	logger.info(logPrefix + " Job " + jobId + " completed while we weren't looking!");
                    currentStatusMap.remove(jobId);
                }
            }
            
            // map of jobs that changed status since the last check
            Map<String, GridJobStatus.JobState> changedJobStateMap = null;
            
            // get status changes and update current map
            changedJobStateMap = getBulkJobStatusChanges(currentStatusMap);
            if (numErrors>MAX_ERRORS) {
            	errorText = "Max DRMAA errors reached ("+numErrors+">"+MAX_ERRORS+")";
            	logger.warn(errorText);
            	return false;
            }
            
            // update status log, if necessary
            if (statusLogger != null && 0<changedJobStateMap.size()) {
                statusLogger.bulkUpdateJobStatus(changedJobStateMap);
            }
            
            // Loop through the changed jobs and get their info
            Map<String, Map<String, String>> changedJobResourceMap = new HashMap<>();
            for (String jobId : changedJobStateMap.keySet()) {
            	int jobStatus = currentStatusMap.get(jobId);
            	
                if (logger.isDebugEnabled()) {
                    String newState;
                    switch (jobStatus) {
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
                
                JobInfo jobInfo;
                if (jobStatus == Session.DONE || jobStatus == Session.FAILED) {
                    try {
                        jobInfo = wait(jobId, Session.TIMEOUT_NO_WAIT);

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
                                                
                        changedJobResourceMap.put(jobId, jobInfo.getResourceUsage());
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
                
            } // end of for loop for changed jobs

            // update job info data - assumes job is terminal (DONE or FAILED)
            // Make a single call instead of looped
            if (statusLogger != null && changedJobResourceMap != null && changedJobResourceMap.size()>0) {
                statusLogger.bulkUpdateJobInfo(changedJobStateMap, changedJobResourceMap);
            }


            // if there where no changes - increase wait time
            if (changedJobStateMap.size() == 0) {
                // - at least one is not done - break inner loop, and increase wait time
                if (dynamicCheckInterval < MAX_STATUS_CHECK_INTERVAL)
                    dynamicCheckInterval *= 2;
            }
            // reset interval
            else {
                // reset check interval to min - jobs are coming back now
                dynamicCheckInterval = MIN_STATUS_CHECK_INTERVAL;
            }

            if (changedJobStateMap.isEmpty()) {
                logger.debug("Nothing has changed.  Sample status of some remaining jobs:");
                int x=0;
                for (String jobId : currentStatusMap.keySet()) {
                    GridJobStatus.JobState tmpState = translateStatusCode(currentStatusMap.get(jobId));
                    logger.debug(jobId+" - "+tmpState);
                    if (GridJobStatus.JobState.UNKNOWN==tmpState) {
                        logger.warn("Defaulted state "+currentStatusMap.get(jobId)+" to UNKNOWN!");
                    }
                    x++;
                    if (x>=20) {break;}
                }
            }
            // Check the parent job
//            String parentJobId=mainJobID.substring(0, mainJobID.lastIndexOf('.'));
//            GridJobStatus.JobState parentState = translateStatusCode(getJobProgramStatus(parentJobId));
//            logger.debug("The parent state is "+parentState.name());
//            if (GridJobStatus.JobState.DONE==parentState || GridJobStatus.JobState.FAILED==parentState) {
//                logger.debug("The job "+parentJobId+" is terminal with "+currentStatusMap.size()+" left in the currentStatusMap");
//                JobInfo finalInfo = wait(parentJobId,Session.TIMEOUT_NO_WAIT);
//                // Interrogate job exit status
//                if (finalInfo.wasAborted ()) {
//                    System.out.println("Job " + finalInfo.getJobId () + " never ran");
//                }
//                else if (finalInfo.hasExited ()) {
//                    System.out.println("Job " + finalInfo.getJobId () +
//                            " finished regularly with exit status " +
//                            finalInfo.getExitStatus ());
//                }
//                else if (finalInfo.hasSignaled ()) {
//                    System.out.println("Job " + finalInfo.getJobId () +
//                            " finished due to signal " +
//                            finalInfo.getTerminatingSignal ());
//
//                    if (finalInfo.hasCoreDump()) {
//                        System.out.println("A core dump is available.");
//                    }
//                }
//                else {
//                    System.out.println("Job " + finalInfo.getJobId () +
//                            " finished with unclear conditions");
//                }
//
//                System.out.println ("\nJob Usage:");
//
//                Map rmap = finalInfo.getResourceUsage ();
//
//                for (Object o : rmap.keySet()) {
//                    String name = (String) o;
//                    String value = (String) rmap.get(name);
//
//                    System.out.println("  " + name + "=" + value);
//                }
//
//                parentJobDone = true;
//            }
        } // end of while loop
        logger.info(logPrefix + " - all parts are done or assumed done");
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
            Map<String, GridJobStatus.JobState> statusMap = new HashMap<>();
            logger.debug("Asking SGE in a loop about info for "+jobStatusMap.keySet().size()+" jobs");
            for (String jobId : jobStatusMap.keySet()) {
                try {
                    int status = sunSession.getJobProgramStatus(jobId);
                    if (status != jobStatusMap.get(jobId)) {
                        GridJobStatus.JobState tmpState = translateStatusCode(status);
                        if (null==tmpState || GridJobStatus.JobState.UNKNOWN==tmpState) {
                            logger.debug("Job:"+jobId+" has null or state unknown: "+tmpState);
                        }
                        statusMap.put(jobId, tmpState);
                        jobStatusMap.put(jobId, status); 
                    }
                }
                catch (DrmaaException e) {
                	logger.error("Error getting job status for "+jobId+": "+e.getMessage());
                    numErrors++;
                }
            }
            logger.debug("Done asking SGE in a loop about info for "+jobStatusMap.keySet().size()+" jobs");
            return statusMap;
        }
    }

    public Version getVersion() {
        synchronized (sunSession) {
            return sunSession.getVersion();
        }
    }

    private void logJobError(JobInfo jobInfo, String jobId) {
        if (jobInfo != null && logger.isDebugEnabled()) {
            try {
                StringBuilder jobstats = new StringBuilder();
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
        List<String> clParams = new LinkedList<>();
        clParams.add(DrmaaSubmitter.OPT_TEMPLATE_FILE + DrmaaSubmitter.DELIMETER + jtFile.getAbsolutePath());
        clParams.add(DrmaaSubmitter.OPT_TASK_ID + DrmaaSubmitter.DELIMETER + taskId);
        clParams.add(DrmaaSubmitter.OPT_RETURN_BY + DrmaaSubmitter.DELIMETER + shellReturnMethod);
        if (submissionKey != null && submissionKey.length() > 0)
            clParams.add(DrmaaSubmitter.OPT_SUBMISSION_KEY + DrmaaSubmitter.DELIMETER + submissionKey);
        int statusPoolPeriod = -1;
        if (statusPoolPeriod > 0)
            clParams.add(DrmaaSubmitter.OPT_LOOP_SLEEP_PERIOD + DrmaaSubmitter.DELIMETER + statusPoolPeriod);
        if (start >= 0 && end > 0 && incr > 0) {
            clParams.add(DrmaaSubmitter.OPT_JOB_ARRAY_PARAMS + DrmaaSubmitter.DELIMETER + start + "," + end + "," + incr);
        }
        if (0<timeoutInSeconds) {
            clParams.add(DrmaaSubmitter.OPT_TIMEOUT_SECONDS+DrmaaSubmitter.DELIMETER+timeoutInSeconds);
        }

        List<String> cmdList = new LinkedList<>();
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

}