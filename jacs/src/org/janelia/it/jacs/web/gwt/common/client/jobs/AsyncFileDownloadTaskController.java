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

package org.janelia.it.jacs.web.gwt.common.client.jobs;

import com.google.gwt.user.client.Window;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * A flavor of AsyncTaskController that streams a task's output file back to the browser.
 *
 * @author Michael Press
 */
public abstract class AsyncFileDownloadTaskController extends AsyncTaskController {
    private static Logger _logger = Logger.getLogger("AsyncFileDownloadTaskController");

    protected AsyncFileDownloadTaskController(Task task) {
        super(task);
    }

    protected void onComplete(JobInfo job) {
        _logger.info("Calling server to stream file for task id " + job.getJobId());
        String url = "/jacs/fileDelivery.htm?taskId=" + job.getJobId();
        Window.open(url, "_self", "");
    }
}
