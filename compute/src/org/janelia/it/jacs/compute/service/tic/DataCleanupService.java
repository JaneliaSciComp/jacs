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
import java.util.Scanner;

/**
 * @author Todd Safford
 */
public class DataCleanupService implements IService {

    public static final String SPOT_FILE_NAME = "spotFiles.txt";
    public static final int POSX_INDEX=1;
    public static final int POSY_INDEX=2;
    public static final int POSZ_INDEX=3;
    public static final int SIGX_INDEX=7;
    public static final int SIGY_INDEX=8;
    public static final int SIGZ_INDEX=9;
    private Logger _logger;
    private Task task;
    private TICResultNode resultFileNode;
    private String sessionName;
    private int thrownOutCounter;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            _logger = ProcessDataHelper.getLoggerForTask(processData, DataCleanupService.class);
            this.task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
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
            // todo This is unnecessary and the grid slots should position these results automatically
            else {
                File mainDir = new File(resultFileNode.getDirectoryPath());
                File reconDir = FileUtil.ensureDirExists(resultFileNode.getDirectoryPath()+File.separator+"Reconstructed");
                File correctDir = FileUtil.ensureDirExists(resultFileNode.getDirectoryPath()+File.separator+"Reconstructed"+File.separator+"corrected");
                Scanner scanner = new Scanner(new File(resultFileNode.getDirectoryPath()+ File.separator+SPOT_FILE_NAME));
                while (scanner.hasNextLine()) {
                    File tmpBaseDir = new File(scanner.nextLine()).getParentFile();
                    File tmpReconDir = new File(tmpBaseDir+File.separator+"Reconstructed");
                    File tmpCorrectedDir = new File(tmpReconDir.getAbsolutePath()+File.separator+"corrected");
                    FileUtil.moveOnlyFiles(tmpBaseDir, mainDir);
                    FileUtil.moveOnlyFiles(tmpReconDir, reconDir);
                    FileUtil.moveOnlyFiles(tmpCorrectedDir, correctDir);
                    FileUtil.deleteDirectory(tmpBaseDir);
                }
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