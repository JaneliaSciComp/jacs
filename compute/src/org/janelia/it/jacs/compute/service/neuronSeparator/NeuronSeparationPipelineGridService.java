package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.launcher.archive.ArchiveAccessHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
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
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparationPipelineGridService extends SubmitDrmaaJobService {
	
    public static final String NAME = "neuronSeparatorPipeline";
    
    private static final String CONFIG_PREFIX = "neuSepConfiguration.";
    private static final int TIMEOUT_SECONDS = 60 * 60;

    private FileNode outputFileNode;
    private String inputFilename;
    private String objective;
    private String previousResultFilepath;
    private String signalChannels;
    private String referenceChannel;
    private String consolidatedLabelFilepath;
    private File fromArchiveDir;
    
    @Override
    protected String getGridServicePrefixName() {
        return "neuSep";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputFileNode==null) {
        	outputFileNode = resultFileNode;
        }
        
        inputFilename = (String)processData.getItem("INPUT_FILENAME");
        if (inputFilename==null || "".equals(inputFilename)) {
        	throw new ServiceException("Input parameter INPUT_FILENAME may not be empty");
        }
    	
        previousResultFilepath = (String)processData.getItem("PREVIOUS_RESULT_FILENAME");
        
        signalChannels = (String)processData.getItem("SIGNAL_CHANNELS");
        if (signalChannels==null) {
        	signalChannels = "0 1 2";
        }

        objective = (String)processData.getItem("OBJECTIVE");
        
        referenceChannel = (String)processData.getItem("REFERENCE_CHANNEL");
        if (referenceChannel==null) {
        	referenceChannel = "3";
        }

        consolidatedLabelFilepath = (String)processData.getItem("ALIGNED_CONSOLIDATED_LABEL_FILEPATH");        
        
        copyInputsFromArchive();
        
        logger.info("Starting NeuronSeparationPipelineService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath()+
                " workingDir="+outputFileNode.getDirectoryPath() + " inputFilename="+inputFilename+ " signalChannels="+signalChannels+ " referenceChannel="+referenceChannel+ " objective="+objective+
                " previousResultFile="+previousResultFilepath+" consolidatedLabel="+consolidatedLabelFilepath);
    }

    private void copyInputsFromArchive() throws Exception {
        
        List<String> archivedFiles = new ArrayList<String>();
        List<String> targetFiles = new ArrayList<String>();
        
        File fromArchiveDir = new File(resultFileNode.getDirectoryPath(), "from_archive");
        
        if (previousResultFilepath!=null && previousResultFilepath.startsWith("/archive")) {
            File prevResultFile = new File(previousResultFilepath);
            File previousCompanionFile = new File(prevResultFile.getParent(), prevResultFile.getName().substring(0, prevResultFile.getName().lastIndexOf('.'))+".pbd");
            
            archivedFiles.add(previousResultFilepath);
            previousResultFilepath = new File(fromArchiveDir, prevResultFile.getName()).getAbsolutePath();
            targetFiles.add(previousResultFilepath);
            
            archivedFiles.add(previousCompanionFile.getAbsolutePath());
            previousCompanionFile = new File(fromArchiveDir, previousCompanionFile.getName());
            targetFiles.add(previousCompanionFile.getAbsolutePath());
        }
        
        if (consolidatedLabelFilepath!=null && consolidatedLabelFilepath.startsWith("/archive")) {
            archivedFiles.add(consolidatedLabelFilepath);
            consolidatedLabelFilepath = new File(fromArchiveDir, new File(consolidatedLabelFilepath).getName()).getAbsolutePath();
            targetFiles.add(consolidatedLabelFilepath);
        }
        
        if (!archivedFiles.isEmpty()) {
            logger.info("Creating temporary directory: "+fromArchiveDir.getAbsolutePath());
            fromArchiveDir.mkdirs();

            logger.info("Copying files from archive (task_id="+task.getObjectId()+")");
            ArchiveAccessHelper.sendCopyFromArchiveMessage(archivedFiles, targetFiles);
//            ArchiveAccessHelper.synchronousGridifiedArchiveCopy(task, archivedFiles, targetFiles, false);

            logger.info("Waiting for files to appear (task_id="+task.getObjectId()+"). Last file: "+targetFiles.get(targetFiles.size()-1));
            FileUtil.waitForFiles(targetFiles, TIMEOUT_SECONDS*1000);

            logger.info("Files have appeared (task_id="+task.getObjectId()+"). Last file: "+targetFiles.get(targetFiles.size()-1));
            
            this.fromArchiveDir = fromArchiveDir;
        }
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
    	File[] coreFiles = outputDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});
    	
    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Neuron separation core dumped for "+resultFileNode.getDirectoryPath());
    	}

    	File[] csFiles = outputDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("Consolidated") || name.equals("SeparationResult.nsp");
			}
		});

    	if (csFiles.length < 3) {
    		throw new MissingDataException("SeparationResult, ConsolidatedSignal, and ConsolidatedLabel not found for "+resultFileNode.getDirectoryPath());
    	}
    	
    	File archiveDir = new File(outputDir, "archive");
    	if (!archiveDir.exists()) {
    		logger.warn("No archive directory was generated at "+archiveDir.getAbsolutePath());
    	}
	}
}