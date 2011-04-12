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

package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 8, 2010
 * Time: 12:46:08 PM
 */
public class SubmitJobAndWaitHelper {
    public final int WAIT_INTERVAL_IN_SECONDS = 5;
    String processName;
    Long taskId;
    Logger logger;

    public SubmitJobAndWaitHelper(String processName, Long taskId) {
        this.processName = processName;
        this.taskId = taskId;
    }

    public Node startAndWaitTillDone() throws Exception {
        if (taskId == null || taskId == 0) {
            throw new Exception("taskId must be non-null and have non-zero value");
        }
        this.logger = ProcessDataHelper.getLoggerForTask(taskId.toString(), this.getClass());
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        if (computeBean == null) {
            String em = "computeBean is unexpectedly null";
            logger.error(em);
            throw new Exception(em);
        }
        logger.info("computeBean.submitJob() processName=" + processName + " taskId=" + taskId);
        computeBean.submitJob(processName, taskId);
        logger.info("starting waitForTask for taskId=" + taskId);
        return waitForTask(taskId);
    }

    protected Node waitForTask(Long taskId) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        String[] taskStatus = null;
        while (taskStatus == null || !Task.isDone(taskStatus[0])) {
            taskStatus = computeBean.getTaskStatus(taskId);
            Thread.sleep(WAIT_INTERVAL_IN_SECONDS * 1000);
        }
        if (!taskStatus[0].equals(Event.COMPLETED_EVENT)) {
            throw new Exception("Task " + taskId + " finished with non-complete status=" + taskStatus[0]);
        }
        return computeBean.getResultNodeByTaskId(taskId);
    }

}
