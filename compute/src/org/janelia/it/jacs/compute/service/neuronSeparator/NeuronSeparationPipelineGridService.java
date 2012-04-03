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
	
    private static final String CONFIG_PREFIX = "neuSepConfiguration.";
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private FileNode outputFileNode;
    private String inputFilename;
    private String previousResultFile;
    
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
    	
        previousResultFile = (String)processData.getItem("PREVIOUS_RESULT_FILENAME");
        
        logger.info("Starting NeuronSeparationPipelineService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath()+
                " workingDir="+outputFileNode.getDirectoryPath() + " inputFilename="+inputFilename);
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
            fw.write("neuronSeparatorPipeline\n");
            fw.write(inputFilename + "\n");
            fw.write(previousResultFile==null?"":previousResultFile + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read NAME\n");
        script.append("read INPUT_FILE\n");
        script.append("read PREVIOUS_OUTPUT\n");
        script.append(NeuronSeparatorHelper.getNeuronSeparationCommands());
        script.append("\n");
        writer.write(script.toString());
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	// Reserve all 8 slots on 96 GB node. 
    	jt.setNativeSpecification("-pe batch 8 -l mem96=true");
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