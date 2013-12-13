
package org.janelia.it.jacs.compute.service.tools;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.SageLoaderResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 26, 2010
 * Time: 3:31:05 PM
 */
public class SageLoaderService extends SubmitDrmaaJobService {

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
            SageLoaderTask sageLoaderTask = (SageLoaderTask)task;
            String perlModulePath = SystemConfigurationProperties.getString("Sage.Perllib");
            String perlBinPath = SystemConfigurationProperties.getString("Perl.Path");
            String cmdPrefix = "export PATH=$PATH:" + perlModulePath + ";export PERL5LIB=$PERL5LIB:" + perlModulePath + ";";

            StringBuilder script = new StringBuilder();
            script.append("whoami").append("\n");
            script.append(cmdPrefix).append("\n");
            script.append(perlBinPath).append(" ");
            script.append(SystemConfigurationProperties.getString("Executables.ModuleBase")).append(SystemConfigurationProperties.getString("Sage.loader.Cmd"));
            script.append(" ").append("-").append(SageLoaderTask.PARAM_ITEM).append(" ").append(task.getParameter(SageLoaderTask.PARAM_ITEM)).
            append(" ").append("-").append(SageLoaderTask.PARAM_CONFIG).append(" ").append(task.getParameter(SageLoaderTask.PARAM_CONFIG)).
            append(" ").append("-").append(SageLoaderTask.PARAM_GRAMMAR).append(" ").append(task.getParameter(SageLoaderTask.PARAM_GRAMMAR)).
            append(" ").append("-").append(SageLoaderTask.PARAM_LAB).append(" ").append(task.getParameter(SageLoaderTask.PARAM_LAB)).
            append(" ").append("-user jacs");

            if (null!=task.getParameter(SageLoaderTask.PARAM_DEBUG)){
                script.append(" ").append("-").append(SageLoaderTask.PARAM_DEBUG);
            }
            if (null!=task.getParameter(SageLoaderTask.PARAM_LOCK) && !"".equals(task.getParameter(SageLoaderTask.PARAM_LOCK))){
                script.append(" ").append("-").append(SageLoaderTask.PARAM_LOCK).append(" ").append(task.getParameter(SageLoaderTask.PARAM_LOCK));
            }
            script.append("\n");
            writer.write(script.toString());
            File configFile = new File(getSGEConfigurationDirectory(), getConfigPrefix()+1);
            FileOutputStream fos = new FileOutputStream(configFile);
            PrintWriter configWriter = new PrintWriter(fos);
            try {
                // Write the full path file name of the file to compress or decompress
                configWriter.println("\n");
            }
            finally {
                configWriter.close();
            }

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        task = ProcessDataHelper.getTask(processData);
        if (computeDAO == null) {computeDAO = new ComputeDAO(logger);}
        resultFileNode = createResultFileNode();
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
    }

    private SageLoaderResultNode createResultFileNode() throws Exception {
        SageLoaderResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof SageLoaderResultNode) {
                return (SageLoaderResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new SageLoaderResultNode(task.getOwner(), task,
                "SageLoaderResultNode", "SageLoaderResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, null);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    @Override
    protected String getNativeSpecificationOverride() {
//        return "-l short=true";
        return "-l archive=true";
    }

    @Override
    protected String getAdditionalNativeSpecification() {
        return "-l archive=true";
    }


}