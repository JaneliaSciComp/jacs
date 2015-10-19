package org.janelia.it.jacs.compute.service.vaa3d;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Stitch a bunch of merged files together and blend them. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   SAMPLE_AREA - the sample tiles 
 *   REFERENCE_CHANNEL_INDEX (optional; defaults to 4) - the index of the reference channel in each image
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DStitchAndBlendService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "stitchConfiguration.";
    
    private int referenceChannelIndex = 4;
    private AnatomicalArea sampleArea;
    private String stitchedFilename;
    
    @Override
    protected String getGridServicePrefixName() {
        return "stitch";
    }
    
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        FileNode inputFileNode = (FileNode)processData.getItem("INPUT_FILE_NODE");
        if (inputFileNode==null) {
        	throw new ServiceException("Input parameter INPUT_FILE_NODE may not be null");
        }

        this.sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");
        
        String referenceChannelIndexStr = (String)processData.getItem("REFERENCE_CHANNEL");
        if (referenceChannelIndexStr!=null) {
        	referenceChannelIndex = Integer.parseInt(referenceChannelIndexStr)+1;	
        }
        
        writeInstanceFiles();
        setJobIncrementStop(1);

        this.stitchedFilename = sampleArea.getStitchedFilename();
    	List<MergedLsmPair> mergedLsmPairs = sampleArea.getMergedLsmPairs();
    	if (mergedLsmPairs.size()==1) {
    		logger.warn("Creating stitching bypass script. This is an old code path that should not longer be exercised!");
    		createBypassShellScript(writer, mergedLsmPairs.get(0).getMergedFilepath(), stitchedFilename);
    	}            
    	else {
        	createShellScript(writer, inputFileNode.getDirectoryPath(), stitchedFilename);
    	}
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) { 
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath()); 
    	}
    }
    
    /**
     * TODO: remove this method
     * if there is just one merged file we have to bypass the stitcher altogether because otherwise we get a bogus 
     * output from it. 
     * @param writer
     * @param mergedFilepath
     * @param stitchedFilepath
     * @throws Exception
     */
    private void createBypassShellScript(FileWriter writer, String mergedFilepath, String stitchedFilepath) throws Exception {
        StringBuffer script = new StringBuffer();
    	if ((mergedFilepath.endsWith("lsm")||mergedFilepath.endsWith("v3draw")) && stitchedFilepath.endsWith("v3dpbd")) {
    		// need to convert
    		script.append(Vaa3DHelper.getFormattedConvertCommand(mergedFilepath, stitchedFilepath));
    	}
    	else {
    		// just copy 
    		script.append("cp "+mergedFilepath+" "+stitchedFilepath);	
    	}
        script.append("\n");
        writer.write(script.toString());
    }

    /**
     * Write the shell script that runs the stitcher on the merged files.
     * @param writer
     * @param mergedFilepath
     * @param stitchedFilepath
     * @throws Exception
     */
    private void createShellScript(FileWriter writer, String mergedFilepath, String stitchedFilepath) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedStitcherCommand(referenceChannelIndex, mergedFilepath));
        script.append("\n");
        script.append(Vaa3DHelper.getFormattedBlendCommand(mergedFilepath, stitchedFilepath));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 64;
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
    		throw new MissingGridResultException(file.getAbsolutePath(), "Stitch/blend core dumped for "+resultFileNode.getDirectoryPath());
    	}

        File stitchedFile = new File(stitchedFilename);        
    	if (!stitchedFile.exists()) {
    		throw new MissingGridResultException(file.getAbsolutePath(), "Stitched output file not found for "+resultFileNode.getDirectoryPath());
    	}
	}
    
}
