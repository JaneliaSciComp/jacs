
package org.janelia.it.jacs.web.gwt.common.client.jobs;

import org.janelia.it.jacs.shared.tasks.JobInfo;

/**
 * @author Michael Press
 */
public interface JobSelectionListener {
    public void onSelect(JobInfo job);

    public void onUnSelect();
}
