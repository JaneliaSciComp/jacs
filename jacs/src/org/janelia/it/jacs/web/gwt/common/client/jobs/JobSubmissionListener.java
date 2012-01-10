
package org.janelia.it.jacs.web.gwt.common.client.jobs;

/**
 * @author Michael Press
 */
public interface JobSubmissionListener {
    public void onFailure(Throwable throwable);

    /**
     * When job creation is successful, returns the new job ID.
     */
    public void onSuccess(String jobId);
}
