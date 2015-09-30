
package org.janelia.it.jacs.web.gwt.frv.client.panels.tabs;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;

/**
 * @author Michael Press
 */
public interface FrvControlsPanelTab {
    public Widget getPanel();

    public String getTabLabel();

    public void setJob(JobInfo job);

    public void setRecruitableJobSelectionListener(JobSelectionListener listener);

    /**
     * This method is intended to alert the panels that someone is going to submit a job and they should update any
     * changes they care about.
     *
     * @return boolean to state whether changes occurred
     */
    public boolean updateJobChanges();

}
