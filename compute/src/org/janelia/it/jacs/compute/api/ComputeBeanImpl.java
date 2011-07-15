/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.compute.api;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.log4j.Logger;
import org.hibernate.*;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.engine.launcher.ProcessManager;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastDatabaseNode;
import org.janelia.it.jacs.model.user_data.tools.GenericServiceDefinitionNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.MailHelper;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

/**
 * This class implements service calls used by remote clients of Compute server.  It also contains service
 * calls used throughout Compute that need to run in a separate transaction
 *
 * @author Sean Murphy
 * @author Tareq Nabeel
 */
@Stateless(name = "ComputeEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 200, timeout = 10000)
public class ComputeBeanImpl implements ComputeBeanLocal, ComputeBeanRemote {
    private Logger logger = Logger.getLogger(this.getClass());
    public static final String APP_VERSION = "jacs.version";
    public static final String COMPUTE_EJB_PROP = "ComputeEJB.Name";
    public static final String MDB_PROVIDER_URL_PROP = "AsyncMessageInterface.ProviderURL";
    public static final String FILE_STORE_CENTRAL_DIR_PROP = "FileStore.CentralDir";
    private ComputeDAO computeDAO = new ComputeDAO(logger);

    public ComputeBeanImpl() {
    }

    public String getAppVersion() {
        String appVersion = null;
        try {
            appVersion = SystemConfigurationProperties.getString(APP_VERSION);
        }
        catch (Exception e) {
            logger.error("Unexpected error while getting the application version", e);
        }
        return appVersion;
    }

    public boolean buildUserFilestoreDirectory(String userLoginName) {
        try {
            User user = computeDAO.getUserByName(userLoginName);
            if (user == null) {
                logger.error("User object for userLoginName " + userLoginName + " returns null");
                throw new Exception("Received null user");
            }
            String fileNodeStorePath = SystemConfigurationProperties.getString(FILE_STORE_CENTRAL_DIR_PROP);
            FileUtil.ensureDirExists(fileNodeStorePath + "/" + userLoginName);
            return true;
        }
        catch (Throwable t) {
            t.printStackTrace();
            logger.error("Error in createUserFilestoreDirectory: " + t, t);
        }
        return false;
    }

    public void setTaskNote(long taskId, String note) throws DaoException {
        Task task = computeDAO.getTaskById(taskId);
        task.setTaskNote(note);
        computeDAO.saveOrUpdate(task);
    }

    public void addTaskNote(long taskId, String note) throws DaoException {
        Task task = computeDAO.getTaskById(taskId);
        task.getMessages().add(new TaskMessage(task,note));
        task.setTaskNote(note);
        computeDAO.saveOrUpdate(task);
    }

    public Long submitJob(String processDefName, Map<String, Object> processConfiguration) {
        ProcessManager processManager = new ProcessManager();
        return processManager.launch(processDefName, processConfiguration);
    }

    public void submitJob(String processDefName, long taskId) {
        ProcessManager processManager = new ProcessManager();
        processManager.launch(processDefName, taskId);
    }

    public void submitJobs(String processDefName, List<Long> taskIds) {
        ProcessManager processManager = new ProcessManager();
        processManager.launch(processDefName, taskIds);
    }

    public String[] getTaskStatus(long taskId) throws DaoException {
        return computeDAO.getTaskStatus(taskId);
    }

    public Task getTaskById(long taskId) {
        return computeDAO.getTaskById(taskId);
    }

    public Task getTaskWithMessages(long taskId) {
        return computeDAO.getTaskWithMessages(taskId);
    }

    public User getUserByName(String name) {
        return computeDAO.getUserByName(name);
    }
    
    public void removePreferenceCategory(String categoryName) throws DaoException {

        Session session = null;
        Transaction tx = null;
        
        try {
            session = computeDAO.getCurrentSession();
            tx = session.beginTransaction();
            // Hard coding SQL is bad, but Hibernate doesn't make it easy to delete from a component map
    		Query query = session.createSQLQuery("delete from user_preference_map where category='"+categoryName+"'");
    		int num = query.executeUpdate();
        	logger.info("Deleted "+num+" preferences in category "+categoryName);
        	session.flush();
            tx.commit();
        }
        catch (Exception e) {
        	tx.rollback();
        	logger.error("Error trying to remove preference category "+categoryName,e);
            throw new DaoException(e);
        }
        finally {
        	if (session != null) session.close();
        }
    }

    public Node getBlastDatabaseFileNodeByName(String name) {
        return computeDAO.getBlastDatabaseFileNodeByName(name);
    }

    public Node getResultNodeByTaskId(long taskId) {
        return computeDAO.getResultNodeByTaskId(taskId);
    }

    public List<Node> getResultNodesByTaskId(long taskId) {
        return computeDAO.getResultNodesByTaskId(taskId);
    }

    public Node getNodeById(long nodeId) {
        return computeDAO.getNodeById(nodeId);
    }

    public Object genericSave(Object object) throws DaoException {
        computeDAO.genericSave(object);
        return object;
    }

    public void genericDelete(Object object) throws DaoException {
        computeDAO.genericDelete(object);
    }
    
    public Node createNode(Node node) throws DaoException {
        return (Node) computeDAO.genericCreateAndReturn(node);
    }

    public Object genericLoad(Class c, Long id) throws DaoException {
        return computeDAO.genericLoad(c, id);
    }

    public Long getBlastDatabaseFileNodeIdByName(String name) {
        Node node = getBlastDatabaseFileNodeByName(name);
        if (node == null) {
            throw new RuntimeException("Node " + name + " does not exist");
        }
        return node.getObjectId();
    }

    public BlastResultNode getBlastHitResultDataNodeByTaskId(Long taskId) {
        return computeDAO.getBlastHitResultDataNodeByTaskId(taskId);
    }

    public Long getBlastHitCountByTaskId(Long taskId) throws DaoException {
        return computeDAO.getBlastHitCountByTaskId(taskId);
    }

    public BlastResultFileNode getBlastResultFileNodeByTaskId(Long taskId) throws DaoException {
        return computeDAO.getBlastResultFileNodeByTaskId(taskId);
    }

    public Task saveOrUpdateTask(Task task) throws DaoException {
        computeDAO.saveOrUpdate(task);
        return task;
    }

    public Node saveOrUpdateNode(Node node) throws DaoException {
        computeDAO.saveOrUpdate(node);
        return node;
    }

    public void saveTaskMessages(long taskId, Set<TaskMessage> messages) throws DaoException {
        computeDAO.saveTaskMessages(taskId, messages);
    }

    public Event saveEvent(Long taskId, String eventType, String description, Date timestamp) throws DaoException {
        try {
            if (Event.ERROR_EVENT.equals(eventType) || Event.SUBTASKERROR_EVENT.equals(eventType)) {
                String errorMessage = "Recording '" + eventType + "' Event: TaskID=" + taskId + " Descr=" + description;
                logger.debug(errorMessage);
                formatAndSendErrorMessage(taskId, description, timestamp.toString());
            }
        }
        catch (Throwable e) {
            logger.error("Error trying to record a failure event.  Continuing...\n" + e.getMessage());
        }
        return computeDAO.createEvent(taskId, eventType, description, timestamp);
    }

    public void addEventToTask(Long taskId, Event event) throws DaoException {
        try {
            if (Event.ERROR_EVENT.equals(event.getEventType()) || Event.SUBTASKERROR_EVENT.equals(event.getEventType())) {
                String errorMessage = "Recording '" + event.getEventType() + "' Event: TaskID=" + taskId + " Descr=" + event.getDescription();
                logger.debug(errorMessage);
                formatAndSendErrorMessage(taskId, event.getDescription(), event.getTimestamp().toString());
            }
        }
        catch (Throwable e) {
            logger.error("Error trying to record a failure event.  Continuing...\n" + e.getMessage());
        }
        computeDAO.addEventToTask(taskId, event);
    }

    private void formatAndSendErrorMessage(Long taskId, String eventDescription, String eventTimestamp) {
        StringBuffer sbuf = new StringBuffer();
        try {
            if (!SystemConfigurationProperties.getBoolean("System.SendErrorEmails")) {
                return;
            }
            Task targetTask = EJBFactory.getLocalComputeBean().getTaskById(taskId);
            String hostname = SystemConfigurationProperties.getString("System.ServerName");
            String logFile = SystemConfigurationProperties.getString("Logs.Dir") + File.separator + "task" + taskId + ".log";
            sbuf.append("Host: ").append(hostname).append("\n");
            sbuf.append("User: ").append(targetTask.getOwner()).append("\n");
            sbuf.append("Task Id: ").append(taskId.toString()).append("\n");
            sbuf.append("Task log: ").append(logFile).append("\n");
            sbuf.append("Task Type: ").append(targetTask.getTaskName()).append("\n");
            sbuf.append("Job Name: ").append(targetTask.getJobName()).append("\n");
            sbuf.append("Event time: ").append(eventTimestamp).append("\n");
            sbuf.append("Error Message:\n").append(eventDescription).append("\n");
            // todo need to show session info, event history, node info, etc 
            MailHelper helper = new MailHelper();
            // Comma-separated list of emails
            String[] errorMessageDestinations = SystemConfigurationProperties.getString("System.ErrorMessageDestination").split(",");
            for (String errorMessageDestination : errorMessageDestinations) {
                helper.sendEmail("saffordt@janelia.hhmi.org", errorMessageDestination, "VICS Error - " + hostname,
                        sbuf.toString());
            }
        }
        catch (Throwable e) {
            logger.error("Problem creating the error message");
        }
    }

    public void updateTaskStatus(long taskId, String status, String comment) throws DaoException {
        computeDAO.updateTaskStatus(taskId, status, comment);
    }

    public void recordProcessSuccess(ProcessDef processDef, Long processId) {
        try {
            computeDAO.recordProcessSuccess(processDef, processId);
        }
        catch (Exception e) {
            logger.error("Caught exception updating status of process: " + processDef, e);
        }
    }

    public void recordProcessError(ProcessDef processDef, Long processId, Throwable e) {
        try {
            updateTaskStatus(processId, Event.ERROR_EVENT, e.getMessage());
        }
        catch (Exception ee) {
            logger.error("Caught exception updating status of process: " + processDef, ee);
        }
    }

    public Map<Long, String> getAllTaskPvoStrings() {
        Map<Long, String> result = null;
        try {
            result = computeDAO.getAllTaskPvoStrings();
        }
        catch (Exception e) {
            logger.error("Caught exception in getAllTaskPvoStrings(): " + e, e);
        }
        return result;
    }


    public void setRVHitsForNode(Long recruitmentNodeId, String numRecruited) {
        try {
            computeDAO.setRVHitsForNode(recruitmentNodeId, numRecruited);
        }
        catch (Exception e) {
            logger.error("Caught exception updating the number of rv hits\n" + e.getMessage());
        }
    }


    public void setBlastHitsForNode(Long nodeId, Long numHits) throws DaoException {
        BlastResultFileNode node = (BlastResultFileNode) computeDAO.getNodeById(nodeId);
        node.setBlastHitCount(numHits);
        computeDAO.saveOrUpdate(node);
    }

    public Task getTaskForNodeId(long nodeId) {
        return computeDAO.getTaskForNode(nodeId);
    }

    // 1st test method... move to test ejb if test methods grow
    public void verifyBlastResultContents(Long blastResultFileNodeId, String expectedBlastResultsZipFilePath) throws DaoException,
            IOException {
        BlastResultFileNode resultFileNode = (BlastResultFileNode) genericLoad(BlastResultFileNode.class, blastResultFileNodeId);
        String actualBlastResultsZipFilePath = FileUtil.checkFileExists(resultFileNode.getDirectoryPath()).getAbsolutePath()
                + FileUtil.FILE_SEPARATOR + "blastResults.xml";
        FileUtil.ensureFileContentsEqual(expectedBlastResultsZipFilePath, actualBlastResultsZipFilePath);
    }

    public List<Node> getNodesByClassAndUser(String className, String username) throws DaoException {
        return computeDAO.getNodesByClassAndUser(className, username);
    }

    public Node getInputNodeForTask(Long objectId) {
        Task task = computeDAO.getTaskById(objectId);
        if (null == task.getInputNodes() || !task.getInputNodes().iterator().hasNext()) {
            System.out.println("Task " + objectId + " does not have an input node.");
            return null;
        }
        return task.getInputNodes().iterator().next();
    }

    public RecruitmentResultFileNode getSystemRecruitmentResultNodeByRecruitmentFileNodeId(String giNumber) throws DaoException {
        return computeDAO.getSystemRecruitmentResultNodeByRecruitmentFileNodeId(giNumber);
    }

    public List getSampleInfo() throws DaoException {
        return computeDAO.getSampleInfo();
    }

    public String getSystemConfigurationProperty(String propertyKey) {
        return SystemConfigurationProperties.getString(propertyKey);
    }

    public List getHeaderDataForFRV(ArrayList readAccList) throws DaoException {
        return computeDAO.getHeaderDataForFRV(readAccList);
    }

    public String getRecruitmentFilterDataTaskForUserByGenbankId(String genbankName, String userLogin) {
        return computeDAO.getRecruitmentFilterDataTaskForUserByGenbankId(genbankName, userLogin);
    }

    public void setTaskParameter(Long taskId, String parameterKey, String parameterValue) throws DaoException {
        computeDAO.setTaskParameter(taskId, parameterKey, parameterValue);
    }

    /**
     * Method to clean up the db and filestore upon deletion of a node.
     *
     * @param nodeId - node id of what is to be deleted
     * @param clearFromFilestoreIfAppropriate
     *               - whether to nuke the filestore dir, if node is a FileNode
     * @return boolean of success
     */
    public boolean deleteNode(String username, Long nodeId, boolean clearFromFilestoreIfAppropriate) {
        try {
            // Test the ownership of the node to the username provided.  Basic security.
            // To add group security nodes and tasks should be owned by groups
            Node targetNode = computeDAO.getNodeById(nodeId);
            if (null==targetNode || !username.equals(targetNode.getOwner())){
                throw new SecurityException("The provided user does not own the node!");
            }
            // Get the node and deprecate the status
            targetNode.setVisibility(Node.VISIBILITY_PRIVATE_DEPRECATED);
            computeDAO.saveOrUpdate(targetNode);

            // Clean up the filestore if asked
            if (clearFromFilestoreIfAppropriate && targetNode instanceof FileNode) {
                FileNode tmpFileNode = (FileNode) targetNode;
                boolean successful = FileUtil.deleteDirectory(tmpFileNode.getDirectoryPath());
                if (successful) {
                    logger.debug("Successfully deleted " + tmpFileNode.getDirectoryPath());
                }
                else {
                    logger.debug("Warning: Unsuccessful in deleting " + tmpFileNode.getDirectoryPath() + ". Could already have been deleted.");
                    return false;
                }
            }
        }
        catch (Exception e) {
            logger.error("Unsuccessful in deleting the node." + e.getMessage());
            return false;
        }
        return true;
    }

    public int getNumCategoryResults(Long nodeId, String category) {
        try {
            String sql =
                    "select cast(count(1) as Integer)" +
                            "from " + category + "_ts_result " +
                            "where node_id=" + nodeId;
            SQLQuery sqlQuery = computeDAO.getCurrentSession().createSQLQuery(sql);
            return (Integer) sqlQuery.uniqueResult();
        }
        catch (Exception e) {
            logger.error("Unable to get result count for " + category + " on search " + nodeId);
            return -1;
        }

    }

    protected boolean invalidFastaFileNodeDir(File dir) {
        File[] fileList = dir.listFiles();
        if (fileList == null || fileList.length == 0) {
            return true;
        }
        boolean containsFasta = false;
        for (File f : fileList) {
            if (f.length() == 0) {
                return true;
            }
            if (f.getName().endsWith(".fasta")) {
                containsFasta = true;
            }
        }
        return !containsFasta;
    }

    protected void createDummyFastaFileNode(File dir, String type) throws Exception {
        String filename = "nucleotide.fasta";
        if (type.trim().toLowerCase().equals("peptide")) {
            filename = "peptide.fasta";
        }
        File file = new File(dir, filename);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(">null\n");
        fileWriter.write("NNNNN\n");
        fileWriter.close();
    }

    public List<Node> getNodeByName(String nodeName) throws DaoException {
        return computeDAO.getNodeByName(nodeName);
    }

    public void setSystemDataRelatedToGiNumberObsolete(String giNumber) throws DaoException {
        computeDAO.setSystemDataRelatedToGiNumberObsolete(giNumber);
    }

    /**
     * This method gets the list of samples.  This list is THE SAME data that's used by the front-end and the
     * sample.info file.
     *
     * @return list of sample names
     * @throws org.janelia.it.jacs.compute.access.DaoException
     *          problem getting the data
     */
    public List getAllSampleNamesAsList() throws DaoException {
        ArrayList<String> returnList = new ArrayList<String>();
        List sampleInfoList = getSampleInfo();
        for (Object o : sampleInfoList) {
            Object[] sampleInfo = (Object[]) o;
            returnList.add((String) sampleInfo[2]);
        }
        // TODO This is a HACK and needs to be fixed
        returnList.add("AUSOIL_managed_454PE3KB");
        returnList.add("AUSOIL_managed_454UP");
        returnList.add("AUSOIL_remnant_454PE3KB");
        returnList.add("AUSOIL_remnant_454UP");
        Collections.sort(returnList);
        return returnList;
    }

    public List<BlastDatabaseFileNode> getBlastDatabases() {
        return (List<BlastDatabaseFileNode>) computeDAO.getBlastDatabases(BlastDatabaseFileNode.class.getSimpleName());
    }

    public List<BlastDatabaseFileNode> getBlastDatabasesOfAUser(String username) {
        return (List<BlastDatabaseFileNode>) computeDAO.getBlastDatabasesOfAUser(BlastDatabaseFileNode.class.getSimpleName(), username);
    }

    public List<HmmerPfamDatabaseNode> getHmmerPfamDatabases() {
        return (List<HmmerPfamDatabaseNode>) computeDAO.getBlastDatabases(HmmerPfamDatabaseNode.class.getSimpleName());
    }

    public List<ReversePsiBlastDatabaseNode> getReversePsiBlastDatabases() {
        return (List<ReversePsiBlastDatabaseNode>) computeDAO.getBlastDatabases(ReversePsiBlastDatabaseNode.class.getSimpleName());
    }

    /**
     * This method creates a tar of the directory and then returns the name of the archive file (no path included!)
     *
     * @param archiveNamePrefix prefix of the new archive name
     * @param sourceNodeId      directory to tar
     * @return full name of the archive file itself (no path included)
     * @throws Exception there was a problem generating the archive
     */
    public String createTemporaryFileNodeArchive(String archiveNamePrefix, String sourceNodeId) throws Exception {
        try {
            FileNode tmpSourceNode = (FileNode) computeDAO.getNodeById(Long.valueOf(sourceNodeId));
            return FileUtil.tarCompressDirectoryWithSystemCall(new File(tmpSourceNode.getDirectoryPath()),
                    tmpSourceNode.getDirectoryPath() + File.separator + archiveNamePrefix + tmpSourceNode.getObjectId() + ".tar");
        }
        catch (Exception e) {
            logger.error("There was a problem in createTemporaryFileNodeArchive", e);
            throw new Exception("Unable to create the file archive.");
        }
    }

    public List<String> getFiles(String tmpDirectory, final boolean directoriesOnly) {
        File tmpDir = new File(tmpDirectory);
        String[] tmpResults = new String[0];
        if (tmpDir.exists() && tmpDir.isDirectory()) {
            tmpResults = tmpDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (new File(dir.getAbsolutePath() + File.separator + name)).isDirectory() == directoriesOnly;
                }
            });
        }
        List<String> finalResults = new ArrayList<String>();
        finalResults.add(tmpDirectory);
        finalResults.addAll(Arrays.asList(tmpResults));
        return finalResults;
    }

    public HashSet<String> getProjectCodes() throws Exception {
        return new DrmaaHelper(logger).getProjectCodes();
    }

    /**
     * THis method is intended to give a service a view of which child tasks have completed, or are in a given state.
     * @param parentTaskId - parent task id
     * @return a map of the taskId's as string and the event string
     * @throws Exception throws an error if there is a problem building the task map
     */
    public HashMap<String, String> getChildTaskStatusMap(Long parentTaskId) throws Exception {
        HashMap<String, String> childTaskStatusMap = new HashMap<String, String>();
        List<Task> childTaskList = computeDAO.getChildTasksByParentTaskId(parentTaskId);
        for (Task task : childTaskList) {
            childTaskStatusMap.put(task.getObjectId().toString(), task.getLastEvent().getEventType());
        }
        return childTaskStatusMap;
    }

    public List<Task> getUserTasksByType(String simpleName, String userName) {
        return computeDAO.getUserTasksByType(simpleName, userName);
    }

    public Task getRecruitmentFilterTaskByUserPipelineId(Long objectId) throws DaoException {
        return computeDAO.getRecruitmentFilterTaskByUserPipelineId(objectId);
    }

    public List<Task> getUserTasks(String userLogin) {
        return computeDAO.getUserTasks(userLogin);
    }

    public void setParentTaskId(Long parentTaskId, Long childTaskId) throws DaoException {
        computeDAO.setParentTaskId(parentTaskId, childTaskId);
    }

    public List<User> getAllUsers() {
        return computeDAO.getAllUsers();
    }

    public List<Task> getChildTasksByParentTaskId(long taskId) {
        return computeDAO.getChildTasksByParentTaskId(taskId);
    }

    public Long getSystemDatabaseIdByName(String databaseName) {
        return computeDAO.getSystemDatabaseIdByName(databaseName);
    }

    public long getCumulativeCpuTime(long taskId) {
        return computeDAO.getCumulativeCpuTime(taskId);
    }

    // todo Can this not be within the blast service?
    public void validateBlastTaskQueryDatabaseMatch(BlastTask blastTask) throws Exception {
        Long queryNodeId = new Long(blastTask.getParameter(BlastTask.PARAM_query));
        String databaseCsvString = blastTask.getParameter(BlastTask.PARAM_subjectDatabases);
        String[] dbArr = databaseCsvString.split(",");
        List<Long> databaseNodeIdList = new ArrayList<Long>();
        for (String db : dbArr) {
            Long databaseNodeId = new Long(db.trim());
            databaseNodeIdList.add(databaseNodeId);
        }
        FastaFileNode queryNode = (FastaFileNode) computeDAO.getNodeById(queryNodeId);
        if (null == queryNode) {
            throw new Exception("The query id passed in does not exist.");
        }
        String querySequenceType = queryNode.getSequenceType();
        String databaseSequenceType = null;
        for (Long dbId : databaseNodeIdList) {
            BlastDatabaseFileNode dbNode = (BlastDatabaseFileNode) computeDAO.getNodeById(dbId);
            if (null == dbNode) {
                throw new Exception("The subject db id passed in does not exist.");
            }
            if (databaseSequenceType == null) {
                databaseSequenceType = dbNode.getSequenceType();
            }
            else {
                if (!dbNode.getSequenceType().equals(databaseSequenceType)) {
                    throw new Exception("Database sequenceTypes for BlastTask must all be identical, i.e., all nucleotide or all peptide");
                }
            }
        }
        String errorMessage = "";
        if (blastTask instanceof BlastNTask ||
                blastTask instanceof MegablastTask ||
                blastTask instanceof TBlastXTask) {
            if (!querySequenceType.equals(FastaFileNode.NUCLEOTIDE)) {
                errorMessage += "QueryNode=" + queryNodeId + " does not have sequenceType=" + FastaFileNode.NUCLEOTIDE + " ";
            }
            if (null == databaseSequenceType || !databaseSequenceType.equals(BlastDatabaseFileNode.NUCLEOTIDE)) {
                errorMessage += "BlastDatabaseFileNode=" + dbArr[0] + " does not have sequenceType=" + BlastDatabaseFileNode.NUCLEOTIDE + " ";
            }
        }
        else if (blastTask instanceof BlastPTask) {
            if (!querySequenceType.equals(FastaFileNode.PEPTIDE)) {
                errorMessage += "QueryNode=" + queryNodeId + " does not have sequenceType=" + FastaFileNode.PEPTIDE + " ";
            }
            if (null == databaseSequenceType || !databaseSequenceType.equals(BlastDatabaseFileNode.PEPTIDE)) {
                errorMessage += "BlastDatabaseFileNode=" + dbArr[0] + " does not have sequenceType=" + BlastDatabaseFileNode.PEPTIDE + " ";
            }
        }
        else if (blastTask instanceof BlastXTask) {
            if (!querySequenceType.equals(FastaFileNode.NUCLEOTIDE)) {
                errorMessage += "QueryNode=" + queryNodeId + " does not have sequenceType=" + FastaFileNode.NUCLEOTIDE + " ";
            }
            if (null == databaseSequenceType || !databaseSequenceType.equals(BlastDatabaseFileNode.PEPTIDE)) {
                errorMessage += "BlastDatabaseFileNode=" + dbArr[0] + " does not have sequenceType=" + BlastDatabaseFileNode.PEPTIDE + " ";
            }
        }
        else if (blastTask instanceof TBlastNTask) {
            if (!querySequenceType.equals(FastaFileNode.PEPTIDE)) {
                errorMessage += "QueryNode=" + queryNodeId + " does not have sequenceType=" + FastaFileNode.PEPTIDE + " ";
            }
            if (null == databaseSequenceType || !databaseSequenceType.equals(BlastDatabaseFileNode.NUCLEOTIDE)) {
                errorMessage += "BlastDatabaseFileNode=" + dbArr[0] + " does not have sequenceType=" + BlastDatabaseFileNode.NUCLEOTIDE + " ";
            }
        }
        if (errorMessage.length() > 0) {
            errorMessage = "BlastTask has incompatible query vs db: " + errorMessage.trim();
            throw new Exception(errorMessage);
        }
    }

    public void deleteTaskById(Long taskId) throws Exception {
        // First get entire task tree with this task as parent task
        List<Long> taskList = getTaskTreeIdList(taskId);
        for (Long tid : taskList) {
            Task t = computeDAO.getTaskById(tid);
            List<Node> nodes = computeDAO.getResultNodesByTaskId(t.getObjectId());
            for (Node n : nodes) {
                deleteNode(t.getOwner(), n.getObjectId(), true);
            }
            t.setTaskDeleted(true);
            computeDAO.saveOrUpdate(t);
        }
    }

    // This method returns the ids of all tasks in the tree starting with given parent taskId
    public List<Long> getTaskTreeIdList(Long taskId) throws Exception {
        List<Long> taskTreeIdList = new ArrayList<Long>();
        taskTreeIdList.add(taskId);
        List<Task> childTasks = computeDAO.getChildTasksByParentTaskId(taskId);
        for (Task t : childTasks) {
            taskTreeIdList.add(t.getObjectId());
            List<Long> c2List = getTaskTreeIdList(t.getObjectId());
            for (Long l : c2List) {
                if (!taskTreeIdList.contains(l))
                    taskTreeIdList.add(l);
            }
        }
        return taskTreeIdList;
    }

    @Override
    public GenericServiceDefinitionNode getGenericServiceDefinitionByName(String serviceName) throws Exception {
        return computeDAO.getGenericServiceDefinitionByName(serviceName);
    }

    @Override
    public void validateFile(String filePath) throws Exception {
        File tmpFile = new File(filePath);
        if (!tmpFile.exists() || !tmpFile.canRead()) {
            throw new Exception("File (" + filePath + ") does not exist or can't be reached by VICS.");
        }
    }

}
