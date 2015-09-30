
package org.janelia.it.jacs.compute.service.genomeProject;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.ftp.FtpClientFactory;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.genomeProject.GenomeProjectImportTask;
import org.janelia.it.jacs.model.tasks.genomeProject.GenomeProjectUpdateTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author Todd Safford
 */
public class UpdateGenomeProjectService implements IService {
    private GenomeProjectUpdateTask task;
    private Logger logger;

    /**
     * This is the most important method when updating Genome Project data from NCBI.
     * This action for complete bacteria takes >3 minutes and does the following:
     * 1) Download the ~1.7GB tar.
     * 2) Check for new genome projects; add new files
     * 3) Check for adds/deletions/modifications in old *.gbk files; copy old versions and install new ones
     * 4) Clean the update directory
     * <p/>
     * Draft bacterial genomes are handled differently
     */
    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            this.task = (GenomeProjectUpdateTask) ProcessDataHelper.getTask(processData);
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.debug("\n\nStarting downloadAndImportMostRecentGenomeProjectData...");
            String filestorePath = SystemConfigurationProperties.getString("FileStore.CentralDir");
            String updateMode = task.getParameter(GenomeProjectUpdateTask.PARAM_PROJECT_MODE).replaceAll(" ", "_");
            String updateDir = filestorePath + File.separator + User.SYSTEM_USER_LOGIN + File.separator + "gpupdate_" + updateMode;
            FileUtil.deleteDirectory(updateDir);
            FileUtil.ensureDirExists(updateDir);
            if (!GenomeProjectUpdateTask.PROJECT_MODE_DRAFT_BACTERIAL.equals(task.getParameter(GenomeProjectUpdateTask.PARAM_PROJECT_MODE))) {
                // Grab the proper tar.gz of all complete
                downloadMostRecentGenomeProjectArchive(updateDir);
                importGenomeProjectData(updateDir, GenomeProjectImportTask.COMPLETE_GENOME_PROJECT_STATUS, new CompleteGBKFilenameFilter());
            }
            // NOTE: Building the NC_ file within the crawl... method is a little brute-force if we've already seen the
            else {
                crawlFTPAndGetDraftGenomes(updateDir);
                expandArchivesAndBuildSingleGBK(updateDir);
                importGenomeProjectData(updateDir, GenomeProjectImportTask.DRAFT_GENOME_PROJECT_STATUS, new DraftGBKFilenameFilter());
            }
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private void downloadMostRecentGenomeProjectArchive(String updateDir) throws IOException, InterruptedException {
        logger.debug("downloading most recent genome project file...");
        String localFilename = updateDir + File.separator + "all.gbk.tar.gz";

        // Step 1: Get the file
        String targetMode = task.getParameter(GenomeProjectUpdateTask.PARAM_PROJECT_MODE);
        String specDir = "";
        String specFile = "all.gbk.tar.gz";
        if (GenomeProjectUpdateTask.PROJECT_MODE_BACTERIAL.equals(targetMode)) {
            specDir = "/genomes/Bacteria/";
        }
        else if (GenomeProjectUpdateTask.PROJECT_MODE_VIRAL.equals(targetMode)) {
            specDir = "/genomes/Viruses/";
        }
        FTPClient tmpClient = FtpClientFactory.createConnection("ftp.ncbi.nih.gov", 21, "anonymous".toCharArray(),
                "anonymous".toCharArray(), specDir, new FileSystemOptions());
        logger.debug("Connected to ftp.ncbi.nih.gov");
        logger.debug(tmpClient.getReplyString());
        logger.debug("File:" + specFile);
        File tmpDownloadedFile = new File(localFilename);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmpDownloadedFile);
            tmpClient.retrieveFile(specFile, fos);
        }
        finally {
            if (null != fos) {
                fos.close();
            }
        }


        // Step 2: uncompress and untar the file
        logger.debug("uncompressing and untarring...");
        SystemCall untarCall = new SystemCall(logger);
        untarCall.emulateCommandLine("tar -xvzf " + localFilename + " -C " + updateDir, true);
        boolean deleteSuccess = new File(localFilename).delete();
        if (!deleteSuccess){
            logger.error("Unable to delete file "+localFilename);
        }
    }

    private void crawlFTPAndGetDraftGenomes(String updateDir) throws Exception {
        // Crawl the directories and create an expanded GenomeProject area
        FTPClient tmpClient = FtpClientFactory.createConnection("ftp.ncbi.nih.gov", 21, "anonymous".toCharArray(),
                "anonymous".toCharArray(), "/genomes/Bacteria_DRAFT/", new FileSystemOptions());
        logger.debug("Connected to ftp.ncbi.nih.gov");
        logger.debug(tmpClient.getReplyString());
        FTPFile[] files = tmpClient.listFiles();
        int ftpFileSize = files.length;
        int archivesRetrieved = 0;
        for (FTPFile genomeDir : files) {
            boolean fileSuccess = false;
            int attempts = 0;
            while (!fileSuccess && attempts < 10) {
                attempts++;
                // If a dir is found crawl in
                if (genomeDir.isDirectory()) {
                    try {
                        getFileFromDirectory(tmpClient, genomeDir, updateDir);
                        fileSuccess = true;
                    }
                    catch (Exception e) {
                        logger.debug("Problem downloading files.  Trying again...");
                        Thread.sleep(2000);
                        tmpClient = FtpClientFactory.createConnection("ftp.ncbi.nih.gov", 21, "anonymous".toCharArray(),
                                "anonymous".toCharArray(), "/genomes/Bacteria_DRAFT/", new FileSystemOptions());
                    }
                    // Throw in a sleep so we don't tick-off NCBI.  The rule is <3 calls per second.
                    Thread.sleep(2000);
                }
                else {
                    // If not a directory decrement the total
                    ftpFileSize--;
                    fileSuccess = true;
                }
            }
            if (attempts >= 10) {
                throw new ServiceException("Unable to ftp file after 10 attempts.  Quitting genome project update.");
            }
        }
        logger.debug("Downloaded files: " + ftpFileSize + " out of " + archivesRetrieved + " retrieved.");
    }

    private void getFileFromDirectory(FTPClient tmpClient, FTPFile genomeDir, String updateDir) throws IOException {
        tmpClient.changeWorkingDirectory(genomeDir.getName());
        FileUtil.ensureDirExists(updateDir + File.separator + genomeDir.getName());
        FTPFile[] tmpInnerFiles = tmpClient.listFiles();
        String tmpLocalDestination = updateDir + File.separator + genomeDir.getName() + File.separator;
        // There should be only one file per genome but I am looping anyway.
        for (FTPFile tmpInnerFile : tmpInnerFiles) {
            if (tmpInnerFile.getName().toLowerCase().endsWith("contig.gbk.tgz")) {
                // Download a file from the FTP Server
                logger.debug("File:" + tmpInnerFile.getName());
                File tmpDownloadedFile = new File(tmpLocalDestination + tmpInnerFile.getName());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(tmpDownloadedFile);
                    tmpClient.retrieveFile(tmpInnerFile.getName(), fos);
                }
                finally {
                    if (null != fos) {
                        fos.close();
                    }
                }
            }
        }
        // Go back to the parent dir
        tmpClient.changeToParentDirectory();
    }

    private void expandArchivesAndBuildSingleGBK(String updateDir) throws Exception {
        File[] genomeDirs = new File(updateDir).listFiles();
        for (File tmpGenomeDir : genomeDirs) {
            if (!tmpGenomeDir.isDirectory()) {
                continue;
            }
            File[] tmpFiles = tmpGenomeDir.listFiles();
            for (File tmpFile : tmpFiles) {
                if (tmpFile.getName().toLowerCase().endsWith("contig.gbk.tgz")) {
                    // uncompress and untar the file
                    logger.debug("uncompressing and untarring...");
                    SystemCall untarCall = new SystemCall(logger);
                    untarCall.emulateCommandLine("tar -xvzf " + tmpFile.getAbsolutePath() + " -C " +
                            tmpFile.getParent(), true);
                    boolean deleteSuccess = tmpFile.delete();
                    if (!deleteSuccess) {
                        logger.error("Unable to delete the original downloaded file.");
                    }
                }
            }

            // Build the emulated complete GBK file
            File[] expandedLocalFiles = tmpGenomeDir.listFiles(
                    new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_WGS_DATA) && name.toLowerCase().endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
                        }
                    }
            );
            // Build a list of the contigs by length
            ArrayList<ContigFileVO> contigFileList = new ArrayList<ContigFileVO>();
            Long totalLength = (long) 0;
            for (File expandedLocalFile : expandedLocalFiles) {
                GenbankFile tmpGbkFile = new GenbankFile(expandedLocalFile.getAbsolutePath());
                // Ignore any contigs less than 2kb in size
                if (tmpGbkFile.getMoleculeLength() > 2000) {
                    totalLength += tmpGbkFile.getMoleculeLength();
                    contigFileList.add(new ContigFileVO(tmpGbkFile.getMoleculeLength(), expandedLocalFile.getAbsolutePath()));
                }
            }
            // If we didn't find any contig files do nothing
            if (contigFileList.size() > 0) {
                Collections.sort(contigFileList);
                // Write out the mapping info so we can align things later
                FileWriter mapWriter = null;
                try {
                    mapWriter = new FileWriter(new File(tmpGenomeDir + File.separator + "contigFileToLengthMap.tsv"));
                    for (ContigFileVO contigFileVO : contigFileList) {
                        File tmpFile = new File(contigFileVO.getFilePath());
                        mapWriter.write(tmpFile.getName() + "\t" + contigFileVO.getLength() + "\n");
                    }
                }
                finally {
                    if (null != mapWriter) {
                        mapWriter.close();
                    }
                }

                GenbankFileWriter writer = new GenbankFileWriter(contigFileList, totalLength);
                writer.write();
            }
            else {
                logger.error("There are no contigs for " + tmpGenomeDir);
            }
        }
    }

    /**
     * Electing to go on a genome project-by-project inspection instead of looping through all GBK files first
     *
     * @param pathToGenomeProjectData drive location of the unpacked genome project directories
     */
    private void importGenomeProjectData(String pathToGenomeProjectData, String genomeProjectStatus, FilenameFilter fileFilter) {
        try {
            if (logger.isInfoEnabled())
                logger.info("Starting importGenomeProjectDataFromViewableList() with source path: " + pathToGenomeProjectData);
            // VI path should be something like XgpData
            File tmpGPDirectory = new File(pathToGenomeProjectData);
            File[] tmpGenomeProjects = tmpGPDirectory.listFiles();
            int newGPs = 0;
            for (File tmpGenomeProject : tmpGenomeProjects) {
                if (!tmpGenomeProject.isDirectory()) {
                    continue;
                }
                File[] newGenbankFiles = tmpGenomeProject.listFiles(fileFilter);
                String tmpGenomeProjectName = tmpGenomeProject.getName().replace("_", " ");
                List<Node> originalGenomeProjectList = EJBFactory.getRemoteComputeBean().getNodeByName(tmpGenomeProjectName);

                // If the Genome Project doesn't exist, add it and it's files
                if (null != tmpGenomeProjects && originalGenomeProjectList.size() < 1) {
                    logger.debug("Learning genome project " + tmpGenomeProjectName);
                    newGPs++;
                    // Get the names of Genbank files imported
                    ArrayList<String> newGenbankFileNameList = new ArrayList<String>();
                    for (File genbankFile : newGenbankFiles) {
                        newGenbankFileNameList.add(genbankFile.getName());
                    }

                    GenomeProjectImportTask importTask = new GenomeProjectImportTask(tmpGenomeProject.getAbsolutePath(),
                            tmpGenomeProjectName, genomeProjectStatus,
                            newGenbankFileNameList, null, User.SYSTEM_USER_LOGIN, new ArrayList<Event>(),
                            new HashSet<TaskParameter>());
                    importTask = (GenomeProjectImportTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(importTask);
                    try {
                        EJBFactory.getRemoteComputeBean().submitJob("GenomeProjectDataImport", importTask.getObjectId());
                    }
                    catch (Exception e) {
                        logger.error("\n\n\n*****\nError running Genome Project import task for " + tmpGenomeProjectName + "\n" + e.getMessage() + "*****\n\n\n");
                    }
                }
                // If the Genome Project is known, look for differences in the number of gbk files and the version numbers
                // of those files.
                else {
                    //logger.debug("\nAlready know genome project "+tmpGenomeProjectName);
                    // If this is the case, there's big trouble...
                    if (originalGenomeProjectList.size() > 1) {
                        logger.debug("\n\nWARNING: " + tmpGenomeProjectName + " - There are " + originalGenomeProjectList.size() + " database entries for Genome Project " + tmpGenomeProjectName + ". Skipping...\n\n");
                        continue;
                    }
                    GenomeProjectFileNode originalGPNode = (GenomeProjectFileNode) originalGenomeProjectList.iterator().next();
                    File originalGPDir = new File(originalGPNode.getDirectoryPath());
                    File[] originalGenbankFiles = originalGPDir.listFiles(fileFilter);
                    for (File originalGenbankFile : originalGenbankFiles) {
                        File testFile = new File(tmpGenomeProject.getAbsolutePath() + File.separator + originalGenbankFile.getName());
                        if (!testFile.exists()) {
                            logger.debug("Moving \"deleted\" file: " + originalGenbankFile.getAbsolutePath());
                            // Invalidate the old system recruitment
                            String originalPath = originalGenbankFile.getAbsolutePath();
                            GenbankFile genbankFile = new GenbankFile(originalPath);
                            // todo Need a way to communicate to users that saved images in RecruitmentResultFileNodes are against old data
                            // NOTE: above to-do is a similar situation as incremental Blast-FRV pipeline except the latter simply adds samples
                            // which show as unchecked in the sample filtering UI
                            EJBFactory.getRemoteComputeBean().setSystemDataRelatedToGiNumberObsolete(genbankFile.getGINumber());
                            FileUtil.moveFileUsingSystemCall(originalPath, originalPath + "." + System.currentTimeMillis());
                        }
                    }
                    // Do checks against the new Genome Project dir: changes to old gbk files and new gbk files
                    for (File newGenbankFile : newGenbankFiles) {
                        File originalGenbankFile = new File(originalGPDir.getAbsolutePath() + File.separator + newGenbankFile.getName());
                        if (originalGenbankFile.exists()) {
                            // Compare the files
                            try {
                                GenbankFile oldGBK = new GenbankFile(originalGenbankFile.getAbsolutePath());
                                GenbankFile newGBK = new GenbankFile(newGenbankFile.getAbsolutePath());
                                boolean giNumberSame = newGBK.getGINumber().equals(oldGBK.getGINumber());
                                boolean lengthSame = newGBK.getMoleculeLength() == oldGBK.getMoleculeLength();
                                boolean accessionSame = newGBK.getVersionString().equals(oldGBK.getVersionString());
                                if (!giNumberSame || !lengthSame || !accessionSame) {
                                    logger.debug("\n\nNew Version detected with " + tmpGenomeProjectName);
                                    if (!giNumberSame) {
                                        logger.debug("Gi-Number different(old, new): (" + oldGBK.getGINumber() + "," + newGBK.getGINumber() + ")");
                                    }
                                    if (!lengthSame) {
                                        logger.debug("Size different(old, new): (" + oldGBK.getMoleculeLength() + "," + newGBK.getMoleculeLength() + ")");
                                    }
                                    if (!accessionSame) {
                                        logger.debug("Version different(old, new): (" + oldGBK.getVersionString() + "," + newGBK.getVersionString() + ")");
                                    }
                                    String originalPath = originalGenbankFile.getAbsolutePath();
                                    // Invalidate the old system recruitment
                                    EJBFactory.getRemoteComputeBean().setSystemDataRelatedToGiNumberObsolete(oldGBK.getGINumber());
                                    // Copy the new file to replace the old file
                                    FileUtil.moveFileUsingSystemCall(originalPath, originalPath + "." + System.currentTimeMillis());
                                    FileUtil.copyFile(newGenbankFile.getAbsolutePath(), originalGPDir.getAbsolutePath() + File.separator + newGenbankFile.getName());
                                }
                            }
                            catch (Exception e) {
                                // Allow to continue after the error
                                logger.debug("Error trying to compare GBK files:" + tmpGenomeProjectName + ", file: " + newGenbankFile.getName() + "\n" + e.getMessage());
                            }
                        }
                        // New Genbank file doesn't exist, so copy it
                        else {
                            logger.debug("\n\nAdding file " + newGenbankFile.getName() + " to " + tmpGenomeProjectName);
                            FileUtil.copyFile(newGenbankFile, originalGenbankFile);
                        }
                    }
                }
            }
            logger.debug("\nLearned " + newGPs + " new genome projects.");
        }
        catch (Throwable t) {
            t.printStackTrace();
            logger.error("Error in importRecruitmentDataFromViewableList: " + t.getMessage(), t);
        }
    }

    private class CompleteGBKFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_COMPLETE)||
                    name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_ALTERNAME_COMPLETE)||
                    name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_NOT_STRUCTURAL)) &&
                    name.endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
        }
    }

    private class DraftGBKFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_WGS_DATA) && name.endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
        }
    }

}