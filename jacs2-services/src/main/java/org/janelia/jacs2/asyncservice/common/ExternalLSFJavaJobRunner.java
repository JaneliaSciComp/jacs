package org.janelia.jacs2.asyncservice.common;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.janelia.cluster.*;
import org.janelia.jacs2.asyncservice.common.cluster.LsfJavaJobInfo;
import org.janelia.jacs2.asyncservice.common.cluster.MonitoredJobManager;
import org.janelia.jacs2.asyncservice.qualifier.LSFJavaJob;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * External runner which uses the java-lsf library to submit and manage cluster jobs.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@LSFJavaJob
public class ExternalLSFJavaJobRunner extends AbstractExternalProcessRunner {

    private JobManager jobMgr;

    private static final ExecutorService completionMessageExecutor = Executors.newCachedThreadPool((runnable) -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            // Ensure that we can shut down without these threads getting in the way
            thread.setName("CompletionMessageThread");
            thread.setDaemon(true);
            return thread;
        });

    private int jobIncrementStop = 1;

    @Inject
    public ExternalLSFJavaJobRunner(MonitoredJobManager jobMgr, JacsServiceDataPersistence jacsServiceDataPersistence, Logger logger) {
        super(jacsServiceDataPersistence, logger);
        this.jobMgr = jobMgr.getJobMgr();
    }

    @Override
    public ExeJobInfo runCmds(ExternalCodeBlock externalCode, Map<String, String> env, String workingDirName, JacsServiceData serviceContext) {
        logger.debug("Begin bsub job invocation for {}", serviceContext);
        try {

            JobTemplate jt = prepareJobTemplate(externalCode, env, workingDirName, serviceContext);
            String processingScript = jt.getRemoteCommand();

            final JobFuture future = jobMgr.submitJob(jt, 1, jobIncrementStop);

            Long jobId = future.getJobId();

            logger.info("Task was submitted to the cluster as job " + future.getJobId());
            logger.info("Start {} for {} using  env {}", jt.getRemoteCommand(), serviceContext, env);
            logger.info("Submitted job {} for {}", jobId, serviceContext);

            future.whenCompleteAsync((infos, e) -> {
                processJobCompletion(jt, jobId, infos, e);
            }, completionMessageExecutor);


            jacsServiceDataPersistence.addServiceEvent(
                    serviceContext,
                    JacsServiceData.createServiceEvent(JacsServiceEventTypes.CLUSTER_SUBMIT, String.format("Submitted job %s {%s} running: %s", serviceContext.getName(), jobId, processingScript))
            );

            return new LsfJavaJobInfo(jobMgr, jobId, processingScript);
        } catch (Exception e) {
            jacsServiceDataPersistence.updateServiceState(
                    serviceContext,
                    JacsServiceState.ERROR,
                    Optional.of(JacsServiceData.createServiceEvent(JacsServiceEventTypes.CLUSTER_JOB_ERROR, String.format("Error creating DRMAA job %s - %s", serviceContext.getName(), e.getMessage())))
            );
            logger.error("Error creating a cluster job for {}", serviceContext, e);
            throw new ComputationException(serviceContext, e);
        }
    }

    protected JobTemplate prepareJobTemplate(ExternalCodeBlock externalCode, Map<String, String> env, String workingDirName, JacsServiceData serviceContext) throws Exception {

        String processingScript = createProcessingScript(externalCode, env, workingDirName, serviceContext);
        jacsServiceDataPersistence.updateServiceState(serviceContext, JacsServiceState.RUNNING, Optional.empty());

        JobTemplate jt = new JobTemplate();
        jt.setJobName(serviceContext.getName());
        jt.setRemoteCommand(processingScript);
        jt.setArgs(Collections.emptyList());

        File workingDirectory = setJobWorkingDirectory(jt, workingDirName);
        logger.debug("Using working directory {} for {}", workingDirectory, serviceContext);

        String configDir = getConfigurationDirectory(jt);
        String errorDir = getConfigurationDirectory(jt);
        String outputDir = getConfigurationDirectory(jt);

        jt.setInputPath(configDir + File.separator + getGridServicePrefixName() + "Configuration.#");
        jt.setErrorPath(errorDir + File.separator + getGridServicePrefixName() + "Error.#");
        jt.setOutputPath(outputDir + File.separator + getGridServicePrefixName() + "Output.#");
        // Apply a RegEx to replace any non-alphanumeric character with "_".

        String owner = serviceContext.getOwner();
        if (owner==null) {
            owner = ProcessorHelper.getGridBillingAccount(serviceContext.getResources());
        }

        jt.setJobName(owner.replaceAll("\\W", "_") + "_" + getGridServicePrefixName());
        // Check if the SGE grid requires account info
        // TODO: need to port over ComputeAccounting.getInstance().getComputeAccount
        //setAccount(jt);

        if (StringUtils.isNotBlank(serviceContext.getInputPath())) {
            jt.setInputPath(":" + serviceContext.getInputPath());
        }

        List<String> nativeSpec = createNativeSpec(serviceContext.getResources());
        if (nativeSpec!=null) {
            jt.setNativeSpecification(nativeSpec);
        }

        return jt;
    }
    private void processJobCompletion(JobTemplate jt, Long jobId, Collection<JobInfo> infos, Throwable e) {

        logger.info("Process completion for job "+jobId);

        if (e!=null) {
            logger.error("There was an exception with the grid execution", e);
        }
        else {

            boolean gridActionSuccessful = true;
            for (JobInfo jobInfo : infos) {

                if (jobInfo.getStatus()== JobStatus.EXIT || jobInfo.getExitCode()!=0) {
                    gridActionSuccessful = false;
                }

                LocalDateTime startTime = jobInfo.getStartTime();
                LocalDateTime submitTime = jobInfo.getSubmitTime();
                LocalDateTime finishTime = jobInfo.getFinishTime();
                long queueTimeSeconds = ChronoUnit.SECONDS.between(submitTime, startTime);
                long runTimeSeconds = ChronoUnit.SECONDS.between(startTime, finishTime);

                String queueTime = queueTimeSeconds+" sec";
                if (queueTimeSeconds>300) { // More than 5 minutes, just show the minutes
                    queueTime = TimeUnit.MINUTES.convert(queueTimeSeconds, TimeUnit.SECONDS) + " min";
                }

                String runTime = runTimeSeconds+" sec";
                if (runTimeSeconds>300) {
                    runTime = TimeUnit.MINUTES.convert(runTimeSeconds, TimeUnit.SECONDS) + " min";
                }

                String maxMem = jobInfo.getMaxMem();
                String jobIdStr = jobInfo.getJobId()+"";
                if (jobInfo.getArrayIndex()!=null) {
                    jobIdStr += "."+jobInfo.getArrayIndex();
                }

                logger.info("Job {} was queued for {}, ran for {}, and used "+maxMem+" of memory.", jobIdStr, queueTime, runTime);
                if (jobInfo.getExitCode()!=0) {
                    logger.error("Job {} exited with code {} and reason {}", jobIdStr, jobInfo.getExitCode(), jobInfo.getExitReason());
                }

            }

            if (!gridActionSuccessful) {
                String error = "Some jobs exited with non-zero codes: "+jt.getWorkingDir();
                logger.error("There was an error with the grid execution of "+jobId+": "+error);
            }
        }
    }

    protected List<String> createNativeSpec(Map<String, String> jobResources) {
        List<String> spec = new ArrayList<>();
        // append accountID for billing
        String billingAccount = ProcessorHelper.getGridBillingAccount(jobResources);
        if (StringUtils.isNotBlank(billingAccount)) {
            spec.add("-P "+billingAccount);
        }
        int nProcessingSlots = ProcessorHelper.getProcessingSlots(jobResources);
        StringBuilder resourceBuffer = new StringBuilder();
        if (nProcessingSlots > 1) {
            // append processing environment
            spec.add("-n "+nProcessingSlots);
            resourceBuffer
                    .append("affinity")
                    .append('[')
                    .append("core(1)")
                    .append(']');
        }

        long softJobDurationInMins = Math.round(new Double(ProcessorHelper.getSoftJobDurationLimitInSeconds(jobResources)) / 60);
        if (softJobDurationInMins > 0) {
            spec.add("-We "+softJobDurationInMins);
        }

        long hardJobDurationInMins = Math.round(new Double(ProcessorHelper.getHardJobDurationLimitInSeconds(jobResources)) / 60);
        if (hardJobDurationInMins > 0) {
            spec.add("-W "+hardJobDurationInMins);
        }

        String queue = jobResources.get("gridQueue");
        if (StringUtils.isNotBlank(queue)) {
            spec.add("-q "+queue);
        }

        StringBuilder selectResourceBuffer = new StringBuilder();
        String gridNodeArchitecture = ProcessorHelper.getCPUType(jobResources); // sandy, haswell, broadwell, avx2
        if (StringUtils.isNotBlank(gridNodeArchitecture)) {
            selectResourceBuffer.append(gridNodeArchitecture);
        }
        String gridResourceLimits = ProcessorHelper.getGridJobResourceLimits(jobResources);
        if (StringUtils.isNotBlank(gridResourceLimits)) {
            if (selectResourceBuffer.length() > 0) {
                selectResourceBuffer.append(',');
            }
            selectResourceBuffer.append(gridResourceLimits);
        }
        if (selectResourceBuffer.length() > 0) {
            if (resourceBuffer.length() > 0) {
                resourceBuffer.append(' ');
            }
            resourceBuffer
                    .append("select")
                    .append('[')
                    .append(selectResourceBuffer)
                    .append(']');
            ;
        }
        if (resourceBuffer.length() > 0) {
            spec.add("-R \""+resourceBuffer+"\"");
        }
        return spec;
    }

    @Override
    protected void writeProcessingCode(ExternalCodeBlock externalCode, Map<String, String> env, ScriptWriter scriptWriter) {
        scriptWriter.add("#!/bin/bash");
        for(String key : env.keySet()) {
            scriptWriter.add("export "+key+"="+env.get(key));
        }
        scriptWriter.add(externalCode.toString());
    }

    private File setJobWorkingDirectory(JobTemplate jt, String workingDirName) {
        File workingDirectory;
        if (StringUtils.isNotBlank(workingDirName)) {
            workingDirectory = new File(workingDirName);
        } else {
            workingDirectory = Files.createTempDir();
        }
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
        if (!workingDirectory.exists()) {
            throw new IllegalStateException("Cannot create working directory " + workingDirectory.getAbsolutePath());
        }
        jt.setWorkingDir(workingDirectory.getAbsolutePath());
        return workingDirectory;
    }

    private String getGridServicePrefixName() {
        return "grid";
    }

    protected String getConfigurationDirectory(JobTemplate jt) {
        return jt.getWorkingDir() + File.separator + "sge_config";
    }

    protected String getErrorDirectory(JobTemplate jt) {
        return jt.getWorkingDir() + File.separator + "sge_error";
    }

    protected String getOutputDirectory(JobTemplate jt) {
        return jt.getWorkingDir() + File.separator + "sge_output";
    }

    public int getJobIncrementStop() {
        return jobIncrementStop;
    }

    public void setJobIncrementStop(int jobIncrementStop) {
        this.jobIncrementStop = jobIncrementStop;
    }
}
