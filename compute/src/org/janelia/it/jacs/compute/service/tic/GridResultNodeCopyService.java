package org.janelia.it.jacs.compute.service.tic;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.tasks.tic.SingleTicTask;
import org.janelia.it.jacs.model.tasks.tic.TicTask;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;

public class GridResultNodeCopyService extends SubmitDrmaaJobService {
    private static final String CONFIG_PREFIX = "nodeCopyConfiguration.";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "nodeCopy";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, DataCleanupService.class);
            this.task = ProcessDataHelper.getTask(processData);
            resultFileNode = ProcessDataHelper.getResultFileNode(processData);

            File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
            if (!configFile.createNewFile()) {
                throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath());
            }
            setJobIncrementStop(1);

            // There should be only one final spots file
            File[] finalSpotFiles = new File(resultFileNode.getDirectoryPath()).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return (name.startsWith("FISH_QUANT_")&&name.endsWith("_final_spots.txt"));
                }
            });
            // If the user only wants the spots file scuttle the dir copy and give them only the spots
            String tmpDestination = task.getParameter(TicTask.PARAM_finalOutputLocation);
            if (null!=task.getParameter(SingleTicTask.PARAM_spotDataOnly)&&Boolean.valueOf(task.getParameter(SingleTicTask.PARAM_spotDataOnly))){
                if (null!=tmpDestination) {
                    writer.write("mkdir "+tmpDestination+"\n");
                    writer.write("cp "+finalSpotFiles[0].getAbsolutePath()+" " + tmpDestination + File.separator + finalSpotFiles[0].getName()+"\n");
                }
                else {
                    logger.error("GridResultNodeCopyService - Cannot copy files as the destination was not defined.");
                }
            }
            // else regroup the broken apart files into Reconstructed and corrected dirs like it was a single submission to the grid
            else {
                writer.write("cp -R "+resultFileNode.getDirectoryPath()+" " +tmpDestination+"\n");
            }
            writer.write("chmod -R ug+rwx "+tmpDestination+"\n");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + NORMAL_QUEUE);
        jt.setNativeSpecification(NORMAL_QUEUE);
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 3;
    }
    
    @Override
    protected boolean isShortPipelineJob() {
    	return true;
    }

}