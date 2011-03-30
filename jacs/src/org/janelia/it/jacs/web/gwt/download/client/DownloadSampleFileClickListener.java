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

package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.ResultReceiver;

public class DownloadSampleFileClickListener implements ClickListener {

    private String _contextEntityId;
    private DownloadableDataNode _file;

    public DownloadSampleFileClickListener(String contextEntityId, DownloadableDataNode file) {
        _contextEntityId = contextEntityId;
        _file = file;
    }

    public void onClick(Widget widget) {
        final Widget currentWidget = widget;
        ResultReceiver rcv = new ResultReceiver() {
            public void setResult(Object result) {
                if (result instanceof Boolean) {
                    Boolean fileExists = (Boolean) result;
                    if (fileExists.booleanValue()) { // service handles login or file-not-found failure
                        // track the project sample download
                        SystemWebTracker.trackActivity("DownloadSampleDataFile",
                                new String[]{
                                        _contextEntityId,
                                        _file.getLocation()
                                });
                        // Now, launch popup to retrieve the data file.
                        new PopupAboveLauncher(new DownloadFilePopup(_file, false)).showPopup(currentWidget);
                    }
                }
            }
        };
        PublicationServiceHelper.checkFileLocation(rcv, _file.getLocation());
    }

}
