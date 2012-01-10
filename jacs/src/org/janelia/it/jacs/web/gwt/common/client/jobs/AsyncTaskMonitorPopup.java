
package org.janelia.it.jacs.web.gwt.common.client.jobs;

/**
 * @author Michael Press
 */
public interface AsyncTaskMonitorPopup {
    public void setProcessingMessage(String message);

    public void setFailureMessage(String message);

    public void setFailureMessage(String message, String logMessage);

    public void close();
}
