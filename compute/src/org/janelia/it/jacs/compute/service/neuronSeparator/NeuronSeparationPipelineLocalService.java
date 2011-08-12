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
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

public class NeuronSeparationPipelineLocalService implements IService {

    private static final int TIMEOUT_SECONDS = 7200;  // 2 hours

    private Logger logger;
    private NeuronSeparatorPipelineTask task;
    private NeuronSeparatorResultNode parentNode;

    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);

            logger.info("Starting NeuronSeparationPipelineLocalService with taskId="+task.getObjectId()+" resultNodeId="+parentNode.getObjectId()+" resultDir="+parentNode.getDirectoryPath());

            NeuronSeparatorHelper.deleteExistingNeuronSeparationResult(task);
            
            String cmdLine = NeuronSeparatorHelper.getNeuronSeparationCommands(task, parentNode, "mylib.fedora", " ; ");

            StringBuffer stdout = new StringBuffer();
            StringBuffer stderr = new StringBuffer();
            SystemCall call = new SystemCall(logger, stdout, stderr);
            int exitCode = call.emulateCommandLine(cmdLine.toString(), true, TIMEOUT_SECONDS);

        	File outFile = new File(parentNode.getDirectoryPath(), "stdout");
        	FileUtils.writeStringToFile(outFile, stdout.toString());
            
            if (0!=exitCode) {
                File errFile = new File(parentNode.getDirectoryPath(), "stderr");
                FileUtils.writeStringToFile(errFile, stderr.toString());
            	throw new ServiceException("NeuronSeparationPipelineLocalService failed with exitCode "+exitCode+" for resultDir="+parentNode.getDirectoryPath());
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