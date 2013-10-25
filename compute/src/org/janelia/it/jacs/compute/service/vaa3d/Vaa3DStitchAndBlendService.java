package org.janelia.it.jacs.compute.service.vaa3d;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Stitch a bunch of merged files together and blend them. Parameters:
 *   RESULT_FILE_NODE - the directory to use for SGE config and output
 *   BULK_MERGE_PARAMETERS - a list of MergedLsmPair
 *   STITCHED_FILENAME - the output file to create
 *   REFERENCE_CHANNEL_INDEX (optional; defaults to 4) - the index of the reference channel in each image
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Vaa3DStitchAndBlendService extends SubmitDrmaaJobService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "stitchConfiguration.";
    
    private int referenceChannelIndex = 4;
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "stitch";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        FileNode inputFileNode = (FileNode)processData.getItem("INPUT_FILE_NODE");
        if (inputFileNode==null) {
        	throw new ServiceException("Input parameter INPUT_FILE_NODE may not be null");
        }

        String stitchedFilename = (String)processData.getItem("STITCHED_FILENAME");
        if (stitchedFilename==null) {
        	throw new ServiceException("Input parameter STITCHED_FILENAME may not be null");
        }

        String referenceChannelIndexStr = (String)processData.getItem("REFERENCE_CHANNEL");
        if (referenceChannelIndexStr!=null) {
        	referenceChannelIndex = Integer.parseInt(referenceChannelIndexStr)+1;	
        }
        
        writeInstanceFiles();
        setJobIncrementStop(1);
        
        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }

        if (bulkMergeParamObj instanceof List) {
        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
        	if (mergedLsmPairs.size()==1) {
        		createBypassShellScript(writer, mergedLsmPairs.get(0).getMergedFilepath(), stitchedFilename);
        	}            
        	else {
            	createShellScript(writer, inputFileNode.getDirectoryPath(), stitchedFilename);
        	}
        }
        else {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS must be an ArrayList<MergedLsmPair>");
        }
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) { 
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath()); 
    	}
    }
    
    /**
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
    	
    	File[] coreFiles = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});
    	
    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Stitch/blend core dumped for "+resultFileNode.getDirectoryPath());
    	}

        String stitchedFilename = (String)processData.getItem("STITCHED_FILENAME");
        File stitchedFile = new File(stitchedFilename);
        
    	if (!stitchedFile.exists()) {
    		throw new MissingDataException("Stitched output file not found for "+resultFileNode.getDirectoryPath());
    	}
	}
    
}
