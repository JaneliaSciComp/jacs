
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.jtc.PropertyHelper;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.ParameterVOMapUserType;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.blast.TBlastNTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.vo.*;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.FileWriter;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 11:22:23 AM
 *
 * @version $Id: BlastTest.java 1 2011-02-16 21:07:19Z tprindle $
 */
@Singleton
@Startup
public class BlastTest extends AbstractComponentMBean implements BlastTestMBean {
    // todo This test is out of date.  OrderEJB doesn't exist.
    public static final String ORDER_EJB_PROP = "BlastServer.OrderEJB";

    Session session;

    private PropertyHelper helper;
    private static final Logger logger = Logger.getLogger(BlastTest.class);

    public BlastTest() {
        super("jacs");
    }

    public void submitFileNodeQueryBlastNTest() {
        try {
            System.out.println("\33[31m");
            BlastNTask blastNTask = new BlastNTask();
            //pmap.put(BlastNTask.PARAM_query, new TextParameterVO("1042614268924526946", 30));
            blastNTask.setParameter(BlastNTask.PARAM_query, "1232035500672418425");

            MultiSelectVO ms = new MultiSelectVO();
            ArrayList<String> dbList = new ArrayList<String>();
            //dbList.add("1015439607359078752");
            dbList.add("1054893807616655712");

            ms.setPotentialChoices(dbList);
            ms.setActualUserChoices(dbList);
            blastNTask.setParameter(BlastNTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
            blastNTask.setOwner("sreenath");
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.saveOrUpdateTask(blastNTask);
            computeBean.submitJob("BlastWithGridMerge", blastNTask.getObjectId());
            System.out.println("\33[0m");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param numOfJobs      Number of identical jobs to run
     * @param numOfAlinments Number of alignmens for each job
     */
    public String submitMultipleFileNodeQueryBlastNTest(int numOfJobs, int numOfAlinments) {
//        System.out.println("\33[31m");
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < numOfJobs; i++) {
            try {
                TBlastNTask blastTask = new TBlastNTask();
                //pmap.put(BlastNTask.PARAM_query, new TextParameterVO("1042614268924526946", 30));
                blastTask.setParameter(BlastNTask.PARAM_query, "1387963929233197227");

                MultiSelectVO ms = new MultiSelectVO();
                ArrayList<String> dbList = new ArrayList<String>();
                //dbList.add("1015439607359078752");
                dbList.add("1359306933256847992");

                ms.setPotentialChoices(dbList);
                ms.setActualUserChoices(dbList);
                blastTask.setParameter(TBlastNTask.PARAM_databaseAlignments, String.valueOf(numOfAlinments));
                blastTask.setParameter(TBlastNTask.PARAM_evalue, "-3");
                blastTask.setParameter(TBlastNTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
                blastTask.setParameter(TBlastNTask.PARAM_project, "08020");
                blastTask.setParameter(TBlastNTask.PARAM_finalGappedDropoff, "0");
                blastTask.setParameter(TBlastNTask.PARAM_gappedAlignmentDropoff, "0");
                blastTask.setParameter(TBlastNTask.PARAM_hitExtensionThreshold, "0");
                blastTask.setParameter(TBlastNTask.PARAM_multiHitWindowSize, "0");
                blastTask.setParameter(TBlastNTask.PARAM_ungappedExtensionDropoff, "0");
                blastTask.setParameter(TBlastNTask.PARAM_wordsize, "0");

                blastTask.setOwner("lkagan");
                blastTask.setJobName("Test Huge File " + i);
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                blastTask = (TBlastNTask) computeBean.saveOrUpdateTask(blastTask);
                System.out.println("Submitted task " + blastTask.getObjectId());
                out.append("Submitted task ").append(blastTask.getObjectId()).append("\n");
                computeBean.submitJob("BlastWithGridMerge", blastTask.getObjectId());
//                computeBean.submitJob("BlastSplitterTest", blastTask.getObjectId());

            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
//        System.out.println("\33[0m");
        return out.toString();
    }

    public void submitDataNodeQueryBlastNTest() {
        try {

            BlastNTask blastNTask = new BlastNTask();
            blastNTask.setParameter(BlastNTask.PARAM_query, "1023035858808209761");
            MultiSelectVO ms = new MultiSelectVO();
            ArrayList<String> dbList = new ArrayList<String>();
            dbList.add("1054893807616655712");
            ms.setPotentialChoices(dbList);
            ms.setActualUserChoices(dbList);
            blastNTask.setParameter(BlastNTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
            blastNTask.setOwner("smurphy");
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.saveOrUpdateTask(blastNTask);
            computeBean.submitJob("BlastWithGridMerge", blastNTask.getObjectId());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void submitFrvBlastNTest() {
        try {
            System.out.println("Start testing FRV blast");

            // Run BlastN using the new query node and All Metagenomic Reads subject db
            BlastNTask blastNTask = new BlastNTask();
            blastNTask.setJobName("FRV Blast");

            blastNTask.setParameter(BlastNTask.PARAM_query, "1232035500672418425");
            MultiSelectVO ms = new MultiSelectVO();

            ArrayList<String> dbList = new ArrayList<String>();
            // Get the id for the node, name="All Metagenomic Sequence Reads (N)" subject db
            dbList.add("1054893807616655712");

            ms.setPotentialChoices(dbList);
            ms.setActualUserChoices(dbList);
            blastNTask.setParameter(BlastNTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
            blastNTask.setParameter(BlastNTask.PARAM_databaseAlignments, "5000");
            blastNTask.setParameter(BlastNTask.PARAM_lowerCaseFiltering, "false");
            blastNTask.setParameter(BlastNTask.PARAM_evalue, "-4");
            blastNTask.setParameter(BlastNTask.PARAM_mismatchPenalty, "-5");

            // NOTE: The databaseSize is calculated on-the-fly and not on any number passed to the task
            //blastNTask.setParameter(BlastNTask.PARAM_databaseSize, "3000000000");
            blastNTask.setParameter(BlastNTask.PARAM_databaseDescriptions, "10");
            blastNTask.setParameter(BlastNTask.PARAM_gappedAlignmentDropoff, "150");
            blastNTask.setParameter(BlastNTask.PARAM_matchReward, "4");
            blastNTask.setParameter(BlastNTask.PARAM_filter, "m L");

            blastNTask.setOwner("sreenath");

            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.saveOrUpdateTask(blastNTask);
            computeBean.submitJob("FRVBlast", blastNTask.getObjectId());

            System.out.println("End testing FRV blast");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void submitBlastJobType1() {
        try {
            submitBlastJob("SARGNT,SKA53,ELI,NRNT");
        }
        catch (Exception ex) {
            logger.error("Exception: " + ex, ex);
        }
    }

    private void submitBlastJob(String dbTagList) throws Exception {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        props.put(Context.PROVIDER_URL, "jnp://localhost:1099");
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        InitialContext ic = new InitialContext(props);
        //String lookupString = "java:/"+orderEJBName;
        String lookupString = helper.getProperty(ORDER_EJB_PROP, "OrderEJB");
        if (logger.isInfoEnabled()) {
            logger.info("Using ejb lookup string: " + lookupString);
        }
        Object ejbHome = ic.lookup(lookupString);
        if (logger.isInfoEnabled()) {
            logger.info("Received class: " + ejbHome.getClass());
        }
        if (logger.isInfoEnabled()) {
            logger.info("submitting BlastJob with database as: " + dbTagList);
        }
        logger.info("Create order returned orderId: TODO - create createtask");
    }

    public void start() {
        helper = PropertyHelper.getInstance();
    }

    public void stop() {
    }

    public void saveTaskParameterTest() {
        try {
            // First, save a createtask
            BlastNTask b1 = new BlastNTask();
            EJBFactory.getRemoteComputeBean().saveOrUpdateTask(b1);
            // Then, read a createtask and compare
            BlastNTask b2 = (BlastNTask) (EJBFactory.getLocalComputeBean().genericLoad(BlastNTask.class, b1.getObjectId()));
            Set keySet = b2.getParameterKeySet();
            System.err.println("PVOMUT Debug - value of createtask after query:");
            for (Object o : keySet) {
                System.err.println(o.toString() + ":" + b2.getParameter((String) o));
            }
            System.err.println("Evalue is:" + (b2.getParameter(BlastTask.PARAM_evalue)));
            if (b1.equals(b2)) {
                System.err.println("B1 equals B2");
            }
            else {
                System.err.println("B1 does not equal B2");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTasksToParameterStringMap() {
        Map<Long, String> taskPvoMap;
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            taskPvoMap = computeBean.getAllTaskPvoStrings();
            // This is commented out to simplify permissions issues when running at UCSD
            //java.io.PrintWriter taskParamFile = new java.io.PrintWriter(new java.io.FileOutputStream("taskParamFile.tsv"));
            for (Long key : taskPvoMap.keySet()) {
                String pvoString = taskPvoMap.get(key);
                if (pvoString != null) {
                    Map pvoMap = new HashMap();
                    logger.info("Task=" + key + " populating PVO map");
                    ParameterVOMapUserType.populateMapWithPVOs(pvoMap, pvoString);
                    logger.info("Task=" + key + " getting new string map");
                    Map<String, String> newParamMap = generateStringParameterMapFromPvoMap(pvoMap);
                    logger.info("Task=" + key + " loading");
                    Task task = computeBean.getTaskById(key);
                    logger.info("Task=" + key + " setting new values");
                    for (String sk : newParamMap.keySet()) {
                        String value = newParamMap.get(sk);
                        //taskParamFile.println(key.toString() + "\t" + sk + "\t" + value);
                        task.setParameter(sk, value);
                    }
                    logger.info("Task=" + key + " updating");
                    computeBean.saveOrUpdateTask(task);
                }
                else {
                    logger.info("Task=" + key + " skipping because pvo string is null");
                }
            }
            //taskParamFile.flush();
            //taskParamFile.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> generateStringParameterMapFromPvoMap(Map pvoMap) {
        Map<String, String> result = new HashMap<String, String>();
        for (Object o : pvoMap.keySet()) {
            ParameterVO pvo = (ParameterVO) pvoMap.get(o);
            String s = (String) o;
            String newValue = "";
            if (pvo instanceof LongParameterVO) {
                newValue = String.valueOf(((LongParameterVO) pvo).getActualValue());
            }
            else if (pvo instanceof DoubleParameterVO) {
                newValue = String.valueOf(((DoubleParameterVO) pvo).getActualValue());
            }
            else if (pvo instanceof MultiSelectVO) {
                newValue = pvo.getStringValue();
            }
            else if (pvo instanceof SingleSelectVO) {
                newValue = pvo.getStringValue();
            }
            else if (pvo instanceof TextParameterVO) {
                newValue = pvo.getStringValue();
            }
            else if (pvo instanceof BooleanParameterVO) {
                newValue = String.valueOf(((BooleanParameterVO) pvo).getBooleanValue());
            }
            else {
                result.put(s, pvo.getStringValue());
            }
            result.put(s, newValue);
        }
        return result;
    }


    public void deleteBlastResultsOrOrphanDirsForUser(String username, boolean isDebug) {
        File[] userDirs = new File(SystemConfigurationProperties.getString("FileStore.CentralDir") + File.separator + username).listFiles();
        for (File userFile : userDirs) {
            // The line below verifies a dir name that is all digits (node ids)
            if (userFile.isDirectory() && userFile.getName().matches("^\\d+$")) {
                try {
                    ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                    Node tmpNode = computeBean.getNodeById(Long.parseLong(userFile.getName()));
                    if (null == tmpNode) {
                        // If we get here, we have a numeric dirname which the db knows nothing about.  Delete it.
                        if (!isDebug) FileUtil.deleteDirectory(userFile);
                        logger.debug("Deleted orphaned node " + userFile.getAbsolutePath());
                        continue;
                    }
                    if (tmpNode instanceof BlastResultFileNode) {
                        if (!isDebug) {
                            computeBean.deleteNode(username, tmpNode.getObjectId(), true);
                        }
                        else {
                            logger.debug("Would attempt deletion of node: " + tmpNode.getObjectId());
                        }
                    }
                    else {

                    }
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void deleteUserBlastNodesOrOrphanDirsBeforeDate(String username, int month, int day, int year, boolean isDebug) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day);
        Long deletionCutoffDate = TimebasedIdentifierGenerator.getUidApproximationOfDate(calendar.getTime());
        File[] userDirs = new File(SystemConfigurationProperties.getString("FileStore.CentralDir") + File.separator + username).listFiles();
        for (File userFile : userDirs) {
            // The line below verifies a dir name that is all digits (node ids)
            if (userFile.isDirectory() && userFile.getName().matches("^\\d+$")) {
                try {
                    ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                    Node tmpNode = computeBean.getNodeById(Long.parseLong(userFile.getName()));
                    if (null == tmpNode) {
                        // If we get here, we have a numeric dirname which the db knows nothing about.  Delete it.
                        FileUtil.deleteDirectory(userFile);
                        logger.debug("Deleted orphaned node " + userFile.getAbsolutePath());
                        continue;
                    }
                    if (tmpNode instanceof BlastResultFileNode && tmpNode.getObjectId() < deletionCutoffDate) {
                        if (!isDebug) {
                            computeBean.deleteNode(username, tmpNode.getObjectId(), true);
                        }
                        else {
                            logger.debug("Would attempt deletion of node: " + tmpNode.getObjectId());
                        }
                    }
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // This is a simple test designed to test a particular filesystem node, using Blast. It is not part of
    // the compute server services. This test is informal and designed to be manually configured and deployed
    // for filesystem performance testing.
    public void submitSimpleBlastGridTest(String queryPath, String dbPrefixPath, String outputPrefixPath, int numJobs, String queue) {
        try {
            DrmaaHelper drmaa = new DrmaaHelper(logger);
            // Validate input
            logger.info("Validating query file");
            File queryFile = new File(queryPath);
            if (!queryFile.exists()) {
                throw new Exception("Could not find query file=" + queryFile);
            }
            // Validate databases
            logger.info("Validating databases");
            List<File> dbList = new ArrayList<File>();
            for (int i = 0; i < numJobs; i++) {
                File dbFile = new File(dbPrefixPath + "_" + i + ".fasta");
                if (!dbFile.exists()) {
                    throw new Exception("Could not find database file=" + dbFile);
                }
                dbList.add(dbFile);
            }
            // Create jobs
            logger.info("Creating jobs");
            //List<SerializableJobTemplate> jtList = new ArrayList<SerializableJobTemplate>();
            List<String> jobList = new ArrayList<String>();
            int i = 0;
            for (File dbFile : dbList) {
                File outputFile = new File(outputPrefixPath + "_" + i);
                SerializableJobTemplate jt = drmaa.createJobTemplate(new SerializableJobTemplate());
                jt.setRemoteCommand("bash");
                File jobFile = new File(outputPrefixPath + "_" + i + ".sh");
                FileWriter writer = new FileWriter(jobFile);
                writer.write("touch " + jobFile.getAbsolutePath() + ".start");
                writer.write("\n");
                writer.write("time /groups/jacs/jacsHosts/servers/jacs-data/executables/blast-2.2.15/bin/blastall ");
                String[] args = new String[8];
                args[0] = "-p";
                args[1] = "blastn";
                args[2] = "-i";
                args[3] = queryFile.getAbsolutePath();
                args[4] = "-d";
                args[5] = dbFile.getAbsolutePath();
                args[6] = "-o";
                args[7] = outputPrefixPath + "_" + i;
                for (String arg : args) {
                    writer.write(" " + arg);
                }
                writer.write("\n");
                writer.write("touch " + jobFile.getAbsolutePath() + ".end");
                writer.write("\n");
                writer.close();
                jt.setArgs(Arrays.asList(jobFile.getAbsolutePath()));
                String equivCmd = "/groups/jacs/jacsHosts/servers/jacs-data/executables/blast-2.2.15/bin/blastall -p blastn -i " + queryFile.getAbsolutePath() + " -d " + dbFile.getAbsolutePath() + " -o " + outputPrefixPath + "_" + i;
                logger.info("Using equivalent command=" + equivCmd);
                jt.setWorkingDirectory(dbFile.getParentFile().getAbsolutePath());
                jt.setErrorPath(":" + outputFile.getAbsolutePath() + ".err");
                jt.setOutputPath(":" + outputFile.getAbsolutePath() + ".out");
                // Apply a RegEx to replace any non-alphanumeric character with "_".  SGE is finicky that way.
                jt.setJobName("blastTest_" + i);
                jt.setNativeSpecification(queue);
                jt.setNativeSpecification("-P 08020");
                //jtList.add(jt);
                logger.info("calling drmaa runJob for outputFile=" + outputFile.getAbsolutePath());
                jobList.add(drmaa.runJob(jt));
                logger.info("done - now deleting job template");
                drmaa.deleteJobTemplate(jt);
                logger.info("done deleting job template");
                i++;
            }
            // Wait for jobs
            i = 0;
            for (String jobId : jobList) {
                logger.info("Waiting for job=" + i);
                drmaa.waitForJob(jobId, "waiting for job " + i, null, -1);
                logger.info("done");
                i++;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Method uses a text file of dir id's to remove, mined by this...
     * select * from node where (user_id=60 or node_owner='system') and subclass in ('BlastResultFileNode', 'FastaFileNode')
     */
    public void removeFastaAndResultFileNodesForSystem() {
        try {
            String rootDir = "/filestore/system/";
            Scanner scanner = new Scanner(new File(rootDir + "rmDirs.txt"));
            while (scanner.hasNextLine()) {
                String tmpDirToNuke = scanner.nextLine();
                SystemCall call = new SystemCall(logger);
                call.emulateCommandLine("rm -rf " + rootDir + tmpDirToNuke, true);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getCumulativeCpuTime(long taskId) {
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            return computeBean.getCumulativeCpuTime(taskId);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

//    public void convertBlastXmlToAnotherFormat(String pathToSourceXmlResultFile, String outputFormat, String newFileDestinationDir){
//        try {
//            TestUtils.writeSerializedBlastResultsFile(new File(pathToSourceXmlResultFile),new File(newFileDestinationDir));
//            // persist results as desired output format
//            BlastResultCollectionConverter brcc =
//                    new BlastResultCollectionConverter(testDataOutDir,
//                                                       0,
//                                                       bgcms.getQueryCountWithHits(),
//                                                       true,
//                                                       true,
//                                                       new String[]{outputFormat});
//            brcc.process();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

}
