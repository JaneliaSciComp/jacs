package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.SubmitJobAndWaitHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.colorSeparator.ColorSeparatorTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorTask;
import org.janelia.it.jacs.model.user_data.colorSeparator.ColorSeparatorResultNode;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class NeuronSeparationPipelineService implements IService {
    private NeuronSeparatorPipelineTask task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            logger.debug("\n\nExecuting Neuron Separation...\n\n");
            NeuronSeparatorResultNode parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);

            // todo this should be a separate process running on the grid
            String cmdLine = "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64:" +
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"genelib/mylib;"+
                    SystemConfigurationProperties.getString("Executables.ModuleBase")+"singleNeuronTools/genelib/mylib/sampsepNA "+
                    parentNode.getDirectoryPath()+" neuronSeparatorPipeline "+ addQuotesToCsvString(task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList));
            logger.info("NeuronSeparatorPipelineTask cmdLine="+cmdLine);
            SystemCall call = new SystemCall(logger);
            int exitCode = call.emulateCommandLine(cmdLine, true);
            if (0!=exitCode) {
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

    String addQuotesToCsvString(String csvString) {
        String[] clist=csvString.split(",");
        StringBuffer sb=new StringBuffer();
        for (int i=0;i<clist.length;i++) {
            sb.append("\"");
            sb.append(clist[i].trim());
            sb.append("\"");
            if (i<clist.length-1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

}