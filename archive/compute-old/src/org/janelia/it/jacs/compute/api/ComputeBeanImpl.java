
package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SubjectDAO;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.engine.def.ProcessDef;
import org.janelia.it.jacs.compute.engine.launcher.ProcessManager;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.blast.*;
import org.janelia.it.jacs.model.tasks.utility.ContinuousExecutionTask;
import org.janelia.it.jacs.model.user_data.*;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastDatabaseNode;
import org.janelia.it.jacs.model.user_data.tools.GenericServiceDefinitionNode;
import org.janelia.it.jacs.shared.utils.ControlledVocabElement;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.MailHelper;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//@Interceptors({UsageInterceptor.class})
@PoolClass(value = StrictMaxPool.class, maxSize = 200, timeout = 10000)
public class ComputeBeanImpl implements ComputeBeanLocal, ComputeBeanRemote {
    
    private Logger logger = Logger.getLogger(this.getClass());
    
    public static final String APP_VERSION = "jacs.version";
    public static final String COMPUTE_EJB_PROP = "ComputeEJB.Name";
    public static final String MDB_PROVIDER_URL_PROP = "AsyncMessageInterface.ProviderURL";
    public static final String FILE_STORE_CENTRAL_DIR_PROP = "FileStore.CentralDir";

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
    
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    private ComputeDAO computeDAO = new ComputeDAO(logger);
    private SubjectDAO subjectDAO = new SubjectDAO(logger);
    
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


    /**
     * @deprecated
     * @param userLogin
     * @param clientVersion
     */
    public void beginSession(String userLogin, String clientVersion) {
        logger.info("Begin session for "+userLogin+" using "+clientVersion);
    }

    /**
     * @deprecated
     * @param userLogin
     */
    public void endSession(String userLogin) {
        logger.info("End session for "+userLogin);
    }

    /**
     * Method used to initiate a session.  A call to this forces a unique session id to be created and passed
     * back to a calling client
     * @param userLogin the principal user who is performing the action
     * @param clientVersion version of the tool being used; assumes the tool is the Workstation at this point
     * @return the first UserToolEvent object.  The calling client is going to grab the session id from here to use on
     *          all subsequent calls
     */
    public UserToolEvent beginSession(String userLogin, String toolName, String clientVersion) {
    	logger.info("Begin session for "+userLogin+" using "+clientVersion);
        return addEventToSession(new UserToolEvent(null, userLogin, toolName,
                UserToolEvent.TOOL_CATEGORY_SESSION, UserToolEvent.TOOL_EVENT_LOGIN, new Date()));
    }

    public UserToolEvent addEventToSession(UserToolEvent userToolEvent) {
        // Try to log an event but DO NOT tank anything if this fails.  Record the error only in the log.
        try {
            return computeDAO.addEventToSession(userToolEvent);
        }
        catch (DaoException e) {
            logger.error("Cannot log event to session: "+(null==userToolEvent?"null":userToolEvent.toString()));
            logger.error("Error: "+e.getMessage());
        }
        return null;
    }

    public void endSession(String userLogin, String toolName, Long userSessionId) {
    	logger.info("End session for "+userLogin);
        addEventToSession(new UserToolEvent(userSessionId, userLogin, toolName,
                UserToolEvent.TOOL_CATEGORY_SESSION, UserToolEvent.TOOL_EVENT_LOGOUT, new Date()));
    }

    public Subject login(String userLogin, String password) {
        try {
            userLogin = EntityUtils.getNameFromSubjectKey(userLogin);
            
            // Connect to LDAP server.
            String ldapBase = SystemConfigurationProperties.getString("LDAP.Base");
            String ldapURL  = SystemConfigurationProperties.getString("LDAP.URL");
            Hashtable<String, String> env = new Hashtable<String, String>(2);
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://"+ldapURL);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, "cn="+userLogin+","+ldapBase);
            env.put(Context.SECURITY_CREDENTIALS, password);

            DirContext ctx = new InitialDirContext(env);
            if (null!=ctx) {
                logger.debug("Authenticated user "+userLogin+" successfully.");
            }

//            Attributes attrs = ctx.getAttributes("cn="+userLogin+","+ldapBase);
//            NamingEnumeration enumeration = attrs.getAll();
//            while (enumeration.hasMore()) {
//                Attribute tmpAtt = (Attribute)enumeration.next();
//                System.out.println(tmpAtt.getID()+" "+tmpAtt.get().toString());
//            }
//
            Subject subject = getSubjectWithPreferences(userLogin);
            // If we don't know them, and they authenticated, add to the database and create a location in the filestore
            if (null == subject) {
                boolean successful = createUser(userLogin);
                if (!successful) {
                    // will not be able to execute any computes, so throw an exception
                    logger.error("Unable to create directory and/or account for user " + userLogin);
                    return null;
                }
            }
            return getSubjectWithPreferences(userLogin);
        }
        catch (Exception e) {
            logger.error("There was a problem logging in the user "+userLogin+"\n"+e.getMessage());
            return null;
        }
    }

    public Subject getSubjectByNameOrKey(String name) throws ComputeException {
        Subject subject = computeDAO.getSubjectByNameOrKey(name);
        if (subject==null) return null;
        // Eager load relationships
        if (subject instanceof User) {
            User user = ((User) subject);
            for(SubjectRelationship relation : user.getGroupRelationships()) {
                relation.getUser().getKey();
                relation.getGroup().getKey();
            }
        }
        else if (subject instanceof Group) {
            Group group = ((Group) subject);
            for(SubjectRelationship relation : group.getUserRelationships()) {
                relation.getUser().getKey();
                relation.getGroup().getKey();
            }
        }
        return subject;
    }
    
    public Subject getSubjectWithPreferences(String nameOrKey) throws ComputeException {
        Subject subject = getSubjectByNameOrKey(nameOrKey);
        if (subject==null) return null;
        // Eager load preferences
        subject.getPreferenceMap().size(); 
        return subject;
    }
    
    public User getUserByNameOrKey(String name) throws ComputeException {
        return computeDAO.getUserByNameOrKey(name);
    }
    
    public Group getGroupByNameOrKey(String name) throws ComputeException {
        return computeDAO.getGroupByNameOrKey(name);
    }
    
	public List<Subject> getSubjects() throws ComputeException {
        return subjectDAO.getSubjects();
	}
	
    public List<User> getUsers() throws ComputeException {
        return subjectDAO.getUsers();
    }
	
	public List<Group> getGroups() throws ComputeException {
        return subjectDAO.getGroups();
	}
    
    public boolean createUser(String newUserName) throws DaoException {
        try {
            logger.info("Creating user " + newUserName);
            User user = subjectDAO.createUser(newUserName);
            String fileNodeStorePath = SystemConfigurationProperties.getString(FILE_STORE_CENTRAL_DIR_PROP);
            File tmpUserDir = FileUtil.ensureDirExists(fileNodeStorePath + File.separator + newUserName);
            if (null!=user && null!=tmpUserDir && tmpUserDir.exists()) {
                return true;
            }
        }
        catch (IOException e) {
            logger.error("Error creating user's filestore",e);
            throw new DaoException("Error creating user's filestore", e);
        }
        catch (DaoException e) {
            logger.error("Error creating user",e);
            throw e;
        }
        return false;
    }
    
    public User createUser(String newUserName, String newFullName) throws ComputeException {
        try {
            createUser(newUserName);
            User user = getUserByNameOrKey(newUserName);
            user.setFullName(newFullName);
            saveOrUpdateSubject(user);
            return user;
        }
        catch (DaoException e) {
            logger.error("Error creating user",e);
            throw e;
        }
    }
    
    public Group createGroup(String userLogin, String groupName) throws DaoException {
        try {
            logger.info("Creating group " + groupName);
            return subjectDAO.createGroup(userLogin, groupName);
        }
        catch (DaoException e) {
            logger.error("Error creating group", e);
            throw e;
        }
    }

    public void removeGroup(String groupName) throws DaoException {
        try {
            logger.info("Removing group " + groupName);
            subjectDAO.removeGroup(groupName);
        }
        catch (DaoException e) {
            logger.error("Error removing group", e);
            throw e;
        }
    }
    
    public void addUserToGroup(String userName, String groupName) throws DaoException {
        try {
            logger.info("Adding user "+userName+" to group " + groupName);
            subjectDAO.addUserToGroup(userName, groupName);
        }
        catch (DaoException e) {
            logger.error("Error adding user to group", e);
            throw e;
        }
    }

    public void removeUserFromGroup(String groupUser, String groupName) throws DaoException {
        try {
            logger.info("Removing user "+groupUser+" from group " + groupName);
            subjectDAO.removeUserFromGroup(groupUser, groupName);
        }
        catch (DaoException e) {
            logger.error("Error removing user from group", e);
            throw e;
        }
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
    
    public void stopContinuousExecution(long taskId) throws ServiceException {
    	Task task = getTaskById(taskId);
    	if (task==null) throw new ServiceException("No such task with id "+taskId);
    	if (!(task instanceof ContinuousExecutionTask)) throw new ServiceException("This task is not a ContinuousExecutionTask: "+taskId);
    	ContinuousExecutionTask cet = (ContinuousExecutionTask)task;
    	cet.setEnabled(false);
    	try {
        	// This saves the enabled flash as well
        	computeDAO.createEvent(cet, "stopped", "User requested end of continuous execution", new Date());	
    	}
    	catch (DaoException e) {
    		throw new ServiceException(e);
    	}
    }

    public Task getMostRecentTaskWithNameAndParameters(String owner, String taskName, HashSet<TaskParameter> taskParameters) {
        Task matchingTask = null;
        logger.debug("Looking for matching task with params:");
        for(TaskParameter taskParameter : taskParameters) {
            logger.debug("  "+taskParameter.getName()+"="+taskParameter.getValue());
        }
        for(Task task : computeDAO.getMostRecentTasksWithName(owner, taskName)) {
            logger.debug("Considering recent "+task.getTaskName()+" task with params:");
            for(TaskParameter taskParameter : task.getTaskParameterSet()) {
                logger.debug("  "+taskParameter.getName()+"="+taskParameter.getValue());
            }
            
            if (areEqual(task.getTaskParameterSet(),taskParameters)) {
                logger.debug("Found match.");
                matchingTask = task;
                break;
            }
        }
        // Init lazy-loading events
        if (matchingTask!=null) {
            matchingTask.getEvents().size();
        }
        return matchingTask;
    }
    
    public int cancelIncompleteTasksWithName(String owner, String name) throws ComputeException {
        try {
            return computeDAO.cancelIncompleteTasksWithName(owner, name);
        }
        catch (DaoException e) {
            throw new ServiceException(e);
        }
    }

    public int cancelIncompleteTasksForUser(String subjectKey) throws ComputeException{
        try {
            return computeDAO.cancelIncompleteTasksForUser(subjectKey);
        }
        catch (DaoException e) {
            throw new ServiceException(e);
        }
    }

    private boolean areEqual(Set<TaskParameter> taskParameters1, Set<TaskParameter> taskParameters2) {
    	if (taskParameters1.size()!=taskParameters2.size()) return false;
    	Map<String, String> taskParameterMap1 = asMap(taskParameters1);
    	Map<String, String> taskParameterMap2 = asMap(taskParameters2);
    	return taskParameterMap1.equals(taskParameterMap2);
    }
    
    private Map<String,String> asMap(Set<TaskParameter> taskParameters) {
    	Map<String, String> map = new HashMap<String, String>();
    	for(TaskParameter taskParameter : taskParameters) {
    		map.put(taskParameter.getName(), taskParameter.getValue());
    	}
    	return map;
    }
    
    public Task getTaskById(long taskId) {
        Task task = computeDAO.getTaskById(taskId);
        // Init lazy-loading events
        task.getEvents().size();
        // Init lazy-loading messages
        task.getMessages().size();
        return task;
    }

    public Task getTaskWithMessages(long taskId) {
        return computeDAO.getTaskWithMessages(taskId);
    }
    
    public Subject saveOrUpdateSubject(Subject subject) throws DaoException{
        computeDAO.saveOrUpdate(subject);
        return subject;
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
                helper.sendEmail("saffordt@janelia.hhmi.org", errorMessageDestination, "JACS Error - " + hostname,
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
    
    private Node retireNode(String username, Long nodeId) throws Exception {
        // Test the ownership of the node to the username provided.  Basic security.
        // To add group security nodes and tasks should be owned by groups
        Node targetNode = computeDAO.getNodeById(nodeId);
        if (null==targetNode || !username.equals(targetNode.getOwner())){
            throw new SecurityException("The provided user does not own the node!");
        }
        // Get the node and deprecate the status
        targetNode.setVisibility(Node.VISIBILITY_PRIVATE_DEPRECATED);
        computeDAO.saveOrUpdate(targetNode);
        return targetNode;
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
        	Node targetNode = retireNode(username, nodeId);
        	
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

    /**
     * Method to clean up the db and filestore upon deletion of a node.
     *
     * @param nodeId - node id of what is to be deleted
     * @param clearFromFilestoreIfAppropriate
     *               - whether to nuke the filestore dir, if node is a FileNode
     * @return boolean of success
     */
    public boolean trashNode(String username, Long nodeId, boolean clearFromFilestoreIfAppropriate) {
        try {
        	Node targetNode = retireNode(username, nodeId);

            // Clean up the filestore if asked
            if (clearFromFilestoreIfAppropriate && targetNode instanceof FileNode) {
                FileNode tmpFileNode = (FileNode) targetNode;
                String centralDir = SystemConfigurationProperties.getString(FileNode.CENTRAL_DIR_PROP); 
                String archiveDir = SystemConfigurationProperties.getString(FileNode.CENTRAL_DIR_ARCHIVED_PROP); 
                String dir = tmpFileNode.getDirectoryPath();
                File nodeDir = new File(dir);
                String prefix = dir.startsWith(archiveDir) ? archiveDir : centralDir;
                File trashedDir = new File(dir.replace(prefix, prefix+"/"+username+"/trash"));
                if (!trashedDir.getAbsolutePath().contains("trash")) {
                    throw new IllegalStateException("Trashed dir was not formed correctly: "+trashedDir);
                }
                File trashDir = trashedDir.getParentFile();
            	FileUtil.ensureDirExists(trashDir.getAbsolutePath());
            	logger.info("Moving " + tmpFileNode.getDirectoryPath()+" to trash directory "+trashDir);
            	FileUtil.moveFileUsingSystemCall(nodeDir, trashDir);
            }
        }
        catch (Exception e) {
            logger.error("Error trashing node",e);
            return false;
        }
        return true;
    }

    public int moveFileNodesToArchive(String filepath) throws DaoException {
        try {
            int nodesUpdated = 0;
            logger.debug("moveFileNodesToArchive: "+filepath);
            
            String parentFileNodePath = null;
            Long nodeId = null;
            
            Pattern p = Pattern.compile("((.*?)/(\\d+)/(\\d+)/(\\d+))(.*?)?");
            Matcher m = p.matcher(filepath);
            if (!m.matches()) {
                
                p = Pattern.compile("((.*?)/(\\d+))(.*?)?");
                m = p.matcher(filepath);
                if (!m.matches()) {
                    throw new Exception("Could not parse file node information from filepath: "+filepath);
                }
                else {
                    parentFileNodePath = m.group(1);
                    nodeId = Long.parseLong(m.group(3));   
                }
            }
            else {
                parentFileNodePath = m.group(1);
                nodeId = Long.parseLong(m.group(5));   
            }

            logger.debug("  parentFileNodePath: "+parentFileNodePath);
            logger.debug("  nodeId: "+nodeId);
            
            if (!parentFileNodePath.equals(filepath)) {
                // Update child node too
                FileNode childNode = (FileNode)computeDAO.getFileNodeByPathOverride(filepath);
                if (childNode!=null) {
                    childNode.setPathOverride(filepath.replaceFirst(JACS_DATA_DIR, JACS_DATA_ARCHIVE_DIR));
                    computeDAO.saveOrUpdate(childNode);
                    nodesUpdated++;
                    logger.debug("  changed path override on child node "+childNode.getObjectId()+" to: "+childNode.getPathOverride());
                }
            }

            String archiveParentFileNodePath = parentFileNodePath.replaceFirst(JACS_DATA_DIR, JACS_DATA_ARCHIVE_DIR);
            
            FileNode node = (FileNode)computeDAO.getNodeById(nodeId);
            if (node!=null) {
                node.setPathOverride(archiveParentFileNodePath);
                computeDAO.saveOrUpdate(node);
                nodesUpdated++;
                logger.debug("  changed path override on node "+node.getObjectId()+" to: "+node.getPathOverride());
            }
            
            nodesUpdated += computeDAO.bulkUpdateNodePathOverridePrefix(parentFileNodePath, archiveParentFileNodePath);
        
            return nodesUpdated;
        }
        catch (Exception e) {
            throw new DaoException(e);
        }
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

    public List<Node> getNodeByName(String nodeName) throws DaoException {
        return computeDAO.getNodeByName(nodeName);
    }

    public List<Node> getNodeByPathOverride(String pathOverride) throws DaoException {
        return computeDAO.getNodeByPathOverride(pathOverride);
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

    public List<Task> getUserTasksByType(String simpleName, String ownerKey) {
        return computeDAO.getUserTasksByType(simpleName, ownerKey);
    }

    public Task getRecruitmentFilterTaskByUserPipelineId(Long objectId) throws DaoException {
        return computeDAO.getRecruitmentFilterTaskByUserPipelineId(objectId);
    }

    public List<Task> getRecentUserParentTasks(String ownerKey) {
    	List<Task> tasks = computeDAO.getRecentUserParentTasksByOwner(ownerKey);
        for(Task task : tasks) {
        	// Init lazy-loading events
        	task.getEvents().size();
        }
        return tasks;
    }
    
    public List<Task> getUserParentTasks(String ownerKey) {
    	List<Task> tasks = computeDAO.getUserParentTasksByOwner(ownerKey);
        for(Task task : tasks) {
        	// Init lazy-loading events
        	task.getEvents().size();
        }
        return tasks;
    }
    
    public List<Task> getUserTasks(String ownerKey) {
    	List<Task> tasks = computeDAO.getUserTasks(ownerKey);
        for(Task task : tasks) {
        	// Init lazy-loading events
        	task.getEvents().size();
        }
        return tasks;
    }

    public List<Event> getEventsForTask(long taskId) throws DaoException {
    	Task task = getTaskById(taskId);
    	if (task == null) throw new DaoException("No such task with id "+taskId);
    	return task.getEvents();
    }
    
    public void setParentTaskId(Long parentTaskId, Long childTaskId) throws DaoException {
        computeDAO.setParentTaskId(parentTaskId, childTaskId);
    }

    public List<Task> getChildTasksByParentTaskId(long taskId) {
        List<Task> tasks = computeDAO.getChildTasksByParentTaskId(taskId);
        for(Task task : tasks) {
        	// Init lazy-loading events
        	task.getEvents().size();
        }
    	return tasks;
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
    
    public void cancelTaskById(Long taskId) throws Exception {
        Task t = computeDAO.getTaskById(taskId);
        if (t==null) {
            throw new IllegalArgumentException("No such task: "+taskId);
        }
        computeDAO.cancelTaskById(t);
    }
    
    public void cancelTaskTreeById(Long taskId) throws Exception {
        // First get entire task tree with this task as parent task
        List<Long> taskList = getTaskTreeIdList(taskId);
        for (Long tid : taskList) {
            cancelTaskById(tid);
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


    public ControlledVocabElement[] getControlledVocab(Long objectId, int vocabIndex) throws ServiceException {
        throw new ServiceException ("Controlled Vocab is a future piece, currently unsupported.");
    }
}
