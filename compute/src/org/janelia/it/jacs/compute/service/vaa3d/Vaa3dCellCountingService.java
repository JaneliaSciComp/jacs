package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Merge neuron fragments.
 * Parameters:
 *
 * @author <a href="mailto:saffordt@janelia.hhmi.org">Todd Safford</a>
 */
public class Vaa3dCellCountingService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "cellCountingConfiguration.";
    public static final String DEFAULT_PLAN =
            "-ist 40 -nt 40 -cst 130 -dc 1 -ec 7 -mnr 90 -mr 13\n" +
            "-ist 38 -nt 35 -cst 115 -dc 2 -ec 6 -mnr 90 -mr 13\n" +
            "-ist 36 -nt 30 -cst 100 -dc 3 -ec 5 -mnr 90 -mr 13\n" +
            "-ist 34 -nt 30 -cst 90 -dc 3 -ec 5 -mnr 90 -mr 13\n" +
            "-ist 32 -nt 30 -cst 80 -dc 3 -ec 5 -mnr 90 -mr 13\n" +
            "-ist 30 -nt 30 -cst 70 -dc 3 -ec 4 -mnr 90 -mr 13\n" +
            "-ist 25 -nt 30 -cst 60 -dc 2 -ec 4 -mnr 90 -mr 13\n" +
            "-ist 20 -nt 30 -cst 55 -dc 2 -ec 3 -mnr 90 -mr 13\n" +
            "-ist 20 -nt 30 -cst 50 -dc 2 -ec 3 -mnr 90 -mr 13\n" +
            "-ist 20 -nt 30 -cst 50 -dc 0 -ec 2 -mnr 90 -mr 13\n\n";

    private String signalChannel, referenceChannel, inputFilePath, convertedFilePath, tifOutputPath, rawOutputPath;

    public void init(IProcessData processData) throws Exception {
        super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "cellCounting";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        signalChannel = (String)processData.getItem("SIGNAL_CHANNELS");
        if (signalChannel==null) {
            throw new IllegalArgumentException("SIGNAL_CHANNELS may not be null");
        }

        referenceChannel = (String)processData.getItem("REFERENCE_CHANNEL");
        if (referenceChannel==null) {
            throw new IllegalArgumentException("REFERENCE_CHANNEL may not be null");
        }
        inputFilePath = (String) processData.getItem("INPUT_FILE");
        File tmpInputFile = new File(inputFilePath);
        String tmpName = tmpInputFile.getName();
        convertedFilePath = resultFileNode.getDirectoryPath()+File.separator+tmpName.substring(0,tmpName.lastIndexOf("."))+".v3dpbd";
        tifOutputPath = resultFileNode.getDirectoryPath()+File.separator+tmpName.substring(0,tmpName.lastIndexOf("."))+"_CellCounterImage.tif";
        rawOutputPath = tifOutputPath.substring(0,tifOutputPath.lastIndexOf("_"))+"-CellCounterImage.v3draw";
        String planPath = resultFileNode.getDirectoryPath()+File.separator+"cellCounterPlan.txt";
        FileWriter planWriter = new FileWriter(new File(planPath));
        try {
            planWriter.append(DEFAULT_PLAN);
        }
        finally {
            planWriter.flush();
            planWriter.close();
        }

        writeInstanceFiles();
        setJobIncrementStop(1);
        createShellScript(writer, planPath);
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+"1");
        FileWriter writer = new FileWriter(configFile);
        try {
            writer.append("\n");
        }
        finally {
            writer.flush();
            writer.close();
        }
    }

    /**
     * Write the shell script that runs the stitcher on the merged files.
     */
    private void createShellScript(FileWriter writer, String planPath) throws Exception {
        StringBuilder script = new StringBuilder();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix()).append("\n");
        script.append(Vaa3DHelper.getFormattedConvertCommand(inputFilePath, convertedFilePath, "8")).append("\n");
        script.append(Vaa3DHelper.getFormattedCellCounterCommand(planPath, convertedFilePath, signalChannel, referenceChannel)).append("\n");
        script.append(Vaa3DHelper.getFormattedConvertCommand(tifOutputPath, rawOutputPath)).append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        writer.write(script.toString());
    }

    @Override
    protected boolean isShortJob() {
    	return true;
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 12;
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    public void postProcess() throws MissingDataException {
        super.postProcess();
        ArrayList<String> archiveList = new ArrayList<String>();
        if (null!=resultFileNode && null!=resultFileNode.getDirectoryPath()) {
            archiveList.add(resultFileNode.getDirectoryPath());
        }
        processData.putItem("ARCHIVE_FILE_PATHS", archiveList);

        //Look for the resultant stacks
        File tmpDir = new File(resultFileNode.getDirectoryPath());
        List<String> mipPathList = new ArrayList<String>();
        for (File file : tmpDir.listFiles()) {
            if (file.getName().endsWith(".v3dpbd")||file.getName().endsWith(".v3draw")) {
                mipPathList.add(file.getAbsolutePath());
            }
        }
        processData.putItem("CELL_COUNTING_MIP_FILES", mipPathList);

        processData.putItem("RAW_RESULT_FILE", rawOutputPath);
    }
}
