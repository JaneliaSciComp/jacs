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

    void getPagedRnaSeqPipelineTaskResultsForUser(String classname, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

    // Recruitment Viewer Methods
    void getNumRVUserTaskResults(String likeString, AsyncCallback async);

    void getNumRVSystemTaskResults(String likeString, AsyncCallback async);

    void getPagedRVSystemTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

    void getPagedRVUserTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

    void getUserTaskQueryNames(AsyncCallback async);

    void getSystemTaskQueryNames(AsyncCallback async);

    // Search Task Methods
    void getPagedSearchInfoForUser(int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback callback);

    void markTaskForDeletion(String taskId, AsyncCallback async);

    void purgeTask(String taskId, AsyncCallback async);

    void getRecruitmentTaskById(String taskId, AsyncCallback async);

    void replaceTaskJobName(String taskId, String jobName, AsyncCallback async);

    void getRecruitmentFilterTaskByUserPipelineId(String userPipelineTaskId, AsyncCallback asyncCallback);

//     void getTaskOrder (String taskId, AsyncCallback callback);
//     void getPercentCompleteOfTask (String taskId, AsyncCallback callback);

    void getFilteredNumTaskResultsByUserAndClass(String userLogin, String className, String searchString, AsyncCallback async);

    void getFilteredPagedTaskResultsByUserAndClass(String userLogin, String className, String searchString, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback async);

    void getSystemTaskNamesByClass(String className, AsyncCallback asyncCallback);

    void getPagedTaskResults(String taskClassNameToWatch, int startIndex, int numRows, SortArgument[] sortArgs, AsyncCallback asyncCallback);

    void getNumTaskResults(String taskClassNameToWatch, AsyncCallback asyncCallback);
}
