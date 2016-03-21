package org.janelia.it.jacs.compute.service.image;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainGridService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Generates MIPs and movies for any number of input stacks. Supports customizable colors 
 * and other features controlled by the InputImage parameter class. 
 * 
 * Also supports the normalization of all inputs to the first (currently this is only supported when there are two inputs). 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class BasicMIPandMovieGenerationService extends AbstractDomainGridService {

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");
    protected static final String FIJI_BIN_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Bin.Path");
    protected static final String FIJI_MACRO_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Macro.Path");
    protected static final String SCRATCH_DIR = SystemConfigurationProperties.getString("computeserver.ClusterScratchDir");
    protected static final String MACRO_NAME = "Basic_MIP_StackAvi.ijm";
    
    protected static final int START_DISPLAY_PORT = 890;
    protected static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    protected static final String CONFIG_PREFIX = "fijiConfiguration.";

    protected int randomPort; 
    protected int configIndex = 1;
    
    protected File outputDir;
    protected List<InputImage> inputImages; 
    protected boolean normalizeToFirst;
    protected String options;
    
    @Override
    protected void init() throws Exception {

        FileNode outputNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputNode == null) {
            throw new IllegalArgumentException("OUTPUT_FILE_NODE may not be null");
        }
        outputDir = new File(outputNode.getDirectoryPath());
        
    	this.inputImages = (List<InputImage>)data.getRequiredItem("INPUT_IMAGES");
        this.normalizeToFirst = data.getItemAsBoolean("NORMALIZE_TO_FIRST_IMAGE");
        this.options = data.getItemAsString("OPTIONS");
        if (options==null) {
            this.options = "mips:movies:legends";
        }
        
        if (inputImages.isEmpty()) {
            cancel();
            return;
        }
        
        logger.info("Running Fiji macro for "+inputImages.size()+" images");
    }
    
    @Override
    protected String getGridServicePrefixName() {
        return "fiji";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        this.randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
        writeInstanceFiles();
        setJobIncrementStop(configIndex-1);
        createShellScript(writer);
    }

    protected void writeInstanceFiles() throws Exception {
        if (normalizeToFirst) {
            // TODO: In the future we need a better normalization mechanism in the Fiji macro.
            // It should write the intensity to a text file so we can use it to normalize multiple secondary inputs. 
            // For now, we can normalize a single input to the first (e.g. VNC to Brain). 
            InputImage inputImage1 = inputImages.get(0);
            InputImage inputImage2 = inputImages.get(1);
            writeInstanceFile(inputImage1, inputImage2, configIndex++);
        }
        else {
            for(InputImage inputImage : inputImages) {
                writeInstanceFile(inputImage, null, configIndex++);
            }
        }
    }

    private void writeInstanceFile(InputImage inputImage, InputImage inputImage2, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            String inputFile = inputImage.getFilepath();
            Integer laser = inputImage.getLaser();
            Integer gain = inputImage.getGain();
            String chanSpec = inputImage.getChanspec();
            String colorSpec = inputImage.getColorspec();
            String divSpec = inputImage.getDivspec();
            
            fw.write(outputDir.getAbsolutePath() + "\n");
            fw.write(inputImage.getOutputPrefix() + "\n");
            fw.write((inputImage2==null?"":inputImage2.getOutputPrefix()) + "\n");
            fw.write((inputFile==null?"":inputFile) + "\n");
            fw.write((inputImage2==null?"":inputImage2.getFilepath()) + "\n");
            fw.write((laser==null?"":laser) + "\n");
            fw.write((gain==null?"":gain) + "\n");
            fw.write((chanSpec==null?"":chanSpec) + "\n");
            fw.write((colorSpec==null?"":colorSpec) + "\n");
            fw.write((divSpec==null?"":divSpec) + "\n");
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

    protected void createShellScript(FileWriter writer) throws Exception {

        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read OUTPUT_PREFIX_1\n");
        script.append("read OUTPUT_PREFIX_2\n");
        script.append("read INPUT_FILE1\n");
        script.append("read INPUT_FILE2\n");
        script.append("read LASER\n");
        script.append("read GAIN\n");
        script.append("read CHAN_SPEC\n");
        script.append("read COLOR_SPEC\n");
        script.append("read DIV_SPEC\n");
        script.append("read OPTIONS\n");
        script.append("read DISPLAY_PORT\n");
        script.append("cd "+resultFileNode.getDirectoryPath()).append("\n");
        
        // Start Xvfb
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT", "1280x1024x24")).append("\n");

        // TODO: use Vaa3DHelper.getScratchDirCreationScript instead
        
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
        cmd.append(" $TEMP_DIR,$OUTPUT_PREFIX_1,$OUTPUT_PREFIX_2,$INPUT_FILE1,$INPUT_FILE2,$LASER,$GAIN,$CHAN_SPEC,$COLOR_SPEC,$DIV_SPEC,$OPTIONS");
        script.append("echo \"Executing:\"\n");
        script.append("echo \""+cmd+"\"\n");
        script.append(cmd).append(" & \n");
        
        // Monitor Fiji and take periodic screenshots, killing it eventually
        script.append("fpid=$!\n");
        script.append(Vaa3DHelper.getXvfbScreenshotLoop("./xvfb.${PORT}", "PORT", "fpid", 30, 600));
        
        // Encode avi movies as mp4 and delete the input avi's
        script.append("cd $TEMP_DIR\n");
        script.append("for fin in *.avi; do\n");
        script.append("    fout=${fin%.avi}.mp4\n");
        script.append("    "+Vaa3DHelper.getFormattedH264ConvertCommand("$fin", "$fout", false)).append(" && rm $fin\n");
        script.append("done\n");
        
        // Move everything to the final output directory
        script.append("cp $TEMP_DIR/*.png $OUTPUT_DIR || true\n");
        script.append("cp $TEMP_DIR/*.mp4 $OUTPUT_DIR || true\n");
        script.append("cp $TEMP_DIR/*.properties $OUTPUT_DIR || true\n");
        
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix()).append("\n");
        
        writer.write(script.toString());
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 20;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

        File[] files = outputDir.listFiles();

        File[] aviFiles = FileUtil.getFilesWithSuffixes(outputDir, ".avi");
        if (aviFiles.length > 0) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "MP4 generation failed for "+resultFileNode.getDirectoryPath());
        }
        
        for(InputImage inputImage : inputImages) {
            int count = 0;
            for(File file : files) {
                String filename = file.getName();
                if (filename.startsWith(inputImage.getOutputPrefix()) && (filename.endsWith(".png") || filename.endsWith(".mp4"))) {
                    count++;
                }
            }
            if (count==0) {
                throw new MissingGridResultException(outputDir.getAbsolutePath(), "No output files found for input "+inputImage.getFilepath()+" in "+outputDir);
            }   
        }
	}
}
