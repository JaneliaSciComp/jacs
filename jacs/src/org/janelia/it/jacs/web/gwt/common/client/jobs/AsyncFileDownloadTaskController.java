
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
