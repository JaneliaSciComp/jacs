
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationBulkLoadGenomeDataTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationLoadGenomeDataTask;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class BulkLoadNcbiGenomeSetupService implements IService {
    private Logger logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            ProkaryoticAnnotationBulkLoadGenomeDataTask task = (ProkaryoticAnnotationBulkLoadGenomeDataTask) ProcessDataHelper.getTask(processData);
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            String annotationDir = SystemConfigurationProperties.getString("ProkAnnotation.BaseDir");
            String NCBI_FTP_BASE_DIR = SystemConfigurationProperties.getString("ProkAnnotation.NcbiBaseDir", null);
            String tmpDirectoryName = SystemConfigurationProperties.getString("Upload.ScratchDir");
            File listFile = new File(tmpDirectoryName + File.separator + task.getParameter(ProkaryoticAnnotationBulkLoadGenomeDataTask.PARAM_genomeListFile));
            Scanner scanner = new Scanner(listFile);
            while (scanner.hasNextLine()) {
                try {
                    String tmpNext = scanner.nextLine();
                    if (null == tmpNext || tmpNext.indexOf("\t") < 0) {
                        break;
                    }
                    String[] pieces = tmpNext.trim().split("\t");
                    String tmpGenomeDir = pieces[0].trim().toUpperCase();
                    String tmpNcbiFtpLocation = pieces[1].trim();
                    // Create and save the loader task
                    ProkaryoticAnnotationLoadGenomeDataTask _currentTask = new ProkaryoticAnnotationLoadGenomeDataTask();
                    _currentTask.setJobName(tmpGenomeDir);
                    _currentTask.setOwner(task.getOwner());
                    _currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_ftpSourceDirectory, NCBI_FTP_BASE_DIR + tmpNcbiFtpLocation);
                    _currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_targetDirectory, annotationDir + "/" + tmpGenomeDir != null ? (annotationDir + "/" + tmpGenomeDir) : null);
                    _currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_username, task.getParameter(ProkaryoticAnnotationBulkLoadGenomeDataTask.PARAM_username));
                    _currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_sybasePassword, task.getParameter(ProkaryoticAnnotationBulkLoadGenomeDataTask.PARAM_sybasePassword));
                    _currentTask.getFirstEvent().setTimestamp(new Date());
                    logger.debug("Starting load of genome: " + tmpGenomeDir);
                    // Submit the loader task
                    try {
                        _currentTask = (ProkaryoticAnnotationLoadGenomeDataTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(_currentTask);
                        EJBFactory.getRemoteComputeBean().submitJob("ProkAnnotationLoadGenomeData", _currentTask.getObjectId());
                        waitAndVerifyCompletion(_currentTask.getObjectId());
                        logger.debug("Completed loading of genome: " + tmpGenomeDir + "\n\n\n");
                    }
                    catch (RemoteException e) {
                        logger.error("There was an error loading data for genome: " + tmpGenomeDir + ", " + tmpNcbiFtpLocation, e);
                    }
                }
                catch (Exception e) {
                    logger.error("Error uploading data for new NCBI genomes.", e);
                }
            }
        }
        catch (Exception e) {
            throw new ServiceException("There was a problem bulk loading the NCBI genomes.", e);
        }
    }

    private String waitAndVerifyCompletion(Long taskId) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        String[] statusTypeAndValue = computeBean.getTaskStatus(taskId);
        while (!Task.isDone(statusTypeAndValue[0])) {
            Thread.sleep(5000);
            statusTypeAndValue = computeBean.getTaskStatus(taskId);
        }
        logger.debug(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }

}