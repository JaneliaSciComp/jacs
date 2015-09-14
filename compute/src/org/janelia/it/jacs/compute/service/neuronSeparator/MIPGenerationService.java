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
 * Generate MIPs for any number of 3d volumes in parallel. 
 * 
 * This is similar to the MIPGenerationService in the vaa3d package, except it uses the Neuron Separator pipeline
 * tools in order to generate MIPs for specific sets of channels.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MIPGenerationService extends SubmitDrmaaJobService {

    private static final int START_DISPLAY_PORT = 990;
    private String signalChannels;
    private String referenceChannel;
    private List<String> inputFilenames;
    private List<ImageStack> inputImages;
    
    @Override
    protected String getGridServicePrefixName() {
        return "mip";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);

        inputFilenames = (List<String>)processData.getItem("INPUT_FILENAMES");
    
        if (inputFilenames==null) {
            String inputFilename = (String)processData.getItem("INPUT_FILENAME");
            if (inputFilename!=null) {
                logger.info("Got input filename: "+inputFilename);
                inputFilenames = new ArrayList<>();
                inputFilenames.add(inputFilename);
            }
            else {
                inputImages = (List<ImageStack>)processData.getItem("INPUT_IMAGES");
                logger.info("Got "+inputImages.size()+" input images");
                if (inputImages==null) {
                    throw new IllegalArgumentException("All of the following may not be null: INPUT_FILENAMES, INPUT_FILENAME, INPUT_IMAGES");
                }
            }
        }
        else {
            logger.info("Got "+inputFilenames.size()+" input filenames");
        }
        
        if (inputImages==null) {
            signalChannels = (String)processData.getItem("SIGNAL_CHANNELS");
            if (signalChannels==null) {
                throw new IllegalArgumentException("SIGNAL_CHANNELS may not be null if INPUT_IMAGES is not used");
            }
            referenceChannel = (String)processData.getItem("REFERENCE_CHANNEL");
            if (referenceChannel==null) {
                throw new IllegalArgumentException("REFERENCE_CHANNEL may not be null if INPUT_IMAGES is not used");
            }
        }
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        int configIndex = 1;
        
        if (inputFilenames!=null) {
            for(String inputFilename : inputFilenames) {    
                File inputFile = new File(inputFilename);
                File outputDir = inputFile.getParentFile();
                writeInstanceFiles(inputFile, outputDir, signalChannels, referenceChannel, configIndex++);
            }
        }
        else if (inputImages!=null) {
            for(ImageStack inputImage : inputImages) {    
                File inputFile = new File(inputImage.getFilepath());
                File outputDir = inputFile.getParentFile();
                String chanSpec = inputImage.getChannelSpec();
                // The MIP pipeline expects a space-delimited list of indexes which are zero-indexed.
                String signalChannels = ChanSpecUtils.getSignalChannelIndexes(chanSpec);
                String referenceChannel = ChanSpecUtils.getReferenceChannelIndexes(chanSpec);
                writeInstanceFiles(inputFile, outputDir, signalChannels, referenceChannel, configIndex++);
            }
        }
        
        writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    protected void writeInstanceFiles(File inputFile, File outputDir, String signalChannels, String referenceChannel, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            writeInstanceFile(fw, inputFile, outputDir, signalChannels, referenceChannel, configIndex);
        }
        catch (IOException e) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
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
    protected void writeInstanceFile(FileWriter fw, File inputFile, File outputFile, String signalChannels, String referenceChannel, int configIndex) throws IOException {
        int randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
        fw.write(outputFile.getAbsolutePath() + "\n");
        fw.write(inputFile.getAbsolutePath() + "\n");
        fw.write(signalChannels + "\n");
        fw.write(referenceChannel + "\n");
        fw.write((randomPort+configIndex) + "\n");
    }

    /**
     * Write the shell script used for all instances in the job array. The default implementation read INPUT_FILENAME
     * and OUTPUT_FILENAME.
     */
    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read INPUT_FILE\n");
        script.append("read SIGNAL_CHAN\n");
        script.append("read REF_CHAN\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(NeuronSeparatorHelper.getMipCreatorCommands(getGridResourceSpec().getSlots()));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 3;
    }
}
