package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

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
            "-ist 60 -nt 40 -cst 110 -dc 3 -ec 6 -mnr 90\n" +
                    "-ist 50 -nt 35 -cst 90 -dc 3 -ec 5 -mnr 80\n" +
                    "-ist 45 -nt 30 -cst 80 -dc 3 -ec 4 -mnr 70\n" +
                    "-ist 40 -nt 25 -cst 80 -dc 3 -ec 4 -mnr 60\n" +
                    "-ist 30 -nt 25 -cst 80 -dc 3 -ec 4 -mnr 50\n" +
                    "-ist 25 -nt 25 -cst 75 -dc 3 -ec 3 -mnr 50\n" +
                    "-ist 25 -nt 25 -cst 70 -dc 3 -ec 3 -mnr 50\n";


    private File targetFile;
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "cellCounting";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        String tmpPath = (String) processData.getItem("INPUT_FILE");
        targetFile = new File(tmpPath);
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
            writer.append(targetFile.getAbsolutePath());
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
        script.append("read INPUT_FILE\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedCellCounterCommand(planPath));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        writer.write(script.toString());
    }

    @Override
    protected boolean isShortJob() {
    	return true;
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    public void postProcess() throws MissingDataException {
        super.postProcess();
        ArrayList<String> archiveList = new ArrayList<String>();
        archiveList.add(resultFileNode.getDirectoryPath());
        processData.putItem("ARCHIVE_FILE_PATHS", archiveList);
    }
}
