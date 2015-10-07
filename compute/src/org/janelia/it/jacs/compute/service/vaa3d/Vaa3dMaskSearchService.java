package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.maskSearch.MaskSearchTask;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * Merge neuron fragments.
 * Parameters:
 *
 * @author <a href="mailto:saffordt@janelia.hhmi.org">Todd Safford</a>
 */
public class Vaa3dMaskSearchService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "maskSearchConfiguration.";

    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "maskSearch";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeInstanceFiles();
        setJobIncrementStop(1);
        createShellScript(writer);
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+"1");
        FileWriter writer = new FileWriter(configFile);
        try {
            writer.append(task.getParameter(MaskSearchTask.PARAM_inputFilePath)).append("\n");
        }
        finally {
            writer.flush();
            writer.close();
        }
    }

    /**
     * Write the shell script that runs the stitcher on the merged files.
     */
    private void createShellScript(FileWriter writer) throws Exception {
        StringBuilder script = new StringBuilder();
        script.append("read INPUT_FILE\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedMaskSearchCommand(SystemConfigurationProperties.getString("FileStore.CentralDir")+
                SystemConfigurationProperties.getString("MaskSearch.Index.fly_whole_20x"),
                task.getParameter(MaskSearchTask.PARAM_queryChannel),
                task.getParameter(MaskSearchTask.PARAM_matrix),
                task.getParameter(MaskSearchTask.PARAM_maxHits),
                task.getParameter(MaskSearchTask.PARAM_skipZeroes),
                resultFileNode.getDirectoryPath()+File.separator+"searchResults.txt"));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        writer.write(script.toString());
    }

    @Override
    protected boolean isImmediateProcessingJob() {
        return true;
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    public void postProcess() throws MissingDataException {
        super.postProcess();
        ArrayList<String> archiveList = new ArrayList<>();
        archiveList.add(resultFileNode.getDirectoryPath());
        processData.putItem("ARCHIVE_FILE_PATHS", archiveList);

        File tmpFile = new File(task.getParameter(MaskSearchTask.PARAM_inputFilePath));
        String outputPath=resultFileNode.getDirectoryPath()+File.separator+"mipArtifact_"+
                tmpFile.getName().substring(0, tmpFile.getName().lastIndexOf("."))+".tif";

        ArrayList<String> mipInputList = new ArrayList<>();
        mipInputList.add(tmpFile.getAbsolutePath());
        processData.putItem("MIP_INPUT_LIST", mipInputList);

        ArrayList<String> mipOutputList = new ArrayList<>();
        mipOutputList.add(outputPath);
        processData.putItem("MIP_OUTPUT_LIST", mipOutputList);
    }
}
