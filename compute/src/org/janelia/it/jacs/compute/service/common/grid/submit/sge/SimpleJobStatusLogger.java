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

package org.janelia.it.jacs.compute.service.common.grid.submit.sge;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.model.status.GridJobStatus;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Sep 9, 2008
 * Time: 1:39:59 PM
 */
public class SimpleJobStatusLogger implements JobStatusLogger {
    private long taskId;

    public SimpleJobStatusLogger(long taskId) {
        this.taskId = taskId;
    }

    public long getTaskId() {
        return this.taskId;
    }

    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
        EJBFactory.getLocalJobControlBean().bulkAddGridJobStatus(this.taskId, queue, jobIds, state);
    }

    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
        EJBFactory.getLocalJobControlBean().updateJobStatus(this.taskId, jobId, state);
    }

    public void bulkUpdateJobStatus(Map<String, GridJobStatus.JobState> jobStates) {
        EJBFactory.getLocalJobControlBean().bulkUpdateGridJobStatus(this.taskId, jobStates);
    }

    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
        if (infoMap == null)
            updateJobStatus(jobId, state);
        else
            EJBFactory.getLocalJobControlBean().updateJobInfo(this.taskId, jobId, state, infoMap);
    }

    public void cleanUpData() {
        EJBFactory.getLocalJobControlBean().cleanUpJobStatus(this.taskId);
    }

//    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
//    try {
//        EJBFactory.getRemoteJobControlBean().bulkAddGridJobStatus(this.taskId, queue, jobIds, state);
//    } catch (RemoteException e) {
//        logger.error(e);  //To change body of catch statement use File | Settings | File Templates.
//    }
//}
//
//    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
//        try {
//        EJBFactory.getRemoteJobControlBean().updateJobStatus(this.taskId, jobId, state);
//    } catch (RemoteException e) {
//        logger.error(e);  //To change body of catch statement use File | Settings | File Templates.
//    }
//    }
//
//    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
//        if (infoMap == null)
//            updateJobStatus(jobId, state);
//        else
//        {
//            try {
//            EJBFactory.getRemoteJobControlBean().updateJobInfo(this.taskId, jobId, state, infoMap);
//            } catch (RemoteException e) {
//                logger.error(e);  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//    }

}
