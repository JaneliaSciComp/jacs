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

package org.janelia.it.jacs.compute.engine.util;

import org.apache.log4j.Logger;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Session;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.JobControlBeanLocal;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Aug 19, 2008
 * Time: 3:00:02 PM
 */
public class TaskCancelUtil implements IService {
    private Logger logger;

    ComputeBeanRemote computeBean;

    private static final String[] CANCELABLE =
            new String[]{GridJobStatus.JobState.QUEUED.name(),
                    GridJobStatus.JobState.RUNNING.name()};

    public void execute(IProcessData processData) {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init();
            //long taskId = processData.getLong("TASK_ID");
            long taskId = ProcessDataHelper.getTask(processData).getObjectId();
            cancelTaskJobs(taskId);
        }
        catch (Exception e) {
            // Warn of exception
        }
    }

    public void init() {
        computeBean = EJBFactory.getRemoteComputeBean();
    }

    public void cancelTaskJobsUsingDrmaa(long taskID) {
        JobControlBeanLocal jobControl = EJBFactory.getLocalJobControlBean();
        List<String> jobSet = jobControl.getJobIdsByTaskId(taskID, CANCELABLE);
        if (jobSet == null || jobSet.size() == 0) {
            logger.info("Unable to cancel jobs for task " + taskID + " - no active jobs are found");
            return;
        }
        DrmaaHelper drmaa;
        try {
            drmaa = new DrmaaHelper(logger);
        }
        catch (DrmaaException e) {
            logger.error("Unable to get Drmaa interface", e);
            return;
        }
        for (String jobId : jobSet) {
            try {
                drmaa.control(jobId, Session.TERMINATE);
            }
            catch (DrmaaException e) {
                logger.error("Error sending cancel command to grid for job " + jobId, e);
            }
        }
        // TODO: update task status - add user terminated error
    }

    public void cancelTaskJobs(long taskID) {
        String deleteCmd = SystemConfigurationProperties.getString("Grid.Delete.Cmd");
        JobControlBeanLocal jobControl = EJBFactory.getLocalJobControlBean();
        List<String> jobSet = jobControl.getJobIdsByTaskId(taskID, CANCELABLE);
        if (jobSet == null || jobSet.size() == 0) {
            logger.info("Unable to cancel jobs for task " + taskID + " - no active jobs are found");
            return;
        }

        //  The job ids in the jobset are in the format '<jobid>.<index>' Strip an index value get
        // just the job id.
        String gridId = jobSet.get(0);
        gridId = gridId.substring(0, gridId.indexOf('.'));

        // Run command to delete the grid id
        deleteCmd = deleteCmd + " " + gridId;

        try {
            // Add cancel event to task event.
            if (computeBean == null) {
                computeBean = EJBFactory.getRemoteComputeBean();
            }

            computeBean.addEventToTask(taskID, new Event(taskID + " cancelled by the user", new Date(), Event.CANCELED_EVENT));

            SystemCall call = new SystemCall(logger);
            call.emulateCommandLine(deleteCmd, true);
        }
        catch (Exception e) {
            logger.warn("Error executing command " + deleteCmd + "." + e.getMessage());
        }
    }
}
