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

package org.janelia.it.jacs.web.gwt.admin.client.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.FlexTable;
import org.janelia.it.jacs.web.gwt.admin.client.service.AdminService;
import org.janelia.it.jacs.web.gwt.admin.client.service.AdminServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 23, 2009
 * Time: 3:58:54 PM
 */
public class DiskUsagePanel extends TitledPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.admin.client.panels.DiskUsagePanel");
    private FlexTable usageTable;
    private static AdminServiceAsync _adminservice = (AdminServiceAsync) GWT.create(AdminService.class);

    static {
        ((ServiceDefTarget) _adminservice).setServiceEntryPoint("admin.srv");
    }

    public DiskUsagePanel() {
        super();
        init();
        _adminservice.getDiskUsageReport(new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                Window.alert("Boom! " + throwable.getMessage());
                _logger.error("Problem getting the user disk usage info. " + throwable.getMessage());
            }

            public void onSuccess(Object o) {
                if (null == o) {
                    _logger.error("Disk usage information is null");
                    return;
                }
                ArrayList diskInfo = (ArrayList) o;
                addInformation(diskInfo);
            }
        });
        realize();
    }

    private void addInformation(ArrayList diskInfo) {
        for (Object s : diskInfo) {
            String[] pieces = ((String) s).split("\t");
            int numRows = usageTable.getRowCount();
            usageTable.setWidget(numRows, 0, HtmlUtils.getHtml(pieces[0], "nowrapText"));
            usageTable.setWidget(numRows, 1, HtmlUtils.getHtml(pieces[1], "nowrapText"));
        }
    }

    public void init() {
        usageTable = new FlexTable();
        usageTable.setHTML(0, 0, HtmlUtils.getHtml("Size (MB)", "prompt").toString());
        usageTable.setHTML(0, 1, HtmlUtils.getHtml("User", "prompt").toString());
        add(usageTable);
    }


}
