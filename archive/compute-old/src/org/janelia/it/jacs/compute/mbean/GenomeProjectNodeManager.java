
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.genomeProject.GenomeProjectUpdateTask;
import org.janelia.it.jacs.model.tasks.recruitment.FRVSamplingFastaGenerationTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.tasks.GenbankFileInfo;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.MailHelper;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 3, 2008
 * Time: 9:29:31 AM
 */
public class GenomeProjectNodeManager implements GenomeProjectNodeManagerMBean {
    private static final Logger LOGGER = Logger.getLogger(GenomeProjectNodeManager.class);

    // Property Keys
    public static final String SHELL_PATH_PROP = "SystemCall.ShellPath";
    public static final String STREAM_DIRECTOR_PROP = "SystemCall.StreamDirector";
    public static final String PARTITION_SIZE_PROP = "BlastServer.PartitionSize";

    public void runGenomeProjectUpdate() {
        try {
            String messageBody = "";
            // todo Need to record which specific genome projects were added/modified
            // Update the Bacterial genome projects
            GenomeProjectUpdateTask gpUpdateBacterialTask = new GenomeProjectUpdateTask(GenomeProjectUpdateTask.PROJECT_MODE_BACTERIAL,
                    GenomeProjectUpdateTask.COMPLETE_GENOME_PROJECT_STATUS, null, User.SYSTEM_USER_LOGIN, null, null);
            gpUpdateBacterialTask = (GenomeProjectUpdateTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(gpUpdateBacterialTask);
            EJBFactory.getLocalComputeBean().submitJob("GenomeProjectUpdate", gpUpdateBacterialTask.getObjectId());
            String status = waitAndVerifyCompletion(gpUpdateBacterialTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                System.out.println("\n\n\nERROR: the Genome Project job has not actually completed!\nStatus is " + status);
                messageBody += "There was a problem updating the " + GenomeProjectUpdateTask.PROJECT_MODE_BACTERIAL + "Genome Projects\n";
            }
            else {
                messageBody += "The " + GenomeProjectUpdateTask.PROJECT_MODE_BACTERIAL + " Genome Projects were updated successfully.\n";
            }

            // Update the Viral genome projects
            GenomeProjectUpdateTask gpUpdateViralTask = new GenomeProjectUpdateTask(GenomeProjectUpdateTask.PROJECT_MODE_VIRAL,
                    GenomeProjectUpdateTask.COMPLETE_GENOME_PROJECT_STATUS, null, User.SYSTEM_USER_LOGIN, null, null);
            gpUpdateViralTask = (GenomeProjectUpdateTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(gpUpdateViralTask);
            EJBFactory.getLocalComputeBean().submitJob("GenomeProjectUpdate", gpUpdateViralTask.getObjectId());
            status = waitAndVerifyCompletion(gpUpdateViralTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                System.out.println("\n\n\nERROR: the Genome Project job has not actually completed!\nStatus is " + status);
                messageBody += "There was a problem updating the " + GenomeProjectUpdateTask.PROJECT_MODE_VIRAL + "Genome Projects\n";
            }
            else {
                messageBody += "The " + GenomeProjectUpdateTask.PROJECT_MODE_VIRAL + " Genome Projects were updated successfully.\n";
            }

            // Update the Draft bacterial genome projects
            GenomeProjectUpdateTask gpUpdateDraftTask = new GenomeProjectUpdateTask(GenomeProjectUpdateTask.PROJECT_MODE_DRAFT_BACTERIAL,
                    GenomeProjectUpdateTask.DRAFT_GENOME_PROJECT_STATUS, null, User.SYSTEM_USER_LOGIN, null, null);
            gpUpdateDraftTask = (GenomeProjectUpdateTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(gpUpdateDraftTask);
            EJBFactory.getLocalComputeBean().submitJob("GenomeProjectUpdate", gpUpdateDraftTask.getObjectId());
            status = waitAndVerifyCompletion(gpUpdateDraftTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                System.out.println("\n\n\nERROR: the Genome Project job has not actually completed!\nStatus is " + status);
                messageBody += "There was a problem updating the " + GenomeProjectUpdateTask.PROJECT_MODE_DRAFT_BACTERIAL + "Genome Projects\n";
            }
            else {
                messageBody += "The " + GenomeProjectUpdateTask.PROJECT_MODE_DRAFT_BACTERIAL + " Genome Projects were updated successfully.\n";
            }

            // Update the Genome Project lookup file
            RecruitmentDataHelper.buildGenbankFileList();

            // Rebuild the 1000 fasta files used for recruitment sampling
            FRVSamplingFastaGenerationTask fastaTask = new FRVSamplingFastaGenerationTask(SystemConfigurationProperties.getString("Recruitment.GenomeProjectFastaFileNode"),
                    User.SYSTEM_USER_LOGIN);
            fastaTask = (FRVSamplingFastaGenerationTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(fastaTask);
            EJBFactory.getLocalComputeBean().submitJob("FRVSamplingFastaGeneration", fastaTask.getObjectId());
            status = waitAndVerifyCompletion(fastaTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                System.out.println("\n\n\nERROR: the FRV Sampling Fasta generation is not complete.\nStatus is " + status);
                messageBody += "There was a problem updating the FRV Sampling FASTA\n";
            }
            else {
                messageBody += "The FRV Sampling FASTA files were updated successfully.\n";
            }

            // todo Diff the file and report the differences.

            // Notify interested parties that the update is complete.
            // Might want to get the email list from a Notification table (Notification flag, user email) people can register
            MailHelper helper = new MailHelper();
            helper.sendEmail("saffordt@janelia.hhmi.org", "saffordt@janelia.hhmi.org", "Genome Project Updater Complete", messageBody);
        }
        catch (Exception e) {
            System.out.println("There was a problem updating the genome project data.");
        }
        System.out.println("Genome project updating complete.");
    }


    public void reportEmptyGenomeProjectFileNodeDirs() {
        try {
            List nodeList = this.getSystemGenomeProjectFileNodeList();
            for (Object node : nodeList) {
                GenomeProjectFileNode tmpNode = (GenomeProjectFileNode) node;
                File gpDir = new File(tmpNode.getDirectoryPath());
                if (0 >= gpDir.listFiles().length) {
                    System.out.println("Missing data file: node=" + gpDir.getAbsolutePath());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Node> getSystemGenomeProjectFileNodeList() throws DaoException {
        return EJBFactory.getLocalComputeBean().getNodesByClassAndUser("GenomeProjectFileNode", User.SYSTEM_USER_LOGIN);
    }


    /**
     * Method to test our ability to parse the Genbank files
     *
     * @param pathToGenbankFile - path to NC_*.gbk file
     */
    public void parseGenbankFile(String pathToGenbankFile) {
        try {
            GenbankFile file = new GenbankFile(pathToGenbankFile);
            file.populateAnnotations();
            ArrayList<String> genes = file.getGeneEntries(null);
            for (String gene : genes) {
                // Cut the rest of the entry off at newline
                String tmpLine = gene.substring(gene.indexOf("gene") + 4, gene.indexOf("\n")).trim();
                System.out.println("Trying entry\n" + tmpLine);
                System.out.println(file.getAnnotationCoordinate(tmpLine, true));
                System.out.println(file.getAnnotationCoordinate(tmpLine, false));
            }
        }
        catch (Throwable e) {
            System.out.println("Error parsing GenbankFile: " + pathToGenbankFile + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Method to calculate how many RecruitmentDataSets have annotations to display.
     */
    public void howManyDataSetsHaveAnnotations() {
        try {
            List dataNodes = getSystemGenomeProjectFileNodeList();
            int haves = 0, haveNots = 0;
            for (Object dataNode : dataNodes) {
                GenomeProjectFileNode tmpNode = (GenomeProjectFileNode) dataNode;
                File gpDir = new File(tmpNode.getDirectoryPath());
                File[] genbankFiles = gpDir.listFiles();
                for (File genbankFile : genbankFiles) {
                    GenbankFile tmpGB = new GenbankFile(genbankFile.getAbsolutePath());
                    tmpGB.populateAnnotations();
                    List tmpList = tmpGB.getGeneEntries(null);
                    if (null == tmpList) {
                        continue;
                    }
                    // This method is returning the number of datasets with annotations
                    if (tmpList.size() > 0) {
                        haves++;
                    }
                    else {
                        haveNots++;
                        System.out.println(genbankFile.getAbsolutePath() + " has no annotations.");
                    }
                }
            }
            System.out.println("Done: " + haves + " have annotations. " + haveNots + " don't.");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String waitAndVerifyCompletion(Long taskId) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        String[] statusTypeAndValue = computeBean.getTaskStatus(taskId);
        while (!Task.isDone(statusTypeAndValue[0])) {
            Thread.sleep(5000);
            statusTypeAndValue = computeBean.getTaskStatus(taskId);
        }
        LOGGER.debug(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }

    /**
     * This method is suppossed to be temporary and misses RefSeq prefixes like AC, NS and also some files which have
     * numbers on the end of the gbk file (ie: NC_004088.gbk.1259188273752)
     *
     * @throws Exception
     */
    public void updateGenomeProjectDataTypeAndSeqTypeValues() throws Exception {
        String systemGenomeProjectDir = SystemConfigurationProperties.getString("FileStore.CentralDir") + File.separator +
                "system" + File.separator + GenomeProjectFileNode.SUB_DIRECTORY;
        File gpDir = new File(systemGenomeProjectDir);
        if (!gpDir.exists() || !gpDir.isDirectory()) {
            throw new Exception("Cannot find the system GenomeProject directory");
        }
        File[] genomeProjects = gpDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir.getAbsolutePath() + File.separator + name).isDirectory();
            }
        });
        System.out.println("There are " + genomeProjects.length + " genome projects found.");
//        HashSet<String> featureTypes = new HashSet<String>();
        for (File genomeProject : genomeProjects) {
            GenomeProjectFileNode tmpNode = (GenomeProjectFileNode) EJBFactory.getLocalComputeBean().getNodeById(Long.valueOf(genomeProject.getName()));
            // Setting the seq type here
            tmpNode.setSequenceType(GenomeProjectFileNode.SEQ_TYPE_COMPLETE);
            // Get the sequences
            File[] genbankFiles = genomeProject.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_COMPLETE) && name.endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
                }
            });
            // Check for no gbk files.  Shouldn't happen but still.
            if (null == genbankFiles) {
                System.out.println("The list of *.gbk files returned null for project " + genomeProject.getName() + ".  Continuing...");
                continue;
            }
            for (File genbankFile : genbankFiles) {
                GenbankFile tmpGBK = new GenbankFile(genbankFile.getAbsolutePath());
                if (null == tmpGBK.getOrganismKeywords() || "".equals(tmpGBK.getOrganismKeywords())) {
                    System.out.println("There is a problem parsing organism keywords for " + tmpGBK.getGenbankFilePath());
                    break;
                }
                tmpNode.setDataType(tmpGBK.getKingdom());
            }

            // Now save the changes
            EJBFactory.getRemoteComputeBean().saveOrUpdateNode(tmpNode);
        }
    }

    public void buildAllGenbankFileInfoList() {
        try {
            RecruitmentDataHelper.buildGenbankFileList();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildFastaArchive(){
        String fastaDir = SystemConfigurationProperties.getString("FileStore.CentralDir") + File.separator +
                "system" + File.separator + GenomeProjectFileNode.SUB_DIRECTORY+File.separator+"allGenbankFasta";
        try {
            FileUtil.deleteDirectory(fastaDir);
            FileUtil.ensureDirExists(fastaDir);
            List<GenbankFileInfo> genbankList = RecruitmentDataHelper.getGenbankFileList();
            for (GenbankFileInfo genbankFileInfo : genbankList) {
                GenbankFile tmpGBK = new GenbankFile(genbankFileInfo.getGenbankFile().getAbsolutePath());
                FileWriter writer = new FileWriter(new File(fastaDir+File.separator+tmpGBK.getAccession()+".fasta"));
                try {
                    writer.append(">").append(tmpGBK.getAccession()).append(" /definition=\"").append(tmpGBK.getDefinition()).append("\" ");
                    writer.append("/length=").append(Long.toString(tmpGBK.getMoleculeLength())).append("\n");
                    writer.append(tmpGBK.getFastaFormattedSequence());
                }
                finally {
                    writer.close();
                }
            }
            FileUtil.tarCompressDirectoryWithSystemCall(new File(fastaDir), fastaDir+".tar");
            // After the archive is built, clean it all up.
            FileUtil.deleteDirectory(fastaDir);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
