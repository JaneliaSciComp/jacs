
package org.janelia.it.jacs.compute.service.tools;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.utility.ScalityMigrationTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.ScalityMigrationResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import scala.collection.mutable.StringBuilder;

import java.io.*;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 26, 2010
 * Time: 3:31:05 PM
 */
public class ScalityMigrationService extends SubmitDrmaaJobService {

    public static final int NUM_INPUT_FILES=10;
    protected String getGridServicePrefixName() {
        return "scalityMigration";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }



//    /**
//     * Method which defines the general job script and node configurations
//     * which ultimately get executed by the grid nodes
//     */
//    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
//        createShellScript(writer);
//        String inputFile = task.getParameter(ScalityMigrationTask.PARAM_SOURCE);
//        Scanner scanner = new Scanner(new File(inputFile));
//        int counter = 0;
//        while (scanner.hasNextLine()) {
//            counter++;
//            File configFile = new File(getSGEConfigurationDirectory(), getConfigPrefix()+counter);
//            FileOutputStream fos = new FileOutputStream(configFile);
//            try (PrintWriter configWriter = new PrintWriter(fos)) {
//                String counterdir = resultFileNode.getDirectoryPath() + File.separator + "tmpconfig" + counter;
//                configWriter.println(counterdir);
//                // Write the full path file name of the file to migrate
//                for (int x = 0; x < NUM_INPUT_FILES; x++) {
//                    if (scanner.hasNextLine()) {
//                        String tmpLine = scanner.nextLine().trim();
//                        configWriter.println(tmpLine);
//                        configWriter.println(counterdir + File.separator + "temp" + x);
//                    }
//                }
//            }
//        }
//
//        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
//        setJobIncrementStop(counter);
//    }
//
//    private void createShellScript(FileWriter writer)
//            throws IOException, ParameterException {
//        try {
//            StringBuilder script = new StringBuilder();
//            script.append("read COUNTERDIR").append("\n");
//            script.append("mkdir $COUNTERDIR").append("\n");
//            for (int i = 0; i < NUM_INPUT_FILES; i++) {
//                addTarget(script, i);
//            }
//            script.append("sleep 120").append("\n");
//            script.append("rm -rf $COUNTERDIR").append("\n");
//            writer.write(script.toString());
//        }
//        catch (Exception e) {
//            logger.error(e, e);
//            throw new IOException(e);
//        }
//    }
//
//    private void addTarget(StringBuilder script, int index) {
//        script.append("read TARGET").append(index).append("\n");
//        script.append("read TEMPFILE").append(index).append("\n");
//        script.append("/misc/local/python-2.7.8/bin/python /groups/jacs/jacsHosts/servers/jacs-data3/executables/scality/restSync.py GET R $TARGET").append(index).append(" $TEMPFILE").append(index).append(" http://schauderd-ws1:8880\n");
//        script.append("/misc/local/python-2.7.8/bin/python /groups/jacs/jacsHosts/servers/jacs-data3/executables/scality/restSync.py PUT NR $TEMPFILE").append(index).append(" $TARGET").append(index).append(" http://schauderd-ws1:8880\n");
////        script.append("rm $TEMPFILE").append(index).append("\n");
//    }
//
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
        return "-l scalityr=1";
    }

    private ScalityMigrationResultNode createResultFileNode() throws Exception {
        ScalityMigrationResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof ScalityMigrationResultNode) {
                return (ScalityMigrationResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new ScalityMigrationResultNode(task.getOwner(), task,
                "ScalityMigrationResultNode", "ScalityMigrationResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, null);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }


//    public static void main(String[] args) {
//        String errorBase = "/nrs/jacs/jacsData/filestore/system/ScalityMigrationResult/177/674/2196450647841177674/sge_error/scalityMigrationError.";
//        String outputBase = "/nrs/jacs/jacsData/filestore/system/ScalityMigrationResult/177/674/2196450647841177674/sge_output/scalityMigrationOutput.";
//
//        // Error Appending
//        try (FileWriter writer = new FileWriter("/nrs/jacs/jacsData/filestore/system/ScalityMigrationResult/177/674/2196450647841177674/errors.log")){
//            for (int i = 0; i < 26415; i++) {
//                File tmpFile = new File(errorBase+i);
//                if (tmpFile.exists()) {
//                    try (Scanner scanner = new Scanner(tmpFile)) {
//                        while (scanner.hasNextLine()) {
//                            writer.append(scanner.nextLine()).append("\n");
//                        }
//                    }
//                    catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//                else {
//                    writer.append("ERROR - Cannot find file ").append(tmpFile.getAbsolutePath()).append("\n");
//                }
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//        // Output Appending
//        try (FileWriter writer = new FileWriter("/nrs/jacs/jacsData/filestore/system/ScalityMigrationResult/177/674/2196450647841177674/output.log")){
//            for (int i = 0; i < 26415; i++) {
//                File tmpFile = new File(outputBase+i);
//                if (tmpFile.exists()) {
//                    try (Scanner scanner = new Scanner(tmpFile)) {
//                        while (scanner.hasNextLine()) {
//                            writer.append(scanner.nextLine()).append("\n");
//                        }
//                    }
//                    catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//                else {
//                    writer.append("ERROR - Cannot find file ").append(tmpFile.getAbsolutePath()).append("\n");
//                }
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
// Validation methods
    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        createShellScript(writer);
        String inputFile = task.getParameter(ScalityMigrationTask.PARAM_SOURCE);
        Scanner scanner = new Scanner(new File(inputFile));
        int counter = 0;
        while (scanner.hasNextLine()) {
            counter++;
            File configFile = new File(getSGEConfigurationDirectory(), getConfigPrefix()+counter);
            FileOutputStream fos = new FileOutputStream(configFile);
            try (PrintWriter configWriter = new PrintWriter(fos)) {
//                String counterdir = resultFileNode.getDirectoryPath() + File.separator + "tmpconfig" + counter;
//                configWriter.println(counterdir);
                // Write the full path file name of the file to migrate
                for (int x = 0; x < NUM_INPUT_FILES; x++) {
                    if (scanner.hasNextLine()) {
                        String tmpLine = scanner.nextLine().trim();
                        configWriter.println(tmpLine);
//                        configWriter.println(counterdir + File.separator + "temp" + x);
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
//            script.append("read COUNTERDIR").append("\n");
//            script.append("mkdir $COUNTERDIR").append("\n");

            for (int i = 0; i < NUM_INPUT_FILES; i++) {
                addTarget(script, i);
            }
//            script.append("sleep 120").append("\n");
//            script.append("rm -rf $COUNTERDIR").append("\n");
            script.append("/misc/local/jdk1.7.0_67/bin/java -Xmx6000m -jar /groups/jacs/jacsHosts/servers/jacs-data3/executables/scality/validatescality-1.0.jar -shttp://localhost:81/proxy/bparc2 -hmongodb2 -ujosApp -p0hmd3n0s@urusW -dbjos -cworkstation_lsms -f").
                    append("$TARGET0,$TARGET1,$TARGET2,$TARGET3,$TARGET4,$TARGET5,$TARGET6,$TARGET7,$TARGET8,$TARGET9\n");
            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    private void addTarget(StringBuilder script, int index) {
        script.append("read TARGET").append(index).append("\n");
//        script.append("read TEMPFILE").append(index).append("\n");
//        script.append("curl http://localhost:81/proxy/bparc2/$TARGET").append(index).append(" > $TEMPFILE").append(index).append("\n");
//        script.append("curl -X PUT -H \"Content-Type: application/octet-stream\" --data-binary @$TEMPFILE")
//                .append(index).append(" \"http://localhost:81/proxy/bparc/$TARGET").append(index).append("\"\n");
//        script.append("rm $TEMPFILE").append(index).append("\n");
    }
}