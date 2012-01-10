
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationBulkTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationTask;

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
public class ProkAnnotationBulkService implements IService {
    private Logger logger;
    private ProkaryoticAnnotationBulkTask task;
    private ComputeBeanRemote computeBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (ProkaryoticAnnotationBulkTask) ProcessDataHelper.getTask(processData);
            computeBean = EJBFactory.getRemoteComputeBean();
            String annotationDir = SystemConfigurationProperties.getString("ProkAnnotation.BaseDir");
            String tmpDirectoryName = SystemConfigurationProperties.getString("Upload.ScratchDir");
            File listFile = new File(tmpDirectoryName + File.separator + task.getParameter(ProkaryoticAnnotationBulkTask.PARAM_genomeListFile));
            Scanner scanner = new Scanner(listFile);
            while (scanner.hasNextLine()) {
                try {
                    String tmpNext = scanner.nextLine();
                    if (null == tmpNext || tmpNext.indexOf("\t") < 0) {
                        break;
                    }
                    String[] pieces = tmpNext.trim().split("\t");
                    String tmpGenomeDir = pieces[0].trim().toUpperCase();
                    // For CMR Genomes this is ftpFile path, for JCVI Genomes this is path to FASTA file
                    String tmpLocation = pieces[1].trim();
                    // Create and save the loader task
                    ProkaryoticAnnotationTask _currentTask = new ProkaryoticAnnotationTask();
                    _currentTask.setJobName(tmpGenomeDir);
                    _currentTask.setOwner(task.getParameter(ProkaryoticAnnotationTask.PARAM_username));
                    _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_username, task.getParameter(ProkaryoticAnnotationTask.PARAM_username));
                    _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_sybasePassword, task.getParameter(ProkaryoticAnnotationTask.PARAM_sybasePassword));
                    _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_targetDirectory, annotationDir + "/" + tmpGenomeDir != null ? (annotationDir + "/" + tmpGenomeDir) : null);
                    if (ProkaryoticAnnotationTask.MODE_JCVI_GENOME.equals(task.getParameter(ProkaryoticAnnotationTask.PARAM_annotationMode))){
                        _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_contigFilePath, tmpLocation);
                        _currentTask.setParameter(ProkaryoticAnnotationTask.LOAD_CONTIGS, task.getParameter(ProkaryoticAnnotationTask.LOAD_CONTIGS));
                        _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString,
                                reconfigureBulkJCVIGipSettings(tmpGenomeDir, tmpLocation));

                    }
                    else {
                        _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString,
                            reconfigureBulkCMRGipSettings(tmpGenomeDir));
                    }
                    _currentTask.setActionList(task.getActionList());
                    _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_project,
                            task.getParameter(ProkaryoticAnnotationTask.PARAM_project));
                    _currentTask.getFirstEvent().setTimestamp(new Date());

                    logger.debug("Starting annotation of genome: " + tmpGenomeDir);
                    // Submit the loader task
                    try {
                        _currentTask = (ProkaryoticAnnotationTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(_currentTask);
                        EJBFactory.getRemoteComputeBean().submitJob("ProkAnnotationPipeline", _currentTask.getObjectId());
                    }
                    catch (RemoteException e) {
                        logger.error("There was an error annotating data for genome: " + tmpGenomeDir + ", " + tmpLocation, e);
                    }
                }
                catch (Exception e) {
                    logger.error("Error annotating data for new NCBI genomes.", e);
                }
            }
        }
        catch (Exception e) {
            try {
                computeBean.addEventToTask(task.getObjectId(), new Event("ERROR running the Prokaryotic Pipeline:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                logger.error("Error trying to log the error message.", e1);
            }
            throw new ServiceException("Error running the ProkAnnotation ProkAnnotationService:" + e.getMessage(), e);
        }
    }

    private String reconfigureBulkCMRGipSettings(String tmpGenomeDir) {
        // If bulk mode, for CMR genomes insert the db string
        String dbLine = "db: "+tmpGenomeDir.toLowerCase();
        String gipConfiguration = task.getParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString);
        StringBuffer sbuf = new StringBuffer();
        Scanner scanner = new Scanner(gipConfiguration);
//        logger.debug("The original gip config is \n"+gipConfiguration);
        while (scanner.hasNextLine()){
            String tmpLine = scanner.nextLine().trim();
            if (tmpLine.startsWith("db:")) {
                sbuf.append(dbLine).append("\n");
            }
            else {
                sbuf.append(tmpLine).append("\n");
            }
        }
        return sbuf.toString();
//        logger.debug("The final gip config is \n"+sbuf.toString());
    }

    private String reconfigureBulkJCVIGipSettings(String tmpGenomeDir, String tmpLocation) {
        // If bulk mode, JCVI genomes, replace name, type, topology with fixed string and FASTA path with the
        // Load Contig path
        String dbLine       = "db: "+tmpGenomeDir.toLowerCase();
        String seqSourceLine= "seq_source: "+ tmpLocation;
        String nameLine     = "name: pseudomolecule_1";
        String typeLine     = "type: pseudomolecule";
        String topologyLine = "topology: linear";
        String gipConfiguration = task.getParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString);
        StringBuffer sbuf = new StringBuffer();
        Scanner scanner = new Scanner(gipConfiguration);
//        logger.debug("The original gip config is \n"+gipConfiguration);
        while (scanner.hasNextLine()){
            String tmpLine = scanner.nextLine().trim();
            if (tmpLine.startsWith("seq_source:")){
                sbuf.append(seqSourceLine).append("\n");
            }
            else if (tmpLine.startsWith("db:")) {
                sbuf.append(dbLine).append("\n");
            }
            else if (tmpLine.startsWith("name:")) {
                sbuf.append(nameLine).append("\n");
            }
            else if (tmpLine.startsWith("type:")) {
                sbuf.append(typeLine).append("\n");
            }
            else if (tmpLine.startsWith("topology:")) {
                sbuf.append(topologyLine).append("\n");
            }
            else {
                sbuf.append(tmpLine).append("\n");
            }
        }
        return sbuf.toString();
//        logger.debug("The final gip config is \n"+sbuf.toString());
    }

}