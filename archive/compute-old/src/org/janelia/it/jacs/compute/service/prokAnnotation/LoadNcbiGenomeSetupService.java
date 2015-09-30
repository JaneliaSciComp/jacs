
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
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationLoadGenomeDataTask;
import org.janelia.it.jacs.model.tasks.utility.FtpFileTask;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class LoadNcbiGenomeSetupService implements IService {
    private Logger logger;
    private ProkaryoticAnnotationLoadGenomeDataTask task;
    private ComputeBeanRemote computeBean;
    private String targetGenomeDirectory;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (ProkaryoticAnnotationLoadGenomeDataTask) ProcessDataHelper.getTask(processData);
            String targetDatabase = task.getJobName();
            targetGenomeDirectory = task.getParameter(FtpFileTask.PARAM_targetDirectory);
            String scriptBaseDir = SystemConfigurationProperties.getString("Executables.ModuleBase") + SystemConfigurationProperties.getString("ProkAnnotation.PerlBaseDir");
            computeBean = EJBFactory.getRemoteComputeBean();
            // Step 1 - Get the files from NCBI
            computeBean.addEventToTask(task.getObjectId(), new Event("Grabbing the files from NCBI", new Date(), "Calling NCBI"));
            FtpFileTask loadTask = new FtpFileTask();
            loadTask.setParameter(FtpFileTask.PARAM_targetExtensions, "gbk,ptt");
            loadTask.setParameter(FtpFileTask.PARAM_ftpSourceDirectory, task.getParameter(FtpFileTask.PARAM_ftpSourceDirectory));
            loadTask.setParameter(FtpFileTask.PARAM_targetDirectory, targetGenomeDirectory);
            loadTask.setOwner(task.getOwner());
            // update the first event date to RIGHT NOW
            loadTask.getFirstEvent().setTimestamp(new Date());
            loadTask = (FtpFileTask) computeBean.saveOrUpdateTask(loadTask);
            computeBean.submitJob("NCBIFtpFile", loadTask.getObjectId());
            String status = waitAndVerifyCompletion(loadTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                logger.error("\n\n\nERROR: the NCBI Ftp job has not actually completed!\nStatus is " + status);
            }

            // Step 2 - Run the load_genome_setup script
            computeBean.addEventToTask(task.getObjectId(), new Event("Running Setup Script", new Date(), "Running Setup"));
            String loadingScript = scriptBaseDir + SystemConfigurationProperties.getString("ProkAnnotation.loadGenomeSetupScript");
            String[] gbkFileNames = new File(targetGenomeDirectory).list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
                }
            });
            if (gbkFileNames.length <= 0) {
                throw new ServiceException("Could not find GBK files for Prokaryotic Pipeline.");
            }
            int numberOfMolecules = gbkFileNames.length;
            logger.debug("There are " + numberOfMolecules + " gbk files (molecules).");
            if (0 >= numberOfMolecules) {
                throw new ServiceException("Molecule data was not loaded from NCBI!  Check paths and try again.");
            }
            SystemCall call = new SystemCall(logger);
            String tmpCmd = "export PATH=$PATH:" + scriptBaseDir + ";export PERL5LIB=" + scriptBaseDir + ";" + loadingScript + " -U " + task.getParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_username)
                    + " -P " + task.getParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_sybasePassword) + " -X junkPassword -D " +
                    targetDatabase.toLowerCase() + " -M " + numberOfMolecules;
            logger.debug("Calling:\n" + tmpCmd);
            int exitValue = call.emulateCommandLine(tmpCmd, true, null, new File(targetGenomeDirectory));
            if (0 != exitValue) {
                logger.error("WARNING: Loading script exited with value " + exitValue);
                throw new ServiceException("Loading script exited with value " + exitValue);
            }

            // Step 3 - Copy the genome data into the load_genome dir.  Give all permissions.
            String tmpLoadGenomeDir = targetGenomeDirectory + File.separator + "load_genome/";
            call.emulateCommandLine("mv " + targetGenomeDirectory + File.separator + "*.gbk " + tmpLoadGenomeDir + ".", true);
            call.emulateCommandLine("mv " + targetGenomeDirectory + File.separator + "*.ptt " + tmpLoadGenomeDir + ".", true);
            call.emulateCommandLine("chmod 777 -R " + tmpLoadGenomeDir, true);

            // Step 4 - Parse necessary data and load the data to the database
            computeBean.addEventToTask(task.getObjectId(), new Event("Calling Loader Scripts", new Date(), "Calling Loaders"));
            String[] loaderSteps = getSectionCommands("<GENOMELOADER_RUN>").split("\n");
            // Now, get the list of GenbankFiles and sort by size
            ArrayList<GenbankFile> genbankFileList = new ArrayList<GenbankFile>();
            for (String gbkFileName : gbkFileNames) {
                genbankFileList.add(new GenbankFile(targetGenomeDirectory + File.separator + "load_genome" + File.separator + gbkFileName));
            }
            Collections.sort(genbankFileList, new MyGenbankFileComparator());
            // Loop through the files for the GBK AND PTT TO DB STEP
            for (int i = 0; i < genbankFileList.size(); i++) {
                GenbankFile tmpGenbankFile = genbankFileList.get(i);
                String currentStep = loaderSteps[i];
                // Get the ACCESSION
                currentStep = safeReplaceTerms(currentStep, "./", targetGenomeDirectory + File.separator + "load_genome" + File.separator);
                currentStep = safeReplaceTerms(currentStep, "<ACCESSION>", tmpGenbankFile.getAccession());
                currentStep = safeReplaceTerms(currentStep, "<TYPE>", (i == 0 ? "chromosome" : "plasmid"));
                String tmpMolecule = tmpGenbankFile.getMoleculeType();
                // If we think it is a chromosome, but it is not the largest in the Genbank file list we probably want
                // to give it a number
                if ("chromosome".equalsIgnoreCase(tmpMolecule) && i > 0) {
                    tmpMolecule = "Chromosome " + (i + 1);
                }
                currentStep = safeReplaceTerms(currentStep, "<MOLECULE>", tmpMolecule);
                currentStep = safeReplaceTerms(currentStep, "<TOPOLOGY>", tmpGenbankFile.getTopology());
                currentStep = safeReplaceTerms(currentStep, "<NT LOCUS PREFIX>", tmpGenbankFile.getLocusPrefix());
                exitValue = call.emulateCommandLine(currentStep, true, null, new File(targetGenomeDirectory));
                if (0 != exitValue) {
                    logger.error("WARNING: Loading script exited with value " + exitValue);
                    throw new ServiceException("Error running the GBK_TO_PTT. Exit value not 0");
                }
                else {
                    Scanner tmpErrorFileScanner = null;
                    try {
                        tmpErrorFileScanner = new Scanner(new File(targetGenomeDirectory + File.separator + "load_genome" + File.separator +
                                "GBK_and_PTT_to_DB_std_err_log_" + tmpGenbankFile.getAccession()));
                        if (tmpErrorFileScanner.hasNextLine()) {
                            logger.error("WARNING: Error running the GBK_and_PTT_to_DB. The error file for GBK_and_PTT_to_DB_std_err_log_"
                                    + tmpGenbankFile.getAccession() + " is not empty");
                            throw new ServiceException("Error running the GBK_TO_PTT. The error file for GBK_and_PTT_to_DB_std_err_log_"
                                    + tmpGenbankFile.getAccession() + " is not empty");
                        }
                    }
                    finally {
                        if (null != tmpErrorFileScanner) {
                            tmpErrorFileScanner.close();
                        }
                    }
                }
            }
            // Loop through the files for the GBK RNA step
            for (int i = 0; i < genbankFileList.size(); i++) {
                GenbankFile tmpGenbankFile = genbankFileList.get(i);
                String currentStep = loaderSteps[i + genbankFileList.size()];
                // Get the ACCESSION
                currentStep = safeReplaceTerms(currentStep, "./", targetGenomeDirectory + File.separator + "load_genome" + File.separator);
                currentStep = safeReplaceTerms(currentStep, "<ACCESSION>", tmpGenbankFile.getAccession());
                // todo ASMBL_ID is an integer assigned by the database.  If the user runs the data more than once the
                // todo id will not be right.
                currentStep = safeReplaceTerms(currentStep, "<ASMBL_ID>", Integer.toString(i + 1));
                currentStep = safeReplaceTerms(currentStep, "<NT LOCUS PREFIX>", tmpGenbankFile.getLocusPrefix());
                exitValue = call.emulateCommandLine(currentStep, true, null, new File(targetGenomeDirectory));
                if (0 != exitValue) {
                    logger.error("WARNING: Loading script exited with value " + exitValue);
                    throw new ServiceException("Error running the GBK_RNA. Exit value not 0");
                }
                else {
                    Scanner tmpErrorFileScanner = null;
                    try {
                        tmpErrorFileScanner = new Scanner(new File(targetGenomeDirectory + File.separator + "load_genome" + File.separator +
                                "GBK_RNA_std_err_log_" + tmpGenbankFile.getAccession()));
                        if (tmpErrorFileScanner.hasNextLine()) {
                            logger.error("WARNING: Error running the GBK_RNA. The error file for GBK_RNA_std_err_log_"
                                    + tmpGenbankFile.getAccession() + " is not empty");
                            throw new ServiceException("Error running the GBK_RNA. The error file for GBK_RNA_std_err_log_"
                                    + tmpGenbankFile.getAccession() + " is not empty");
                        }
                    }
                    finally {
                        if (null != tmpErrorFileScanner) {
                            tmpErrorFileScanner.close();
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            try {
                computeBean.addEventToTask(task.getObjectId(), new Event("ERROR running the LoadNcbiGenomeSetupService:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                System.err.println("Error trying to log the error message.");
            }
            throw new ServiceException("Error running the ProkAnnotation LoadNcbiGenomeSetupService:" + e.getMessage(), e);
        }
    }

    private String getSectionCommands(String targetSection) throws ServiceException, FileNotFoundException {
        File loaderDir = new File(targetGenomeDirectory + File.separator + "load_genome");
        File[] readmeFiles = loaderDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("LOAD_GENBANK_GENOME") && !name.endsWith("~");
            }
        });
        StringBuffer returnString = new StringBuffer();
        Scanner scanner = new Scanner(readmeFiles[0]);
        try {
            while (scanner.nextLine().indexOf(targetSection) < 0) {
            }
            while (scanner.hasNextLine()) {
                String tmpLine = scanner.nextLine().trim();
                if (!tmpLine.startsWith("*") && !"".equals(tmpLine)) {
                    returnString.append(tmpLine).append("\n");
                }
                // This is very fragile, but is very temporary!
                if (tmpLine.startsWith("* <")) {
                    break;
                }
            }
        }
        finally {
            scanner.close();
        }
        return returnString.toString();
    }

    private String safeReplaceTerms(String sourceString, String targetString, String replaceString) throws ServiceException {
        if (sourceString.indexOf(targetString) >= 0) {
            return sourceString.replace(targetString, replaceString);
        }
        else {
            return sourceString;
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

    private class MyGenbankFileComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            GenbankFile g1 = (GenbankFile) o1;
            GenbankFile g2 = (GenbankFile) o2;
            return Long.valueOf(g2.getMoleculeLength()).compareTo(g1.getMoleculeLength());
        }
    }
}
