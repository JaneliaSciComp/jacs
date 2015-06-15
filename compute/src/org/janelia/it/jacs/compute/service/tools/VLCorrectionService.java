
package org.janelia.it.jacs.compute.service.tools;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.tasks.utility.VLCorrectionTask;
import org.janelia.it.jacs.model.user_data.GenericResultNode;
import org.janelia.it.jacs.model.user_data.Node;
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
public class VLCorrectionService extends SubmitDrmaaJobService {

    protected String getGridServicePrefixName() {
        return "vlCorrectionTest";
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
        String inputFile = task.getParameter(VLCorrectionTask.PARAM_SOURCE);
        String targetOwner = task.getParameter(VLCorrectionTask.PARAM_TARGET_OWNER);
        Scanner scanner = new Scanner(new File(inputFile));
        int counter = 0;
        while (scanner.hasNextLine()) {
            String tmpLine = scanner.nextLine();
            if (tmpLine.contains(targetOwner)) {
                String originalPDB = tmpLine.substring(0,tmpLine.lastIndexOf(".h5j"))+".v3dpbd";
                File tmpOriginalPBD = new File(originalPDB);
                File tmpOriginalVL = new File(tmpLine);
                boolean foundFiles = true;
                if (!tmpOriginalVL.exists()) {
                    System.out.println("Can't find the original VL file: "+tmpLine);
                    foundFiles = false;
                }
                if (!tmpOriginalPBD.exists()) {
                    System.out.println("Can't find the original PBD file: "+originalPDB);
                    foundFiles = false;
                }

                if (foundFiles) {
                    counter++;
                    tmpLine = tmpLine.trim();
                    File configFile = new File(getSGEConfigurationDirectory(), getConfigPrefix()+counter);
                    FileOutputStream fos = new FileOutputStream(configFile);
                    try (PrintWriter configWriter = new PrintWriter(fos)) {
                        // Write the full path file name of the original PBD
                        configWriter.println(tmpOriginalPBD);
                        // Write the full path file name of the original H5J
                        configWriter.println(tmpLine);
                    }
                }
            }
        }

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        setJobIncrementStop(counter);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuilder script = new StringBuilder();
            script.append("read PBD_FILENAME\n");
            script.append("read H5J_FILENAME\n");
            script.append("rm -rf $H5J_FILENAME\n");
            script.append(Vaa3DHelper.getVaa3dHeadlessGridCommandPrefix());
            script.append("\n");
            script.append(Vaa3DHelper.getFormattedConvertScriptCommand("$PBD_FILENAME", "$H5J_FILENAME", "")).append("\n");
            script.append("\n");
            script.append(Vaa3DHelper.getHeadlessGridCommandSuffix());
            writer.write(script.toString());
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

    private GenericResultNode createResultFileNode() throws Exception {
        GenericResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof GenericResultNode) {
                return (GenericResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new GenericResultNode(task.getOwner(), task,
                "VLCorrectionResultNode", "VLCorrectionResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, null);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

}