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
            // Run the Neuron Separation...
            NeuronSeparatorTask neuSepTask = new NeuronSeparatorTask();
            neuSepTask.setOwner(task.getOwner());
            neuSepTask.setParentTaskId(task.getObjectId());
            neuSepTask.setJobName("Neuron Separation");
            neuSepTask.setParameter(NeuronSeparatorTask.PARAM_inputFilePath, task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputFilePath));
            EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event("Running Neuron Separation Step", new Date(), "Neuron Separation"));
            neuSepTask = (NeuronSeparatorTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(neuSepTask);
            SubmitJobAndWaitHelper jobHelper = new SubmitJobAndWaitHelper("NeuronSeparation", neuSepTask.getObjectId());
            jobHelper.startAndWaitTillDone();
            EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event("Completed Step:" + neuSepTask.getDisplayName(), new Date(), neuSepTask.getDisplayName()));
            logger.debug("\n\nNeuron Separation executed successfully.\n\n");

            // Run the Color Separation...
            ColorSeparatorTask colorSepTask = new ColorSeparatorTask();
            colorSepTask.setOwner(task.getOwner());
            colorSepTask.setParentTaskId(task.getObjectId());
            colorSepTask.setJobName("Color Separation");
            // Get the output from the previous step and use as input here
            NeuronSeparatorResultNode tmpNode = (NeuronSeparatorResultNode) EJBFactory.getRemoteComputeBean().getResultNodeByTaskId(neuSepTask.getObjectId());
            String segDirPath = tmpNode.getFilePathByTag(NeuronSeparatorResultNode.TAG_SEGMENTATION_PATH);
            File tmpFile = new File(segDirPath);
            File[] tmpFiles = tmpFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String name) {
                    return (name.indexOf(".seg_")>0);
                }
            });
            StringBuffer sbuf = new StringBuffer();
            for (int i = 0; i < tmpFiles.length; i++) {
                File file = tmpFiles[i];
                sbuf.append(file.getAbsolutePath());
                if ((i+1)<tmpFiles.length) { sbuf.append(","); }
            }
            colorSepTask.setParameter(ColorSeparatorTask.PARAM_inputFileList, sbuf.toString());
            EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event("Running Color Separation Step", new Date(), "Color Separation"));
            colorSepTask = (ColorSeparatorTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(colorSepTask);
            SubmitJobAndWaitHelper colorJobHelper = new SubmitJobAndWaitHelper("ColorSeparation", colorSepTask.getObjectId());
            colorJobHelper.startAndWaitTillDone();
            EJBFactory.getRemoteComputeBean().addEventToTask(task.getObjectId(), new Event("Completed Step:" + colorSepTask.getDisplayName(), new Date(), colorSepTask.getDisplayName()));
            logger.debug("\n\nColor Separation executed successfully.\n\n");

            // Run the Consolidator Tool...
            ColorSeparatorResultNode colorNode = (ColorSeparatorResultNode) EJBFactory.getRemoteComputeBean().getResultNodeByTaskId(colorSepTask.getObjectId());
            // todo this should be a separate process running on the grid
            String cmdLine = "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib64:/groups/scicomp/jacsData/servers/saffordt-ws1/executables/v3d/v3d_main/common_lib/lib/;"+SystemConfigurationProperties.getString("Executables.ModuleBase")+"/v3d/v3d/v3d -cmd consolidate-color-separator-output "
                    +colorNode.getDirectoryPath()+" "+parentNode.getDirectoryPath()+File.separator+"final.mask";
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

}