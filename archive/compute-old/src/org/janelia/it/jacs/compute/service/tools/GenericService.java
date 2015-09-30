
package org.janelia.it.jacs.compute.service.tools;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.TextFileIO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.tools.GenericServiceTask;
import org.janelia.it.jacs.model.user_data.GenericResultNode;
import org.janelia.it.jacs.model.user_data.tools.GenericServiceDefinitionNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 20, 2010
 * Time: 3:11:10 PM
 */
public class GenericService extends SubmitDrmaaJobService {

    private String serviceName;
    private String serviceOptions;
    private GenericServiceDefinitionNode serviceDefinition;
    private String jobName;
    private String userId;
    private String userEmail;
    private String gridOptions;
    private String sessionId = "";
    private String resultDirectory;

    private File configurationDirectory;

    SystemCall sc;

    protected void init(IProcessData processData) throws Exception {

        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            computeDAO = new ComputeDAO(logger);
            this.processData = processData;
            task = ProcessDataHelper.getTask(processData);
            jobSet = new HashSet<String>();

            serviceName = task.getParameter(GenericServiceTask.PARAM_service_name);
            serviceOptions = task.getParameter(GenericServiceTask.PARAM_service_options);
            gridOptions = task.getParameter(GenericServiceTask.PARAM_grid_options);
            if (null == gridOptions || gridOptions.length() == 0) {
                gridOptions = "\"\"";
            }
            serviceDefinition = computeDAO.getGenericServiceDefinitionByName(serviceName);

            jobName = "\"".concat(task.getJobName()).concat("\"");
            userId = task.getOwner();
            userEmail = computeDAO.getUserByNameOrKey(userId).getEmail();
            if (null == userEmail) {
                userEmail = userId.concat("@janelia.hhmi.org");
            }

            if (null != task.getParentTaskId()) {
                sessionId = task.getParentTaskId().toString();
            }

            createResultFileNode(jobName, serviceName, serviceOptions, sessionId);
            resultDirectory = resultFileNode.getDirectoryPath();
            configurationDirectory = new File(resultDirectory + "/config");
            configurationDirectory.mkdir();
            File scratchDirectory = new File(resultDirectory + "/scratch");
            scratchDirectory.mkdir();
//            super.init(processData);
        }
        catch (Exception e) {
            throw new Exception("GenericService init definition failed: ".concat(e.getMessage()));
        }

        try {
            String initializationScript = serviceDefinition.getFilePathByTag("initialization");
            if (new File(initializationScript).exists()) {
                StringBuffer script = new StringBuffer(removeTrailingCrLf(TextFileIO.readTextFile(initializationScript)));

                script.append(" ".concat(jobName));
                script.append(" ".concat(userId));
                script.append(" ".concat(userEmail));
                script.append(" ".concat(resultDirectory));
                if (null != serviceOptions) {
                    script.append(" ".concat(serviceOptions));
                }
                script.append(" > ".concat(resultDirectory).concat("/config/initialization.log"));

                SystemCall system = new SystemCall(logger);
                system.emulateCommandLine(script.toString(), true);

                int partitionCount = new Integer(removeTrailingCrLf(TextFileIO.readTextFile(getConfigurationDirectory().concat("/partition.count"))));
                setJobIncrementStop(partitionCount);
            }
            else {
                setJobIncrementStop(1);
            }
        }
        catch (Exception e) {
            throw new Exception("GenericService init initialization script failed: ".concat(e.getMessage()));
        }
    }


    protected String getSGEConfigurationDirectory() {
        return configurationDirectory.getAbsolutePath();
    }

    protected String getSGEErrorDirectory() {
        return configurationDirectory.getAbsolutePath();
    }

    protected String getSGEOutputDirectory() {
        return resultFileNode.getDirectoryPath() + File.separator + "scratch";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter notUsed) throws Exception {
        try {

            String executionScript = serviceDefinition.getFilePathByTag("execution");
            if (!new File(executionScript).exists()) {
                throw new Exception("GenericService: no execution link");
            }
            StringBuffer script = new StringBuffer(removeTrailingCrLf(TextFileIO.readTextFile(executionScript)));

            script.append(" ".concat(jobName));
            script.append(" ".concat(userId));
            script.append(" ".concat(userEmail));
            script.append(" ".concat(resultDirectory));
            script.append(" $SGE_TASK_ID");
            if (null != serviceOptions) {
                script.append(" ".concat(serviceOptions));
            }
            script.append(" > ".concat(resultDirectory).concat("/config/execution.log.$SGE_TASK_ID\n"));

            File jobScript = new File(getConfigurationDirectory(), getGridServicePrefixName() + "Cmd.sh");
            jobScript.createNewFile();
            jobScript.setWritable(true);
            TextFileIO.writeTextFile(jobScript, script.toString());
            for (int i = 1; i <= getJobIncrementStop(); i++) {
                (new File(getConfigurationDirectory(), getGridServicePrefixName() + "Configuration." + (new Integer(i)).toString())).createNewFile();
            }
        }
        catch (Exception e) {
            throw new MissingDataException("GenericService postProcess execution script failed: ".concat(e.getMessage()));
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            SystemCall system = new SystemCall(logger);

            try {
                system.emulateCommandLine("rm -f " + resultDirectory + "/DrmaaTemplate*.oos", true);
            }
            catch (Exception e) {
                // Do nothing
            }
            try {
                system.emulateCommandLine("rm -f " + resultDirectory + "/DrmaaSubmitter.log", true);
            }
            catch (Exception e) {
                // Do nothing
            }

            String finalizationScript = serviceDefinition.getFilePathByTag("finalization");
            if (new File(finalizationScript).exists()) {
                StringBuffer script = new StringBuffer(removeTrailingCrLf(TextFileIO.readTextFile(finalizationScript)));

                script.append(" ".concat(jobName));
                script.append(" ".concat(userId));
                script.append(" ".concat(userEmail));
                script.append(" ".concat(resultDirectory));
                if (null != serviceOptions) {
                    script.append(" ".concat(serviceOptions));
                }
                script.append(" > ".concat(resultDirectory).concat("/config/finalization.log"));
                system.emulateCommandLine(script.toString(), true);
            }

            try {
                system.emulateCommandLine("rm -rf " + resultDirectory + "/config", true);
            }
            catch (Exception e) {
                // Do nothing
            }
            try {
                system.emulateCommandLine("rm -rf " + resultDirectory + "/scratch", true);
            }
            catch (Exception e) {
                // Do nothing
            }
        }
        catch (Exception e) {
            throw new MissingDataException("GenericService postProcess finalization script failed: ".concat(e.getMessage()));
        }
    }

    protected String getConfigurationDirectory() {
        return configurationDirectory.getAbsolutePath();
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        jt.setNativeSpecification("-l ".concat(gridOptions));
    }

    protected String getGridServicePrefixName() {
        String prefix = serviceName;
        return prefix.replaceAll("\\W", "_");
    }

    private String removeTrailingCrLf(String text) {
        String newtext = text;
        while (null != newtext
                && newtext.length() > 0
                && (newtext.substring(newtext.length() - 1).equals("\n") ||
                newtext.substring(newtext.length() - 1).equals("\r"))) {
            newtext = newtext.substring(0, newtext.length() - 1);
        }
        return newtext;
    }

    private void createResultFileNode(String jobName, String serviceName, String serviceOptions, String sessionId) throws Exception {

        // Create new node
        if (null == resultFileNode) {
            logger.info("Creating GenericResultNode with sessionName=" + sessionId);
            String description = "service: " + serviceName + " options: ";
            if (null != serviceOptions) {
                description = description + serviceOptions;
            }
            resultFileNode = new GenericResultNode();
            resultFileNode = (GenericResultNode) EJBFactory.getLocalComputeBean().createNode(resultFileNode);
//            computeDAO.saveOrUpdate(resultFileNode);

            FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
            FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        }
    }
}
