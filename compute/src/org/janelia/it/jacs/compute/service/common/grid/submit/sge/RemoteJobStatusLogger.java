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

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.JobControlBeanRemote;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.model.status.GridJobStatus;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Jun 2, 2009
 * Time: 5:26:35 PM
 */
public class RemoteJobStatusLogger implements JobStatusLogger {
    private Logger logger;

    private long taskId;
    private JobControlBeanRemote jobControlRemoteBean;

    public RemoteJobStatusLogger() {
        logger = Logger.getLogger(this.getClass());
    }

    public RemoteJobStatusLogger(Logger l) {
        logger = l;
    }

    public RemoteJobStatusLogger(long taskId, JobControlBeanRemote bean, Logger l) {
        this.taskId = taskId;
        this.jobControlRemoteBean = bean;
        logger = l;
    }

    public long getTaskId() {
        return this.taskId;
    }

    public void bulkAdd(Set<String> jobIds, String queue, GridJobStatus.JobState state) {
        try {
            jobControlRemoteBean.bulkAddGridJobStatus(this.taskId, queue, jobIds, state);
        }
        catch (RemoteException e) {
            logger.error("Unable to save job state. Error:" + e.getMessage());
        }
    }

    public void updateJobStatus(String jobId, GridJobStatus.JobState state) {
        try {
            jobControlRemoteBean.updateJobStatus(this.taskId, jobId, state);
        }
        catch (RemoteException e) {
            logger.error("Unable to save job state. Error:" + e.getMessage());
        }
    }

    public void bulkUpdateJobStatus(Map<String, GridJobStatus.JobState> jobStates) {
        try {
            jobControlRemoteBean.bulkUpdateGridJobStatus(this.taskId, jobStates);
        }
        catch (RemoteException e) {
            logger.error("Unable to save job state. Error:" + e.getMessage());
        }
    }

    public void updateJobInfo(String jobId, GridJobStatus.JobState state, Map<String, String> infoMap) {
        if (infoMap == null) {
            updateJobStatus(jobId, state);
        }
        else {
            try {
                jobControlRemoteBean.updateJobInfo(this.taskId, jobId, state, infoMap);
            }
            catch (RemoteException e) {
                logger.error("Unable to save job state. Error:" + e.getMessage());
            }

        }
    }

    @Override
    public void cleanUpData() {
        try {
            jobControlRemoteBean.cleanUpJobStatus(this.taskId);
        }
        catch (RemoteException e) {
            logger.error("Unable to clean up job status. Error:" + e.getMessage());
        }
    }
}
