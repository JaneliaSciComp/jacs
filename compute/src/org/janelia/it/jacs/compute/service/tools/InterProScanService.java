
package org.janelia.it.jacs.compute.service.tools;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.SimpleMultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tools.InterProScanTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.InterProScanResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: May 180, 2010
 * Time: 2:31:05 PM
 */
public class InterProScanService extends SubmitDrmaaJobService {

    public static final String INTERPROSCAN_CMD = SystemConfigurationProperties.getString("InterProScan.InterProScanCmd");
    public static final String INTERPROSCAN_QUEUE = SystemConfigurationProperties.getString("InterProScan.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("InterProScan.MaxEntriesPerJob");

    private static final String resultFilename = InterProScanResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    InterProScanTask interproscanTask;
    InterProScanResultNode interproscanResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "interproscan";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(interproscanTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        outputFiles = new ArrayList<File>();
        for (File inputFile : inputFiles) {
            File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + "." + configIndex);
            outputFiles.add(outputFile);
            configIndex = writeConfigFile(inputFile, outputFile, configIndex);
            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(File inputFile, File outputFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getAbsolutePath());
            configWriter.println(outputFile.getAbsolutePath());
        }
        finally {
            configWriter.close();
        }
        return configIndex;
    }

    private class ConfigurationFileFilter implements FilenameFilter {
        public ConfigurationFileFilter() {
        }

        public boolean accept(File dir, String name) {
            return name != null && name.startsWith(getConfigPrefix());
        }
    }

    private String buildConfigFileName(int configIndex) {
        return getConfigPrefix() + configIndex;
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("setQueue = " + INTERPROSCAN_QUEUE);
        jt.setNativeSpecification(INTERPROSCAN_QUEUE);
    }

    private void createShellScript(InterProScanTask interproscanTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String optionString = interproscanTask.generateCommandOptions();

            String interproscanStr = INTERPROSCAN_CMD + " -cli -i " + inputFileString + optionString +
                    " -o " + outputFileString;
            script.append(interproscanStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File interproscanResultFile = new File(interproscanResultNode.getFilePathByTag(InterProScanResultNode.TAG_INTERPROSCAN_OUTPUT));
            File interproscanDir = new File(interproscanResultNode.getDirectoryPath());
            List<File> filteredFiles = new ArrayList<File>();
            logger.info("postProcess for interproscanTaskId=" + interproscanTask.getObjectId() + " and resultNodeDir=" + interproscanDir.getAbsolutePath());
            int index = 0;
            for (File f : outputFiles) {
                if (!f.exists()) {
                    throw new Exception("Could not locate expected output file=" + f.getAbsolutePath());
                }
                if (index == 0) {
                    filteredFiles.add(f);
                }
                else {
                    File filteredFile = new File(f.getAbsolutePath() + ".noheader");
                    createFilteredFile(f, filteredFile, "#");
                    filteredFiles.add(filteredFile);
                }
                index++;
            }
            FileUtil.concatFilesUsingSystemCall(filteredFiles, interproscanResultFile);
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    public void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        interproscanTask = getInterProScanTask(processData);
        task = interproscanTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Priam call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for InterProScanTask=" + interproscanTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        interproscanResultNode = createResultFileNode();
        logger.info("Setting interproscanResultNode=" + interproscanResultNode.getObjectId() + " path=" + interproscanResultNode.getDirectoryPath());
        resultFileNode = interproscanResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        Long inputFastaNodeId = new Long(interproscanTask.getParameter(InterProScanTask.PARAM_fasta_input_node_id));
        FastaFileNode inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
        if (inputFastaNode != null) {
            logger.info("Using inputNode=" + inputFastaNode.getObjectId() + " path=" + inputFastaNode.getDirectoryPath());
        }
        else {
            String errorMessage = "Unexpectedly received null inputFastaNode using inputFastaNodeId=" + inputFastaNodeId;
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
        File inputFile = new File(inputFastaNode.getFastaFilePath());
        inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, new File(inputFastaNode.getFastaFilePath()),
                DEFAULT_ENTRIES_PER_EXEC, logger);
        for (File file : inputFiles) {
            logger.info("InterProScan taskId=" + interproscanTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " interproscanTaskId=" + task.getObjectId() + " init() end");
    }

    private InterProScanTask getInterProScanTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null InterProScanTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (InterProScanTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getInterProScanTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private InterProScanResultNode createResultFileNode() throws Exception {
        InterProScanResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof InterProScanResultNode) {
                logger.info("Found already-extant interproscanResultNode path=" + ((InterProScanResultNode) node).getDirectoryPath());
                return (InterProScanResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating InterProScanResultNode with sessionName=" + sessionName);
        resultFileNode = new InterProScanResultNode(task.getOwner(), task,
                "InterProScanResultNode", "InterProScanResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private void createFilteredFile(File source, File target, String filter) throws Exception {
        if (sc == null) {
            sc = new SystemCall(logger);
        }
        String cmd = "cat " + source.getAbsolutePath() + " | grep -v \"" + filter + "\" > " + target.getAbsolutePath();
        int ev = sc.emulateCommandLine(cmd, true);
        if (ev != 0) {
            throw new Exception("System cmd returned non-zero exit state=" + cmd);
        }
    }

}