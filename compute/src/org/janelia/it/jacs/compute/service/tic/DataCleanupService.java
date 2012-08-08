package org.janelia.it.jacs.compute.service.tic;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tic.SingleTicTask;
import org.janelia.it.jacs.model.tasks.tic.TicTask;
import org.janelia.it.jacs.model.user_data.tic.TICResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * @author Todd Safford
 */
public class DataCleanupService implements IService {

    private Logger _logger;
    private Task task;
    private TICResultNode resultFileNode;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            _logger = ProcessDataHelper.getLoggerForTask(processData, DataCleanupService.class);
            this.task = ProcessDataHelper.getTask(processData);
            resultFileNode = (TICResultNode)ProcessDataHelper.getResultFileNode(processData);

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
                    FileUtil.ensureDirExists(tmpDestination);
                    FileUtil.copyFile(finalSpotFiles[0].getAbsolutePath(), tmpDestination + File.separator + finalSpotFiles[0].getName());
                }
            }
            // else regroup the broken apart files into Reconstructed and corrected dirs like it was a single submission to the grid
            else {
                try {
                    FileUtil.copyDirectory(resultFileNode.getDirectoryPath(), tmpDestination);
                }
                catch (IOException e) {
                    throw new ServiceException("Could not copy the output files to the final destination (from " +
                            resultFileNode.getDirectoryPath() + " to " + tmpDestination);
                }
            }
            SystemCall call = new SystemCall(_logger);
            call.emulateCommandLine("chmod -R ug+rwx "+tmpDestination,true);
       }
       catch (Exception e) {
         throw new ServiceException(e);
       }
    }

}