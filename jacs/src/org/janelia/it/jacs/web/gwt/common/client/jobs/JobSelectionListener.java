
package org.janelia.it.jacs.web.gwt.common.client.jobs;

/**
 * @author Michael Press
 */
public interface JobSelectionListener {
    public void onSelect(org.janelia.it.jacs.shared.tasks.JobInfo job);

    public void onUnSelect();
}
