
package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.SortArgument;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 30, 2006
 * Time: 2:39:17 PM
 */
public interface StatusServiceAsync {

    // GenericService Task Monitoring Methods
    void getNumTaskResultsForUser(String taskClassName, AsyncCallback async);

    void getPagedTaskResultsForUser(String taskClassName,
                                    int startIndex,
                                    int numRows,
                                    SortArgument[] sortArgs, AsyncCallback async);

    // Blast Methods
    void getTaskResultForUser(String taskId, AsyncCallback async);

    void getPagedBlastTaskResultsForUser(String classname, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

//    void getPagedRnaSeqPipelineTaskResultsForUser(String classname, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

    // Recruitment Viewer Methods
//    void getNumRVUserTaskResults(String likeString, AsyncCallback async);
//
//    void getNumRVSystemTaskResults(String likeString, AsyncCallback async);
//
//    void getPagedRVSystemTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);
//
//    void getPagedRVUserTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

    void getUserTaskQueryNames(AsyncCallback async);

    void getSystemTaskQueryNames(AsyncCallback async);

    // Search Task Methods
    void getPagedSearchInfoForUser(int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback callback);

    void markTaskForDeletion(String taskId, AsyncCallback async);

    void purgeTask(String taskId, AsyncCallback async);

//    void getRecruitmentTaskById(String taskId, AsyncCallback async);

    void replaceTaskJobName(String taskId, String jobName, AsyncCallback async);

//    void getRecruitmentFilterTaskByUserPipelineId(String userPipelineTaskId, AsyncCallback asyncCallback);

//     void getTaskOrder (String taskId, AsyncCallback callback);
//     void getPercentCompleteOfTask (String taskId, AsyncCallback callback);

    void getFilteredNumTaskResultsByUserAndClass(String userLogin, String className, String searchString, AsyncCallback async);

    void getFilteredPagedTaskResultsByUserAndClass(String userLogin, String className, String searchString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

    void getSystemTaskNamesByClass(String className, AsyncCallback asyncCallback);

    void getPagedTaskResults(String taskClassNameToWatch, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback asyncCallback);

    void getNumTaskResults(String taskClassNameToWatch, AsyncCallback asyncCallback);
}
