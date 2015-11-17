
package org.janelia.it.jacs.compute.service.common.grid.submit.sge;

import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.DrmaaSubmitter;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.common.ProcessDataAccessor;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobService;
import org.janelia.it.jacs.compute.service.common.grid.submit.WaitForJobException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service submit a job to the Grid.  It's entirely extracted from work done by Sean Murphy
 * and Todd Safford.
 *
 * @author Sean Murphy
 * @author Todd Safford
 * @author Tareq Nabeel
 * @author Konrad Rokicki
 */
public abstract class SubmitDrmaaJobService implements SubmitJobService {

    protected Logger logger;
    protected ContextLogger contextLogger;

    protected Task task;
    protected IProcessData processData;
    protected ProcessDataAccessor data;
    protected FileNode resultFileNode;
    protected Set<String> jobSet = null;
    // This attribute keeps track of how many nodes we want to engage.  Minimum value is 1
    private int jobIncrementStop = 1;
    protected ComputeDAO computeDAO;
    protected static final String NORMAL_QUEUE = SystemConfigurationProperties.getString("Grid.NormalQueue");
    protected static final int MAX_JOBS_IN_ARRAY = SystemConfigurationProperties.getInt("Grid.MaxNumberOfJobs");
    private GridResourceSpec gridResourceSpec;
    private boolean cancelled = false;
    
    /**
     * This method is part of IService interface and used when this class
     * or it's child is used as a processor in process file
     *
     * @param processData the running state of the process
     * @throws org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException
     */
    public void submitJobAndWait(IProcessData processData) throws SubmitJobException {
        try {
            init(processData);
            if (cancelled) {
                // Nothing to do.
            }
            else {
                submitJob();
                handleErrors();
                cleanup();
                postProcess();
            }
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }
    
    public void cleanup() {
        SystemCall system = new SystemCall(logger);
        try {
            String resultDirectory = resultFileNode.getDirectoryPath();
            system.emulateCommandLine("rm -f " + resultDirectory + "/DrmaaTemplate*.oos", true);
        }
        catch (Exception e) {
            logger.error("Error cleaning up after DRMAA job",e);
        }
    }

    /**
     * Can be called during initialization to cause the job to be skipped. 
     */
    protected void cancel() {
        this.cancelled = true;
    }

    /**
     * This method is invoked from GridSubmitAndWaitMDB
     *
     * @param processData the running state of the process
     * @throws SubmitJobException
     */
    public Process submitAsynchJob(IProcessData processData, String submissionKey) throws SubmitJobException {
        //logger.debug(getClass().getSimpleName() + " Process Data : " + processData);
        try {
            init(processData);
            if (cancelled) {
                return null;
            }
            if (logger.isInfoEnabled()) {
                logger.info("Preparing " + task.getTaskName() + " (task id = " + this.task.getObjectId() + " for asyncronous DRMAA submission)");
            }

            DrmaaHelper drmaa = new DrmaaHelper(logger);
            if (!checkSubmitJobPrecondition(drmaa)) {
                throw new SubmitJobException("Precondition check prevented job from submission");
            }

            drmaa.setShellReturnMethod(DrmaaSubmitter.OPT_RETURN_VIA_QUEUE_VAL);
            drmaa.setSubmissionKey(submissionKey);
            SerializableJobTemplate jt = prepareJobTemplate(drmaa);

            if (logger.isDebugEnabled()) {
                logger.debug("Calling drmaa.runBulkJobs() with jobIncrementStop=" + jobIncrementStop);
            }

            logger.info("Running runBulkJobs with nativeSpec=" + jt.getNativeSpecification());
            Process proc = drmaa.runBulkJobsThroughShell(
                    task.getObjectId(), task.getOwner(), resultFileNode.getDirectoryPath(),
                    jt, 1, jobIncrementStop, 1, getJobTimeoutSeconds());

            drmaa.deleteJobTemplate(jt);

            return proc;
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }


    protected void setJobIncrementStop(int stop) {
        if (logger.isDebugEnabled()) logger.debug("setJobIncrementStop() called with stop=" + stop);
        this.jobIncrementStop = stop;
    }


    protected int getJobIncrementStop() {
        return this.jobIncrementStop;
    }


    protected void init(IProcessData processData) throws Exception {
        initLoggersAndData(processData);

        // Permit the task to be predefined elsewhere
        if (this.task == null) {
            this.task = ProcessDataHelper.getTask(processData);
        }

        this.contextLogger.appendToLogContext(this.task);

        // Permit the resultNode to be defined elsewhere
        if (this.resultFileNode == null) {
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
        }
        this.jobSet = new HashSet<>();
        if (resultFileNode == null) {
            throw new MissingDataException("ResultFileNode for createtask " + task.getObjectId() +
                    " must exist before a grid job is submitted");
        }
        // Needs to run in separate transaction
        if (computeDAO == null) {computeDAO = new ComputeDAO(logger);}

        // ensure the SGE dirs exist
        FileUtil.ensureDirExists(getSGEConfigurationDirectory());
        FileUtil.ensureDirExists(getSGEOutputDirectory());
        FileUtil.ensureDirExists(getSGEErrorDirectory());
    }

    protected void initLoggersAndData(IProcessData processData)  throws Exception {
        this.logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.contextLogger = new ContextLogger(this.logger);

        this.processData = processData;
        this.data = new ProcessDataAccessor(processData, this.contextLogger);
    }

    /**
     * Override this method to specify that the job needs to run exclusively on the entire node.
     * 
     * Defaults to false.
     */
    protected boolean isExclusive() {
        return false;
    }
    
    /**
     * Override this method to specify the minimum number of slots needed for this grid job. At least this many slots
     * will be allocated, but more slots may be allocated to fulfill the memory requirement provided by 
     * getRequiredMemoryInGB.
     * 
     * Defaults to 1 slot.
     *
     * @return the minimum number of slots needed for this grid job.
     */
    protected int getRequiredSlots() {
    	return 1;
    }
    
    /**
     * Override this method to specify the minimum amount of memory needed for this grid job. Enough slots will be 
     * allocated to achieve this memory requirement.
     * 
     * Defaults to 1 GB.
     *
     * @return the minimum amount of memory needed for this grid job.
     */
    protected int getRequiredMemoryInGB() {
    	return 1;
    }

    /**
     * Override this to return true if the job is going to finish quickly but is part of large pipeline runs. This isn't
     * guaranteed to do anything, but on some grid it may queue to a specific "short job" queue or resource.
     *
     * @return true if the job is going to finish quickly.
     */
    protected boolean isShortPipelineJob() {
    	return false;
    }

    /**
     * Override this to return true if the job needs to finish immediately. This will direct the job to the dedicated
     * jacs queue  and is reserved for user-interactive actions; Channel splitting, Sage Loading, Blast
     *
     * @return true if the job needs to finish immediately.
     */
    protected boolean isImmediateProcessingJob() {
        return false;
    }

    /**
     * Override this method to add flags to the native specification. If you override this method, call super
     * in order to process ARCHIVE_FLAG.
     *
     * @return additional flags for the native specification.
     */
    protected String getAdditionalNativeSpecification() {
    	Object archiveFlag = processData.getItem("ARCHIVE_FLAG");
        if (archiveFlag!=null && Boolean.parseBoolean((String)archiveFlag)) {
        	logger.info("ARCHIVE_FLAG is true, job will run on archive queue");
            return "-l archive=true";    
        }
        return null;
    }
    
    /**
     * Override this method to specify a complete override to the native specification. This method returns null
     * by default. If it is overridden to return a non-null value, then isShortPipelineJob, getRequiredSlots,
     * getRequiredMemoryInGB, and getAdditionalNativeSpecification will be ignored.
     * 
     * @return null or the overriden native specification.
     */
    protected String getNativeSpecificationOverride() {
    	return null;
    }

    public GridResourceSpec getGridResourceSpec() {
        return gridResourceSpec;
    }
    
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {

        SerializableJobTemplate jt = drmaa.createJobTemplate(new SerializableJobTemplate());

        String nsOverride = getNativeSpecificationOverride();
        if (nsOverride != null) {
        	logger.info("Setting native specification override: "+nsOverride);
        	jt.setNativeSpecification(nsOverride);
        }
        else {
        	int mem = getRequiredMemoryInGB();
        	int slots = getRequiredSlots();
        	this.gridResourceSpec = new GridResourceSpec(mem, slots, isExclusive());
        	String ns = gridResourceSpec.getNativeSpec();
        	if (isShortPipelineJob()) {
        		ns += " -l short=true -now n";
        	}
            if (isImmediateProcessingJob()) {
                if (isShortPipelineJob()) {
                    throw new IllegalStateException("Job cannot be both a shortPipeline and an immediateProcessing job: "+task.getObjectId());
                }
                ns += " -l jacs=true -now n";
            }
        	String ans = getAdditionalNativeSpecification();
        	if (!Strings.isNullOrEmpty(ans)) {
        	    ns += " "+ans;
        	}
        	logger.info("Setting native specification to accomodate "+mem+" GB of memory and "+slots+" slot(s): "+ns);
        	jt.setNativeSpecification(ns);	
        }
        
        File configDir = new File(getSGEConfigurationDirectory());
        if (!configDir.exists()) {
            throw new MissingDataException("Could not find work directory for order " + configDir.getAbsolutePath());
        }

        File jobScript = new File(configDir, getGridServicePrefixName() + "Cmd.sh");
        if (logger.isInfoEnabled()) {
            logger.info("Set script location to:\n" + jobScript.getAbsolutePath());
        }
        FileWriter writer = new FileWriter(jobScript);
        String sgeCell = System.getenv("SGE_CELL");
        logger.info("DRMAA using SGE_CELL=" + sgeCell);
        try {
            createJobScriptAndConfigurationFiles(writer);
            verifyConfigurationFiles();
            
            boolean permissionsSuccessful = jobScript.setExecutable(true, false);
            if (!permissionsSuccessful) {
                logger.error("Unsuccessful on setting permissions of job script "+jobScript.getAbsolutePath()+". Continuing...");
            }
            jt.setRemoteCommand("bash");
            jt.setArgs(Arrays.asList(jobScript.getAbsolutePath()));
            jt.setWorkingDirectory(resultFileNode.getDirectoryPath());
            jt.setInputPath(":" + getSGEConfigurationDirectory() + File.separator + getGridServicePrefixName() + "Configuration." + JobTemplate.PARAMETRIC_INDEX);
            jt.setErrorPath(":" + getSGEErrorDirectory() + File.separator + getGridServicePrefixName() + "Error." + JobTemplate.PARAMETRIC_INDEX);
            jt.setOutputPath(":" + getSGEOutputDirectory() + File.separator + getGridServicePrefixName() + "Output." + JobTemplate.PARAMETRIC_INDEX);
            // Apply a RegEx to replace any non-alphanumeric character with "_".  SGE is finicky that way.
            jt.setJobName(task.getOwner().replaceAll("\\W", "_") + "_" + getGridServicePrefixName());
            // setNativeSpecification(jt);
            setQueue(jt);
            // Check if the SGE grid requires account info
            if (SystemConfigurationProperties.getBoolean("Grid.RequiresAccountInfo")) {
                setAccount(jt);
            }
        }
        finally {
            writer.flush();
            writer.close();
        }
        return jt;
    }

    protected Set<String> submitJob() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Preparing " + task.getTaskName() + " (task id = " + this.task.getObjectId() + " for DRMAA submission");
        }

        DrmaaHelper drmaa = new DrmaaHelper(logger);

        if (!checkSubmitJobPrecondition(drmaa)) {
            return new HashSet<>();
        }

        SerializableJobTemplate jt = prepareJobTemplate(drmaa);

        if (logger.isDebugEnabled()) {
            logger.debug("Calling drmaa.runBulkJobs() with jobIncrementStop=" + jobIncrementStop);
            logger.debug("Running runBulkJobs with nativeSpec=" + jt.getNativeSpecification());
        }

        /* DRMAA has a limitation of 75000 jobs in a single array
           Submittion in a loop is a work around for this limitation
          */
        for (int startIdx = 1, endIdx = 0; startIdx <= jobIncrementStop; startIdx += MAX_JOBS_IN_ARRAY) {
            endIdx += MAX_JOBS_IN_ARRAY;
            if (endIdx > jobIncrementStop) endIdx = jobIncrementStop;

            jobSet.addAll(drmaa.runBulkJobs(jt, startIdx, endIdx, 1));
        }

        // Intantiate Status Logger
        JobStatusLogger jsl = new SimpleJobStatusLogger(task.getObjectId());
        // store statuses
        jsl.bulkAdd(jobSet, getQueueName(jt), GridJobStatus.JobState.QUEUED);

        drmaa.deleteJobTemplate(jt);
        logger.info("******** " + jobSet.size() + " jobs submitted to grid **********");

        // now wait for completion
        boolean gridActionSuccessful = drmaa.waitForJobs(jobSet, "Computing results for " + resultFileNode.getObjectId(), jsl, -1, getJobTimeoutSeconds());
        if (!gridActionSuccessful) {
            String err = "Error ' \" + drmaa.getError() + \" ' executing grid jobs.";
            logger.error("err");
            throw new WaitForJobException(err);
        }
        return jobSet;
    }


    public void postProcess() throws MissingDataException {
        // Optional processing after completion of jobs can be overridden here
    }


    public void handleErrors() throws Exception {
        collectStdErr();
    }

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected abstract String getGridServicePrefixName();


    /**
     * This method will allow us to name the SGE queue directly in the process' definition
     * instead of the properties file
     *
     * @return SGE queue name
     */
    protected String getSGEQueue() {
        String sgeQueue = (String) processData.getItem("SGEQueue");
        if (sgeQueue == null || sgeQueue.trim().length() == 0) {
            sgeQueue = NORMAL_QUEUE;
        }
        return sgeQueue;
    }


    /**
     * This method is intended for adding native commands for sge queue
     * "-q <queue name>"
     *
     * @param jt SerializableJobTemplate
     * @throws org.ggf.drmaa.DrmaaException - Drmaa had an issue setting the specification
     */
    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        String queue = getSGEQueue();
        logger.info("setQueue queue=" + queue);
        jt.setNativeSpecification(queue);
    }


    protected String getQueueName(SerializableJobTemplate jt) {
        String queueName = "default";
        Pattern p = Pattern.compile("\\-l\\s+(\\w+)");
        String nativeSpec = jt.getNativeSpecification();
        if (nativeSpec != null) {
            Matcher m = p.matcher(nativeSpec);
            if (m.matches())
                queueName = m.group(1);
        }
        return queueName;
    }


    /**
     * This method is intended for adding native commands for sge to specify project
     * "-p <project>"
     *
     * @param jt SerializableJobTemplate
     * @throws org.ggf.drmaa.DrmaaException - Drmaa had an issue setting the specification
     */
    protected void setProject(SerializableJobTemplate jt) throws DrmaaException {
        String project = task.getParameter(Task.PARAM_project);
        if (project != null) {
            project = project.trim();
            if (project.length() > 0) {
                logger.info("setProject = -P " + project);
                jt.setNativeSpecification("-P " + project);
            }
        }
    }


    /**
     * This method is intended for adding native commands for sge to specify account
     * "-A <account(userlogin)>"
     *
     * @param jt SerializableJobTemplate
     * @throws org.ggf.drmaa.DrmaaException - Drmaa had an issue setting the specification
     */
    protected void setAccount(SerializableJobTemplate jt) throws DrmaaException {
        String account = task.getOwner();
        if (account != null && account.length() > 0) {
            logger.info("setaccount = -A " + account);
            jt.setNativeSpecification("-A " + account);
        }
    }


    /**
     * If the method returns false no job is submitted to the grid;
     * It isused as a mechanism to prevent useless job submission to the grid
     * @param drmaaHelper reference to the helper class for DRMAA
     * @return boolean whether the job submission preconditions are satisified
     * @throws Exception error checking preconditions
     */
    protected boolean checkSubmitJobPrecondition(DrmaaHelper drmaaHelper) throws Exception {
        return true;
    }


    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     *
     * @param writer file writer
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                            - cannot find a file needed for processing
     * @throws IOException        - error accessing a file
     * @throws DaoException       - error interacting with the database
     * @throws ParameterException - error accessing task parameters
     */
    protected abstract void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception;


    protected void verifyConfigurationFiles() throws Exception {
    	for(int i=0; i<getJobIncrementStop(); i++) {
    		File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+(i+1));
    		if (!configFile.exists()) {
    			throw new IllegalStateException("Configuration file for job "+i+" does not exist: "+configFile.getAbsolutePath());
    		}
    	}
    }
    

    protected String getSGEConfigurationDirectory() {
        return resultFileNode.getDirectoryPath() + File.separator + "sge_config";
    }


    protected String getSGEErrorDirectory() {
        return resultFileNode.getDirectoryPath() + File.separator + "sge_error";
    }


    protected String getSGEOutputDirectory() {
        return resultFileNode.getDirectoryPath() + File.separator + "sge_output";
    }


    protected boolean collectStdErr() throws WaitForJobException, DaoException {
        // Use this hash to avoid identical messages
        File configDir = new File(getSGEErrorDirectory());
        File[] errorFiles = configDir.listFiles(new StdErrorFilenameFilter());
        if (errorFiles==null) {
            logger.warn("List of std error files came back null for dir=" + configDir.getAbsolutePath());
            return false;
        }
        if (logger.isInfoEnabled())
            logger.info("Found " + errorFiles.length + " stderr files in dir=" + configDir.getAbsolutePath());
        int numBytes = 0;
        for (File f : errorFiles) {
        	numBytes += f.length();
        }
        if (numBytes > 0) {
        	String note = numBytes+" bytes of output were written to stderr files in dir="+configDir.getAbsolutePath();
            EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(), note);
            return true;
        }
        return false;
    }


    public int getJobTimeoutSeconds() {
        return -1;
    }


    protected class StdErrorFilenameFilter implements FilenameFilter {
        public StdErrorFilenameFilter() {
        }
    	public boolean accept(File dir, String name) {
            return name.startsWith(getGridServicePrefixName() + "Error.");
        }
    }
}
