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
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Run the fast load pipeline on a set of neuron separation result directories.
 * 
 * Inputs variables:
 *   FILE_PATHS - input paths (e.g. /.../separate)
 *   
 * Output variables:
 *   FASTLOAD_PATHS - resulting paths to the fastLoad artifacts (e.g. /.../separate/fastLoad)
 *   ARCHIVE_FILE_PATHS - resulting paths to archive (e.g. /.../separate/archive)
 *   ARCHIVE_FILE_PATH - first path, if there is just one (e.g. /.../separate/archive)
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FastLoadArtifactPipelineGridService extends SubmitDrmaaJobService {
	
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private static final int NUM_ARTIFACTS = 28; // number of fast load artifacts expected
    
    private List<String> inputPaths;
    private List<String> fastLoadPaths;
    
    @Override
    protected String getGridServicePrefixName() {
        return "fastLoad";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        inputPaths = (List<String>)processData.getItem("FILE_PATHS");
        if (inputPaths==null) {
        	throw new ServiceException("Input parameter FILE_PATHS may not be empty");
        }
        
        fastLoadPaths = new ArrayList<String>();
        for(String inputPath : inputPaths) {
        	File path = new File(inputPath,"fastLoad");
        	fastLoadPaths.add(path.getAbsolutePath());
        }
        
        processData.putItem("FASTLOAD_PATHS", fastLoadPaths);
        
    	
        logger.info("Starting FastLoadArtifactPipelineGridService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath());
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
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_DIR\n");
        script.append(NeuronSeparatorHelper.getFastLoadCommands(getGridResourceSpec().getSlots(), "$INPUT_DIR") + "\n");
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

        FileNode parentNode = ProcessDataHelper.getResultFileNode(processData);
        File file = new File(parentNode.getDirectoryPath());
        
    	List<String> archivePaths = new ArrayList<String>();
    	
    	for(String fastLoadDir : fastLoadPaths) {

    		File dir = new File(fastLoadDir);
    		if (!dir.exists()) {
				if (fastLoadPaths.size()==1) {
					throw new MissingGridResultException(file.getAbsolutePath(), "Missing fast load directory: "+fastLoadDir);
				}
				else {
	    			logger.error("Missing fast load directory: "+fastLoadDir);
				}
    		}
    		
    		File archiveDir = new File(dir.getParentFile(), "archive");
    		if (archiveDir.exists()) {
    			archivePaths.add(archiveDir.getAbsolutePath());
    		}
        	else {
				if (fastLoadPaths.size()==1) {
					throw new MissingGridResultException(file.getAbsolutePath(), "No archive directory was generated at "+archiveDir.getAbsolutePath());
				}
				else {
	    			logger.error("No archive directory was generated at "+archiveDir.getAbsolutePath());
				}
        	}

    		if (!dir.exists()) {
				if (fastLoadPaths.size()==1) {
					throw new MissingGridResultException(file.getAbsolutePath(), "Missing fast load directory: "+fastLoadDir);
				}
				else {
	    			logger.error("Missing fast load directory: "+fastLoadDir);
				}
    		}
    		
    		int numFiles = dir.list().length;
    		if (numFiles != NUM_ARTIFACTS) {
    			try {
    				FileUtils.deleteDirectory(dir);
    				FileUtils.deleteDirectory(archiveDir);
    				if (fastLoadPaths.size()==1) {
    					throw new MissingGridResultException(file.getAbsolutePath(), "Fast load directory has "+numFiles+" files. Expected "+NUM_ARTIFACTS+".");
    				}
    				else {
    	    			logger.error("Fast load directory has "+numFiles+" files. Expected "+NUM_ARTIFACTS+".");
    				}
    			}
    			catch (IOException e) {
    				logger.error("Error deleting fast load dir: "+e.getMessage());
    			}
    		}
    	}

    	processData.putItem("ARCHIVE_FILE_PATHS", archivePaths);
    	if (archivePaths.size()==1) {
    		processData.putItem("ARCHIVE_FILE_PATH", archivePaths.get(0));
    	}
	}
}