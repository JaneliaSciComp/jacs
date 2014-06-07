
package org.janelia.it.jacs.compute.mbean;

import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.recruitment.FrvStatisticsGenerationService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.recruitment.*;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.tasks.GenbankFileInfo;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 2, 2007
 * Time: 5:00:41 PM
 */
public class RecruitmentNodeManager implements RecruitmentNodeManagerMBean {

    private static final Logger LOGGER = Logger.getLogger(RecruitmentNodeManager.class);
    public static final String SHIFTED_FILE_EXTENSION = ".shifted";
    public static final String RECRUITMENT_PROJECT_CODE = "0116";

    // Property Keys
    public static final String SHELL_PATH_PROP = "SystemCall.ShellPath";
    public static final String STREAM_DIRECTOR_PROP = "SystemCall.StreamDirector";
    public static final String PARTITION_SIZE_PROP = "BlastServer.PartitionSize";

    public RecruitmentNodeManager() {
    }

    public long blastFrvASingleGenbankFileReturnId(String pathToGenbankFile, String ownerLogin) {
        // Example: /usr/local/projects/X/filestore/system/genomeProject/1167236348080292196/NC_010087.gbk
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        try {
            System.out.println("Running BlastFRV for: " + pathToGenbankFile);
            GenbankFileInfo genbankFileInfo = getGenbankFileInfoForGivenFile(pathToGenbankFile);
            // For the Genbank file, run blast, import recruitment file node, and recruitment result file node
            // Get the id for the node, name="All Metagenomic Sequence Reads (N)", subject db "1054893807616655712"
            GenomeProjectBlastFrvTask task = new GenomeProjectBlastFrvTask(
                    genbankFileInfo.getGenomeProjectNodeId().toString(),
                    genbankFileInfo.getGenbankFile().getName(),
                    "1054893807616655712",
                    null,
                    ownerLogin,
                    new ArrayList(),
                    new HashSet());
            task.setParameter(Task.PARAM_project, RECRUITMENT_PROJECT_CODE);
            task = (GenomeProjectBlastFrvTask) computeBean.saveOrUpdateTask(task);
            // Submit the job
            computeBean.submitJob("GenomeProjectBlastFRVGrid", task.getObjectId());
            return task.getObjectId();
        }
        catch (Exception e) {
            LOGGER.error("Could not generate data for " + pathToGenbankFile, e);
        }
        return -1;
    }

    public void blastFrvASingleGenbankFile(String pathToGenbankFile, String ownerLogin) {
        blastFrvASingleGenbankFileReturnId(pathToGenbankFile, ownerLogin);
    }

    private GenbankFileInfo getGenbankFileInfoForGivenFile(String pathToGenbankFile) throws Exception {
        File genbankFile = new File(pathToGenbankFile);
        GenbankFile tmpGF = new GenbankFile(genbankFile.getAbsolutePath());
        long tmpBaseLength = tmpGF.getMoleculeLength();
        String tmpSequenceWithoutGaps = tmpGF.getFastaFormattedSequence().replaceAll("[Nn\n]","");
        long tmpBaseLengthWithoutGaps = tmpSequenceWithoutGaps.length(); 
        return new GenbankFileInfo(Long.valueOf(genbankFile.getParentFile().getName()), genbankFile, tmpBaseLength,
                tmpBaseLengthWithoutGaps);
    }

    public void genomeProjectBlastFrvServiceForEachDataset() {
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            List<GenbankFileInfo> genbankFiles = RecruitmentDataHelper.getGenbankFileList();
            Subject tmpUser = computeBean.getSubjectByNameOrKey(User.SYSTEM_USER_LOGIN);
            // For each Genbank file, run blast, import recruitment file node, and recruitment result file node
            for (GenbankFileInfo genbankFileInfo : genbankFiles) {
                try {
                    String genbankFileName = genbankFileInfo.getGenbankFile().getName();
                    if (null == computeBean.getRecruitmentFilterDataTaskForUserByGenbankId(genbankFileName, tmpUser.getName())) {
                        System.out.println("Could not find " + genbankFileName + " recruited and filtered for user " + tmpUser.getName() + ". Blast-Recruiting...");
                        GenomeProjectBlastFrvTask tmpTask = new GenomeProjectBlastFrvTask(genbankFileInfo.getGenomeProjectNodeId().toString(),
                                genbankFileName, "1054893807616655712", null, tmpUser.getName(), new ArrayList<Event>(), 
                                new HashSet<TaskParameter>());
                        tmpTask.setParameter(Task.PARAM_project, RECRUITMENT_PROJECT_CODE);
                        tmpTask = (GenomeProjectBlastFrvTask) computeBean.saveOrUpdateTask(tmpTask);
                        // Submit the job
                        computeBean.submitJob("GenomeProjectBlastFRVGrid", tmpTask.getObjectId());
                        // Don't want to overwhelm JDBC or the Grid processing upon start-up
                        Thread.sleep(500);
                    }
//                    else {
//                        System.out.println("User "+tmpUser.getUserLogin()+" already has generated FRV data for "+
//                                genbankFileName+".  Skipping...");
//                    }
                }
                catch (RemoteException e) {
                    LOGGER.error("Could not generate data for " + genbankFileInfo.getGenbankFile().getName(), e);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method takes all old FRV tasks and re-submits the tasks to the Process Manager.  This updates any
     * information or images contained within the result node directories.
     */
    public void regenerateSystemRecruitedImages() {
        if (LOGGER.isInfoEnabled()) LOGGER.info("Starting regenerateRecruitedImages");
        try {
            List targetNodes = getSystemRecruitmentResultFileNodeList();
            int x = 1;
            for (Object tmpDir : targetNodes) {
                RecruitmentResultFileNode node = (RecruitmentResultFileNode) tmpDir;
                System.out.println("\nProcessing " + x);
                x++;
                regenerateSystemRecruitedImagesForNode(node);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
            LOGGER.error("Error in regenerateSystemRecruitedImages: " + t.getMessage(), t);
        }
    }

    /**
     * Gets a list of recruitment result file nodes.  For times when we need to regenerate all images.
     *
     * @return - list of RecruitmentResultFileNodes
     * @throws org.janelia.it.jacs.compute.access.DaoException
     *          problem getting the data
     */
    private List<Node> getSystemRecruitmentResultFileNodeList() throws DaoException {
        return EJBFactory.getLocalComputeBean().getNodesByClassAndUser("RecruitmentResultFileNode", User.SYSTEM_USER_LOGIN);
    }


    private void regenerateSystemRecruitedImagesForNode(RecruitmentResultFileNode node) throws RemoteException {
        RecruitmentViewerFilterDataTask rvTask = (RecruitmentViewerFilterDataTask) EJBFactory.getRemoteComputeBean().getTaskById(node.getTask().getObjectId());
        try {
            System.out.println("\nTask=" + rvTask.getObjectId() + ", Node=" + node.getObjectId() + "...");
            EJBFactory.getRemoteComputeBean().saveEvent(rvTask.getObjectId(), Event.RESUBMIT_EVENT, "Resubmitting the task for processing", new Date());
            EJBFactory.getRemoteComputeBean().submitJob("FrvResubmitImagesOnlyGrid", rvTask.getObjectId());
        }
        catch (Throwable e) {
            e.printStackTrace();
            LOGGER.error("Error processing RecruitmentFileNode " + node + ". Continuing loop...");
        }
    }

    /**
     * Feed this method the node id of a RecruitmentResultFileNode (images) dir and it will regenerate the images
     *
     * @param nodeId - name of a RecruitmentResultFileNode (images) dir
     */
    public void regenerateSystemRecruitedImagesForSpecificNodeId(String nodeId) {
        try {
            RecruitmentResultFileNode tmpNode = (RecruitmentResultFileNode) EJBFactory.getLocalComputeBean().getNodeById(Long.valueOf(nodeId));
            regenerateSystemRecruitedImagesForNode(tmpNode);
        }
        catch (RemoteException e) {
            LOGGER.error("Error processing regenerateSystemRecruitmentDataForSpecificNodeId():" + e.getMessage());
        }
    }

    /**
     * This method takes all old FRV tasks and re-submits the tasks to the Process Manager.  This updates any
     * information contained within the recruitment node directories.
     */
    public void regenerateSystemRecruitmentData() {
        if (LOGGER.isInfoEnabled()) LOGGER.info("Starting regenerateRecruitmentData");
        try {
            List targetNodes = getSystemRecruitmentFileNodeList();
            int x = 1;
            for (Object tmpDir : targetNodes) {
                RecruitmentFileNode node = (RecruitmentFileNode) tmpDir;
                System.out.println("\nProcessing " + x);
                x++;
                regenerateSystemRecruitmentDataForNode(node);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
            LOGGER.error("Error in regenerateSystemRecruitmentData: " + t.getMessage(), t);
        }
    }

    /**
     * Gets a list of recruitment result file nodes.  For times when we need to regenerate all images.
     *
     * @return - list of RecruitmentResultFileNodes
     * @throws org.janelia.it.jacs.compute.access.DaoException
     *          problem getting the data
     */
    private List getSystemRecruitmentFileNodeList() throws DaoException {
        return EJBFactory.getLocalComputeBean().getNodesByClassAndUser("RecruitmentFileNode", User.SYSTEM_USER_LOGIN);
    }


    private void regenerateSystemRecruitmentDataForNode(RecruitmentFileNode node) throws RemoteException {
        RecruitmentViewerRecruitmentTask rvTask = (RecruitmentViewerRecruitmentTask) EJBFactory.getRemoteComputeBean().getTaskById(node.getTask().getObjectId());
        try {
            System.out.println("\nTask=" + rvTask.getObjectId() + ", Node=" + node.getObjectId() + "...");
            EJBFactory.getRemoteComputeBean().saveEvent(rvTask.getObjectId(), Event.RESUBMIT_EVENT, "Resubmitting the task for processing", new Date());
            EJBFactory.getRemoteComputeBean().submitJob("FrvResubmitRecruitFromBlast", rvTask.getObjectId());
        }
        catch (Throwable e) {
            e.printStackTrace();
            LOGGER.error("Error processing RecruitmentFileNode " + node + ". Continuing loop...");
        }
    }

    /**
     * Feed this method the node id of a RecruitmentFileNode (FRV data) dir and it will regenerate the data from
     * the blast output
     *
     * @param nodeId - name of a RecruitmentFileNode (FRV data) dir
     */
    public void regenerateSystemRecruitmentDataForSpecificNodeId(String nodeId) {
        try {
            RecruitmentFileNode tmpNode = (RecruitmentFileNode) EJBFactory.getLocalComputeBean().getNodeById(Long.valueOf(nodeId));
            regenerateSystemRecruitmentDataForNode(tmpNode);
        }
        catch (RemoteException e) {
            LOGGER.error("Error processing regenerateSystemRecruitmentDataForSpecificNodeId():" + e.getMessage());
        }
    }

    private String checkForErrors(File errorFile) {
        if (!errorFile.exists()) {
            return "No error file exists.";
        }

        StringBuffer sbuf = new StringBuffer();
        FileReader reader;
        BufferedReader br = null;
        try {
            reader = new FileReader(errorFile);
            br = new BufferedReader(reader);
            String s;
            while ((s = br.readLine()) != null) {
                sbuf.append(s);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
            LOGGER.error("Error in checkForErrors: " + t.getMessage(), t);
        }
        finally {
            if (null != br) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sbuf.toString();
    }


    /**
     * Method to check for processing errors while on the grid
     *
     * @param pathToSystemRecruitmentFileDirectory
     *         - path to recruitment directories
     */
    public void checkAllSubdirectoriesForErrors(String pathToSystemRecruitmentFileDirectory) {
        try {
            File systemRecDir = new File(pathToSystemRecruitmentFileDirectory);
            String[] recruitmentDirs = systemRecDir.list();
            StringBuffer sbuf = new StringBuffer();
            int count = 0;
            for (String tmpDir : recruitmentDirs) {
                String errorTest = checkForErrors(new File(pathToSystemRecruitmentFileDirectory + File.separator + tmpDir + File.separator + "rvUpdateError.1"));
                if (null != errorTest && !"".equals(errorTest)) {
                    sbuf.append(tmpDir).append(",");
                    count++;
//                    LOGGER.error("\n\nError generating images for directory "+tmpDir.getName());//+"\n"+errorTest);
                }
            }
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("The list of " + count + " nodes with grid errors is:\n" + (sbuf.toString().substring(0, sbuf.toString().length() - 1)));
        }
        catch (Throwable t) {
            t.printStackTrace();
            LOGGER.error("Error in checkAllSubdirectoriesForErrors: " + t.getMessage(), t);
        }
    }


    public void generateSampleInfoFile() {
        FileWriter writer = null;
        try {
            // The query used is
            // select b.sample_acc, b.sample_title, b.sample_name, ss.project, ss.project_name, min(l.min_insert_size ) as min_insert_size, max(l.max_insert_size) as max_insert_size from bio_sample b, sample_site ss, library l where ss.sample_name = b.sample_name and b.sample_acc = l.sample_acc group by b.sample_acc, b.sample_title, b.sample_name, ss.project, ss.project_name order by b.sample_acc
            List sampleList = EJBFactory.getLocalComputeBean().getSampleInfo();
            String perlPath = EJBFactory.getLocalComputeBean().getSystemConfigurationProperty("Executables.ModuleBase");
            String basePath = EJBFactory.getLocalComputeBean().getSystemConfigurationProperty("RecruitmentViewer.PerlBaseDir");
            String sampleInfoName = EJBFactory.getLocalComputeBean().getSystemConfigurationProperty("RecruitmentViewer.SampleFile.Name");
            File sampleFile = new File(perlPath + basePath + File.separator + sampleInfoName);
            sampleFile.delete();
            writer = new FileWriter(sampleFile);
            int relativeIndexNumber = 0;
            // Note: can sanity check this loop by running: select distinct sample_acc from sample_site order by sample_acc
            // Note: don't forget the file index starts at 0.  You should have a row for each distinct sample_acc
            for (Object aSampleList : sampleList) {
                // The list of items is: sample_name, sample_acc, location
                Object[] o = (Object[]) aSampleList;

                writer.append(Integer.toString(relativeIndexNumber)).append("\t");
                writer.append(String.valueOf(o[0])).append("\t"); // Sample Accession
                writer.append(String.valueOf(o[1])).append("\t"); // Sample Title
                writer.append(String.valueOf(o[2])).append("\t"); // Sample Name
                writer.append(String.valueOf(o[3])).append("\t"); // Project Accession
                writer.append(String.valueOf(o[4])).append("\t"); // Project Name
                writer.append(String.valueOf(o[5])).append("\t"); // Min insert size
                writer.append(String.valueOf(o[6])).append("\n"); // Max insert size
                relativeIndexNumber++;
            }
            // Add the manual ones
            writer.append("106").append("\t");
            writer.append("AUSOIL_MANAGED_PAIRED").append("\t"); // Sample Accession
            writer.append("AUSOIL - Manangatan Managed Paired").append("\t"); // Sample Title
            writer.append("AUSOIL_managed_454PE3KB").append("\t"); // Sample Name
            writer.append("AUSOIL").append("\t"); // Project Accession
            writer.append("Australia Soil and Rumen Genomics").append("\t"); // Project Name
            writer.append("2000").append("\t"); // Min insert size
            writer.append("4000").append("\n"); // Max insert size

            writer.append("107").append("\t");
            writer.append("AUSOIL_MANAGED_UNPAIRED").append("\t"); // Sample Accession
            writer.append("AUSOIL - Manangatan Managed Unpaired").append("\t"); // Sample Title
            writer.append("AUSOIL_managed_454UP").append("\t"); // Sample Name
            writer.append("AUSOIL").append("\t"); // Project Accession
            writer.append("Australia Soil and Rumen Genomics").append("\t"); // Project Name
            writer.append("null").append("\t"); // Min insert size
            writer.append("null").append("\n"); // Max insert size

            writer.append("108").append("\t");
            writer.append("AUSOIL_REMNANT_PAIRED").append("\t"); // Sample Accession
            writer.append("AUSOIL - Manangatan Remnant Paired").append("\t"); // Sample Title
            writer.append("AUSOIL_remnant_454PE3KB").append("\t"); // Sample Name
            writer.append("AUSOIL").append("\t"); // Project Accession
            writer.append("Australia Soil and Rumen Genomics").append("\t"); // Project Name
            writer.append("2000").append("\t"); // Min insert size
            writer.append("4000").append("\n"); // Max insert size

            writer.append("109").append("\t");
            writer.append("AUSOIL_REMNANT_UNPAIRED").append("\t"); // Sample Accession
            writer.append("AUSOIL - Manangatan Remnant Unpaired").append("\t"); // Sample Title
            writer.append("AUSOIL_remnant_454UP").append("\t"); // Sample Name
            writer.append("AUSOIL").append("\t"); // Project Accession
            writer.append("Australia Soil and Rumen Genomics").append("\t"); // Project Name
            writer.append("null").append("\t"); // Min insert size
            writer.append("null").append("\n"); // Max insert size
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null != writer) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void updateSystemRecruitmentDataWithNewBlastDatabases(String commaSeparatedListOfBlastDatabaseNodeIds) {
        try {
            // If no good list provided, exit
            if (null == commaSeparatedListOfBlastDatabaseNodeIds || "".equals(commaSeparatedListOfBlastDatabaseNodeIds)) {
                System.out.println("No list of blast databases provided for update.  Exiting...");
                return;
            }
            // Update the sample.info file
            //System.out.println("Updating the sample.info file");
            // Commented out because the database doesn't have new sample/project/library data
            // todo This situation should be fixed
            // generateSampleInfoFile();

            // Now run the pipeline
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            List<GenbankFileInfo> genbankFiles = RecruitmentDataHelper.getGenbankFileList();
            Subject tmpUser = computeBean.getSubjectByNameOrKey(User.SYSTEM_USER_LOGIN);
            // For each Genbank file, run blast, import recruitment file node, and recruitment result file node
            for (GenbankFileInfo genbankFileInfo : genbankFiles) {
                try {
                    String genbankFileName = genbankFileInfo.getGenbankFile().getName();
                    String frvFilterTaskId = computeBean.getRecruitmentFilterDataTaskForUserByGenbankId(genbankFileName, tmpUser.getName());
                    if (null != frvFilterTaskId) {
                        RecruitmentViewerFilterDataTask filterTask = (RecruitmentViewerFilterDataTask) computeBean.getTaskById(Long.valueOf(frvFilterTaskId));
                        System.out.println("Found " + genbankFileName + " recruited and filtered for user " + tmpUser.getName() + ". Blast-Recruiting new data...");
                        GenomeProjectBlastFrvUpdateTask tmpTask = new GenomeProjectBlastFrvUpdateTask(filterTask.getObjectId().toString(),
                                commaSeparatedListOfBlastDatabaseNodeIds,
                                genbankFileInfo.getGenomeProjectNodeId().toString(),
                                genbankFileName, null, tmpUser.getName(), new ArrayList(), new HashSet());
                        tmpTask.setParameter(Task.PARAM_project, RECRUITMENT_PROJECT_CODE);
                        tmpTask = (GenomeProjectBlastFrvUpdateTask) computeBean.saveOrUpdateTask(tmpTask);
                        // Submit the job
                        computeBean.submitJob("GenomeProjectBlastFRVUpdateGrid", tmpTask.getObjectId());
                        // Don't want to overwhelm JDBC or the Grid processing upon start-up
                        Thread.sleep(500);
                    }
//                    else {
//                        System.out.println("User "+tmpUser.getUserLogin()+" has no generated FRV data for "+
//                                genbankFileName+".  Skipping...");
//                    }
                }
                catch (RemoteException e) {
                    LOGGER.error("Could not generate data for " + genbankFileInfo.getGenbankFile().getName(), e);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This method is a post-processor to the other ones which generate novel or incremental recruitment data.
     * Rather than suffer through the db populating giant fatsa files for every user request for all reads against
     * a given organism, this method will pre-populate that file.
     */
    public void updateDataSetFastaFiles() {
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            // Find the recruitment file node related to each Genbank chromosome and generate the "All Read" FASTA file
            List<Node> resultNodes = getSystemRecruitmentResultFileNodeList();
            for (Node resultNode : resultNodes) {
                try {
                    RecruitmentDataFastaBuilderTask fastaTask = new RecruitmentDataFastaBuilderTask(resultNode.getObjectId().toString());
                    fastaTask.setParameter(Task.PARAM_project, RECRUITMENT_PROJECT_CODE);
                    fastaTask.setOwner(User.SYSTEM_USER_LOGIN);
                    fastaTask = (RecruitmentDataFastaBuilderTask) computeBean.saveOrUpdateTask(fastaTask);
                    computeBean.submitJob("FrvDataFastaNonGrid", fastaTask.getObjectId());
                }
                catch (Exception e) {
                    System.out.println("Unable to generate the fasta file:\n" + e.getMessage());
                    throw e;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void profileCombinedHitsFile(String pathToCombinedHitsFile) {
        String[] mateCategoryByBitPosition = new String[]{"Good Left", "Good Right",
                "Too Close Left", "Too Close Right",
                "No Mate Left", "No Mate Right",
                "Too Far Left", "Too Far Right",
                "Anti-Oriented Left", "Anti-Oriented Right",
                "Normal Oriented Left", "Normal Oriented Right",
                "Outie-Oriented Left", "Outie-Oriented Right",
                "Missing Mate Left", "Missing Mate Right"
        };
        try {
            Scanner scanner = new Scanner(new File(pathToCombinedHitsFile));
            // Format the mate category hash
            TreeMap<Integer, ArrayList<Integer>> mateMap = new TreeMap<Integer, ArrayList<Integer>>();
            TreeMap<String, ArrayList<String>> sampleMap = new TreeMap<String, ArrayList<String>>();
            int totalReads = 0;
            while (scanner.hasNextLine()) {
                totalReads++;
                String[] pieces = scanner.nextLine().split("\t");
                grabMateDataForRead(pieces[24], mateMap);
                grabSampleDataForRead(pieces[22], sampleMap);
            }

            // Display the results
            System.out.println("Results for file: " + pathToCombinedHitsFile);
            System.out.println("Numbers by mate category...");
            for (Integer category : mateMap.keySet()) {
                double size = mateMap.get(category).size();
                NumberFormat format = new DecimalFormat("0.00");
                String percentage = format.format((size / totalReads) * 100);
                System.out.println("Mate Category: " + category + " ( " + mateCategoryByBitPosition[category] + ") has " + (int) size + " reads in that category; " + percentage + "% of total reads.");
            }
            System.out.println("Numbers by sample...");
            for (String sample : sampleMap.keySet()) {
                double size = sampleMap.get(sample).size();
                NumberFormat format = new DecimalFormat("0.00");
                String percentage = format.format((size / totalReads) * 100);
                System.out.println("Sample: " + sample + " has " + (int) size + " reads in that category; " + percentage + "% of total reads.");
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("ERROR:" + e.getMessage());
        }
    }

    private void grabSampleDataForRead(String sampleName, TreeMap<String, ArrayList<String>> sampleMap) {
        if (null == sampleName) {
            sampleName = "NA";
        }

        // Now manage the sample recording
        if (null != sampleMap.get(sampleName)) {
            sampleMap.get(sampleName).add(sampleName);
        }
        else {
            ArrayList<String> tmpList = new ArrayList<String>();
            tmpList.add(sampleName);
            sampleMap.put(sampleName, tmpList);
        }
    }


    private void grabMateDataForRead(String mateInfo, TreeMap<Integer, ArrayList<Integer>> mateMap) {
        Integer mateCategory;
        if (null != mateInfo) {
            mateCategory = (Integer.valueOf(mateInfo) - 1) % 16;
        }
        else {
            mateCategory = -1;
        }

        // Now manage the mate recording
        if (null != mateMap.get(mateCategory)) {
            mateMap.get(mateCategory).add(mateCategory);
        }
        else {
            ArrayList<Integer> tmpList = new ArrayList<Integer>();
            tmpList.add(mateCategory);
            mateMap.put(mateCategory, tmpList);
        }
    }

    public void recombineHitsFiles(boolean debugOnly) {
        try {
            List dataNodes = getSystemRecruitmentFileNodeList();
            int totalCombining = 0;
            for (Object dataNode : dataNodes) {
                RecruitmentFileNode rfn = (RecruitmentFileNode) dataNode;
                // Remove the old file.
                File combinedHitsFile = new File(rfn.getDirectoryPath() + File.separator + RecruitmentFileNode.COMBINED_FILENAME);
                if (!debugOnly) {
                    combinedHitsFile.delete();
                    combinedHitsFile.createNewFile();
                }

                // Now rebuild the file by adding all the specific combinedHits files together.
                File rfnDir = new File(rfn.getDirectoryPath());
                File[] combineFiles = rfnDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith("combinedPlusSitePlusMate.hits.1");
                    }
                });
                for (File targetFile : combineFiles) {
                    System.out.println("Adding " + targetFile.getName() + " to " + combinedHitsFile.getName());
                    totalCombining++;
                    if (!debugOnly) {
                        FileUtil.appendOneFileToAnother(combinedHitsFile, targetFile);
                    }
                }
                System.out.println(rfn.getObjectId() + " - Done.");
            }
            System.out.println("Total of " + totalCombining + " files remerged.");
        }
        catch (Exception e) {
            LOGGER.error("\n\n\n*****\nError cleaning up dir\n" + e.getMessage() + "*****\n\n\n");
        }
    }


    /**
     * This is a patch method to account for a change to the grid submission service and fix the num recruited reads.
     */
    public void updateNumRecruitmentData() {
        try {
            List resultNodes = getSystemRecruitmentResultFileNodeList();
            for (Object resultNode : resultNodes) {
                RecruitmentResultFileNode tmpNode = (RecruitmentResultFileNode) resultNode;
                RecruitmentViewerFilterDataTask tmpTask = (RecruitmentViewerFilterDataTask) EJBFactory.getLocalComputeBean().getTaskForNodeId(tmpNode.getObjectId());
                int tmpTaskHits = Integer.valueOf(tmpTask.getParameter(RecruitmentViewerFilterDataTask.NUM_HITS));
                int tmpFileHits = -1;
                File tmpNumRecruitedFile = new File(tmpNode.getDirectoryPath() + File.separator + RecruitmentResultFileNode.NUM_HITS_FILENAME);
                Scanner scanner = new Scanner(tmpNumRecruitedFile);
                if (scanner.hasNextLine()) {
                    tmpFileHits = Integer.valueOf(scanner.nextLine());
                }
                if (tmpTaskHits != tmpFileHits) {
                    System.out.println("Found difference for " + tmpTask.getParameter(RecruitmentViewerTask.QUERY) + " " + tmpTaskHits + "!=" + tmpFileHits);
                    if (tmpTaskHits == 0 && tmpFileHits >= 0) {
                        EJBFactory.getLocalComputeBean().setRVHitsForNode(tmpNode.getObjectId(), Integer.toString(tmpFileHits));
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void accCheck() {
//        EJBFactory.getLocalComputeBean().testAccFile();
//    }

    public void blastPartitionCheck(String pathToBlastDirectories) {
        FileWriter writer = null;
        try {
            File tmpDir = new File(pathToBlastDirectories);
            writer = new FileWriter(new File(pathToBlastDirectories + File.separator + "blastSurvey" + (new Date().getTime())));
            if (!tmpDir.exists()) {
                String msg = "The directory " + pathToBlastDirectories + " does not exist.";
                System.out.println(msg);
                writer.write(msg + "\n");
                return;
            }
            int probableErrors = 0;
            for (File blastDir : tmpDir.listFiles()) {
                if (!blastDir.isDirectory()) {
                    continue;
                }
                // NOTE: This logic assumes one query set
                BlastResultFileNode blastNode = (BlastResultFileNode) EJBFactory.getLocalComputeBean().getNodeById(new Long(blastDir.getName()));
                BlastTask blastTask = (BlastTask) blastNode.getTask();
                String blastDBId = blastTask.getParameter(BlastTask.PARAM_subjectDatabases);
                BlastDatabaseFileNode blastDBNode = (BlastDatabaseFileNode) EJBFactory.getLocalComputeBean().getNodeById(new Long(blastDBId));
                int expectedPartitionCount = blastDBNode.getPartitionCount();
                File[] tmpResultDirs = blastDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith("r_");
                    }
                });
                int totalOutputFiles = 0;
                if (null != tmpResultDirs && 0 != tmpResultDirs.length) {
                    for (File tmpResultDir : tmpResultDirs) {
                        File[] blastOutputFiles = tmpResultDir.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.startsWith("blast.outr");
                            }
                        });
                        totalOutputFiles += blastOutputFiles.length;
                    }
                    if (totalOutputFiles != expectedPartitionCount) {
                        String msg = "Directory " + blastDir.getName() + " had (found/expected) " + totalOutputFiles + "/" +
                                expectedPartitionCount + " partitions.";
                        System.out.println(msg);
                        writer.write(msg + "\n");
                        probableErrors++;
                    }
                }
            }
            String msg = "Probable errors: " + probableErrors;
            System.out.println(msg);
            writer.write(msg + "\n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null != writer) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void buildRecruitmentSamplingFasta() {
        try {
            FRVSamplingFastaGenerationTask fastaTask = new FRVSamplingFastaGenerationTask(SystemConfigurationProperties.getString("Recruitment.GenomeProjectFastaFileNode"),
                    User.SYSTEM_USER_LOGIN);
            fastaTask = (FRVSamplingFastaGenerationTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(fastaTask);
            EJBFactory.getLocalComputeBean().submitJob("FRVSamplingFastaGeneration", fastaTask.getObjectId());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runRecruitmentSamplingTask(String blastDbIds, String owner) throws Exception {
        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(owner)) {
            throw new Exception("system cannot be the owner of the recruitment sampling jobs.");
        }
        if (null == blastDbIds || "".equals(blastDbIds)) {
            throw new Exception("The blast db list cannot be empty for recruitment sampling jobs.");
        }
        GenomeProjectRecruitmentSamplingTask samplingTask = new GenomeProjectRecruitmentSamplingTask(blastDbIds, owner);
        samplingTask.setJobName("Sampling of " + blastDbIds);
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        samplingTask.setParameter(Task.PARAM_project, RECRUITMENT_PROJECT_CODE);
        samplingTask = (GenomeProjectRecruitmentSamplingTask) computeBean.saveOrUpdateTask(samplingTask);
        // Submit the job
        computeBean.submitJob("GenomeProjectRecruitmentSampling", samplingTask.getObjectId());
    }

    public void buildRecruitmentSamplingBlastDatabases(String originalNucleotideBlastDbIds, String samplingDbName,
                                                       String samplingDbDescription) {
        try {
            RecruitmentSamplingBlastDatabaseBuilderTask builderTask = new RecruitmentSamplingBlastDatabaseBuilderTask(null,
                    User.SYSTEM_USER_LOGIN, null, null, originalNucleotideBlastDbIds, samplingDbName, samplingDbDescription);
            builderTask.setJobName(samplingDbName);
            builderTask = (RecruitmentSamplingBlastDatabaseBuilderTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(builderTask);
            EJBFactory.getLocalComputeBean().submitJob("FRVSamplingBlastDatabaseBuilder", builderTask.getObjectId());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recruitBlastOutput(String blastResultNodeId, String owner) {
        try {
            BlastResultFileNode blastOutputNode = EJBFactory.getRemoteComputeBean().getBlastResultFileNodeByTaskId(Long.valueOf(blastResultNodeId));
            HashSet<BlastResultFileNode> rvRtInputNodes = new HashSet<BlastResultFileNode>();
            rvRtInputNodes.add(blastOutputNode);
            RecruitmentViewerRecruitmentTask recruitmentTask = new RecruitmentViewerRecruitmentTask("-1",
                    "All Genomes",
                    rvRtInputNodes, owner, new ArrayList(), null,
                    "All genomes",
                    "All Genomes",
                    -1l,
                    "-1");
            recruitmentTask.setParameter(Task.PARAM_project, RECRUITMENT_PROJECT_CODE);
            recruitmentTask = (RecruitmentViewerRecruitmentTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(recruitmentTask);
            // SUBMITTING THE RECRUITMENT JOB
            EJBFactory.getRemoteComputeBean().submitJob("FrvDataRecruitment", recruitmentTask.getObjectId());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateRecruitmentStatistics(String recruitmentNodeId) {
        // RecruitmentTask for error logging - 1472021694691410293
        // RecruitmentTask for Air Redux 1515163221856616741
        FileWriter writer = null;
        Scanner scanner = null;
        try {
            FrvStatisticsGenerationService service = new FrvStatisticsGenerationService();
            Task task = EJBFactory.getLocalComputeBean().getTaskById(Long.valueOf("1515163221856616741"));
            EJBFactory.getRemoteComputeBean().saveEvent(task.getObjectId(), "Generating Statistics", "Generating Statistics", new Date());
            // Build the map of multiplier to blast db.  Used to determine coverage and interest
            List<String> blastDBList;
            String tmpDbList;
            boolean useSamplingStrategy = true;
            if (task instanceof GenomeProjectRecruitmentSamplingTask) {
                tmpDbList = task.getParameter(GenomeProjectRecruitmentSamplingTask.BLASTABLE_DATABASE_NODES);
            }
            else if (task instanceof RecruitmentViewerTask) {
                tmpDbList = task.getParameter(RecruitmentViewerTask.BLAST_DATABASE_IDS);
                useSamplingStrategy = false;
            }
            else {
                throw new ServiceException("Task type "+task.getClass()+" unknown to FrvStatisticsGenerationService!");
            }
            if (null!=tmpDbList && !"".equals(tmpDbList)){
                blastDBList = Task.listOfStringsFromCsvString(tmpDbList);
            }
            else {
                throw new ServiceException("Cannot generate statistics for task="+task.getObjectId()+" as there is no db list provided.");
            }
            HashMap<String, Long> blastDBToEntrySizeMap = new HashMap<String, Long>();
            for (String tmpBlastDb : blastDBList) {
                BlastDatabaseFileNode tmpNode = (BlastDatabaseFileNode)EJBFactory.getLocalComputeBean().getNodeById(Long.valueOf(tmpBlastDb));
                // NOTE!!!!: This implies the db name is the same as the sample name.
                // We need to relate sample id in the blast hit to the original db the aligned subject piece came from
                blastDBToEntrySizeMap.put(tmpNode.getName(), (long)tmpNode.getSequenceCount());
            }

            // STEP 1: Go through the results and map out the unique molecules.  Grab useful info.
            // Default columns and filename for blast_comb_file.  Expecting a sampling run
            String dataFileTag = RecruitmentFileNode.BLAST_COMBINED_FILENAME;
            int moleculeIndex=0, alignmentBeginIndex=2, alignmentEndIndex=3, sampleIndex=24;
            // If making stats from an actual recruitment use the columns for the combinedPlusSitePlusMatePlus.hits file
            if (!useSamplingStrategy){
                dataFileTag = RecruitmentFileNode.COMBINED_FILENAME;
                sampleIndex=22;
            }
            File dataFile = new File("/local/camera_grid_new/system/Recruitment/1515170153862529317/Recruitment/1515491489398915365/"+dataFileTag);
            File statsFile = new File("/local/camera_grid_new/system/Recruitment/1515170153862529317/Recruitment/1515491489398915365/statistics.tab");
            writer = new FileWriter(statsFile);
            scanner = new Scanner(dataFile);
            TreeMap<String, RecruitmentMoleculeCoverageInfo> moleculeMap = new TreeMap<String, RecruitmentMoleculeCoverageInfo>();
            HashMap<String, GenbankFileInfo> genbankMap = RecruitmentDataHelper.getGenbankFileMap();
            while (scanner.hasNextLine()) {
                String[] resultLine = scanner.nextLine().split("\t");
                RecruitmentMoleculeCoverageInfo tmpInfo = null;
                // When sampling we don't run official recruitment so throw away blast data which doesn't align 90% or better
                if (useSamplingStrategy) {
                    Double subjectBegin=Double.valueOf(resultLine[6]);
                    Double subjectEnd=Double.valueOf(resultLine[7]);
                    Double subjectLength=Double.valueOf(resultLine[16]);
                    if (subjectEnd<subjectBegin){ double tmp=subjectEnd;subjectEnd=subjectBegin;subjectBegin=tmp;}
                    double percentageAligned = ((subjectEnd-subjectBegin)/subjectLength);
                    if (percentageAligned<=0.9){
                        System.out.println("Dropping data "+subjectBegin+"\t"+subjectEnd+"\t"+subjectLength+"="+percentageAligned);
                        continue;
                    }
                }
                // Count up the raw number of hits
                if (!moleculeMap.containsKey(resultLine[moleculeIndex])) {
                    try {
                        GenbankFileInfo tmpGbkInfo = genbankMap.get(resultLine[moleculeIndex]);
                        if (null==tmpGbkInfo){
                            LOGGER.error("We cannot find Genbank info for "+resultLine[moleculeIndex]+" yet it had an anlignment!?  Skipping...");
                            continue;
                        }
                        Long tmpLength = tmpGbkInfo.getLength();
                        Long tmpUngappedLength = tmpGbkInfo.getLengthWithoutGaps();
                        tmpInfo = new RecruitmentMoleculeCoverageInfo(resultLine[moleculeIndex]);
                        // Since reads won't align to the giant gaps in draft genomes only use the ungapped size to
                        // determine ratios/interesting hits
                        tmpInfo.setMoleculeLength((tmpUngappedLength<tmpLength)?tmpUngappedLength:tmpLength);
                        moleculeMap.put(resultLine[moleculeIndex], tmpInfo);
                    }
                    catch (Exception e) {
                        String error = "Unable to add item " + ((null != tmpInfo) ? tmpInfo.getMoleculeName() : "") + " to the recruitment path.";
                        LOGGER.error(error);
                        LOGGER.error("Data file:"+dataFile.getAbsolutePath()+",result="+resultLine[moleculeIndex]);
                        throw new ServiceException(error);
                    }
                }
                else {
                    tmpInfo = moleculeMap.get(resultLine[moleculeIndex]);
                }

                // Save the alignment
                Alignment tmpNewAlignment = new Alignment(Long.valueOf(resultLine[alignmentBeginIndex]),
                                                          Long.valueOf(resultLine[alignmentEndIndex]));
                tmpInfo.addAlignmentToSample(resultLine[sampleIndex], tmpNewAlignment);

                // Save the item back into the collection
                moleculeMap.put(tmpInfo.getMoleculeName(), tmpInfo);
            }

            // STEP 2: Write out the statistics data.
            // molecule id, mol length, unique coverage length, bases in reads,
            // ratio of bases in reads to unique coverage length, fractional coverage to mol length,
            // raw number of hits, library, genome project node id
            for (String s : moleculeMap.keySet()) {
                RecruitmentMoleculeCoverageInfo tmpInfo = moleculeMap.get(s);
                StringBuffer tmpLine;
                HashMap<String, ArrayList<Alignment>> coverageMap = tmpInfo.getCoverageMap();
                Long aggregateTotalAlignmentBases = 0l;
                // Loop through the samples recruited for the molecule
                // Print a row for the individual samples
                for (String sample : coverageMap.keySet()) {
                    Long blastDbEntrySize = blastDBToEntrySizeMap.get(sample);
                    boolean randomlySelect = (blastDbEntrySize > 5000);
                    double targetEntryCount = ((blastDbEntrySize * 0.01) <= 5000) ? 5000 : (blastDbEntrySize * 0.01);
                    if (!randomlySelect) {
                        targetEntryCount = blastDbEntrySize;
                    }
                    // Recruitment sampling is how many times smaller than the whole db?
                    double scalingMultiplier = blastDbEntrySize/targetEntryCount;
                    tmpLine = new StringBuffer();
                    Long tmpUniqueCoverageLength = tmpInfo.getUniqueCoverageLengthForSample(sample);
                    Long tmpTotalAlignmentBasesBySample = tmpInfo.getTotalAlignmentBasesBySample(sample);
                    // Molecule name
                    tmpLine.append(tmpInfo.getMoleculeName()).append("\t");
                    // Molecule Length - minus gaps
                    tmpLine.append(tmpInfo.getMoleculeLength()).append("\t");
                    // Length of unique coverage on the molecule
                    tmpLine.append(tmpUniqueCoverageLength).append("\t");
                    // Total aligned bases by sample aligned to
                    tmpLine.append(tmpTotalAlignmentBasesBySample).append("\t");
                    // Total aligned bases by sample over unique coverage length
                    tmpLine.append(tmpTotalAlignmentBasesBySample.floatValue() / tmpUniqueCoverageLength.floatValue()).append("\t");
                    // Ratio (scaled-up) of unique coverage over the total molecule length
                    tmpLine.append((tmpUniqueCoverageLength.floatValue()*scalingMultiplier) / tmpInfo.getMoleculeLength().floatValue()).append("\t");
                    // Total number of alignments to the sample for this molecule
                    tmpLine.append(tmpInfo.getAlignmentCountForSample(sample)).append("\t");
                    // Sample name
                    tmpLine.append(sample).append("\t");
                    // Node id of the molecule in question
                    tmpLine.append(genbankMap.get(tmpInfo.getMoleculeName()).getGenomeProjectNodeId()).append("\n");
                    writer.write(tmpLine.toString());
                    aggregateTotalAlignmentBases += tmpTotalAlignmentBasesBySample;
                }
                // Print a row for the aggregate of the samples
                Long aggregateTotalUniqueCoverageLength = tmpInfo.getTotalUniqueCoverageLength();
                Long totalDbEntrySize = 0l;
                Long usedSamplingDbSize = 0l;
                for (String sample : coverageMap.keySet()) {
                    Long blastDbEntrySize = blastDBToEntrySizeMap.get(sample);
                    totalDbEntrySize+=blastDbEntrySize;
                    boolean randomlySelect = (blastDbEntrySize > 5000);
                    double targetEntryCount = ((blastDbEntrySize * 0.01) <= 5000) ? 5000 : (blastDbEntrySize * 0.01);
                    if (!randomlySelect) {
                        targetEntryCount = blastDbEntrySize;
                    }
                    usedSamplingDbSize+=(long)targetEntryCount;
                }
                long aggregateScalingMultiplier = totalDbEntrySize/usedSamplingDbSize;
                tmpLine = new StringBuffer();
                tmpLine.append(tmpInfo.getMoleculeName()).append("\t");
                tmpLine.append(tmpInfo.getMoleculeLength()).append("\t");
                tmpLine.append(aggregateTotalUniqueCoverageLength).append("\t");
                tmpLine.append(aggregateTotalAlignmentBases).append("\t");
                tmpLine.append(aggregateTotalAlignmentBases.floatValue() / aggregateTotalUniqueCoverageLength.floatValue()).append("\t");
                tmpLine.append((aggregateTotalUniqueCoverageLength.floatValue()*aggregateScalingMultiplier) / tmpInfo.getMoleculeLength().floatValue()).append("\t");
                tmpLine.append(tmpInfo.getTotalNumberAlignments()).append("\t");
                tmpLine.append(RecruitmentMoleculeCoverageInfo.AGGREGATE_SAMPLES).append("\t");
                tmpLine.append(genbankMap.get(tmpInfo.getMoleculeName()).getGenomeProjectNodeId()).append("\n");
                writer.write(tmpLine.toString());
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (null!=writer) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null!=scanner) {
                scanner.close();
            }
        }
    }

    public void testGPParsing(String filePath) {
        try {
            Scanner scanner = new Scanner(new File(filePath));
            TreeMap<String, String> moleculeMap = new TreeMap<String, String>();
            HashMap<String, GenbankFileInfo> genbankMap = RecruitmentDataHelper.getGenbankFileMap();
            while (scanner.hasNextLine()) {
                String[] blastResultLine = scanner.nextLine().split("\t");
                // Count up the raw number of hits
                if (!moleculeMap.containsKey(blastResultLine[0])) {
                    try {
                        GenbankFileInfo tmpGbkInfo = genbankMap.get(blastResultLine[0]);
                        GenbankFile tmpGbk = new GenbankFile(tmpGbkInfo.getGenbankFile().getAbsolutePath());
                        moleculeMap.put(blastResultLine[0], blastResultLine[0]);
                    }
                    catch (Exception e) {
                        throw new ServiceException("Boom");
                    }
                }
            }
            System.out.println("There are "+moleculeMap.size()+" items in the map.");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void fileCheck(String path){
        try{
            File gpDir = new File(path);
            File[] gpDirs = gpDir.listFiles();
            for (File dir : gpDirs) {
                if (dir.isDirectory()){
                    File[] internalFiles = dir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return (name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_COMPLETE)||
                                    name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_ALTERNAME_COMPLETE)||
                                    name.startsWith(GenomeProjectFileNode.PREFIX_REFSEQ_NOT_STRUCTURAL)) &&
                                    name.endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
                        }
                    });
                    if (null==internalFiles || 0==internalFiles.length){
                        System.out.println("Dir "+dir.getName()+" has no complete GBK file.");
                        File[] draftFiles = dir.listFiles(new FilenameFilter(){
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.indexOf(".gbk.")>=0;
                            }
                        });
                        for (File draftFile : draftFiles) {
                            System.out.println(draftFile.getName());
                            String newName = draftFile.getName().substring(0,draftFile.getName().indexOf(".gbk.")+4);
                            System.out.println("Filename will be "+newName);
                            FileUtil.moveFileUsingSystemCall(draftFile, new File(draftFile.getParentFile().getAbsolutePath()+File.separator+newName));
                        }
                    }
                }
            }
        }
        catch (Exception e){
            e.getStackTrace();
        }
    }

    // XtoddTmp/jirausers.txt
    public void testUsers(String pathUserFile) {
        try {
            Scanner scanner = new Scanner(new File(pathUserFile));
            while (scanner.hasNextLine()){
                String tmpUser = scanner.nextLine().trim();
                SystemCall call = new SystemCall(LOGGER);
                call.emulateCommandLine("groups "+tmpUser+" >> "+pathUserFile+".out", true);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class RecruitmentMoleculeCoverageInfo {
        public static final String AGGREGATE_SAMPLES = "aggregateSamples";
        private String moleculeName;
        private HashMap<String, ArrayList<Alignment>> coverageMap = new HashMap<String, ArrayList<Alignment>>();
        private Long moleculeLength = 0l;

        private RecruitmentMoleculeCoverageInfo(String moleculeName) {
            this.moleculeName = moleculeName;
        }

        public String getMoleculeName() {
            return moleculeName;
        }

        public void setMoleculeName(String moleculeName) {
            this.moleculeName = moleculeName;
        }

        public HashMap<String, ArrayList<Alignment>> getCoverageMap() {
            return coverageMap;
        }

        public void addAlignmentToSample(String sample, Alignment alignment) {
            ArrayList<Alignment> tmpAlignments = coverageMap.get(sample);
            if (null == tmpAlignments) {
                tmpAlignments = new ArrayList<Alignment>();
            }
            tmpAlignments.add(alignment);
            coverageMap.put(sample, tmpAlignments);
        }

        public Long getMoleculeLength() {
            return moleculeLength;
        }

        public void setMoleculeLength(Long moleculeLength) {
            this.moleculeLength = moleculeLength;
        }

        public Long getTotalAlignmentBasesBySample(String sample) {
            ArrayList<Alignment> tmpAlignments = coverageMap.get(sample);
            long totalBases = 0;
            for (Alignment tmpAlignment : tmpAlignments) {
                totalBases += tmpAlignment.length();
            }
            return totalBases;
        }

        public Integer getAlignmentCountForSample(String sample) {
            return coverageMap.get(sample).size();
        }

        public Long getUniqueCoverageLengthForSample(String sample) {
            // Do the collision detection and then report final coverage length
            List<Alignment> uniqueAlignments = getCoverageAlignments(coverageMap.get(sample));
            Long coverageLength = 0l;
            for (Alignment uniqueAlignment : uniqueAlignments) {
                coverageLength += uniqueAlignment.length();
            }
            return coverageLength;
        }

        public Long getTotalUniqueCoverageLength() {
            // Grab all alignments and create a unique set
            ArrayList<Alignment> totalAlignments = new ArrayList<Alignment>();
            for (String s : coverageMap.keySet()) {
                totalAlignments.addAll(coverageMap.get(s));
            }
            List<Alignment> uniqueAlignments = getCoverageAlignments(totalAlignments);
            Long coverageLength = 0l;
            for (Alignment uniqueAlignment : uniqueAlignments) {
                coverageLength += uniqueAlignment.length();
            }
            return coverageLength;
        }

        private ArrayList<Alignment> getCoverageAlignments(ArrayList<Alignment> targetAlignments) {
            ArrayList<Alignment> returnList = new ArrayList<Alignment>();
            // Sort the alignments by begin points
            Collections.sort(targetAlignments);
            for (Alignment tmpNewAlignment : targetAlignments) {
                boolean newAlignmentIntersectsPrevious = false;
                for (Alignment returnAlignment : returnList) {
                    // If not the criteria below, they must be intersecting
                    if (!(tmpNewAlignment.getEnd() <= returnAlignment.getBegin()) && !(tmpNewAlignment.getBegin() >= returnAlignment.getEnd())) {
                        //System.out.println("Old alignment was\t("+alignment.getBegin()+","+alignment.getEnd()
                        //        +") and new alignment ("+tmpNewAlignment.getBegin()+","+tmpNewAlignment.getEnd()+")");
                        returnAlignment.setBegin(tmpNewAlignment.getBegin() <= returnAlignment.getBegin() ? tmpNewAlignment.getBegin() : returnAlignment.getBegin());
                        returnAlignment.setEnd(tmpNewAlignment.getEnd() >= returnAlignment.getEnd() ? tmpNewAlignment.getEnd() : returnAlignment.getEnd());
                        //System.out.println("New alignment is\t("+alignment.getBegin()+","+alignment.getEnd()+")\n");
                        newAlignmentIntersectsPrevious = true;
                        break;
                    }
                }
                if (!newAlignmentIntersectsPrevious) {
                    returnList.add(tmpNewAlignment);
                }
            }

            ArrayList<Alignment> validationList = new ArrayList<Alignment>();
            //  Check if the returnList has overlapping bases
            for (Alignment alignment : returnList) {
                boolean newAlignmentIntersectsPrevious = false;
                for (Alignment returnAlignment : returnList) {
                    // If not the criteria below, they must be intersecting
                    if (!(alignment.getEnd() <= returnAlignment.getBegin()) && !(alignment.getBegin() >= returnAlignment.getEnd())) {
                        if (!(alignment.getEnd()==returnAlignment.getEnd()&&alignment.getBegin()==returnAlignment.getBegin())) {
                            System.out.println("The return alignments overlap!!!!!!!!!!!!!!!!");
                            System.out.println("Return:"+returnAlignment.getBegin()+" "+returnAlignment.getEnd());
                            System.out.println("Align :"+alignment.getBegin()+" "+alignment.getEnd());
                        }
                    }
                }
            }
            return returnList;
        }

        public Long getTotalNumberAlignments() {
            Long numAlignments = 0l;
            for (String s : coverageMap.keySet()) {
                List<Alignment> tmpAlignments = coverageMap.get(s);
                if (null != tmpAlignments) {
                    numAlignments += tmpAlignments.size();
                }
            }
            return numAlignments;
        }
    }

    /**
     * Quick-and-dirty alignment class.  Sorting results in an ordering by begin coordinate.
     */
    public class Alignment implements Comparable{
        private long begin;
        private long end;

        public Alignment(long begin, long end) {
            if (begin<end) {
                this.begin = begin;
                this.end = end;
            }
            else {
                this.begin=end;
                this.end  =begin;
            }
        }

        public long getBegin() {
            return begin;
        }

        public void setBegin(long begin) {
            this.begin = begin;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        public long length() {
            return end - begin;
        }

        @Override
        public int compareTo(Object o) {
            return Long.valueOf(this.begin).compareTo(((Alignment)o).getBegin());
        }
    }

    public void restartSamplingRecruitmentRuns(String pathToRecruitmentTasksTabFile, boolean restartJobs){
        try {
            Scanner scanner = new Scanner(new File("X/filestore/tsafford/Recruitment/1526476431503130917/recruitmentTasks.tab"));
            while(scanner.hasNextLine()){
                String[] pieces = scanner.nextLine().split("\t");
                String[] status = EJBFactory.getLocalComputeBean().getTaskStatus(Long.valueOf(pieces[1]));
                if (Event.ERROR_EVENT.equals(status[0])) {
                    System.out.println("Status of task "+pieces[1]+" : "+status[0]+". Resubmitting...");
                    if (restartJobs) {
                        EJBFactory.getRemoteComputeBean().addEventToTask(Long.valueOf(pieces[1]), new Event(Event.RESUBMIT_EVENT, new Date(), Event.RESUBMIT_EVENT));
                        EJBFactory.getRemoteComputeBean().submitJob("GenomeProjectBlastFRVGrid", Long.valueOf(pieces[1]));
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
