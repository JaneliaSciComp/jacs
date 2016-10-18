
package org.janelia.it.jacs.compute.service.tools;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.utility.BZipTestTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.BzipTestResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 26, 2010
 * Time: 3:31:05 PM
 */
public class BZipTestService extends SubmitDrmaaJobService {

    protected String getGridServicePrefixName() {
        return "bzipTest";
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
        String inputFile = task.getParameter(BZipTestTask.PARAM_SOURCE);
        Scanner scanner = new Scanner(new File(inputFile));
        int counter = 0;
        while (scanner.hasNextLine()) {
            counter++;
            String tmpLine = scanner.nextLine();
            tmpLine = tmpLine.substring(tmpLine.indexOf("/")).trim();
            File configFile = new File(getSGEConfigurationDirectory(), getConfigPrefix()+counter);
            FileOutputStream fos = new FileOutputStream(configFile);
            PrintWriter configWriter = new PrintWriter(fos);
            try {
                // Write the full path file name of the file to compress or decompress
                configWriter.println(tmpLine);
            }
            finally {
                configWriter.close();
            }
        }

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        setJobIncrementStop(counter);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException {
        try {
            BZipTestTask bzipTask = (BZipTestTask)task;
            StringBuilder script = new StringBuilder();
            script.append("read TARGET\n");
            if (BZipTestTask.MODE_COMPRESS.equals(bzipTask.getMode())) {
                script.append("bzip2 -v $TARGET").append("\n");
            }
            else if (BZipTestTask.MODE_DECOMPRESS.equals(bzipTask.getMode())){
                script.append("bzip2 -dv $TARGET").append("\n");
            }

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        task = ProcessDataHelper.getTask(processData);
        if (computeDAO == null) {computeDAO = new ComputeDAO();}
        resultFileNode = createResultFileNode();
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
    }

    private BzipTestResultNode createResultFileNode() throws Exception {
        BzipTestResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof BzipTestResultNode) {
                return (BzipTestResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new BzipTestResultNode(task.getOwner(), task,
                "BzipTestResultNode", "BzipTestResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, null);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

}