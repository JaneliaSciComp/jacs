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

package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.shared.tasks.BlastJobInfo;
import org.janelia.it.jacs.shared.tasks.RnaSeqJobInfo;
import org.janelia.it.jacs.shared.tasks.SearchJobInfo;
import org.springframework.dao.DataAccessException;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Aug 10, 2006
 * Time: 1:34:18 PM
 */
public interface TaskDAO extends DAO {

    Task saveOrUpdateTask(Task targetTask) throws DataAccessException, DaoException;

    void purgeTask(Long taskId) throws DataAccessException, DaoException;

    Task getTaskById(Long taskId) throws DataAccessException, DaoException;

    public Task getTaskWithMessages(Long taskId) throws DataAccessException, DaoException;

    boolean isDone(Long taskId) throws DataAccessException, DaoException;

    List<Task> findTasksByIdRange(Long startId, Long endId) throws DataAccessException, DaoException;

    List<Task> findOrderedTasksByIdRange(Long startId, Long endId) throws DataAccessException, DaoException;

    public <T extends Task> List<T> findSpecificTasksByIdRange(Class<T> cl, Long startId, Long endId, boolean includeSystem) throws DataAccessException, DaoException;

    // Biz API methods
    org.janelia.it.jacs.shared.tasks.JobInfo getJobStatusByJobId(Long taskId) throws Exception;

    public List<BlastJobInfo> getPagedBlastJobsForUserLogin(String classname,
                                                            String userLogin,
                                                            String likeString,
                                                            int startIndex,
                                                            int numRows,
                                                            SortArgument[] sortArgs) throws DaoException;

    public List<RnaSeqJobInfo> getPagedRnaSeqJobsForUserLogin(String classname,
                                                              String userLogin,
                                                              String likeString,
                                                              int startIndex,
                                                              int numRows,
                                                              SortArgument[] sortArgs) throws DaoException;

    List<SearchJobInfo> getPagedSearchInfoForUserLogin(String userLogin,
                                                       int startIndex,
                                                       int numRows,
                                                       SortArgument[] sortArgs) throws DaoException;

    List<Task> getPagedTasksForUserLoginByClass(String userLogin,
                                                String likeString,
                                                String taskClassName,
                                                int startIndex,
                                                int numRows,
                                                SortArgument[] sortArgs,
                                                boolean parentTasksOnly) throws DataAccessException, DaoException;

    Integer getNumTasksForUserLoginByClass(String userLogin,
                                           String likeString,
                                           String targetTaskClassName,
                                           boolean parentTasksOnly) throws DataAccessException, DaoException;

    List<String> getTaskQueryNamesForUser(String userLogin) throws DataAccessException, DaoException;

    public void setTaskExpirationAndName(Long taskId, Date expirationDate, String jobName) throws DaoException;

    public Task getRecruitmentFilterTaskByUserPipelineId(Long pipelineId) throws DaoException;

    public List<String> getTaskQueryNamesByClassAndUser(String userLogin, String className) throws DaoException;

    public void getNodeNames(List<String> subjIds, Map<String, String> nodesCache);

    public List<BaseSequenceEntity> getBlastResultNodeBaseSequenceEntities(BlastResultNode resultNode);
}
