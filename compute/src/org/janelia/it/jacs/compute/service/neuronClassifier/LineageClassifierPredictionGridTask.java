package org.janelia.it.jacs.compute.service.neuronClassifier;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparatorHelper;

/**
 * Run Lineage Classification Prediction.
 *
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class LineageClassifierPredictionGridTask extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 7200;  // 2 hours
    private String metadata;

    @Override
    protected String getGridServicePrefixName() {
        return "lineageClassifier";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);

        metadata = processData.getString("METADATA");
        if (metadata == null) {
            throw new IllegalArgumentException("The following may not be null: METADATA");
        }
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath() + File.separator);
        File dataFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName() + "Data." + configIndex);
        String dataFilePath = dataFile.getAbsolutePath();
        FileWriter dfw = new FileWriter(dataFile);
        dfw.write(metadata);
        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName() + "Configuration." + configIndex);
        FileWriter fw = new FileWriter(configFile);
        fw.write(dataFilePath + "\n");
        fw.write(outputDir.getAbsolutePath());
        dfw.close();
        fw.close();

        writeShellScript(writer);
        setJobIncrementStop(configIndex - 1);
    }


    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }


    private void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read CONFIG_FILE\n");
        script.append("read OUTPUT_DIR\n");
        script.append(NeuronClassifierHelper.getTrainingCommands(1));
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 8;
    }

    @Override
    protected int getRequiredSlots() {
        return 1;
    }
}