package org.janelia.it.jacs.compute.service.image;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;

/**
 * Generates MIPs and movies for any number of input stacks. Supports customizable colors 
 * and other features controlled by the InputImage parameter class. 
 * 
 * This service differs from the BasicMIPandMovieGenerationService in a number of important ways:
 * 1) Supports different enhancement modes for "mcfo", "polarity", and "none".
 * 2) Does not support legends.
 * 3) Does not support normalization between multiple images.
 * 4) Only supports grey reference channels.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EnhancedMIPandMovieGenerationService extends BasicMIPandMovieGenerationService {

    protected static final String MACRO_NAME = "Enhanced_MIP_StackAvi.ijm";
    
    private String mode;
    
    @Override
    protected void init() throws Exception {
        super.init();
        this.mode = data.getItemAsString("MODE");
    }

    @Override
    protected void writeInstanceFiles() throws Exception {
        for(InputImage inputImage : inputImages) {
            writeInstanceFile(inputImage, configIndex++);
        }
    }

    private void writeInstanceFile(InputImage inputImage, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            String inputFile = inputImage.getFilepath();
            String chanSpec = inputImage.getChanspec();
            String colorSpec = inputImage.getColorspec();
            
            fw.write(resultFileNode.getDirectoryPath() + "\n");
            fw.write(inputImage.getOutputPrefix() + "\n");
            fw.write((mode==null?"none":mode) + "\n");
            fw.write((inputFile==null?"":inputFile) + "\n");
            fw.write((chanSpec==null?"":chanSpec) + "\n");
            fw.write((colorSpec==null?"":colorSpec) + "\n");
            fw.write(options + "\n");
            fw.write((randomPort+configIndex) + "\n");
        }
        catch (IOException e) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    @Override
    protected void createShellScript(FileWriter writer) throws Exception {

        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read OUTPUT_PREFIX\n");
        script.append("read MODE\n");
        script.append("read INPUT_FILE\n");
        script.append("read CHAN_SPEC\n");
        script.append("read COLOR_SPEC\n");
        script.append("read OPTIONS\n");
        script.append("read DISPLAY_PORT\n");
        script.append("cd "+resultFileNode.getDirectoryPath()).append("\n");
        
        // Start Xvfb
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT", "1280x1024x24")).append("\n");

        // Create temp dir so that large temporary avis are not created on the network drive
        script.append("export TMPDIR=").append(SCRATCH_DIR).append("\n");
        script.append("mkdir -p $TMPDIR\n");
        script.append("TEMP_DIR=`mktemp -d`\n");
        script.append("function cleanTemp {\n");
        script.append("    rm -rf $TEMP_DIR\n");
        script.append("    echo \"Cleaned up $TEMP_DIR\"\n");
        script.append("}\n");

        // Two EXIT handlers
        script.append("function exitHandler() { cleanXvfb; cleanTemp; }\n");
        script.append("trap exitHandler EXIT\n");
        
        // Run Fiji macro
        StringBuffer cmd = new StringBuffer();
        cmd.append(FIJI_BIN_PATH).append(" -macro ").append(FIJI_MACRO_PATH).append("/").append(MACRO_NAME);
        cmd.append(" $TEMP_DIR,$OUTPUT_PREFIX,$MODE,$INPUT_FILE,$CHAN_SPEC,$COLOR_SPEC,$OPTIONS");
        script.append("echo \"Executing:\"\n");
        script.append("echo \""+cmd+"\"\n");
        script.append(cmd).append(" & \n");
        
        // Monitor Fiji and take periodic screenshots, killing it eventually
        script.append("fpid=$!\n");
        script.append(Vaa3DHelper.getXvfbScreenshotLoop("./xvfb.${PORT}", "PORT", "fpid", 30, 1800));
        
        // Encode avi movies as mp4 and delete the input avi's
        script.append("cd $TEMP_DIR\n");
        script.append("for fin in *.avi; do\n");
        script.append("    fout=${fin%.avi}.mp4\n");
        script.append("    "+Vaa3DHelper.getFormattedH264ConvertCommand("$fin", "$fout", false)).append(" && rm $fin\n");
        script.append("done\n");

        // Move everything to the final output directory
        script.append("cp $TEMP_DIR/*.png $OUTPUT_DIR\n");
        script.append("cp $TEMP_DIR/*.mp4 $OUTPUT_DIR\n");
        
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix()).append("\n");
        
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 50;
    }
}
