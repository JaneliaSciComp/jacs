package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;

/**
 * Run neuron separator on some input files and generate a bunch of stuff. Parameters:
 *   INPUT_FILENAME - input file
 *   RESULT_FILE_NODE - node in which to run the grid job
 *   OUTPUT_FILE_NODE - node to place the results (defaults to RESULT_FILE_NODE)
 *   PREVIOUS_RESULT_FILENAME - name of the previous result (*.nsp file) to map fragments against
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FastLoadArtifactPipelineGridService extends SubmitDrmaaJobService {
	
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private List<String> inputPaths;
    
    @Override
    protected String getGridServicePrefixName() {
        return "fastLoad";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        inputPaths = (List<String>)processData.getItem("INPUT_PATH_LIST");
        if (inputPaths==null) {
        	throw new ServiceException("Input parameter INPUT_PATH_LIST may not be empty");
        }
    	
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
    	// Reserve all 4 slots on a node. This gives us 12 GB of memory. 
    	jt.setNativeSpecification("-pe batch 4");
    	return jt;
    }
    
    @Override
	public void postProcess() throws MissingDataException {
    	for(String inputDir : inputPaths) {
    		File fastLoadDir = new File(inputDir,"fastLoad");
    		if (fastLoadDir.exists()) {
    			throw new MissingDataException("Missing fast load directory: "+fastLoadDir);
    		}
    	}
	}
}