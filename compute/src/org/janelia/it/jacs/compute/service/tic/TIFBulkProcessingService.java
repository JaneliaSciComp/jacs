package org.janelia.it.jacs.compute.service.tic;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.tic.BatchTicTask;
import org.janelia.it.jacs.model.tasks.tic.TicTask;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 4/10/12
 * Time: 2:20 PM
 * This class looks for all the tif files provided in the inputFile path
 */
public class TIFBulkProcessingService implements IService {
    private BatchTicTask task;


    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (BatchTicTask) ProcessDataHelper.getTask(processData);
            String inputFilePath = task.getParameter(TicTask.PARAM_inputFile);
            List<String> tmpFiles;
            List<String> finalFileList = new ArrayList<String>();
            if (null!=inputFilePath) {
                tmpFiles = Task.listOfStringsFromCsvString(inputFilePath);
                for (String tmpFilePath : tmpFiles) {
                    File tmpFile = new File(tmpFilePath);
                    if (tmpFile.isDirectory()) {
                        File[] tmpTifFiles = tmpFile.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File file, String accession) {
                                return accession.toLowerCase().contains(".tif");
                            }
                        });
                        for (File tmpTifFile : tmpTifFiles) {
                            finalFileList.add(tmpTifFile.getAbsolutePath());
                        }
                    }
                    else {
                        if (tmpFile.getName().toLowerCase().contains(".tif")&&tmpFile.exists()) {finalFileList.add(tmpFile.getAbsolutePath());}
                    }
                }
            }

            for (String tmpInputFile : finalFileList) {
                logger.debug("Starting TIC analysis of input file: " + tmpInputFile);
                // Submit the task
                try {
                    TicTask _currentTask = new TicTask();
                    _currentTask.setJobName(new File(tmpInputFile).getName());
                    _currentTask.setOwner(task.getOwner());
                    // Copy all the old parameters
                    for (TaskParameter taskParameter : task.getTaskParameterSet()) {
                        _currentTask.setParameter(taskParameter.getName(),taskParameter.getValue());
                    }
                    // Override the input for the specific instance
                    _currentTask.setParameter(task.PARAM_inputFile,tmpInputFile);

                    // Save the child task
                    _currentTask = (TicTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(_currentTask);
                    // Submit
                    EJBFactory.getRemoteComputeBean().submitJob("TranscriptionImagingConsortium", _currentTask.getObjectId());
                }
                catch (Exception e) {
                    logger.error("There was an error running the TIC pipeline for: " + tmpInputFile, e);
                    throw new ServiceException("There was an error running the TIC pipeline for: " + tmpInputFile);
                }
            }
        }
        catch (Exception e) {
            if (e instanceof ServiceException) throw (ServiceException)e;
            else {
                throw new ServiceException("There was an error running the TIC pipeline for task ="+task.getObjectId(),e);
            }
        }
    }
}
