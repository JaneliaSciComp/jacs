
package org.janelia.it.jacs.web.gwt.common.client.jobs;

/**
 * @author Michael Press
 */
public interface JobStatusListener {
    public void onJobRunning(org.janelia.it.jacs.shared.tasks.JobInfo status);

    public void onJobFinished(org.janelia.it.jacs.shared.tasks.JobInfo status); // could be success or failure

    public void onCommunicationError();
}
