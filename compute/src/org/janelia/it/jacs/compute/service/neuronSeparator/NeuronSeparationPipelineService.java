package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.colorSeparator.ColorSeparatorTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;

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
    private Logger logger;
    private NeuronSeparatorPipelineTask task;
    private ComputeBeanRemote computeBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (NeuronSeparatorPipelineTask) ProcessDataHelper.getTask(processData);
            computeBean = EJBFactory.getRemoteComputeBean();
            logger.debug("\n\nExecuting Neuron Separation...\n\n");

            // Run the Neuron Separation...
            NeuronSeparatorTask neuSepTask = new NeuronSeparatorTask();
            neuSepTask.setOwner(task.getOwner());
            neuSepTask.setParentTaskId(task.getObjectId());
            neuSepTask.setJobName("Neuron Separation");
            computeBean.addEventToTask(task.getObjectId(), new Event("Running Neuron Separation Step", new Date(), "Neuron Separation"));
            neuSepTask = (NeuronSeparatorTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(neuSepTask);
            EJBFactory.getLocalComputeBean().submitJob("NeuronSeparation", neuSepTask.getObjectId());
            waitForTask(neuSepTask.getObjectId());
            computeBean.addEventToTask(task.getObjectId(), new Event("Completed Step:" + neuSepTask.getDisplayName(), new Date(), neuSepTask.getDisplayName()));
            logger.debug("\n\nNeuron Separation executed successfully.\n\n");

            // Run the Color Separation...
            ColorSeparatorTask colorSepTask = new ColorSeparatorTask();
            colorSepTask.setOwner(task.getOwner());
            colorSepTask.setParentTaskId(task.getObjectId());
            colorSepTask.setJobName("Color Separation");
            // Get the output from the previous step and use as input here
            NeuronSeparatorResultNode tmpNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
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
            computeBean.addEventToTask(task.getObjectId(), new Event("Running Color Separation Step", new Date(), "Color Separation"));
            colorSepTask = (ColorSeparatorTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(colorSepTask);
            EJBFactory.getLocalComputeBean().submitJob("ColorSeparation", colorSepTask.getObjectId());
            waitForTask(colorSepTask.getObjectId());
            computeBean.addEventToTask(task.getObjectId(), new Event("Completed Step:" + colorSepTask.getDisplayName(), new Date(), colorSepTask.getDisplayName()));
            logger.debug("\n\nColor Separation executed successfully.\n\n");

            // Run the Consolidator Tool...
//            String cmdLine = "./v3d64 -cmd consolidate-color-separator-output "+inputDirPath + outputFile;
//            SystemCall
//
        }
        catch (Exception e) {
            try {
                computeBean.addEventToTask(task.getObjectId(), new Event("ERROR running the Neuron Separation Pipeline:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                System.err.println("Error trying to log the error message.");
            }
            throw new ServiceException("Error running the Neuron Separation NeuronSeparationPipelineService:" + e.getMessage(), e);
        }
    }

    protected void waitForTask(Long taskId) throws Exception {
        String[] taskStatus = null;
        while (taskStatus == null || !Task.isDone(taskStatus[0])) {
            taskStatus = computeBean.getTaskStatus(taskId);
            Thread.sleep(5000);
        }
        if (!taskStatus[0].equals(Event.COMPLETED_EVENT)) {
            throw new Exception("Task " + taskId + " finished with non-complete status=" + taskStatus[0]);
        }
    }

}