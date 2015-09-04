package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Split channels for any number of images. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   INPUT_FILES - list of input files to split
 *   OUTPUT_DIR - directory in which to place all split output files
 *   OUTPUT_EXTENSION - image type extension (e.g. v3draw, v3dpbd, tif, etc.)
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DBulkChannelSplitService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
	private static final int START_DISPLAY_PORT = 890;
    private static final String CONFIG_PREFIX = "splitConfiguration.";
    
    private int randomPort;
    
    private List<String> inputFiles;
    private String outputDir;
    private String outputExtension;
    
    public void init(IProcessData processData) throws Exception {
    	super.init(processData);

        this.inputFiles = (List<String>)processData.getItem("INPUT_FILES");
        if (inputFiles==null) {
            
            String inputFile = (String)processData.getItem("INPUT_FILE");
            if (inputFile==null) {
                throw new IllegalArgumentException("INPUT_FILES and INPUT_FILE may not both be null");
            }
            
            this.inputFiles = new ArrayList<String>();
            inputFiles.add(inputFile);
        }
        
        this.outputDir = (String)processData.getItem("OUTPUT_DIR");
        if (outputDir==null) {
            this.outputDir = resultFileNode.getDirectoryPath();
        }
        
        this.outputExtension = (String)processData.getItem("OUTPUT_EXTENSION");
        if (outputExtension==null) {
            outputExtension = "v3dpbd";
        }
    }

    @Override
    protected String getGridServicePrefixName() {
        return "split";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        int configIndex = 1;
        this.randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
        for(String inputFile : inputFiles) {
        	writeInstanceFiles(inputFile, configIndex++);
        }
        
    	createShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(String inputFile, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(inputFile + "\n");
            fw.write(outputDir + "\n");
            fw.write(outputExtension + "\n");
            fw.write((randomPort+configIndex) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_FILE\n");
        script.append("read OUTPUT_DIR\n");
        script.append("read OUTPUT_EXTENSION\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT"));
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedSplitChannelsCommand("$INPUT_FILE", "$OUTPUT_DIR", "$OUTPUT_EXTENSION") + "\n");
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 24;
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

    	FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
    	File file = new File(parentNode.getDirectoryPath());
    	
    	File[] coreFiles = FileUtil.getFilesWithPrefixes(file, "core");
    	if (coreFiles.length > 0) {
    		throw new MissingGridResultException(file.getAbsolutePath(), "Split channels core dumped");
    	}
	}
    
}
