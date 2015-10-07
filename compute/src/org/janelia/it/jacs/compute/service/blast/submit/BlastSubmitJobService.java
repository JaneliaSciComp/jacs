
package org.janelia.it.jacs.compute.service.blast.submit;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.BlastServiceUtil;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This service submit a job to the Grid.  It's entirely extracted from work done by Sean Murphy
 * and Todd Safford.
 *
 * @author Sean Murphy
 * @author Todd Safford
 * @author Tareq Nabeel
 */
public class BlastSubmitJobService extends SubmitDrmaaJobService {
    private static final String RESULT_NAME_PROP = "BlastServer.ResultFilename";
    private static final String resultFilename = SystemConfigurationProperties.getString(RESULT_NAME_PROP);
    private static final String gridJavaPath = SystemConfigurationProperties.getString("Java.Path");
    private static final String mergeSortClasspath = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    private static final String temporaryDirectory = SystemConfigurationProperties.getString("SystemCall.ScratchDir");
    private static final String mergeSortProcessor = SystemConfigurationProperties.getString("BlastServer.PostblastMergeSortProcessor");
    private static final String entryOffsetProcessor = SystemConfigurationProperties.getString("BlastServer.PostBlastEntryOffsetProcessor");
    private static final String mergeSortMinMemory = SystemConfigurationProperties.getString("BlastServer.GridMergeSortMinimumMemoryMB");
    private static final String mergeSortMaxMemory = SystemConfigurationProperties.getString("BlastServer.GridMergeSortMaximumMemoryMB");
    private static final String blastResultFileName = SystemConfigurationProperties.getString("BlastServer.ResultFilename");
//    private static final String blastNormalQueue = SystemConfigurationProperties.getString("Grid.NormalQueue");
//    private static final String blastLowQueue = SystemConfigurationProperties.getString("Grid.LowQueue");
//    private static final Float lowThreshold = SystemConfigurationProperties.getFloat("BlastServer.LowThreshold");
    private static final Float fileSizeThresholdInkB = SystemConfigurationProperties.getFloat("Grid.FileSizeThresholdInkB");
    public static final String BLAST_RESULT_PREFIX = "r";
    private static final String BLAST_CONFIG_PREFIX = "blastConfiguration.";

    private int numberOfHitsToKeep;
    private float queueScore = 0.0f;
    // Commenting out since scratch is on NFS anyway.  When we used tmp, local to nodes, this made more sense
    private boolean queryFilesShouldBeLocalToGridNode;

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "blast";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        BlastTask blastTask = (BlastTask) task;
        // todo Need an explicit check for this node's existence in db and filestore!!!!!!!!!!!!!!!
        // todo Validate that this username has access rights to the FASTA file
//        EJBFactory.getLocalComputeBean().updateTaskStatus(blastTask.getObjectId(), Event.RUNNING_EVENT, "Running createtask " + blastTask.getObjectId());
        PartitionList partitionList = BlastServiceUtil.getPartitionList(processData, task);

        Map<File, File> queryOutputFileMap = getQueryOutputFileMap(processData);
        List<File> queryFiles = getBlastQueryFiles(processData);

        long databaseLength = partitionList.getDatabaseLength();
        if (partitionList.size() == 0) {
            throw new MissingDataException("Could not find any partitions in partition list.");
        }

        // Determine if query file copy is needed to lower possible NFS risk
        queryFilesShouldBeLocalToGridNode = (fileSizeThresholdInkB < (queryFiles.get(0).length() / 1000));

        // Adding the result file node id as it has to be unique, and process ids aren't (if scratch is a network mount)
        String tempBlastOutputFileName = FileUtil.ensureDirExists(temporaryDirectory + File.separator + resultFileNode.getObjectId())
                + File.separator + blastResultFileName + ".$BLASTINDEX";
        // Blast job template expects blastConfiguration.[intValue]  ... blastConfiguration.q[indx].[intValue] format didn't work

        createBlastShellScript(blastTask, databaseLength, getDummySubjectDBFile(), tempBlastOutputFileName, writer);

        int blastConfigIndex = 1;
        for (File queryFile : queryFiles) {
            File blastDestOutputDir = queryOutputFileMap.get(queryFile);
            if (blastDestOutputDir == null) {
                throw new MissingDataException("Could not find output directory for query file=" + queryFile.getAbsolutePath());
            }
            if (!blastDestOutputDir.exists()) {
                throw new IOException("Could not confirm that output directory exists=" + blastDestOutputDir.getAbsolutePath());
            }
            for (int i = 0; i < partitionList.getFileList().size(); i++) {
                String partitionResultName = BLAST_RESULT_PREFIX + "_" + i;
                File subjectDatabase = partitionList.getFileList().get(i);
                if (!subjectDatabaseExists(subjectDatabase)) {
                    throw new MissingDataException("Subject database " + subjectDatabase.getAbsolutePath() + " does not exist");
                }
                File outputFile = new File(new File(blastDestOutputDir, resultFilename).getAbsolutePath() + partitionResultName);
                blastConfigIndex = writeBlastConfigFile(queryFile, subjectDatabase, outputFile, blastConfigIndex);
                blastConfigIndex++;
            }
        }

        // Set the stop of the blast configuration
//       int blastConfigFileCount = partitionList.size()*queryFiles.size();
        int blastConfigFileCount = new File(getSGEConfigurationDirectory()).listFiles(new BlastConfigurationFileFilter()).length;
        setJobIncrementStop(blastConfigFileCount);

        // Determine queue score used in setting queue with method setNativeSpecification
        float programScore = 1.0f;
        if (blastTask instanceof BlastNTask) {
            programScore *= 1.0f;
        }
        else if (blastTask instanceof MegablastTask) {
            programScore *= 0.5f;
        }
        else if (blastTask instanceof TBlastXTask) {
            programScore *= 36.0f;
        }
        else if (blastTask instanceof BlastXTask) {
            programScore *= 6.0f;
        }
        else if (blastTask instanceof TBlastNTask) {
            programScore *= 6.0f;
        }
        else if (blastTask instanceof BlastPTask) {
            programScore *= 1.0f;
        }
        queueScore = blastConfigFileCount * programScore;
    }

//      Choosing to use the override method below for immediate processing.
//    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
//
//        String queueName;
//        String queueNameBasedOnScore;
//        String queueNameBasedOnPriority;
//
//        // Determine the queue baded on the queue score
//        if (queueScore >= lowThreshold) {
//            //queueName=blastLowQueue;
//            queueNameBasedOnScore = blastLowQueue;
//        }
//        else {
//            //queueName=blastNormalQueue;
//            queueNameBasedOnScore = blastNormalQueue;
//        }
//
//        logger.info("Drmaa job=" + jt.getJobName() + " has queueScore=" + queueScore + " assigned queue=" + queueNameBasedOnScore);
//
//        // Now, determine the queue based on the assigned job's priority. Get job's priority from the process file.
//        // If the priority is low then assign lower Queue else normal queue
//        String priority = (String) processData.getItem("JobPriority");
//        logger.info("Drmaa job=" + jt.getJobName() + " has priority=" + priority);
//
//
//        // If priority is not null, identify the queue name based on priority.
//        // Set the queue name to the lower queue between the queue based on priority
//        // and queue name based on the queue score.
//        if (priority != null) {
//
//            if (priority.equalsIgnoreCase("High")) {
//                queueNameBasedOnPriority = blastNormalQueue;
//            }
//            else {
//                queueNameBasedOnPriority = blastLowQueue;
//            }
//
//            logger.info("Drmaa job=" + jt.getJobName() + " has priority=" + priority + " assigned queue=" + queueNameBasedOnPriority);
//
//            // Now, assign the lower queue between queueNameBasedOnScore and queueNameBasedOnPriority to the
//            // job's specification.
//            if (queueNameBasedOnScore.equals(queueNameBasedOnPriority)) {
//                queueName = queueNameBasedOnScore;
//            }
//            else {
//                queueName = queueNameBasedOnPriority;
//            }
//        }
//        else {
//            // If priority is null, set the queuename to the queue name obtained
//            // based on the queue score
//            queueName = queueNameBasedOnScore;
//        }
//
//        logger.info("Blast Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + queueName);
//        jt.setNativeSpecification(queueName);
//    }

    @Override
    protected boolean isImmediateProcessingJob() {
        return true;
    }

    private File getDummySubjectDBFile() {
        // Even though subjectDatabase's absolute file path is passed to
        // BlastCommand.getCommandString(), it gets replaced later in createBlastShellScript :
        // else if (j > 0 && blastCommandArr[j - 1].equals("-d")) { genericBlastBuffer.append(" $BLASTDB");
        // .. so we could return f...in sh.. if we wanted to and it wouldn't matter.
        return new File(".");
    }

    private int writeBlastConfigFile(File queryFile, File subjectDatabase, File outputFile, int blastConfigIndex) throws IOException {
        // For each partition, we need to create an input file for the script
        File configFile = new File(getSGEConfigurationDirectory(), BLAST_CONFIG_PREFIX + blastConfigIndex);
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), BLAST_CONFIG_PREFIX + (++blastConfigIndex));
        }
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(queryFile.getAbsolutePath() + "\n");
            fw.write(subjectDatabase.getAbsolutePath() + "\n");
            fw.write(outputFile.getAbsolutePath() + "\n");
            fw.write(blastConfigIndex + "\n");
        }
        finally {
            fw.close();
        }
        return blastConfigIndex;
    }

    private void createBlastShellScript(BlastTask blastTask, long databaseLength, File subjectDatabase, String tempBlastOutputFileName, FileWriter writer) throws IOException, ParameterException {
        String tmpQueryFile = temporaryDirectory + File.separator + resultFileNode.getObjectId() + File.separator + "tmp.$BLASTINDEX.fasta";
        BlastCommand blastCommand = new BlastCommand();
        String blastCommandString = blastCommand.getCommandString(blastTask, subjectDatabase, databaseLength, tempBlastOutputFileName);
        String[] blastCommandArr = blastCommandString.split("\\s+");
        if (blastCommandArr == null || blastCommandArr.length < 2)
            throw new IllegalArgumentException("Could not parse blast command: " + blastCommandString);
        StringBuilder genericBlastBuffer = new StringBuilder();
        for (int j = 0; j < blastCommandArr.length; j++) {
            if (j > 0 && blastCommandArr[j - 1].equals("-o")) {
                genericBlastBuffer.append(" ").append(tempBlastOutputFileName);
            }
            else if (j > 0 && blastCommandArr[j - 1].equals("-d")) {
                genericBlastBuffer.append(" $BLASTDB");
            }
            else if (j > 0 && blastCommandArr[j - 1].equals("-i") && queryFilesShouldBeLocalToGridNode) {
                genericBlastBuffer.append(" ").append(tmpQueryFile);
            }
            else if (j > 0 && (blastCommandArr[j - 1].equals("-b") || blastCommandArr[j - 1].equals("-v"))) {
                numberOfHitsToKeep = Integer.parseInt(blastCommandArr[j]);
                if (numberOfHitsToKeep != BlastServiceUtil.getNumberOfHitsToKeepFromBlastTask(blastTask)) {
                    throw new ParameterException("Discrepancy between bestHitsToKeep from BlastTask and BlastCommand for blastTask=" + blastTask.getObjectId());
                }
                genericBlastBuffer.append(" ").append(numberOfHitsToKeep);
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting numberOfHitsToKeep to " + numberOfHitsToKeep + " for -b and -v");
                }
            }
            else {
                genericBlastBuffer.append((" " + blastCommandArr[j]));
            }
        }
        processData.putItem(BlastProcessDataConstants.BLAST_NUMBER_OF_HITS_TO_KEEP, numberOfHitsToKeep);
        boolean skipLocalMergeSort = Boolean.valueOf(processData.getString("SKIP_LOCAL_MERGE_SORT"));
        // If the workflow has large single entries cut into pieces, have the grid shift the coordinates accordingly
        boolean resolveEntryOffsets = Boolean.valueOf(processData.getString("RESOLVE_OFFSETS"));

        // Format the executable script
        // Note - the persist which runs blast will only parse its own xml output, which is why -f and -l are the same index, below
        StringBuilder script = new StringBuilder();
        script.append("read BLASTQUERY_FILE\n");
        script.append("read BLASTDB\n");
        script.append("read BLASTOUTPUT\n");
        script.append("read BLASTINDEX\n");
        if (queryFilesShouldBeLocalToGridNode) {
            script.append("cp $BLASTQUERY_FILE ").append(tmpQueryFile).append("\n");
            script.append("export BLASTQUERY_FILE=").append(tmpQueryFile).append("\n");
        }
        script.append(genericBlastBuffer.toString()).append("\n");
        if (!skipLocalMergeSort) {
            script.append("sed -i \"/\\\"http:.*NCBI_BlastOutput\\.dtd\\\"/d\" ").append(tempBlastOutputFileName).append("\n");
            script.append(gridJavaPath).append(" -Xms").append(mergeSortMinMemory).append("m -Xmx").
                    append(mergeSortMaxMemory).append("m -classpath ").append(mergeSortClasspath).append(" ").
                    append(mergeSortProcessor).append(" -o ").append(tempBlastOutputFileName);
            script.append("\ncp ").append(tempBlastOutputFileName).append(BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION).append(" ${BLASTOUTPUT}").append(BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
            script.append("\nrm ").append(tempBlastOutputFileName).append(BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
        }
        // For cases where offsets are listed in the defline.
        // Offset is added to the begin and end coordinates and the defline is scrubbed. 
        if (resolveEntryOffsets) {
            script.append(gridJavaPath).append(" -Xms").append(mergeSortMinMemory).append("m -Xmx").
                    append(mergeSortMaxMemory).append("m -classpath ").append(mergeSortClasspath).append(" ").
                    append(entryOffsetProcessor).append(" ").append(tempBlastOutputFileName);
        }
        if (skipLocalMergeSort) {
            script.append("\ncp ").append(tempBlastOutputFileName).append(" $BLASTOUTPUT");
        }
        script.append("\nrm -f ").append(tempBlastOutputFileName);
//        if (queryFilesShouldBeLocalToGridNode) {
//            script.append("\nrm -f ").append(tmpQueryFile);
//        }
        script.append("\n");
        writer.write(script.toString());
    }

    private boolean subjectDatabaseExists(File rootFile) {
        File nucIndexFile = new File(rootFile.getAbsolutePath() + ".nhr");
        File proIndexFile = new File(rootFile.getAbsolutePath() + ".phr");
        return nucIndexFile.exists() || proIndexFile.exists();
    }

    protected List<File> getBlastQueryFiles(IProcessData processData) throws MissingDataException {
        try {
            return (List<File>) processData.getMandatoryItem(BlastProcessDataConstants.BLAST_QUERY_FILES);
        }
        catch (Exception ex) {
            throw new MissingDataException("Could not find proper entry for " + BlastProcessDataConstants.BLAST_QUERY_FILES);
        }
    }

    protected Map<File, File> getQueryOutputFileMap(IProcessData processData) throws MissingDataException {
        try {
            return (Map<File, File>) processData.getMandatoryItem(BlastProcessDataConstants.BLAST_QUERY_OUTPUT_FILE_MAP);
        }
        catch (Exception ex) {
            throw new MissingDataException("Could not find proper entry for " + BlastProcessDataConstants.BLAST_QUERY_OUTPUT_FILE_MAP);
        }
    }

    private static class BlastConfigurationFileFilter implements FilenameFilter {
        public BlastConfigurationFileFilter() {
        }

        public boolean accept(File dir, String name) {
            return name != null && name.startsWith(BLAST_CONFIG_PREFIX);
        }
    }

}
