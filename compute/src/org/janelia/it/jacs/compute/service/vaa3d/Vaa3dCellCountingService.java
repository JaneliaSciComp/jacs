package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.cellCounting.CellCountingTask;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;

/**
 * Merge neuron fragments.
 * Parameters:
 *
 * @author <a href="mailto:saffordt@janelia.hhmi.org">Todd Safford</a>
 */
public class Vaa3dCellCountingService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "cellCountingConfiguration.";
    private File[]targetFiles;
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "celCounting";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        String tmpPath = task.getParameter(CellCountingTask.PARAM_inputFilePath);
        int fileCount=1;
        File tmpPathFile = new File(tmpPath);
        targetFiles = new File[]{tmpPathFile};
        // Should probably add logic to the filter to ensure proper image files are discovered and passed through
        if (tmpPathFile.isDirectory()) {
            targetFiles = tmpPathFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return !file.isDirectory();
                }
            });
            fileCount = (null!=targetFiles)?targetFiles.length:0;
        }

        String planPath = resultFileNode.getDirectoryPath()+File.separator+"cellCounterPlan.txt";
        FileWriter planWriter = new FileWriter(new File(planPath));
        try {
            planWriter.append(task.getParameter(CellCountingTask.PARAM_planInformation));
        }
        finally {
            planWriter.flush();
            planWriter.close();
        }

        writeInstanceFiles();
        setJobIncrementStop(fileCount);
        createShellScript(writer, planPath);
    }

    private void writeInstanceFiles() throws Exception {
        int count = 1;
        for (File targetFile : targetFiles) {
            File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+count);
            FileWriter writer = new FileWriter(configFile);
            try {
                writer.append(targetFile.getAbsolutePath());
                count++;
            }
            finally {
                writer.flush();
                writer.close();
            }
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

}
