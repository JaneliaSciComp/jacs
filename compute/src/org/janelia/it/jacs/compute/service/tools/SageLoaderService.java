package org.janelia.it.jacs.compute.service.tools;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.SageLoaderResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SageLoaderService extends SubmitDrmaaJobService {

    private SageLoaderTask sageLoaderTask;

    protected String getGridServicePrefixName() {
        return "sageLoader";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        createShellScript(writer);
        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        setJobIncrementStop(1);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException {
        try {
            final String perlModulePath = SystemConfigurationProperties.getString("Sage.Perllib");
            final String perlBinPath = SystemConfigurationProperties.getString("Perl.Path");
            final String cmdPrefix =
                    "export PATH=$PATH:" + perlModulePath + ";export PERL5LIB=$PERL5LIB:" + perlModulePath + ";";

            StringBuilder script = new StringBuilder();
            script.append("whoami").append("\n");
            script.append(cmdPrefix).append("\n");
            script.append(perlBinPath).append(" ");
            script.append(SystemConfigurationProperties.getString("Executables.ModuleBase"));
            script.append(SystemConfigurationProperties.getString("Sage.loader.Cmd"));

            final String sageWriteEnvironment = SystemConfigurationProperties.getString("Sage.write.environment");
            if ("production".equals(sageWriteEnvironment)) {
                logger.info("createShellScript: running against production SAGE environment");
            } else {
                logger.info("createShellScript: running against development SAGE environment");
                script.append(" -development");
            }

            script.append(" -user jacs");
            final File dataFile = createInputDataFile();

            script.append(" -file ").append(dataFile.getAbsolutePath());
            appendTaskParameters(script);
            script.append("\n");

            writer.write(script.toString());

            final File configFile = new File(getSGEConfigurationDirectory(), getConfigPrefix()+1);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(configFile);
                final PrintWriter configWriter = new PrintWriter(fos);
                configWriter.println("\n");
            } finally {
                close(configFile.getAbsolutePath(), fos);
            }

        } catch (Exception e) {
            logger.error("failed to create shell script", e);
            throw new IOException(e);
        }
    }

    private File createInputDataFile() throws IOException {
        final File dataFile = new File(getSGEConfigurationDirectory(), "sampleFileList.txt");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(dataFile))) {
            for (String fileName : sageLoaderTask.getItemList()) {
                pw.println(fileName);
            }
        }
        return dataFile;
    }

    private void appendTaskParameters(StringBuilder script) {
        String value;
        for (String name : sageLoaderTask.getScriptArgumentNames()) {
            value = sageLoaderTask.getParameter(name);
            if (! StringUtils.isEmpty(value)) {
                script.append(" -").append(name).append(' ');
                if (value.contains(" ")) {
                    script.append(" \"").append(value).append("\"");
                } else {
                    script.append(value);
                }
            }
        }
        for (String name : sageLoaderTask.getScriptFlagNames()) {
            if (sageLoaderTask.hasParameter(name)) {
                script.append(" -").append(name);
            }
        }
    }

    private void close(String name,
                       Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logger.warn("failed to close " + name, e);
            }
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        task = ProcessDataHelper.getTask(processData);
        sageLoaderTask = (SageLoaderTask) processData.getItem("SAGE_TASK");
        if (sageLoaderTask == null) {
            sageLoaderTask = (SageLoaderTask) task;
        }
        if (computeDAO == null) {computeDAO = new ComputeDAO(logger);}
        resultFileNode = createResultFileNode();
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
    }

    private SageLoaderResultNode createResultFileNode() throws Exception {
        SageLoaderResultNode resultFileNode;

        // Check if we already have a result node for this sageLoaderTask
        if (sageLoaderTask == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + sageLoaderTask.getObjectId());
        Set<Node> outputNodes = sageLoaderTask.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof SageLoaderResultNode) {
                return (SageLoaderResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new SageLoaderResultNode(sageLoaderTask.getOwner(), sageLoaderTask,
                "SageLoaderResultNode", "SageLoaderResultNode for task " + sageLoaderTask.getObjectId(),
                Node.VISIBILITY_PRIVATE, null);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    @Override
    public void postProcess() throws MissingDataException {
        try {
            // reload task from database to avoid Hibernate errors on save
            sageLoaderTask = (SageLoaderTask) computeDAO.getTaskById(sageLoaderTask.getObjectId());
            if (sageLoaderTask.getParentTaskId() != null) {
                task = computeDAO.getTaskById(sageLoaderTask.getParentTaskId());
            } else {
                task = sageLoaderTask;
            }

            final File outputFile = getFile(getSGEOutputDirectory(), "Output");
            final File errorFile = getFile(getSGEErrorDirectory(), "Error");
            addFileMessage("Output", outputFile);
            addFileMessage("Error", errorFile);

            // if the item being loaded is an lsm,
            // verify that the sageLoader script actually found the image
            int expectedImages = 0;
            for (String inputFile : sageLoaderTask.getItemList()) {
                if (inputFile.endsWith(".lsm")) {
                    expectedImages++;
                }
            }
            if (expectedImages > 0) {
                checkImagesFound(outputFile, expectedImages);
            }

            computeDAO.saveOrUpdate(sageLoaderTask);
            if (sageLoaderTask != task) {
                computeDAO.saveOrUpdate(task);
            }
            logger.info("postProcess: saved output messages for " + sageLoaderTask);
        } catch (DaoException e) {
            logger.error("failed to save output messages for " + sageLoaderTask, e);
        }
    }

    private void addFileMessage(String context, File file) {
        if (file.exists()) {
            final long size = file.length();
            if (size > 0) {
                sageLoaderTask.addMessage(context + " file: " + file.getAbsolutePath());
            }
        }
    }

    private File getFile(String directoryName, String name) {
        final File directory = new File(directoryName);
        return new File(directory, getGridServicePrefixName() + name + ".1");
    }

    /**
     * The sageLoader script was originally designed to run in batch mode,
     * so an invalid item name does not explicitly create an error.
     * This method checks that an image was in fact found for the specified item
     * and adds an error event to this task if an image was not found.
     *
     * @param  sageLoaderStdOutFile  standard output from sageLoader script.
     * @param expectedImages - number of expected images.
     */
    private void checkImagesFound(File sageLoaderStdOutFile, int expectedImages) {

        int imagesFound = 0;

        if (sageLoaderStdOutFile.exists()) {
            final long size = sageLoaderStdOutFile.length();
            if (size > 0) {
                imagesFound = getImagesFound(sageLoaderStdOutFile);
            }
        }

        logger.info("Task " + sageLoaderTask.getObjectId() + " expected/Processed images: " +
                expectedImages + "/" + imagesFound);
        if (imagesFound != expectedImages) {
            final Event lastEvent = sageLoaderTask.getLastEvent();
            final String errorMessage = "Task " + sageLoaderTask.getObjectId() +
                    " not all Images Found in " + sageLoaderStdOutFile.getAbsolutePath();
            if (! Event.ERROR_EVENT.equals(lastEvent.getEventType())) {
                logger.warn("checkImagesFound: " + errorMessage);
                final Event invalidOutput = new Event(errorMessage, new Date(), Event.ERROR_EVENT);
                sageLoaderTask.addEvent(invalidOutput);
            }
            if (task != sageLoaderTask) {
                task.addEvent(new Event(errorMessage, new Date(), Event.SUBTASKERROR_EVENT));
            }
        } else {
            if (task != sageLoaderTask) {
                final Event processedOutput = new Event("Processed " + imagesFound + " images", new Date(), Event.SUBTASKCOMPLETED_EVENT);
                task.addEvent(processedOutput);
            }
        }
    }

    private int getImagesFound(File file) {
        int imagesFound = 0;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String currentLine;
            Matcher m;
            while ((currentLine = br.readLine()) != null) {
                m = IMAGES_FOUND.matcher(currentLine);
                if (m.matches()) {
                    if (m.groupCount() == 1) {
                        imagesFound = Integer.parseInt(m.group(1));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("failed to parse " + file.getAbsolutePath(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.error("failed to close " + file.getAbsolutePath(), e);
                }
            }
        }

        logger.info("getImagesFound: returning " + imagesFound + " for " + file.getAbsolutePath());

        return imagesFound;
    }

    private static final Pattern IMAGES_FOUND = Pattern.compile("Images found:(?:\\W)*([\\d])*");

    /**
     * This service must run immediately on the dedicated nodes
     */
    @Override
    protected boolean isImmediateProcessingJob() {
        return true;
    }
}