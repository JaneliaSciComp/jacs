package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class NeuronSeparationPipelineService extends SubmitDrmaaJobService {

	// Reserve 5 out of the 6 slots on a node. This gives us 20 GB of memory, hopefully enough for one lousy neuron separation. 
	private static final int NUM_GRID_SLOTS = 5;
	
    private static final String CONFIG_PREFIX = "neuSepConfiguration.";
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String REMOTE_SCRIPT = "runsep.fedora.sh";
    private static final String STDOUT_FILE = "runsep.out";
    private static final String STDERR_FILE = "runsep.err";

    @Override
    protected String getGridServicePrefixName() {
        return "neuSep";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the Neuron Separation pipeline.");
        }

        createShellScript(writer);
        setJobIncrementStop(1);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {
    	
    	NeuronSeparatorResultNode parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
    	Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        
        logger.info("Starting NeuronSeparationPipelineService with taskId=" + task.getObjectId() + " resultNodeId=" + parentNode.getObjectId() + " resultDir=" + parentNode.getDirectoryPath());

        String script = NeuronSeparatorHelper.getNeuronSeparationCommands((NeuronSeparatorPipelineTask)task, parentNode, "mylib.fedora", " ; ");
    	File scriptFile = new File(parentNode.getDirectoryPath(), REMOTE_SCRIPT);
    	FileUtils.writeStringToFile(scriptFile, script);

    	File outFile = new File(parentNode.getDirectoryPath(), STDOUT_FILE);
        File errFile = new File(parentNode.getDirectoryPath(), STDERR_FILE);
        // Need to use bash to get process substitution for the tricksy tee stuff to work. It is explained here:
        // http://stackoverflow.com/questions/692000/how-do-i-write-stderr-to-a-file-while-using-tee-with-a-pipe
        // The reason we want both outputs is because we want to match the "runsep.out" contract of the other neuron
        // separator services, but we also want to give the STDERR stream to Drmaa so that it knows about errors.
        String cmdLine = "bash "+ scriptFile.getAbsolutePath() +
        	" > >(tee "+outFile.getAbsolutePath()+") 2> >(tee "+errFile.getAbsolutePath()+" >&2)";

        StringBuffer wrapper = new StringBuffer();
        wrapper.append("set -o errexit\n");
        wrapper.append(cmdLine).append("\n");
        writer.write(wrapper.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
    	SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
    	jt.setNativeSpecification("-pe batch "+NUM_GRID_SLOTS);
    	return jt;
    }
    
    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
}