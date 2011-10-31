package org.janelia.it.jacs.compute.service.v3d;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
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
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class V3DStitchAndBlendService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "stitchConfiguration.";
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    }

    @Override
    protected String getGridServicePrefixName() {
        return "stitch";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        // This SHOULD use the mergedFilepaths from the mergedLsmPairs as below, but right now the stitcher does its
        // own file discovery, so we just need to point it at the merged directory.
        
//        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
//        if (bulkMergeParamObj==null) {
//        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
//        }
//
//        if (bulkMergeParamObj instanceof List) {
//        	List<MergedLsmPair> mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
//            List<File> mergedFiles = new ArrayList<File>();
//            for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
//            	mergedFiles.add(new File(mergedLsmPair.getMergedFilepath()));
//            }
//        }
//        else {
//        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS must be an ArrayList<MergedLsmPair>");
//        }
        
        FileNode outputFileNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
        if (outputFileNode==null) {
        	throw new ServiceException("Input parameter OUTPUT_FILE_NODE may not be null");
        }

        String stitchedFilename = (String)processData.getItem("STITCHED_FILENAME");
        if (stitchedFilename==null) {
        	throw new ServiceException("Input parameter STITCHED_FILENAME may not be null");
        }
        
        writeInstanceFiles();
    	createShellScript(writer, outputFileNode.getDirectoryPath(), stitchedFilename);
        setJobIncrementStop(1);
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) { 
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath()); 
    	}
    }

    private void createShellScript(FileWriter writer, String mergedFilepath, String stitchedFilepath) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append(V3DHelper.getV3DGridCommandPrefix());
        script.append("\n");
        script.append(V3DHelper.getFormattedStitcherCommand(mergedFilepath));
        script.append("\n");
        script.append(V3DHelper.getFormattedBlendCommand(mergedFilepath, stitchedFilepath));
        script.append("\n");
        script.append(V3DHelper.getV3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve the entire node
    	jt.setNativeSpecification("-l excl=true");
    	return jt;
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
