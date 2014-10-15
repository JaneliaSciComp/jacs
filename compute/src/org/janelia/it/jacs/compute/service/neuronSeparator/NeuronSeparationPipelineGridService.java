package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Run neuron separator on some input files and generate a bunch of stuff. Parameters:
 *   INPUT_FILENAME - input file
 *   RESULT_FILE_NODE - node in which to run the grid job
 *   OUTPUT_FILE_NODE - node to place the results (defaults to RESULT_FILE_NODE)
 *   PREVIOUS_RESULT_FILENAME - name of the previous result (*.nsp file) to map fragments against
 *   OBJECTIVE - objective of the input, used to estimate the number of grid nodes to use (e.g. 63x files are larger than 20x, and need more processing power)
 *   SIGNAL_CHANNELS - the signal channels of the input file
 *   REFERENCE_CHANNEL - reference channel of the input file
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparationPipelineGridService extends AbstractEntityGridService {
	
    public static final String NAME = "neuronSeparatorPipeline";
    
    private static final String CONFIG_PREFIX = "neuSepConfiguration.";
    private static final int TIMEOUT_SECONDS = 60 * 60;

    protected static final String ARCHIVE_PREFIX = "/archive";
    
    private FileNode outputFileNode;
    private String inputFilename;
    private String objective;
    private Long previousSeparationId;
    private String previousResultFilepath;
    private String signalChannels;
    private String referenceChannel;
    private String consolidatedLabelFilepath;
    private File fromArchiveDir;
    
    @Override
    protected String getGridServicePrefixName() {
        return "neuSep";
    }

    @Override
    protected void init() throws Exception {

        outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputFileNode==null) {
        	outputFileNode = resultFileNode;
        }
        
        inputFilename = data.getItemAsString("INPUT_FILENAME");
        if (inputFilename==null || "".equals(inputFilename)) {
        	throw new ServiceException("Input parameter INPUT_FILENAME may not be empty");
        }

        previousSeparationId = data.getItemAsLong("PREVIOUS_SEPARATION_ID");
        previousResultFilepath = data.getItemAsString("PREVIOUS_RESULT_FILENAME");
        
        signalChannels = data.getItemAsString("SIGNAL_CHANNELS");
        if (signalChannels==null) {
        	signalChannels = "0 1 2";
        }

        objective = data.getItemAsString("OBJECTIVE");
        
        referenceChannel = data.getItemAsString("REFERENCE_CHANNEL");
        if (referenceChannel==null) {
        	referenceChannel = "3";
        }

        consolidatedLabelFilepath = data.getItemAsString("ALIGNED_CONSOLIDATED_LABEL_FILEPATH");        
        
        logger.info("Starting NeuronSeparationPipelineService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath()+
                " workingDir="+outputFileNode.getDirectoryPath() + " inputFilename="+inputFilename+ " signalChannels="+signalChannels+ " referenceChannel="+referenceChannel+ " objective="+objective+
                " previousResultFile="+previousResultFilepath+" consolidatedLabel="+consolidatedLabelFilepath);
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeInstanceFiles(1);
        createShellScript(writer);
        setJobIncrementStop(1);
    }

    private void writeInstanceFiles(int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(outputFileNode.getDirectoryPath() + "\n");
            fw.write(NAME + "\n");
            fw.write(inputFilename + "\n");
            fw.write(signalChannels + "\n");
            fw.write(referenceChannel + "\n");
            fw.write((previousResultFilepath==null?"":previousResultFilepath) + "\n");
            fw.write((consolidatedLabelFilepath==null?"":consolidatedLabelFilepath) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        boolean isWarped = !StringUtils.isEmpty(consolidatedLabelFilepath);
        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read NAME\n");
        script.append("read INPUT_FILE\n");
        script.append("read SIGNAL_CHAN\n");
        script.append("read REF_CHAN\n");
        script.append("read PREVIOUS_OUTPUT\n");
        script.append("read CONSOLIDATED_LABEL\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append(NeuronSeparatorHelper.getNeuronSeparationCommands(getGridResourceSpec().getSlots(), isWarped) + "\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        if (fromArchiveDir!=null) {
            script.append("rm -rf "+fromArchiveDir.getAbsolutePath()+"\n");
        }
        script.append("\n");
        writer.write(script.toString());
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected int getRequiredMemoryInGB() {
        if (StringUtils.isEmpty(objective)) return 96;
        switch (Objective.valueOf("OBJECTIVE_"+objective.toUpperCase())) {
        case OBJECTIVE_10X: return 24;
        case OBJECTIVE_20X: return 24;
        case OBJECTIVE_40X: return 60;
        case OBJECTIVE_63X: return 96;
        default: return 24;
        }
    }
    
    @Override
	public void postProcess() throws MissingDataException {

    	File outputDir = new File(outputFileNode.getDirectoryPath());
    	
    	File[] coreFiles = FileUtil.getFilesWithPrefixes(outputDir, "core");    	
    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Neuron separation core dumped for "+resultFileNode.getDirectoryPath());
    	}
    	
    	File[] csFiles = FileUtil.getFilesWithPrefixes(outputDir, "Consolidated", "SeparationResult.nsp");
    	if (csFiles.length < 3) {
    		throw new MissingDataException("SeparationResult, ConsolidatedSignal, and ConsolidatedLabel not found for "+resultFileNode.getDirectoryPath());
    	}
    	
    	File archiveDir = new File(outputDir, "archive");
    	if (!archiveDir.exists()) {
    		logger.warn("No archive directory was generated at "+archiveDir.getAbsolutePath());
    	}
    	
    	// Copy the mapping file into the target separation directory
    	if (previousSeparationId!=null) {
    		File[] mappingFiles = FileUtil.getFilesWithPrefixes(outputDir, NeuronMappingGridService.MAPPING_FILE_NAME_PREFIX);
	    	if (mappingFiles.length<1) {
	    		logger.warn("No mapping file found for separation "+outputDir);
	    	}
	    	else {
		    	File resultFile = mappingFiles[0];
		    	resultFile.renameTo(new File(outputDir, NeuronMappingGridService.MAPPING_FILE_NAME_PREFIX+"_"+previousSeparationId+".txt"));
	    	}
    	}
	}
}