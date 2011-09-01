package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

public class NeuronSeparationPipelineRemoteService implements IService {

    private static final int TIMEOUT_SECONDS = 7200;  // 2 hours
    private static final String REMOTE_SERVER = SystemConfigurationProperties.getString("Remote.Work.Server.Mac");
    private static final String REMOTE_SCRIPT = "runsep.mac.sh";
    
    private Logger logger;
    private NeuronSeparatorPipelineTask task;
    private NeuronSeparatorResultNode parentNode;

    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);

            logger.info("Starting NeuronSeparationPipelineRemoteService with taskId="+task.getObjectId()+" resultNodeId="+parentNode.getObjectId()+" resultDir="+parentNode.getDirectoryPath());
            
            String script = NeuronSeparatorHelper.getNeuronSeparationCommands(task, parentNode, "mylib.mac", " ; ");
        	File scriptFile = new File(parentNode.getDirectoryPath(), REMOTE_SCRIPT);
        	FileUtils.writeStringToFile(scriptFile, NeuronSeparatorHelper.covertPathsToRemoteServer(script));
            
            String cmdLine = "ssh "+REMOTE_SERVER+" sh "+NeuronSeparatorHelper.covertPathsToRemoteServer(scriptFile.getAbsolutePath());
            
            StringBuffer stdout = new StringBuffer();
            StringBuffer stderr = new StringBuffer();
            SystemCall call = new SystemCall(stdout, stderr);

            int exitCode = call.emulateCommandLine(cmdLine.toString(), true, TIMEOUT_SECONDS);

        	File outFile = new File(parentNode.getDirectoryPath(), "stdout");
        	if (stdout.length() > 0) FileUtils.writeStringToFile(outFile, stdout.toString());

            File errFile = new File(parentNode.getDirectoryPath(), "stderr");

            if (stderr.length() > 0) FileUtils.writeStringToFile(errFile, stderr.toString());
            
            if (0!=exitCode) {
                File exitCodeFile = new File(parentNode.getDirectoryPath(), "neuSepExitCode.txt");
                FileUtils.writeStringToFile(exitCodeFile, ""+exitCode);
            	throw new ServiceException("NeuronSeparationPipelineRemoteService failed with exitCode "+exitCode+" for resultDir="+parentNode.getDirectoryPath());
            }
        }
        catch (Exception e) {
            try {
                EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event("ERROR running the Neuron Separation Pipeline:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                System.err.println("Error trying to log the error message.");
            }
            if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running the Neuron Separation NeuronSeparationPipelineService:" + e.getMessage(), e);
        }
    }

}