
package org.janelia.it.jacs.compute.service.reversePsiBlast;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.util.SubjectDBUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastDatabaseNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.blast.BlastGridMergeSort;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 23, 2008
 * Time: 11:52:21 AM
 */
public class ReversePsiBlastSubmitJobService extends SubmitDrmaaJobService {

    private static final String gridJavaPath = SystemConfigurationProperties.getString("Java.Path");
    private static final String mergeSortClasspath = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    private static final String mergeSortProcessor = SystemConfigurationProperties.getString("BlastServer.PostblastMergeSortProcessor");
    private static final String mergeSortMinMemory = SystemConfigurationProperties.getString("BlastServer.GridMergeSortMinimumMemoryMB");
    private static final String mergeSortMaxMemory = SystemConfigurationProperties.getString("BlastServer.GridMergeSortMaximumMemoryMB");

    private static final String rpsBlastCmd = SystemConfigurationProperties.getString("Executables.ModuleBase")+
            SystemConfigurationProperties.getString("RpsBlast.Cmd");
    private static final String resultFilename = SystemConfigurationProperties.getString("RpsBlast.ResultName");
    private static final String localTmpDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");
    private static final String normalQueue = SystemConfigurationProperties.getString("Grid.NormalQueue");
    private static final String lowQueue = SystemConfigurationProperties.getString("Grid.LowQueue");
    private static final Float lowThreshold = SystemConfigurationProperties.getFloat("RpsBlast.LowThreshold");
    public static final String RESULT_PREFIX = "r";
    private static final String CONFIG_PREFIX = "rpsblastConfiguration.";

    private float queueScore = 0.0f;
    private boolean isQuerySequenceProtein = false;

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "rpsblast";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        ReversePsiBlastTask rpsblastTask = (ReversePsiBlastTask) task;
        PartitionList partitionList = (PartitionList) processData.getItem(FileServiceConstants.PARTITION_LIST);
        String queryNodeIdString = rpsblastTask.getParameter(ReversePsiBlastTask.PARAM_query_node_id);
        String dbNodeIdString = rpsblastTask.getParameter(ReversePsiBlastTask.PARAM_subjectDatabases);
        List<File> queryFiles = (List<File>) processData.getMandatoryItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
        Map<File, File> queryOutputFileMap = (Map<File, File>) processData.getMandatoryItem(FileServiceConstants.INPUT_OUTPUT_DIR_MAP);

        Long queryNodeId = new Long(queryNodeIdString);
        Long dbNodeId = new Long(dbNodeIdString);
        FastaFileNode queryNode = (FastaFileNode) computeDAO.genericLoad(FastaFileNode.class, queryNodeId);
        isQuerySequenceProtein = SequenceType.PEPTIDE.equalsIgnoreCase(queryNode.getSequenceType());
        ReversePsiBlastDatabaseNode rpsblastdbNode = (ReversePsiBlastDatabaseNode) computeDAO.genericLoad(ReversePsiBlastDatabaseNode.class, dbNodeId);
        //String dbFilepath=rpsblastdbNode.getFilePathByTag(ReversePsiBlastDatabaseNode.TAG_RPSDB);

        Long totalProfileCount = rpsblastdbNode.getLength();  // for now we assume only a single db, which may be partitioned
        if (partitionList.size() == 0) {
            throw new MissingDataException("PartitionList is empty");
        }

        createShellScript(rpsblastTask, totalProfileCount, writer);

        int configIndex = 1;
        Map<File, List<File>> inputOutputFileListMap = new HashMap<File, List<File>>();
        for (File queryFile : queryFiles) {
            List<File> outputFileList = new ArrayList<File>();
            File outputDir = queryOutputFileMap.get(queryFile);
            if (outputDir == null) {
                throw new MissingDataException("Could not find output directory for query file=" + queryFile.getAbsolutePath());
            }
            if (!outputDir.exists()) {
                throw new IOException("Could not confirm that output directory exists=" + outputDir.getAbsolutePath());
            }
            for (int i = 0; i < partitionList.getFileList().size(); i++) {
                String partitionResultName = RESULT_PREFIX + "_" + i;
                File subjectDatabase = partitionList.getFileList().get(i);
                if (!subjectDatabase.exists()) {
                    throw new MissingDataException("Subject database " + subjectDatabase.getAbsolutePath() + " does not exist");
                }
                File rpsblastOutputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + partitionResultName);
                configIndex = writeConfigFile(queryFile, subjectDatabase, rpsblastOutputFile, configIndex);
                configIndex++;
                outputFileList.add(rpsblastOutputFile);
            }
            inputOutputFileListMap.put(queryFile, outputFileList);
        }
        processData.putItem(FileServiceConstants.INPUT_OUTPUT_FILE_LIST_MAP, inputOutputFileListMap);

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);

        queueScore = configFileCount;
    }

    protected void setQueue(JobTemplate jt) throws DrmaaException {

        String queueName;
        String queueNameBasedOnScore;
        String queueNameBasedOnPriority;

        // Determine the queue baded on the queue score
        if (queueScore >= lowThreshold) {
            queueNameBasedOnScore = lowQueue;
        }
        else {
            queueNameBasedOnScore = normalQueue;
        }

        logger.info("Drmaa job=" + jt.getJobName() + " has queueScore=" + queueScore + " assigned queue=" + queueNameBasedOnScore);

        // Now, determine the queue based on the assigned job's priority. Get job's priority from the process file.
        // If the priority is low then assign lower Queue else normal queue
        String priority = (String) processData.getItem("JobPriority");
        logger.info("Drmaa job=" + jt.getJobName() + " has priority=" + priority);


        // If priority is not null, identify the queue name based on priority.
        // Set the queue name to the lower queue between the queue based on priority
        // and queue name based on the queue score.
        if (priority != null) {

            if (priority.equalsIgnoreCase("High")) {
                queueNameBasedOnPriority = normalQueue;
            }
            else {
                queueNameBasedOnPriority = lowQueue;
            }

            logger.info("Drmaa job=" + jt.getJobName() + " has priority=" + priority + " assigned queue=" + queueNameBasedOnPriority);

            // Now, assign the lower queue between queueNameBasedOnScore and queueNameBasedOnPriority to the
            // job's specification.
            if (queueNameBasedOnScore.equals(queueNameBasedOnPriority)) {
                queueName = queueNameBasedOnScore;
            }
            else {
                queueName = queueNameBasedOnPriority;
            }
        }
        else {
            // If priority is null, set the queuename to the queue name obtained
            // based on the queue score
            queueName = queueNameBasedOnScore;
        }

        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + queueName);
        jt.setNativeSpecification(queueName);
    }

    private int writeConfigFile(File queryFile, File subjectDatabase, File rpsblastOutputFile, int configIndex)
            throws IOException, MissingDataException {
        File outputDir = rpsblastOutputFile.getParentFile();
        // String serializedOutputFilename = rpsblastOutputFile.getName() + ParsedBlastResultsUtil.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION;
        // File serializedOutputFile = new File(outputDir,serializedOutputFilename);
        File hitCountFile = new File(outputDir, BlastResultFileNode.TAG_TOTAL_BLAST_HITS);
        File queryHitCountFile = new File(outputDir, "queryBlastHits");
        File pbrccOutputFile = new File(outputDir, BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_FILENAME);

        // For each partition, we need to create an input file for the script
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }

        // determine rsp blast subject db name
        File dbFile = SubjectDBUtils.getSubjectDBFile(subjectDatabase, ".rps");
        String subjectDatabaseName = dbFile.getName().substring(0, dbFile.getName().indexOf("."));

        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(queryFile.getAbsolutePath());
            configWriter.println(subjectDatabase.getAbsolutePath() + File.separator + subjectDatabaseName);
            configWriter.println(rpsblastOutputFile.getAbsolutePath());
            configWriter.println(hitCountFile.getAbsolutePath());
            configWriter.println(queryHitCountFile.getAbsolutePath());
            configWriter.println(pbrccOutputFile.getAbsolutePath());
            configWriter.println(configIndex);
        }
        finally {
            configWriter.close();
        }
        return configIndex;
    }

    private String buildConfigFileName(int configIndex) {
        return CONFIG_PREFIX + configIndex;
    }

    private void createShellScript(ReversePsiBlastTask rpsblastTask, long totalProfileCount, FileWriter writer)
            throws IOException, ParameterException {
        /* set up grid node local filesystem locations for
               - the rpsblast command query and output files (should I also copy the blast db dir?)
               - the output file's serialized ParsedBlastResultsCollection and total hit count output files
         */
        String tempQueryFileName = FileUtil.ensureDirExists(localTmpDirPath + File.separator + resultFileNode.getObjectId())
                + File.separator + "rpsblastQueryFile.$BLASTINDEX";
        // String tempDBName = FileUtil.checkFilePath(localTmpDirPath) + File.separator + "rpsblastDB.$BLASTINDEX";
        String tempOutputFileName = FileUtil.ensureDirExists(localTmpDirPath + File.separator + resultFileNode.getObjectId())
                + File.separator + resultFilename + ".$BLASTINDEX";
        String tempSerializedOutputFilename = tempOutputFileName + BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION;
        String tempHitCountFileName = tempOutputFileName + BlastGridMergeSort.BLAST_HIT_COUNT_SUFFIX;
        String tempQueryHitCountFileName = tempOutputFileName + BlastGridMergeSort.QUERY_HIT_COUNT_SUFFIX;

        // build the rpsblast command
        String initialCmd = rpsblastTask.generateDefaultCommandStringNotIncludingIOParams();
        if (totalProfileCount > 0) {
            initialCmd = initialCmd.trim() + " -z " + totalProfileCount;
        }
        String fullCmd = rpsBlastCmd
                + " " + initialCmd
                + " -p " + (isQuerySequenceProtein ? "T" : "F")
                + " -d $DB" // is DB read more than once by rpsblast???
                // +" -d " + tempDBName
                + " -i " + tempQueryFileName
                + " -m 7"
                + " -o " + tempOutputFileName;

        StringBuffer script = new StringBuffer();
        /* read in the script config params for rpsblast query and db locations
           and nfs mounted rpsblast output, serialized output, and hit count files
         */
        script.append("read QUERYFILE\n");
        script.append("read DB\n");
        script.append("read OUTPUTFILE\n");
        script.append("read HITCOUNTFILE\n");
        script.append("read QUERYHITCOUNTFILE\n");
        script.append("read PBRCC_OUTPUTFILE\n");
        script.append("read BLASTINDEX\n");

        // copy the nfs query file to the node local file location
        script.append("cp $QUERYFILE ").append(tempQueryFileName).append("\n");
        // script.append("cp -r $DB " + tempDBName + "\n");

        // run the rpsblast command
        script.append(fullCmd).append("\n");

        /* run local merge sort to generate serialized object file and hit count for next task step
           make sure to first remove the output file dtd to avoid generating multiple simultaneous dtd requests
           that ncbi may treat as a denial of service attack
         */
        script.append("sed -i \"/\\\"http:.*NCBI_BlastOutput\\.dtd\\\"/d\" ").append(tempOutputFileName).append("\n");
        script.append(gridJavaPath).append(" -Xms").append(mergeSortMinMemory).append("m -Xmx")
                .append(mergeSortMaxMemory).append("m -classpath ").append(mergeSortClasspath).append(" ")
                .append(mergeSortProcessor).append(" -o ").append(tempOutputFileName).append(" -full\n");

        /* move the node local result files to the desitred nfs mounted output locations
           using copy and delete operations to avoid issues with mv command non-atomicity
         */
        script.append("cp ").append(tempOutputFileName).append(" $OUTPUTFILE\n");
        script.append("rm -f ").append(tempOutputFileName).append("\n");
        script.append("cp ").append(tempSerializedOutputFilename).append(" $PBRCC_OUTPUTFILE\n");
        script.append("rm -f ").append(tempSerializedOutputFilename).append("\n");
        script.append("cp ").append(tempHitCountFileName).append(" $HITCOUNTFILE\n");
        script.append("rm -f ").append(tempHitCountFileName).append("\n");
        script.append("cp ").append(tempQueryHitCountFileName).append(" $QUERYHITCOUNTFILE\n");
        script.append("rm -f ").append(tempQueryHitCountFileName).append("\n");

        // remove the node local rpsblast query file and blast db dir
        script.append("rm -f ").append(tempQueryFileName).append("\n");
        // script.append("rm -rf " + tempDBName + "\n");

        // script.append("mv $SERIALIZED_OUTPUTFILE $PBRCC_OUTPUTFILE\n");
        writer.write(script.toString());
    }

    private static class ConfigurationFileFilter implements FilenameFilter {
        public ConfigurationFileFilter() {
        }

        public boolean accept(File dir, String name) {
            return name != null && name.startsWith(CONFIG_PREFIX);
        }
    }

}