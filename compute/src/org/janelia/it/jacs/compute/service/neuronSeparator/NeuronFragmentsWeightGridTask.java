package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private static final int TIMEOUT_SECONDS = 600;  // 10 minutes
    private String outputFileDir;
    private String inputFileDir;
    
    @Override
    protected String getGridServicePrefixName() {
        return "fragmentWeight";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);

        inputFileDir = processData.getString("NEURON_SEPARATION_DIR");
        if (inputFileDir==null) {
            throw new IllegalArgumentException("All of the following may not be null: NEURON_SEPARATION_DIR, FRAGMENT_WEIGHTS_DIR");
        }
        outputFileDir = processData.getString("FRAGMENT_WEIGHTS_DIR");
        if (inputFileDir==null) {
            throw new IllegalArgumentException("All of the following may not be null: NEURON_SEPARATION_DIR, FRAGMENT_WEIGHTS_DIR");
        }
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        int configIndex = 1;

        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        File inputDir = new File(inputFileDir);
        File outputDir = new File(outputFileDir);

        writeInstanceFile(fw, inputDir, outputDir, configIndex);
        writeShellScript(writer);

        setJobIncrementStop(1);
    }


    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
    
    /**
     * Write out the parameters passed to a given instance in the job array. The default implementation writes 
     * the input file path and the output file path.
     * @param fw
     * @param inputFile
     * @param outputFile
     * @param configIndex
     * @throws IOException
     */
    private void writeInstanceFile(FileWriter fw, File inputFile, File outputFile, int configIndex) throws IOException {
        fw.write(outputFile.getAbsolutePath() + "\n");
        fw.write(inputFile.getAbsolutePath() + "\n");
    }

    /**
     * Write the shell script used for all instances in the job array. The default implementation read INPUT_FILENAME
     * and OUTPUT_FILENAME.
     */
    private void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read INPUT_DIR\n");
        script.append("\n");
        script.append(NeuronSeparatorHelper.getNeuronWeightsCommands(getGridResourceSpec().getSlots()));
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 12;
    }
}
