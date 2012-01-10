
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
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationServiceLoadGenomeDataTask;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class AnnotationServiceLoadGenomeSetupService implements IService {
    private ProkaryoticAnnotationServiceLoadGenomeDataTask task;
    private ComputeBeanRemote computeBean;
    private String targetGenomeDirectory;
    private String dataDate;
    private String tmpFile = "";
    public static final String INFO_PREFIX = "sub_info";

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (ProkaryoticAnnotationServiceLoadGenomeDataTask) ProcessDataHelper.getTask(processData);
            String targetDatabase = task.getJobName();
            targetGenomeDirectory = task.getParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_targetDirectory);
            String sourceDirectory = task.getParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_sourceDirectory);
            dataDate = task.getParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_dateString);
            computeBean = EJBFactory.getRemoteComputeBean();
            String scriptBaseDir = SystemConfigurationProperties.getString("Executables.ModuleBase") + SystemConfigurationProperties.getString("ProkAnnotation.PerlBaseDir");

            // Step 1 - Get the originalDataFiles from JCVI local dir
            computeBean.addEventToTask(task.getObjectId(), new Event("Grabbing the originalDataFiles from JCVI Directory", new Date(), "Getting originalDataFiles"));
            SystemCall call = new SystemCall(logger);
            String asBaseDir = SystemConfigurationProperties.getString("ProkAnnotation.ASBaseDir");
            call.emulateCommandLine("cp -R " + asBaseDir + File.separator + "\"" + sourceDirectory + "\"" + File.separator + "*" + dataDate +
                    " " + targetGenomeDirectory + File.separator + ".", true);
            File[] originalDataFiles = new File(targetGenomeDirectory).listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.indexOf(dataDate) >= 0;
                }
            });
            int numberOfMolecules = 0;
            for (File tmpFile : originalDataFiles) {
                Scanner scanner = null;
                try {
                    scanner = new Scanner(tmpFile);
                    if (scanner.hasNextLine()) {
                        String tmpLine = scanner.nextLine().trim();
                        if (tmpLine.startsWith(">")) {
                            numberOfMolecules++;
                        }
                    }
                }
                finally {
                    scanner.close();
                }
            }
            logger.debug("There are " + numberOfMolecules + " fasta originalDataFiles (molecules).");
            if (0 >= numberOfMolecules) {
                throw new ServiceException("Molecule data was not loaded from JCVI!  Check paths and try again.");
            }

            // Step 2 - Run the load_genome_setup script
            computeBean.addEventToTask(task.getObjectId(), new Event("Running Setup Script", new Date(), "Running Setup"));
            String loadingScript = scriptBaseDir + SystemConfigurationProperties.getString("ProkAnnotation.loadGenomeSetupScript");
            String tmpCmd = "export PATH=$PATH:" + scriptBaseDir + ";export PERL5LIB=" + scriptBaseDir + ";" + loadingScript + " -U " + task.getParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_username)
                    + " -P " + task.getParameter(ProkaryoticAnnotationServiceLoadGenomeDataTask.PARAM_sybasePassword) + " -X junkPassword -D " +
                    targetDatabase.toLowerCase() + " -A -M " + numberOfMolecules;
            logger.debug("Calling:\n" + tmpCmd);
            int exitValue = call.emulateCommandLine(tmpCmd, true, null, new File(targetGenomeDirectory));
            if (0 != exitValue) {
                logger.error("WARNING: Loading script exited with value " + exitValue);
                throw new ServiceException("Loading script exited with value " + exitValue);
            }

            // Step 3 - Copy the genome data into the load_genome dir
            for (File tmpFile : originalDataFiles) {
                call.emulateCommandLine("mv \"" + tmpFile.getAbsolutePath() + "\" " + targetGenomeDirectory + File.separator + "load_genome/.", true);
            }

            // Step 4 - Parse necessary data and load the data to the database
            computeBean.addEventToTask(task.getObjectId(), new Event("Calling Loader Scripts", new Date(), "Calling Loaders"));
            String[] loaderSteps = getSectionCommands("<GENOMELOADER_RUN>").split("\n");
            // Now, get the list of GenbankFiles and sort by size
            ArrayList<MoleculeInfoVO> moleculeFileList = new ArrayList<MoleculeInfoVO>();
            File[] infoFiles = new File(targetGenomeDirectory + File.separator + "load_genome").listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(INFO_PREFIX);
                }
            });
            // There should only be one info file
            File infoFile = infoFiles[0];
            Scanner scanner = new Scanner(infoFile);
            String MOLECULE_NUMBER = "Molecule Number:";
            String FILE = "file_";
            String TOPOLOGY = "topology_";
            String MOL_TYPE = "mol_type_";
            String SIZE = "size_";

            while (scanner.hasNextLine()) {
                String tmpLine = scanner.nextLine();
                int fileCounter = 0;
                if (tmpLine.startsWith(MOLECULE_NUMBER)) {
                    fileCounter++;
                    long tmpSize = 0;
                    // Wipe any left over values
                    String tmpTopology;
                    String tmpMolType;
                    tmpFile = tmpTopology = tmpMolType = "";
                    while (scanner.hasNextLine()) {
                        tmpLine = scanner.nextLine();
                        if (tmpLine.startsWith(FILE)) {
                            // Get the file in question - the sub_info file has the file prefix with a timestamp added
                            tmpFile = tmpLine.substring(tmpLine.indexOf(":") + 1).trim();
                            File[] testFiles = new File(targetGenomeDirectory + File.separator + "load_genome").listFiles(new FilenameFilter() {
                                public boolean accept(File dir, String name) {
                                    return name.startsWith(tmpFile);
                                }
                            });
                            // There can be only one!
                            tmpFile = targetGenomeDirectory + File.separator + "load_genome" + File.separator + "molecule" + fileCounter + ".fasta";
                            call.emulateCommandLine("mv \"" + testFiles[0].getAbsolutePath() + "\" " + tmpFile,
                                    true, null, new File(targetGenomeDirectory));
                        }
                        else if (tmpLine.startsWith(TOPOLOGY)) {
                            tmpTopology = tmpLine.substring(tmpLine.indexOf(":") + 1).trim();
                        }
                        else if (tmpLine.startsWith(MOL_TYPE)) {
                            tmpMolType = tmpLine.substring(tmpLine.indexOf(":") + 1).trim();
                        }
                        else if (tmpLine.startsWith(SIZE)) {
                            tmpSize = Long.valueOf(tmpLine.substring(tmpLine.indexOf(":") + 1).trim());
                            // Since size seems to be the terminal value, exit loop
                            break;
                        }
                    }
                    moleculeFileList.add(new MoleculeInfoVO(tmpFile, tmpTopology, tmpMolType, tmpSize));
                }
            }
            // Loop through the originalDataFiles for the SeqenceFileToDb steps
            for (int i = 0; i < moleculeFileList.size(); i++) {
                MoleculeInfoVO tmpMolecule = moleculeFileList.get(i);
                String currentStep = loaderSteps[i];
                // Get the ACCESSION
                currentStep = safeReplaceTerms(currentStep, "./", targetGenomeDirectory + File.separator + "load_genome" + File.separator);
                currentStep = safeReplaceTerms(currentStep, "<ACCESSION>", "\"" + tmpMolecule.getFilename() + "\"");
                currentStep = safeReplaceTerms(currentStep, "<TYPE>", tmpMolecule.getMolType());
                String tmpMol = tmpMolecule.getMolType();
                // If we think it is a chromosome, but it is not the largest in the Genbank file list we probably want
                // to give it a number
                if ("chromosome".equalsIgnoreCase(tmpMol)) {
                    tmpMol = "Chromosome " + (i + 1);
                }
                currentStep = safeReplaceTerms(currentStep, "<MOLECULE>", tmpMol);
                currentStep = safeReplaceTerms(currentStep, "<TOPOLOGY>", tmpMolecule.getTopology());

                exitValue = call.emulateCommandLine(currentStep, true, null, new File(targetGenomeDirectory));
                if (0 != exitValue) {
                    logger.error("WARNING: Loading Annotation Service genome script exited with value " + exitValue);
                    throw new ServiceException("Error running the SeqenceFileToDb.dbi. Exit value not 0");
                }
                // todo Is there an error file to scan?
//                else {
//                    Scanner tmpErrorFileScanner=null;
//                    try {
//                        tmpErrorFileScanner = new Scanner(new File(targetGenomeDirectory+File.separator+"load_genome"+File.separator+
//                            "GBK_and_PTT_to_DB_std_err_log_"+tmpGenbankFile.getAccession()));
//                        if (tmpErrorFileScanner.hasNextLine()) {
//                            logger.error("WARNING: Error running the SeqenceFileToDb.");
//                            throw new ServiceException("Error running the SeqenceFileToDb.");
//                        }
//                    }
//                    finally {
//                        if (null!=tmpErrorFileScanner) {
//                            tmpErrorFileScanner.close();
//                        }
//                    }
//                }
            }
        }
        catch (Exception e) {
            try {
                computeBean.addEventToTask(task.getObjectId(), new Event("ERROR running the AnnotationServiceLoadGenomeSetupService:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                System.err.println("Error trying to log the error message.");
            }
            throw new ServiceException("Error running the ProkAnnotation AnnotationServiceLoadGenomeSetupService:" + e.getMessage(), e);
        }
    }

    private String getSectionCommands(String targetSection) throws ServiceException, FileNotFoundException {
        File loaderDir = new File(targetGenomeDirectory + File.separator + "load_genome");
        File[] readmeFiles = loaderDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("LOAD_ANN_ENG") && !name.endsWith("~");
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

}