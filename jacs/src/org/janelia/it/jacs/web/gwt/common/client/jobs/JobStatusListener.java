
package org.janelia.it.jacs.web.gwt.common.client.jobs;

import org.janelia.it.jacs.shared.tasks.JobInfo;

/**
 * @author Michael Press
 */
public interface JobStatusListener {
    public void onJobRunning(JobInfo status);

    public void onJobFinished(JobInfo status); // could be success or failure

    public void onCommunicationError();
}
