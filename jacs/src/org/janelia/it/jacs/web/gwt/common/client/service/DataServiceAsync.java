
package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.Date;

public interface DataServiceAsync {
    public void getTaskMessages(String taskId, AsyncCallback callback);

    public void isTaskDone(String taskId, AsyncCallback callback);

    public void saveUserDefinedFastaNode(String nodeName, String fastaText, String visibility, AsyncCallback<UserDataNodeVO> callback);

    public void getSequenceTypeForFASTA(String fastaText, AsyncCallback callback);

    public void getFilenodeByTaskId(String taskId, String contentType, AsyncCallback callback);

    public void getPagedBlastHitsByTaskId(String taskId, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback callback);

//    public void getSitesForBlastResult(String taskId, AsyncCallback callback);

    public void replaceNodeName(String nodeId, String nodeName, AsyncCallback renameNodeCallback);

    public void deleteNode(String nodeId, AsyncCallback removeJobCallback);

    public void markUserForDeletion(String userId, AsyncCallback removeJobCallback);

    public void getNumBlastableNodesForUser(String searchString, String sequenceType, AsyncCallback callback);

    public void getPagedBlastableNodesForUser(String searchString, String sequenceType, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback callback);

    public void getBlastableNodeNamesForUser(String searchString, String sequenceType, AsyncCallback callback);

    public void setTaskExpirationAndName(String taskId, Date expirationDate, String jobName, AsyncCallback asyncCallback);

    public void getUserLogins(AsyncCallback asyncCallback);

    public void getNumUsers(String searchString, AsyncCallback asyncCallback);
    public void getPagedUsers(String searchString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback asyncCallback);

//    public void getReversePsiBlastDatasets(AsyncCallback asyncCallback);

    public void submitJob(Task newTask, AsyncCallback<String> asyncCallback);

    public void getFiles(String tmpDirectory, boolean directoriesOnly, AsyncCallback callback);

    public void getProjectCodes(AsyncCallback asyncCallback);

//    public void getProkGenomeVO(String targetGenome, AsyncCallback asyncCallback);

    public void validateFilePath(String filePath, AsyncCallback asyncCallback);

    public void getNumNodesForUserByName(String nodeClassName, AsyncCallback asyncCallback);
    public void getPagedNodesForUserByName(String nodeClassName, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback callback);

    public void getEntityTypeNames(AsyncCallback asyncCallback);
    public void getNumEntityTypes(String searchString, AsyncCallback asyncCallback);
    public void getPagedEntityTypes(String searchString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback asyncCallback);
//    public void findPotentialResultNodes(String filePath, AsyncCallback asyncCallback);

    public void getNodeNamesForUserByName(String nodeClassName, AsyncCallback asyncCallback);

    public void syncUserData(String username, AsyncCallback asyncCallback) throws GWTServiceException;
}
