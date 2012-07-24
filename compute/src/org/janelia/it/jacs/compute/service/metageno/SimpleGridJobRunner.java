
package org.janelia.it.jacs.compute.service.metageno;

import org.apache.log4j.Logger;
import org.ggf.drmaa.Session;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SimpleJobStatusLogger;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.status.GridJobStatus;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 23, 2009
 * Time: 2:45:18 PM
 */
public class SimpleGridJobRunner {
    protected Logger logger;

    File workingDir;
    String command;
    String queue;
    int numThreads;
    String projectCode;
    SerializableJobTemplate jt;
    String jobId;
    DrmaaHelper drmaa;
    protected File scriptFile;
    int timeoutMinutes = 0;
    int maxRetry = 0;
    int tryNumber = 0;
    Long taskId;

    public SimpleGridJobRunner(File workingDir, String command, String queue, String projectCode, Long taskId) throws Exception {
        this(workingDir, command, queue, 1, projectCode, 0, 0, taskId);
    }

    public SimpleGridJobRunner(File workingDir, String command, String queue, int numThreads, String projectCode, Long taskId) throws Exception {
        this(workingDir, command, queue, numThreads, projectCode, 0, 0, taskId);
    }

    public SimpleGridJobRunner(File workingDir, String command, String queue, String projectCode, int timeoutMinutes,
                               int maxRetry, Long taskId) throws Exception {
        this(workingDir, command, queue, 1, projectCode, timeoutMinutes, maxRetry, taskId);
    }

    // If timeoutMinutes == 0, then no timeout and no retries
    // If maxRetry == 0, then no limit on retries
    // Note that retry is only permitted for timeout and not for errors - a failed execution always throws an Exception
    public SimpleGridJobRunner(File workingDir, String command, String queue, int numThreads, String projectCode, int timeoutMinutes,
                               int maxRetry, Long taskId) throws Exception {
        this.workingDir = workingDir;
        this.command = command;
        this.queue = queue;
        this.numThreads = numThreads;
        this.projectCode = projectCode;
        this.timeoutMinutes = timeoutMinutes;
        this.maxRetry = maxRetry;
        logger = ProcessDataHelper.getLoggerForTask(taskId.toString(), this.getClass());
        logger.info("SimpleGridJobRunner constructed with taskId=" + taskId);
        this.taskId = taskId;
        drmaa = new DrmaaHelper(logger);
        String uniqueId = TimebasedIdentifierGenerator.generate(1L).toString();
        scriptFile = new File(workingDir, uniqueId + ".sh");
        jt = drmaa.createJobTemplate(new SerializableJobTemplate());
        jt.setRemoteCommand("bash");
        FileWriter fw = new FileWriter(scriptFile);
        fw.write(command.trim() + "\n");
        fw.close();
        jt.setArgs(Arrays.asList(scriptFile.getAbsolutePath()));
        jt.setWorkingDirectory(workingDir.getAbsolutePath());
        jt.setErrorPath(":" + scriptFile.getAbsolutePath() + ".err");
        jt.setOutputPath(":" + scriptFile.getAbsolutePath() + ".out");
        jt.setJobName("job-" + uniqueId);
        jt.setNativeSpecification(queue);
        if (numThreads > 1) {
            jt.setNativeSpecification("-pe threaded " + numThreads);
        }
        if (SystemConfigurationProperties.getBoolean("Grid.RequiresProjectCode")) {
            jt.setNativeSpecification("-P " + projectCode);
        }
        else {
            logger.info("Skipping project code since using camera grid");
        }
    }

    public boolean execute() throws Exception {
        if (taskId == null || taskId == 0L) {
            return executeWithoutTaskId();
        }
        else {
            return executeWithTaskId();
        }
    }

    public boolean executeWithTaskId() throws Exception {
        logger.info("Calling drmaa.runJob()");
        jobId = drmaa.runJob(jt);
        HashSet<String> jobSet = new HashSet<String>();
        jobSet.add(jobId);
        logger.info("Starting jobId=" + jobId + " retry=" + tryNumber + " of " + maxRetry + " for command=" + command);
        JobStatusLogger jsl = new SimpleJobStatusLogger(taskId);
        jsl.bulkAdd(jobSet, queue, GridJobStatus.JobState.QUEUED);
        drmaa.deleteJobTemplate(jt);
        logger.info("******** " + jobSet.size() + " jobs submitted to grid **********");
        boolean gridActionSuccessful = drmaa.waitForJobs(jobSet, "Computing results for task=" + taskId, jsl, -1, -1);
        if (!gridActionSuccessful) {
            logger.warn("\nThere was an error with the grid execution.\n");
        }
        return gridActionSuccessful;
    }

    /* returns true on successful job completion */
    public boolean executeWithoutTaskId() throws Exception {
        logger.info("execute() tryNumber=" + tryNumber + " maxRetry=" + maxRetry);
        boolean result = false;
        while (tryNumber < maxRetry || maxRetry == 0) {
            logger.info("Calling drmaa.runJob()");
            jobId = drmaa.runJob(jt);
            logger.info("Starting jobId=" + jobId + " retry=" + tryNumber + " of " + maxRetry + " for command=" + command);
            long timeLimit = new Date().getTime() + timeoutMinutes * 60 * 1000;
            while (timeoutMinutes == 0 || new Date().getTime() < timeLimit) {
                Thread.sleep(5000);
                int jobStatus = drmaa.getJobProgramStatus(jobId);
                if (jobStatus == Session.DONE || jobStatus == Session.FAILED) {
                    if (jobStatus == Session.FAILED) {
                        String errMsg = "Grid job " + jobId + " failed with non-zero exit status, command=" + command;
                        logger.error(errMsg);
                        drmaa.deleteJobTemplate(jt);
                        checkResults();
                        throw new Exception(errMsg);
                    }
                    else {
                        // Job finished without error - break out of retry loop
                        logger.info("Grid job " + jobId + " finished successfully");
                        result = true;
                        break;
                    }
                }
            }
            if (!result && timeoutMinutes > 0) {
                // Job has exceeded timeout but not exited - restart
                logger.info("Grid job " + jobId + " exceeded timeout - terminating and then restarting command=" + command);
                drmaa.control(jobId, Session.TERMINATE);
                Thread.sleep(5000);
                tryNumber++;
            }
            else {
                break; // implies success
            }
        }
        if (maxRetry > 0 && tryNumber == maxRetry && tryNumber > 0) {
            String errMsg = "Exceeded max retries for grid command=" + command;
            logger.error(errMsg);
            throw new Exception(errMsg);
        }
        drmaa.deleteJobTemplate(jt);
        checkResults();
        return result;
    }

    protected void checkResults() throws Exception {
        File stderrFile = new File(scriptFile.getAbsolutePath() + ".err");
        if (stderrFile.length() == 0L) {
            stderrFile.delete();
        }
        else {
            FileReader reader = new FileReader(stderrFile);
            int errLen = new Long(stderrFile.length()).intValue();
            if (errLen > 300) {
                errLen = 300;
            }
            char[] cbuf = new char[errLen];
            reader.read(cbuf, 0, errLen);
            String errMsg = new String(cbuf);
            throw new Exception("non-zero stderr in file=" + stderrFile.getAbsolutePath() + " : " + errMsg);
        }
//        if (stdoutFile.length()==0) {
//            stdoutFile.delete();
//            scriptFile.delete();
//        }
    }

}
