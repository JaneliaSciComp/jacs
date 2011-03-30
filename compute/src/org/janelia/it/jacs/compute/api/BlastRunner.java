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

package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.service.blast.persist.query.PersistQNodeException;
import org.janelia.it.jacs.compute.service.blast.persist.query.PersistQueryNodeRemote;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskFactory;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.*;


/**
 * This class is a quick and dirty means for unfortunate users to run Non-Gui blast through the command line.
 * It does have the ability to execute Gui blast as well.  You'll just have to look deeper to figure how.
 *
 * @author Tareq Nabeel
 */
public class BlastRunner {

    private static final String PARAM_FILE_PATH_ARG_KEY = "paramfile";
    private static final String PARAM_JOB_ARG_KEY = "job";
    public static final String PARAM_FASTAFILE_KEY = "fastafile";
    public static final String PARAM_FASTATEXT_KEY = "fastatext";
    private static final String PARAM_WAIT_FOR_COMPLTION_KEY = "wait";
    private static final String PARAM_OUTPUT_DIR_KEY = "outputdir";
    public static final String PARAM_USER_ID_KEY = "userid";
    public static final String PARAM_DATASET_KEY = "subjectDatabase";
    public static final String PARAM_BLAST_TYPE_KEY = "blastType";
    public static final String PARAM_EVALUE = "evalue";
    public static final String PARAM_DB_ALIGNMENTS = "databaseAlignments";

    private String jobName;
    private boolean waitForCompletion;
    private String outputDir;
    private Task savedTask;
    private Properties commandLineProperties = new Properties();
    private TaskServiceProperties blastProperties;
    private ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();

    /**
     * Entry point
     *
     * @param args
     */
    public static void main(String args[]) {
        BlastRunner blastRunner = new BlastRunner();
        try {
            blastRunner.init(args);
            blastRunner.run();
        }
        catch (IllegalArgumentException e) {
            System.out.println("Blast failed for job: " + blastRunner.getJobName());
            e.printStackTrace();
            System.out.println(getUsage());
        }
        catch (Exception e) {
            System.out.println("Blast failed for job: " + blastRunner.getJobName());
            e.printStackTrace();
        }
    }

    /**
     * Prepares BlastRunner for execution
     *
     * @param args User supplied parameter file path and overrides
     * @throws IOException
     */
    private void init(String args[]) throws IOException, URISyntaxException, DaoException, PersistQNodeException {
        loadCommandLineArguments(args);
        String propsPath = extractBlastParameterFilePath();
        blastProperties = new TaskServiceProperties(FileUtil.getFileContentsAsStream(FileUtil.checkFileExists(propsPath)));
        // Have command line params override file values.
        blastProperties.putAll(commandLineProperties);
        init();
    }

    public Task init() throws IOException, URISyntaxException, PersistQNodeException, DaoException {
        setDefaultJobName();
        setWaitForCompletion();
        setOutputDir();

        // Create and save the fasta file node
        Long queryNodeId = saveQueryNode();

        // Create and save the task
        return createAndSaveTask(queryNodeId);
    }

    /**
     * @throws Exception
     */
    public void run() throws RemoteException {
        // Submit the job
        submitJob();
    }

    private void submitJob() throws RemoteException {
        System.out.println("Submitted Job: " + jobName + " Id: " + savedTask.getObjectId() + " with blast properties: " + blastProperties);
        String specifiedProcessName = blastProperties.getString("process", "");
        if (specifiedProcessName.equals("")) {
            computeBean.submitJob("BlastWithGridMerge", savedTask.getObjectId());
        }
        else {
            computeBean.submitJob(specifiedProcessName, savedTask.getObjectId());
        }
    }

    private Task createAndSaveTask(Long queryNodeId) throws DaoException, RemoteException {
        BlastTask blastTask = (BlastTask) TaskFactory.createTask(blastProperties.getString(PARAM_BLAST_TYPE_KEY));
        blastTask.setJobName(jobName);
        blastTask.setOwner(blastProperties.getString(PARAM_USER_ID_KEY));
        Map initialBlastParameterMap = initializeBlastParameters();
        for (Object o : initialBlastParameterMap.keySet()) {
            String key = (String) o;
            blastTask.setParameter(key, (String) initialBlastParameterMap.get(key));
        }
        setBlastQuery(blastTask, queryNodeId);
        setBlastDataset(blastTask, blastProperties.getString(PARAM_DATASET_KEY), computeBean);

        // blastTask will not have object id after save because it's passed by value to remote ejb
        // saved createtask will have an object id
        System.out.println("Saving blast task..");
        savedTask = computeBean.saveOrUpdateTask(blastTask);
        return savedTask;
    }

    private Long saveQueryNode() throws RemoteException, PersistQNodeException {
        PersistQueryNodeRemote persistNodeRemote = (PersistQueryNodeRemote)
                EJBFactory.getRemoteInterface("remote/PersistBlastQueryNodeSLSB");

        String username = blastProperties.getString("userid");
        String fastaFilePath = blastProperties.getString(PARAM_FASTAFILE_KEY, "");
        String fastaText = blastProperties.getString(PARAM_FASTATEXT_KEY, "");
        if (fastaFilePath.equals("") && fastaText.equals("")) {
            throw new IllegalArgumentException(PARAM_FASTAFILE_KEY + " or " + PARAM_FASTATEXT_KEY + " must be specified");
        }
        if (fastaFilePath.length() > 0) {
            System.out.println("Saving fasta file node..");
            return persistNodeRemote.saveFastaFileNode(username, fastaFilePath);
        }
        else if (fastaText != null && fastaText.length() > 0) {
            System.out.println("Saving query sequence fasta file node..");
            return persistNodeRemote.saveFastaText(username, fastaText);
        }
        else {
            throw new IllegalArgumentException("Either fastaFileInput or fastaText has to be specified");
        }
    }

    private void setBlastQuery(BlastTask blastTask, Long queryNodeId) {
        System.out.println("Setting blast task query node id:" + queryNodeId);
        blastTask.setParameter(BlastNTask.PARAM_query, Long.toString(queryNodeId));
    }

    private void setBlastDataset(BlastTask blastTask, String datasetName, ComputeBeanRemote computeBean) throws RemoteException {
        System.out.println("Using datasetName:" + datasetName);
        Long datasetNodeId = computeBean.getBlastDatabaseFileNodeIdByName(datasetName);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(String.valueOf(datasetNodeId));
        //ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastTask.setParameter(BlastNTask.PARAM_subjectDatabases, Task.csvStringFromList(ms.getActualUserChoices()));
    }

    private void loadCommandLineArguments(String args[]) {
        if (args != null && args.length < 1) {
            throw new IllegalArgumentException("Path to blast parameters file must be supplied on the command line");
        }
        if (null==args) {
            throw new IllegalArgumentException("Argument list cannot be empty.");
        }
        // Load up command line arguments into commandLineProperties
        for (String arg : args) {
            String[] argNameAndValue = arg.split("=");
            if (argNameAndValue.length != 2) {
                throw new IllegalArgumentException("Parameter name and value must be separated by '='");
            }
            commandLineProperties.put(argNameAndValue[0], argNameAndValue[1]);
        }
    }

    private String extractBlastParameterFilePath() {
        // Validate blast parameters file path
        String paramFilePathValue = (String) commandLineProperties.get(PARAM_FILE_PATH_ARG_KEY);
        if (paramFilePathValue == null) {
            throw new IllegalArgumentException("Path to blast parameters file must be supplied on the command line");
        }
        return paramFilePathValue;
    }

    private void setDefaultJobName() {
        jobName = blastProperties.getString(PARAM_JOB_ARG_KEY, "Job" + String.valueOf(System.currentTimeMillis()));
    }


    private void setWaitForCompletion() {
        waitForCompletion = blastProperties.getBoolean(PARAM_WAIT_FOR_COMPLTION_KEY, false);
    }

    private void setOutputDir() throws IOException {
        outputDir = blastProperties.getString(PARAM_OUTPUT_DIR_KEY, "");
        if (outputDir != null && !outputDir.trim().equals("")) {
            outputDir = FileUtil.ensureDirExists(outputDir).getAbsolutePath();
        }
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }


    public Task getSavedTask() {
        return savedTask;
    }

    private Map initializeBlastParameters() {
        Map<String, String> blastParameters = new HashMap<String, String>();
        blastParameters.put(BlastTask.PARAM_evalue, Long.toString(blastProperties.getLong("evalue", BlastTask.evalue_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_wordsize, Long.toString(blastProperties.getLong("wordsize", BlastNTask.wordsize_BLASTN_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_filter, blastProperties.getString("filter", BlastTask.filter_DEFAULT));
        blastParameters.put(BlastTask.PARAM_gapOpenCost, Long.toString(blastProperties.getLong("gapOpenCost", BlastTask.gapOpenCost_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_gapExtendCost, Long.toString(blastProperties.getLong("gapExtendCost", BlastTask.gapExtendCost_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_gappedAlignmentDropoff, Long.toString(blastProperties.getLong("gappedAlignmentDropoff", BlastNTask.gappedAlignmentDropoff_BLASTN_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_showGIs, Boolean.toString(blastProperties.getBoolean("showGIs", BlastTask.showGIs_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_databaseAlignments, Long.toString(blastProperties.getLong("databaseAlignments", BlastTask.databaseAlignments_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_hitExtensionThreshold, Long.toString(blastProperties.getLong("hitExtensionThreshold", BlastNTask.hitExtensionThreshold_BLASTN_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_believeDefline, Boolean.toString(blastProperties.getBoolean("believeDefline", BlastTask.believeDefline_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_matrix, blastProperties.getString("matrix", BlastTask.matrix_DEFAULT));
        blastParameters.put(BlastTask.PARAM_databaseSize, Double.toString(blastProperties.getDouble("databaseSize", BlastTask.databaseSize_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_bestHitsToKeep, Long.toString(blastProperties.getLong("bestHitsToKeep", BlastTask.bestHitsToKeep_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_searchSize, Double.toString(blastProperties.getDouble("searchSize", BlastTask.searchSize_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_lowerCaseFiltering, Boolean.toString(blastProperties.getBoolean("lowerCaseFiltering", BlastTask.lowerCaseFiltering_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_ungappedExtensionDropoff, Double.toString(blastProperties.getDouble("ungappedExtensionDropoff", BlastNTask.ungappedExtensionDropoff_BLASTN_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_finalGappedDropoff, Double.toString(blastProperties.getDouble("finalGappedDropoff", BlastNTask.finalGappedDropoff_BLASTN_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_multiHitWindowSize, Long.toString(blastProperties.getLong("multiHitWindowSize", BlastNTask.multiHitWindowSize_BLASTN_DEFAULT)));
        blastParameters.put(BlastTask.PARAM_largeBlastOutputDir, outputDir);
        return blastParameters;
    }

    private static String getUsage() {
        return "\nUsage: BlastRunner " + PARAM_FILE_PATH_ARG_KEY + "=<path to blast parameters file>" +
                "\n e.g. BlastRunner paramfile=./blast.parameters fastafile=./input/myfasta large=false evalue=1" +
                "\nAll file parameters can be overridden on commandline.  Separate key and value with '='.  Separate key/value pairs with a space." +
                "\n\nSupported parameters include:\n" +
                getKeyAndDescription("Parameter Name", "Description", "Default Value") +
                getKeyAndDescription("--------------", "-----------", "-------------") +
                getKeyAndDescription(PARAM_USER_ID_KEY, "preexisting user login", "[MANDATORY]") +
                getKeyAndDescription(PARAM_DATASET_KEY, "name of the dataset to run blast against", "[MANDATORY]") +
                getKeyAndDescription(PARAM_FASTAFILE_KEY, "path to fasta file", "[OPTIONAL if fastatext specified]") +
                getKeyAndDescription(PARAM_FASTATEXT_KEY, "sequence to search", "[OPTIONAL if fastafile specified]") +
                getKeyAndDescription(PARAM_BLAST_TYPE_KEY, "[" + BlastNTask.BLASTN_NAME + ", " + BlastPTask.BLASTP_NAME + ", " + BlastXTask.BLASTX_NAME + ", " + MegablastTask.MEGABLAST_NAME + ", " + TBlastNTask.TBLASTN_NAME + ", or " + TBlastXTask.TBLASTX_NAME + "]", BlastNTask.BLASTN_NAME) +
                getKeyAndDescription(PARAM_EVALUE, BlastTask.PARAM_evalue, BlastTask.evalue_DEFAULT) +
                getKeyAndDescription("wordsize", BlastTask.PARAM_wordsize, BlastNTask.wordsize_BLASTN_DEFAULT) +
                getKeyAndDescription("filter", BlastTask.PARAM_filter, BlastTask.filter_DEFAULT) +
                getKeyAndDescription("gapOpenCost", BlastTask.PARAM_gapOpenCost, BlastTask.gapOpenCost_DEFAULT) +
                getKeyAndDescription("gapExtendCost", BlastTask.PARAM_gapExtendCost, BlastTask.gapExtendCost_DEFAULT) +
                getKeyAndDescription("gappedAlignmentDropoff", BlastTask.PARAM_gappedAlignmentDropoff, BlastNTask.gappedAlignmentDropoff_BLASTN_DEFAULT) +
                getKeyAndDescription("showGIs", BlastTask.PARAM_showGIs, BlastTask.showGIs_DEFAULT) +
                getKeyAndDescription("databaseAlignments", BlastTask.PARAM_databaseAlignments, BlastTask.databaseAlignments_DEFAULT) +
                getKeyAndDescription("hitExtensionThreshold", BlastTask.PARAM_hitExtensionThreshold, BlastNTask.hitExtensionThreshold_BLASTN_DEFAULT) +
                getKeyAndDescription("believeDefline", BlastTask.PARAM_believeDefline, BlastTask.believeDefline_DEFAULT) +
                getKeyAndDescription("matrix", BlastTask.PARAM_matrix, BlastTask.matrix_DEFAULT) +
                getKeyAndDescription("databaseSize", BlastTask.PARAM_databaseSize, BlastTask.databaseSize_DEFAULT) +
                getKeyAndDescription("bestHitsToKeep", BlastTask.PARAM_bestHitsToKeep, BlastTask.bestHitsToKeep_DEFAULT) +
                getKeyAndDescription("searchSize", BlastTask.PARAM_searchSize, BlastTask.searchSize_DEFAULT) +
                getKeyAndDescription("lowerCaseFiltering", BlastTask.PARAM_lowerCaseFiltering, BlastTask.lowerCaseFiltering_DEFAULT) +
                getKeyAndDescription("ungappedExtensionDropoff", BlastTask.PARAM_ungappedExtensionDropoff, BlastNTask.ungappedExtensionDropoff_BLASTN_DEFAULT) +
                getKeyAndDescription("finalGappedDropoff", BlastTask.PARAM_finalGappedDropoff, BlastNTask.finalGappedDropoff_BLASTN_DEFAULT) +
                getKeyAndDescription("multiHitWindowSize", BlastTask.PARAM_multiHitWindowSize, BlastNTask.multiHitWindowSize_BLASTN_DEFAULT) +
                getKeyAndDescription(PARAM_WAIT_FOR_COMPLTION_KEY, "whether or not to wait for job completion before exit", Boolean.FALSE) +
                getKeyAndDescription(PARAM_JOB_ARG_KEY, "job name", "Job + current time") +
                getKeyAndDescription(PARAM_OUTPUT_DIR_KEY, "directory to output blast results to for large blast", "[filestore location]/[userid]/[jobId]/");
    }


    /**
     */
    public static String getKeyAndDescription(String paramKey, String description, Object defaultValue) {
        StringBuffer paddedVal = new StringBuffer();
        int numCharsToPad = 25 - paramKey.length();
        paddedVal.append("\n");
        paddedVal.append(paramKey);
        for (int i = 0; i < numCharsToPad; ++i) {
            paddedVal.append(" ");
        }
        paddedVal.append("\t");
        paddedVal.append(description);
        numCharsToPad = 70 - description.length();
        for (int i = 0; i < numCharsToPad; ++i) {
            paddedVal.append(" ");
        }
        paddedVal.append(defaultValue.toString());
        return paddedVal.toString();
    }


    public void setBlastProperties(TaskServiceProperties blastProperties) {
        this.blastProperties = blastProperties;
    }
}
