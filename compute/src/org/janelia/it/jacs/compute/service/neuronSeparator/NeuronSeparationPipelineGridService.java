package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.vo.ParameterException;

/**
 * Run neuron separator on some input files and generate a bunch of stuff. Parameters:
 *   INPUT_FILENAME - input file
 *   RESULT_FILE_NODE - node in which to run the grid job
 *   OUTPUT_FILE_NODE - node to place the results
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronSeparationPipelineGridService extends SubmitDrmaaJobService {
	
    private static final String CONFIG_PREFIX = "neuSepConfiguration.";
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private FileNode outputFileNode;
    
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
        
        String inputFilename = (String)processData.getItem("INPUT_FILENAME");
        if (inputFilename==null) {
        	throw new ServiceException("Input parameter INPUT_FILENAME may not be null");
        }
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the Neuron Separation pipeline.");
        }
        
        createShellScript(writer, outputFileNode);
        setJobIncrementStop(1);
    }

    private void createShellScript(FileWriter writer, FileNode outputFileNode)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {
    	
        logger.info("Starting NeuronSeparationPipelineService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath());

        String script = NeuronSeparatorHelper.getNeuronSeparationCommands(task, processData, outputFileNode, "mylib.fedora", "\n");
        StringBuffer wrapper = new StringBuffer();
        wrapper.append("set -o errexit\n");
        wrapper.append(script).append("\n");
        writer.write(wrapper.toString());
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve 7 out of the 8 slots on a node. This gives us 21 GB of memory. 
    	jt.setNativeSpecification("-pe batch 7");
    	return jt;
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
	            return name.startsWith("Consolidated");
			}
		});

    	if (csFiles.length < 2) {
    		throw new MissingDataException("ConsolidatedSignal and ConsolidatedLabel not found for "+resultFileNode.getDirectoryPath());
    	}
	}
}