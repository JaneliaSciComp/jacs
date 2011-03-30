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

import javax.ejb.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Jul 31, 2008
 * Time: 3:36:17 PM
 */
@Remote
public interface JobControlBeanRemote {
    public List getGridJobStatusesByTaskId(long taskId) throws RemoteException;

    public List getJobIdsByTaskId(long taskId) throws RemoteException;

    public List getJobIdsByTaskId(long taskId, String[] states) throws RemoteException;

    public GridJobStatus getGridJobStatus(long taskId, String jobId) throws RemoteException;

    public TaskStatus getTaskStatus(long taskId) throws RemoteException;

    public List<TaskStatus> getActiveTasks() throws RemoteException;

    public List<TaskStatus> getWaitingTasks() throws RemoteException;

    public TreeMap<Long, Long> getOrderedWaitingTasks() throws RemoteException;

    public void cancelTask(long taskId) throws RemoteException;

    public void cancelTask(Long taskId) throws RemoteException;

    public Integer getPercentCompleteForATask(long taskId) throws RemoteException;

    public void updateJobStatus(long taskId, String jobId, GridJobStatus.JobState state) throws RemoteException;

    public void updateJobInfo(long taskId, String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) throws RemoteException;

    public void saveGridJobStatus(GridJobStatus gridJobStatus) throws RemoteException;

    public void bulkAddGridJobStatus(long taskId, String queue, Set<String> jobIds, GridJobStatus.JobState state) throws RemoteException;

    public void bulkUpdateGridJobStatus(long taskId, Map<String, GridJobStatus.JobState> jobStates) throws RemoteException;

    public void cleanUpJobStatus(long taskId) throws RemoteException;

}
