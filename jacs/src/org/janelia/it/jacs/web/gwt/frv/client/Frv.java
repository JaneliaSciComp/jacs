/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
