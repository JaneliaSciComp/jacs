package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Run the mask/chan pipeline on a set of neuron separation result directories.
 * 
 * Inputs variables:
 *   FILE_PATHS - input paths (e.g. /.../separate)
 *   
 * Output variables:
 *   MASKCHAN_PATHS - resulting paths to the maskChan artifacts (e.g. /.../separate/maskChan)
 *   ARCHIVE_FILE_PATHS - resulting paths to archive (e.g. /.../separate/archive)
 *   ARCHIVE_FILE_PATH - first path, if there is just one (e.g. /.../separate/archive)
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MaskChanArtifactPipelineGridService extends SubmitDrmaaJobService {

    public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
    private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
    
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private List<String> inputPaths;
    private List<String> maskChanPaths;
    
    @Override
    protected String getGridServicePrefixName() {
        return "maskChan";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        inputPaths = (List<String>)processData.getItem("FILE_PATHS");
        if (inputPaths==null) {
        	throw new ServiceException("Input parameter FILE_PATHS may not be empty");
        }
        
        for(String filepath : inputPaths) {
            if (!filepath.contains(centralDir)) {
                throw new IllegalStateException("Cannot create mask/chan artifacts in dir which is not in the FileStore.CentralDir: "+filepath);
            }
        }
        
        maskChanPaths = new ArrayList<String>();
        for(String inputPath : inputPaths) {
        	maskChanPaths.add(inputPath+"/archive/maskChan");
        }
        
        processData.putItem("MASKCHAN_PATHS", maskChanPaths);
        
    	
        logger.info("Starting MaskChanArtifactPipelineGridService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath());
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
    	int configIndex = 1;
    	for(String inputPath : inputPaths) {
    		writeInstanceFiles(inputPath, configIndex++);
    	}
    	writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
        
    }

    private void writeInstanceFiles(String inputFile, int configIndex) throws Exception {
    	File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            writeInstanceFile(fw, inputFile, configIndex);
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    protected void writeInstanceFile(FileWriter fw, String inputDir, int configIndex) throws IOException {
        fw.write(inputDir + "\n");
        fw.write(resultFileNode.getDirectoryPath() + "\n");
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_DIR\n");
        script.append("read WORK_DIR\n");
        script.append(NeuronSeparatorHelper.getMaskChanCommands(getGridResourceSpec().getSlots()) + " $WORK_DIR\n");
        writer.write(script.toString());
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 8;
    }
    
    @Override
	public void postProcess() throws MissingDataException {
    	
    	for(String maskChanDir : maskChanPaths) {

    		File dir = new File(maskChanDir);
    		if (!dir.exists()) {
				if (maskChanPaths.size()==1) {
					throw new MissingDataException("Missing mask/chan directory: "+maskChanDir);
				}
				else {
	    			logger.error("Missing mask/chan directory: "+maskChanDir);
				}
    		}

    		if (!dir.exists()) {
				if (maskChanPaths.size()==1) {
					throw new MissingDataException("Missing fast load directory: "+maskChanDir);
				}
				else {
	    			logger.error("Missing fast load directory: "+maskChanDir);
				}
    		}
    	}

    	processData.putItem("ARCHIVE_FILE_PATHS", maskChanPaths);
    	if (maskChanPaths.size()==1) {
    		processData.putItem("ARCHIVE_FILE_PATH", maskChanPaths.get(0));
    	}
	}
}