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

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.tasks.*;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 30, 2006
 * Time: 2:39:05 PM
 */
public interface StatusService extends RemoteService {
    // GenericService Task Monitoring Methods
    public Integer getNumTaskResultsForUser(String taskClassName) throws GWTServiceException;

    public JobInfo[] getPagedTaskResultsForUser(String taskClassName,
                                                int startIndex,
                                                int numRows,
                                                SortArgument[] sortArgs) throws GWTServiceException;

    // And Non-User-Specific Task Monitoring Methods
    public Integer getNumTaskResults(String taskClassNameToWatch) throws GWTServiceException;

    public JobInfo[] getPagedTaskResults(String taskClassNameToWatch,
                                         int startIndex,
                                         int numRows,
                                         SortArgument[] sortArgs) throws GWTServiceException;

    // Blast Methods
    public String replaceTaskJobName(String taskId, String jobName) throws GWTServiceException;

    public JobInfo getTaskResultForUser(String taskId) throws GWTServiceException;

    public BlastJobInfo[] getPagedBlastTaskResultsForUser(String classname, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException;

    public RnaSeqJobInfo[] getPagedRnaSeqPipelineTaskResultsForUser(String classname, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException;

    // Recruitment Viewer Methods
    public Integer getNumRVUserTaskResults(String likeString) throws GWTServiceException;

    public Integer getNumRVSystemTaskResults(String likeString) throws GWTServiceException;

    public RecruitableJobInfo[] getPagedRVSystemTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs)
            throws GWTServiceException;

    public RecruitableJobInfo[] getPagedRVUserTaskResults(String likeString, int startIndex, int numRows, SortArgument[] sortArgs)
            throws GWTServiceException;

    public List<String> getUserTaskQueryNames() throws GWTServiceException;

    public List<String> getSystemTaskQueryNames() throws GWTServiceException;

    // Search Task Methods
    public List<SearchJobInfo> getPagedSearchInfoForUser(int startIndex,
                                                         int numRows,
                                                         SortArgument[] sortArgs) throws GWTServiceException;

    public void markTaskForDeletion(String taskId) throws GWTServiceException;

    public void purgeTask(String taskId) throws GWTServiceException;

    public RecruitableJobInfo getRecruitmentTaskById(String taskId) throws GWTServiceException;

    public RecruitableJobInfo getRecruitmentFilterTaskByUserPipelineId(String parentTaskId) throws GWTServiceException;

//    public Integer getTaskOrder (String taskId) throws GWTServiceException;
//    public Integer getPercentCompleteOfTask (String taskId) throws GWTServiceException;

    public Integer getFilteredNumTaskResultsByUserAndClass(String userLogin, String className, String searchString) throws GWTServiceException;

    public JobInfo[] getFilteredPagedTaskResultsByUserAndClass(String userLogin, String className, String searchString, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException;

    public List<String> getSystemTaskNamesByClass(String className) throws GWTServiceException;

}
