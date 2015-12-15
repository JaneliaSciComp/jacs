
package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.*;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.model.user_data.tools.GenericServiceDefinitionNode;
import org.janelia.it.jacs.shared.utils.ControlledVocabElement;

import javax.ejb.Remote;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Remote interface to ComputeBeanImpl
 * @author Sean Murphy
 */
@Remote
public interface ComputeBeanRemote {

    public String getAppVersion() throws RemoteException;
    public Subject login(String userLogin, String password) throws ComputeException;
    public UserToolEvent beginSession(String userLogin, String toolName, String clientVersion);
    public void endSession(String userLogin, String toolname, Long userSessionId);
    public Node saveOrUpdateNode(Node node) throws DaoException, RemoteException;
    public Task saveOrUpdateTask(Task task) throws DaoException;
    public Subject saveOrUpdateSubject(Subject subject) throws DaoException;
    public Node getResultNodeByTaskId(long taskId) throws DaoException, RemoteException;
    public List<Node> getResultNodesByTaskId(long taskId) throws DaoException, RemoteException;
    public Node getBlastDatabaseFileNodeByName(String name) throws RemoteException;
    public Long getBlastDatabaseFileNodeIdByName(String name) throws RemoteException;
    public Node getNodeById(long nodeId) throws RemoteException;
    public String[] getTaskStatus(long taskId) throws DaoException, RemoteException;
    public Task getTaskById(long taskId) throws RemoteException;
    public Task getTaskWithMessages(long taskId) throws RemoteException;
    public void setTaskNote(long taskId, String note) throws DaoException;
    public void addTaskNote(long taskId, String note) throws DaoException;
    public Subject getSubjectByNameOrKey(String name) throws ComputeException;
    public Subject getSubjectWithPreferences(String nameOrKey) throws ComputeException;
    public User getUserByNameOrKey(String name) throws ComputeException;
    public Group getGroupByNameOrKey(String name) throws ComputeException;
    public List<Subject> getSubjects() throws ComputeException;
    public List<User> getUsers() throws ComputeException;
	public List<Group> getGroups() throws ComputeException;
	
	public void removePreferenceCategory(String categoryName) throws DaoException;
    public Event saveEvent(Long taskId, String eventType, String description, Date timestamp) throws DaoException;
    public void submitJob(String processDefName,long taskId) throws RemoteException; //Note: Used by Blast API
    public Long submitJob(String processDefName,Map<String, Object> processConfiguration) throws RemoteException; // Note: Used by test
    public void submitJobs(String processDefName, List<Long> taskIds) throws RemoteException; //Note: Used by test
    public Long getBlastHitCountByTaskId(Long taskId) throws RemoteException, DaoException;
    public BlastResultFileNode getBlastResultFileNodeByTaskId(Long taskId) throws DaoException, RemoteException;
    public BlastResultNode getBlastHitResultDataNodeByTaskId(Long taskId) throws DaoException, RemoteException;
    public Map<Long,String> getAllTaskPvoStrings() throws RemoteException;
    public Task getTaskForNodeId(long nodeId) throws RemoteException;

    // 1st test method... move to test ejb if number grows
    public void verifyBlastResultContents(Long blastResultFileNodeId, String expectedBlastResultsZipFilePath) throws DaoException, IOException;

    public Node getInputNodeForTask(Long objectId) throws RemoteException;

    public String getRecruitmentFilterDataTaskForUserByGenbankId(String genbankName, String userlogin) throws RemoteException;

    public void addEventToTask(Long taskId, Event event) throws DaoException, RemoteException ;
    public void setTaskParameter(Long taskId, String parameterKey, String parameterValue) throws DaoException, RemoteException;
    public boolean deleteNode(String username, Long nodeId, boolean clearFromFilestoreIfAppropriate) throws RemoteException;
    public boolean trashNode(String username, Long nodeId, boolean clearFromFilestoreIfAppropriate) throws RemoteException;
    
    public int getNumCategoryResults(Long nodeId, String category) throws RemoteException;
    public Node createNode(Node node) throws DaoException, RemoteException;
    public List getAllSampleNamesAsList() throws RemoteException, DaoException;
    public boolean isServerPathAvailable( String serverPath, boolean directoryOnly );
    public List<BlastDatabaseFileNode> getBlastDatabases() throws RemoteException;
    public List<BlastDatabaseFileNode> getBlastDatabasesOfAUser(String username) throws RemoteException;
    public String createTemporaryFileNodeArchive(String archiveName, String sourceNodeId) throws Exception;
    public List<String> getFiles(String tmpDirectory, boolean directoriesOnly) throws RemoteException;

    public HashSet<String> getProjectCodes() throws Exception, InterruptedException;

    public List<Task> getChildTasksByParentTaskId(long taskId) throws RemoteException;
    public long getCumulativeCpuTime(long taskId) throws RemoteException;
    public List<Node> getNodeByName(String nodeName) throws DaoException;
    public List<Node> getNodeByPathOverride(String pathOverride) throws DaoException;

    public void setSystemDataRelatedToGiNumberObsolete(String giNumber) throws DaoException;

    public Long getSystemDatabaseIdByName(String databaseName) throws RemoteException;

    public void validateBlastTaskQueryDatabaseMatch(BlastTask blastTask) throws Exception;

    public void deleteTaskById(Long taskId) throws Exception;
    public void cancelTaskById(Long taskId) throws Exception;

    public List<Long> getTaskTreeIdList(Long taskId) throws Exception;

    public GenericServiceDefinitionNode getGenericServiceDefinitionByName(String serviceName) throws Exception;

    public void validateFile(String filePath) throws Exception;
    public void stopContinuousExecution(long taskId) throws ServiceException;
    public List<Task> getUserTasks(String userLogin) throws Exception;
    public List<Task> getRecentUserParentTasks(String userLogin) throws Exception;
    public List<Task> getUserParentTasks(String userLogin) throws Exception;
    public List<Task> getUserTasksByType(String simpleName, String userName) throws RemoteException;
    public List<Event> getEventsForTask(long taskId) throws DaoException;
    public ControlledVocabElement[] getControlledVocab(Long objectId, int vocabIndex) throws ServiceException;
    
    public User createUser(String newUserName, String newFullName) throws ComputeException;
	public Group createGroup(String groupOwner, String groupName) throws DaoException;
	public void addUserToGroup(String groupUser, String groupName) throws DaoException;
    public void removeGroup(String groupName) throws DaoException;
    public void removeUserFromGroup(String groupUser, String groupName) throws DaoException;
    
    public UserToolEvent addEventToSession(UserToolEvent userToolEvent);
    public UserToolEvent addEventToSessionAsync(UserToolEvent userToolEvent);

    /**
     * @deprecated
     */
    public void beginSession(String userLogin, String clientVersion);
    /**
     * @deprecated
     */
    public void endSession(String userLogin);

}
