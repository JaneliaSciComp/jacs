
package org.janelia.it.jacs.compute.service.tools;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.utility.JFSExportTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.JFSExportResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import scala.collection.mutable.StringBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 26, 2010
 * Time: 3:31:05 PM
 */
public class JFSExportService extends SubmitDrmaaJobService {

    public static final int NUM_INPUT_FILES=10;
    protected String getGridServicePrefixName() {
        return "jfsExport";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        task = ProcessDataHelper.getTask(processData);
        if (computeDAO == null) {computeDAO = new ComputeDAO(logger);}
        resultFileNode = createResultFileNode();
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
    }

    @Override
    protected boolean isShortPipelineJob() {
        return true;
    }

    @Override
    protected int getRequiredSlots() {
        return 8;
    }

    @Override
    protected String getAdditionalNativeSpecification() {
        return "-l scalityr=1 -l sandy=true";
    }

    private JFSExportResultNode createResultFileNode() throws Exception {
        JFSExportResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof JFSExportResultNode) {
                return (JFSExportResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new JFSExportResultNode(task.getOwner(), task,
                "JFSExportResultNode", "JFSExportResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, null);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        createShellScript(writer);
        String inputFile = task.getParameter(JFSExportTask.PARAM_SOURCE);
        Scanner scanner = new Scanner(new File(inputFile));
        int counter = 0;
        while (scanner.hasNextLine()) {
            counter++;
            File configFile = new File(getSGEConfigurationDirectory(), getConfigPrefix()+counter);
            FileOutputStream fos = new FileOutputStream(configFile);
            try (PrintWriter configWriter = new PrintWriter(fos)) {
                // Write the full command line of the file to export
                for (int x = 0; x < NUM_INPUT_FILES; x++) {
                    if (scanner.hasNextLine()) {
                        String tmpLine = scanner.nextLine().trim();
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

            for (int i = 0; i < NUM_INPUT_FILES; i++) {
                addTarget(script, i);
            }
            for (int i = 0; i < NUM_INPUT_FILES; i++) {
                runTarget(script,i);
            }
            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    private void addTarget(StringBuilder script, int index) {
        script.append("read TARGET").append(index).append("\n");
    }
    private void runTarget(StringBuilder script, int index) {
        script.append("$TARGET").append(index).append("\n");
    }

//    public static void main(String[] args) {
//        try {
//            FileWriter writer = new FileWriter(new File("/groups/jacs/jacsDev/saffordt/symlink__2240583447523360913.sh"));
//            String basename = "/nrs/jacs/jacsData/saffordt/download/drive/20160219_VND_for_Myers_group/";
//            Scanner scanner = new Scanner(new File("/groups/jacs/jacsDev/saffordt/export_2240583447523360913.sh.nrs.txt"));
//            HashMap<String,String> sampleMap = new HashMap<>();
//            int totalFileCount = 0;
//            int skipCount=0;
//            while (scanner.hasNextLine()) {
//                String tmpLine = scanner.nextLine();
//                if (tmpLine.contains("jfs")) {
//                    totalFileCount++;
//                    String tmpFileKey = tmpLine.substring(tmpLine.lastIndexOf(basename)+basename.length()).trim();
//                    String tmpSample = tmpFileKey.substring(tmpFileKey.indexOf("/")+1);
//                    if (sampleMap.containsKey(tmpSample)) {
//                        int backticks = tmpFileKey.split("/").length-1;
//                        String formattedbackticks = "";
//                        for (int i = 0; i < backticks; i++) {
//                            formattedbackticks+="../";
//                        }
//                        if (!sampleMap.get(tmpSample).contains(tmpFileKey)) {
//                            String tmpSymlink = "ln -sf "+formattedbackticks+sampleMap.get(tmpSample)+" "+basename+tmpFileKey;
//                            System.out.println("Already have sample "+tmpSample+". Making a symlink ("+tmpSymlink+")");
//                            // Now write the symlink to a file for processing...
//                            writer.write(tmpSymlink+"\n");
//                        }
//                        else {
//                            System.out.println("Cannot have a link reference itself: "+sampleMap.get(tmpSample)+":"+tmpFileKey);
//                            skipCount++;
//                        }
//                    }
//                    else {
//                        sampleMap.put(tmpSample, tmpFileKey);
//                    }
//                }
//            }
//            writer.close();
//            scanner.close();
//            System.out.println("\nThere are "+totalFileCount+" files.");
//            System.out.println("There are "+sampleMap.keySet().size()+" unique files.");
//            System.out.println("Prevented "+skipCount+" references to themselves.");
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }


}