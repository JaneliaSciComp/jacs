
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;

/**
 * @author Michael Press
 */
public interface QuerySequenceChooserTab {
    public Widget getPanel();

    public String getTabLabel();

    public void setRecruitableJobSelectionListener(JobSelectionListener listener);

    public void realize();
}
