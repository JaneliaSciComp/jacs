package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

public class NeuronSeparationPipelineLocalService implements IService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private Logger logger;
    private NeuronSeparatorPipelineTask task;
    private NeuronSeparatorResultNode parentNode;

    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);

            logger.info("Starting NeuronSeparationPipelineService with taskId="+task.getObjectId()+" resultNodeId="+parentNode.getObjectId()+" resultDir="+parentNode.getDirectoryPath());

            String fileList = NeuronSeparatorHelper.getFileListString(task);
            String[] lsmFilePaths = NeuronSeparatorHelper.getLSMFilePaths(task);
        	String lsmFilePathsFilename = parentNode.getDirectoryPath()+"/"+"lsmFilePaths.txt";
        	
            StringBuffer cmdLine = new StringBuffer();
            cmdLine.append("cd "+parentNode.getDirectoryPath()+";export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64:" +
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"singleNeuronTools/genelib/mylib.fedora;"+
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"singleNeuronTools/genelib/mylib.fedora/sampsepNA -nr -pj "+
                    parentNode.getDirectoryPath()+" neuronSeparatorPipeline "+ NeuronSeparatorHelper.addQuotesToCsvString(fileList) + " >neuSepOutput.txt 2>&1 ").append(" ; ");
        	cmdLine.append("echo '"+lsmFilePaths[0]+"' > "+lsmFilePathsFilename).append(" ; ");
            cmdLine.append("echo '"+lsmFilePaths[1]+"' >> "+lsmFilePathsFilename).append(" ; ");
            cmdLine.append(NeuronSeparatorHelper.getScriptToCreateLsmMetadataFile(parentNode, lsmFilePaths[0])).append(" ; ");
            cmdLine.append(NeuronSeparatorHelper.getScriptToCreateLsmMetadataFile(parentNode, lsmFilePaths[1])).append(" ; ");
            
            logger.info("NeuronSeparatorPipelineTask cmdLine="+cmdLine);
            
            SystemCall call = new SystemCall(logger);
            int exitCode = call.emulateCommandLine(cmdLine.toString(), true, TIMEOUT_SECONDS);
            if (0!=exitCode) {
                File errFile = new File(parentNode.getDirectoryPath(), "error");
            	try {
	                errFile.createNewFile();
            	} 
            	catch (Exception e) {
            		logger.warn("Could not create error file: "+errFile.getAbsolutePath(),e);
            	}
                throw new ServiceException("The NeuronSeparationPipelineService consolidator step did not exit properly.");
            }
        }
        catch (Exception e) {
            try {
                EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event("ERROR running the Neuron Separation Pipeline:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                System.err.println("Error trying to log the error message.");
            }
            throw new ServiceException("Error running the Neuron Separation NeuronSeparationPipelineService:" + e.getMessage(), e);
        }
    }

}