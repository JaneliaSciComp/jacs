
package org.janelia.it.jacs.compute.service.hmmer;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.hmmer.HmmpfamTask;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
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
public class HmmPfamSubmitJobService extends SubmitDrmaaJobService {

    private static final String hmmpfamCmd = SystemConfigurationProperties.getString("HmmerPfam.Cmd");
    private static final String resultFilename = SystemConfigurationProperties.getString("HmmerPfam.ResultName");
    private static final String normalQueue = SystemConfigurationProperties.getString("Grid.NormalQueue");
    private static final String lowQueue = SystemConfigurationProperties.getString("Grid.LowQueue");
    private static final Float lowThreshold = SystemConfigurationProperties.getFloat("HmmerPfam.LowThreshold");
    public static final String RESULT_PREFIX = "r";
    private static final String CONFIG_PREFIX = "hmmpfamConfiguration.";

    private float queueScore = 0.0f;

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "hmmpfam";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        HmmpfamTask hmmpfamTask = (HmmpfamTask) task;
        PartitionList partitionList = (PartitionList) processData.getItem(FileServiceConstants.PARTITION_LIST);
        String pfamdbNodeIdString = hmmpfamTask.getParameter(HmmpfamTask.PARAM_pfam_db_node_id);
        List<File> queryFiles = (List<File>) processData.getMandatoryItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
        Map<File, File> queryOutputFileMap = (Map<File, File>) processData.getMandatoryItem(FileServiceConstants.INPUT_OUTPUT_DIR_MAP);

        Long pfamdbNodeId = new Long(pfamdbNodeIdString);
        HmmerPfamDatabaseNode pfamdbNode = (HmmerPfamDatabaseNode) computeDAO.genericLoad(HmmerPfamDatabaseNode.class, pfamdbNodeId);

        Integer totalHmmCount = pfamdbNode.getNumberOfHmms();  // for now we assume only a single pfamHmmDb, which may be partitioned
        if (partitionList.size() == 0) {
            throw new MissingDataException("PartitionList is empty");
        }

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(hmmpfamTask, totalHmmCount, writer);

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
                File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + partitionResultName);
                outputFileList.add(outputFile);
                configIndex = writeConfigFile(queryFile, subjectDatabase, outputFile, configIndex);
                configIndex++;
            }
            inputOutputFileListMap.put(queryFile, outputFileList);
        }
        processData.putItem(FileServiceConstants.INPUT_OUTPUT_FILE_LIST_MAP, inputOutputFileListMap);

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);

        queueScore = configFileCount;
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {

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

        logger.info("HmmPfam Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + queueName);
        jt.setNativeSpecification(queueName);
    }

    private int writeConfigFile(File queryFile, File subjectDatabase, File outputFile, int configIndex) throws IOException {
        // For each partition, we need to create an input file for the script
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileWriter fw = new FileWriter(configFile);
        fw.write(queryFile.getAbsolutePath() + "\n");
        fw.write(subjectDatabase.getAbsolutePath() + "\n");
        fw.write(outputFile.getAbsolutePath() + "\n");
        fw.close();

        return configIndex;
    }

    private String buildConfigFileName(int configIndex) {
        return CONFIG_PREFIX + configIndex;
    }

    private void createShellScript(HmmpfamTask hmmpfamTask, long totalHmmCount, FileWriter writer)
            throws IOException, ParameterException {
        String initialCmd = hmmpfamTask.generateCommandLineOptionString();
        if (totalHmmCount > 0) {
            initialCmd = initialCmd.trim() + " -Z " + totalHmmCount;
        }
        String hmmpfamFullCmd = hmmpfamCmd + " " + initialCmd + " $HMMPFAMDB $QUERYFILE > $OUTPUTFILE";
        StringBuffer script = new StringBuffer();
        script.append("read QUERYFILE\n");
        script.append("read HMMPFAMDB\n");
        script.append("read OUTPUTFILE\n");
        script.append(hmmpfamFullCmd).append("\n");
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
