package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;

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
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(NeuronSeparatorHelper.getFastLoadCommands() + "\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        writer.write(script.toString());
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve all 2 slots on a node. This gives us 6 GB of memory. 
    	jt.setNativeSpecification("-pe batch 2 ");
    	return jt;
    }
    
    @Override
	public void postProcess() throws MissingDataException {
    	
    	List<String> archivePaths = new ArrayList<String>();
    	
    	for(String fastLoadDir : fastLoadPaths) {
    		
    		File file = new File(fastLoadDir);
    		if (!file.exists()) {
    			throw new MissingDataException("Missing fast load directory: "+fastLoadDir);
    		}
    		
    		File archiveDir = new File(file.getParentFile(), "archive");
    		if (archiveDir.exists()) {
    			archivePaths.add(archiveDir.getAbsolutePath());
    		}
        	else {
        		logger.warn("No archive directory was generated at "+archiveDir.getAbsolutePath());
        	}
    	}

    	processData.putItem("ARCHIVE_FILE_PATHS", archivePaths);
    	if (archivePaths.size()==1) {
    		processData.putItem("ARCHIVE_FILE_PATH", archivePaths.get(0));
    	}
	}
}