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


import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.status.TaskStatus;

import javax.ejb.Local;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Jul 30, 2008
 * Time: 11:28:08 AM
 * <p/>
 * An interface for using the job resource usage data
 */
@Local
public interface JobControlBeanLocal {

    public List getGridJobStatusesByTaskId(long taskId);

    public List getJobIdsByTaskId(long taskId);

    public List getJobIdsByTaskId(long taskId, String[] states);

    public GridJobStatus getGridJobStatus(long taskId, String jobId);

    public TaskStatus getTaskStatus(long taskId);

    public List<TaskStatus> getActiveTasks();

    public List<TaskStatus> getWaitingTasks();

    public TreeMap<Long, Long> getOrderedWaitingTasks();

    public Integer getPercentCompleteForATask(long taskId);

    public void updateJobStatus(long taskId, String jobId, GridJobStatus.JobState state);

    public void updateJobInfo(long taskId, String jobId, GridJobStatus.JobState state, Map<String, String> infoMap);

    public void saveGridJobStatus(GridJobStatus gridJobStatus);

    public void bulkAddGridJobStatus(long taskId, String queue, Set<String> jobIds, GridJobStatus.JobState state);

    public void bulkUpdateGridJobStatus(long taskId, Map<String, GridJobStatus.JobState> jobStates);

    public void cleanUpJobStatus(long taskId);

    public void cancelTask(long taskId);

}
