package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.align.ImageStack;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;

/**
 * Generate Neuron Fragment Weights for a neuron separation.
 *
 * Writes the fragment calculations out to a common folder
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronFragmentsWeightGridTask extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 7200;  // 2 hours
    private static final int BATCH_SEPARATIONS = 1000;
    private List<String> neuronSepBatch;
    
    @Override
    protected String getGridServicePrefixName() {
        return "fragmentWeight";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);

        neuronSepBatch = (List)processData.getItem("NEURON_SEPARATION_BATCH_LIST");
        if (neuronSepBatch==null) {
            throw new IllegalArgumentException("The following may not be null: NEURON_SEPARATION_BATCH_LIST");
        }
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath()+File.separator);
        for (; configIndex<=neuronSepBatch.size(); configIndex++) {
            File dataFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Data."+configIndex);
            String dataFilePath = dataFile.getAbsolutePath();
            FileWriter dfw = new FileWriter(dataFile);
            dfw.write(neuronSepBatch.get(configIndex-1));
            File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
            FileWriter fw = new FileWriter(configFile);
            fw.write (dataFilePath + "\n");
            fw.write(outputDir.getAbsolutePath());
            dfw.close();
            fw.close();
        }
        writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }


    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }


    private void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read CONFIG_FILE\n");
        script.append("read OUTPUT_DIR\n");
        script.append(NeuronSeparatorHelper.getNeuronWeightsCommands(1));
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 8;
    }

}
