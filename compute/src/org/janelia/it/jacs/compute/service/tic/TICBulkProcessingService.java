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
import org.janelia.it.jacs.model.tasks.tic.SingleTicTask;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 4/10/12
 * Time: 2:20 PM
 * This class looks for all the tif files provided in the inputFile path
 */
public class TICBulkProcessingService implements IService {
    private BatchTicTask task;
    private Logger logger;

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (BatchTicTask) ProcessDataHelper.getTask(processData);
            String inputFilePath = task.getParameter(BatchTicTask.PARAM_inputFile);
            HashMap<String, List<String>> relatedFileMap = new HashMap<String, List<String>>();
            List<String> tmpFiles;
            if (null!=inputFilePath) {
                tmpFiles = Task.listOfStringsFromCsvString(inputFilePath);
                for (String tmpFilePath : tmpFiles) {
                    File tmpFile = new File(tmpFilePath);
                    if (tmpFile.isDirectory() && !tmpFilePath.contains("PipelineResults")) {
                        File[] tmpTifFiles = tmpFile.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File file, String accession) {
                                return accession.toLowerCase().contains(".tif");
                            }
                        });
                        for (File tmpTifFile : tmpTifFiles) {
                            addFileToMap(tmpTifFile, relatedFileMap);
                        }
                    }
                    else {
                        if (tmpFile.getName().toLowerCase().contains(".tif")&&tmpFile.exists()) {
                            addFileToMap(tmpFile,relatedFileMap);
                        }
                    }
                }
            }

            for (String key : relatedFileMap.keySet()) {
                logger.debug("Starting TIC analysis of input file: " + key);
                // Submit the task
                try {
                    SingleTicTask _currentTask = new SingleTicTask();
                    _currentTask.setJobName(new File(key).getName());
                    _currentTask.setOwner(task.getOwner());
                    // Copy all the old parameters
                    for (TaskParameter taskParameter : task.getTaskParameterSet()) {
                        _currentTask.setParameter(taskParameter.getName(),taskParameter.getValue());
                    }

                    // Override the input for the specific instance - only need an example of the file
                    boolean runningCalibration = (null!=task.getParameter(BatchTicTask.PARAM_runApplyCalibrationToFrame) && Boolean.valueOf(task.getParameter(BatchTicTask.PARAM_runApplyCalibrationToFrame)));
                    boolean runningCorrection  = (null!=task.getParameter(BatchTicTask.PARAM_runIlluminationCorrection) && Boolean.valueOf(task.getParameter(BatchTicTask.PARAM_runIlluminationCorrection)));

                    String testPath = relatedFileMap.get(key).get(0);
                    File tmpParent = new File(testPath).getParentFile();
                    _currentTask.setParameter(SingleTicTask.PARAM_inputFilePrefix,relatedFileMap.get(key).get(0));
                    if (!runningCalibration && !runningCorrection) {
                        _currentTask.setParameter(SingleTicTask.PARAM_inputFilePrefix,inputFilePath);
                    }
                    File tmpInputFile = new File(testPath);
                    final String targetPrefix = TICHelper.getTargetPrefix(tmpInputFile.getName());
                    if (null==targetPrefix) {
                        throw new ServiceException("TICBulkProcessingService - Cannot successfully determine prefix for files (example: "+tmpInputFile.getName()+")");
                    }
                    _currentTask.setParameter(SingleTicTask.PARAM_finalOutputLocation, tmpParent.getAbsolutePath()+
                            File.separator+"PipelineResults-"+task.getObjectId().toString()+"-"+targetPrefix);
                    // Save the child task
                    _currentTask = (SingleTicTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(_currentTask);
                    // Submit
                    EJBFactory.getRemoteComputeBean().submitJob("TranscriptionImagingConsortium", _currentTask.getObjectId());
                }
                catch (Exception e) {
                    logger.error("There was an error running the TIC pipeline for: " + key, e);
                    throw new ServiceException("There was an error running the TIC pipeline for: " + key);
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


    /**
     * The contract here is that the file will look like 001_a_red.nd2_99_RC.tif where the 99 can be any integer 0..whatever
     * Find everything up to the 99 and make that a unique prefix.  We'll group all files with the same prefix and submit
     * as a single grid job.  We also need to check for the file format of xxx_t0001.tif.
     * @param targetItem file to test in the collection
     * @param targetMap collection of all file prefix and the list of all file paths with that prefix
     */
    private void addFileToMap(File targetItem, HashMap<String, List<String>> targetMap) {
        String targetPrefix = TICHelper.getTargetPrefix(targetItem.getName());
        if (null==targetPrefix) {
            logger.warn("TICBulkProcessingService - Provided file name does not match either expected pattern: " + targetItem.getName());
            return;
        }

        if (!targetMap.containsKey(targetPrefix)) {
            ArrayList<String> targetFilePaths = new ArrayList<String>();
            targetFilePaths.add(targetItem.getAbsolutePath());
            targetMap.put(targetPrefix, targetFilePaths);
        }
        else {
            ArrayList<String> targetFilePaths = (ArrayList<String>)targetMap.get(targetPrefix);
            if (!targetFilePaths.contains(targetItem.getAbsolutePath())) {
                targetFilePaths.add(targetItem.getAbsolutePath());
                targetMap.put(targetPrefix,targetFilePaths);
            }
        }
    }

}
