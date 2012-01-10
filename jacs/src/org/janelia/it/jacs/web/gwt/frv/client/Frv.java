
package org.janelia.it.jacs.web.gwt.frv.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.gwtwidgets.client.util.WindowUtils;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;

public class Frv extends BaseEntryPoint {
    public void onModuleLoad() {
        // Create and fade in the page contents
        clear();
        setBreadcrumb(new Breadcrumb(Constants.VIEWERS_FRV_LABEL), Constants.ROOT_PANEL_NAME);

        // Create the FRV and tell it to retrieve the default job
        FrvPanel frv = new FrvPanel();
        String requestedTaskId = WindowUtils.getLocation().getParameter("taskId");
        //Window.alert("Using id: "+requestedTaskId);
        if (null == requestedTaskId || "".equals(requestedTaskId)) {
            frv.setDefaultJob();
        }
        else {
            //Window.alert("Going to user-selected data");
            frv.setUserTask(requestedTaskId);
        }

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(frv);
        show();
    }

    public static void trackActivity(String activity, RecruitableJobInfo job) {
        String resourceNameForTracking;
        if (job.getJobname() != null && job.getJobname().length() > 0) {
            resourceNameForTracking = job.getJobname();
        }
        else if (job.getQueryName() != null && job.getQueryName().length() > 0) {
            resourceNameForTracking = job.getQueryName();
        }
        else {
            resourceNameForTracking = job.getJobId();
        }
        SystemWebTracker.trackActivity(activity, new String[]{resourceNameForTracking});
    }
}
