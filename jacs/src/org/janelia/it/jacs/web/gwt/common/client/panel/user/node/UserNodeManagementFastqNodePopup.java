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

package org.janelia.it.jacs.web.gwt.common.client.panel.user.node;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.utility.UploadFastqDirectoryTask;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.DataServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 13, 2010
 * Time: 2:42:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserNodeManagementFastqNodePopup extends UserNodeManagementNewNodePopup {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.user.node.UserNodeManagementFastqNodePopup");
    private static DataServiceAsync _dataservice = (DataServiceAsync) GWT.create(DataService.class);
    UserNodeManagementFastqNodePopup thisPopup;

    static {
        ((ServiceDefTarget) _dataservice).setServiceEntryPoint("data.srv");
    }

    public UserNodeManagementFastqNodePopup(String title) {
        super(title);
        thisPopup=this;
    }

    @Override
    protected void populateContent() {
        VerticalPanel panel=new VerticalPanel();

        HorizontalPanel pathMsgPanel = new HorizontalPanel();
        pathMsgPanel.add(HtmlUtils.getHtml("Enter UNIX filesystem path for Fastq directory", "text"));

        DockPanel _topRow = new DockPanel();
        _topRow.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
        _topRow.setStyleName("ChooseMessage");
        _topRow.setWidth("100%");
        _topRow.add(pathMsgPanel, DockPanel.WEST);
        _topRow.setCellHorizontalAlignment(pathMsgPanel, DockPanel.ALIGN_LEFT);
        panel.add(_topRow);

        final TextBox unixPathTextBox = new TextBox();
        panel.add(unixPathTextBox);
        panel.add(HtmlUtils.getHtml("&nbsp;", "text"));

        HorizontalPanel nameMsgPanel = new HorizontalPanel();
        nameMsgPanel.add(HtmlUtils.getHtml("Enter a descriptive name for this Fastq data node", "text"));

        DockPanel _nameRow = new DockPanel();
        _nameRow.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
        _nameRow.setStyleName("ChooseMessage");
        _nameRow.setWidth("100%");
        _nameRow.add(nameMsgPanel, DockPanel.WEST);
        _nameRow.setCellHorizontalAlignment(nameMsgPanel, DockPanel.ALIGN_LEFT);
        panel.add(_nameRow);

        final TextBox nameTextBox = new TextBox();
        panel.add(nameTextBox);
        panel.add(HtmlUtils.getHtml("&nbsp;", "text"));

        HorizontalPanel mateDistanceMsgPanel = new HorizontalPanel();
        mateDistanceMsgPanel.add(HtmlUtils.getHtml("Enter the mate mean inner distance (if applicable)", "text"));
        
        DockPanel _mateDistanceRow = new DockPanel();
        _mateDistanceRow.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
        _mateDistanceRow.setStyleName("ChooseMessage");
        _mateDistanceRow.setWidth("100%");
        _mateDistanceRow.add(mateDistanceMsgPanel, DockPanel.WEST);
        _mateDistanceRow.setCellHorizontalAlignment(mateDistanceMsgPanel, DockPanel.ALIGN_LEFT);
        panel.add(_mateDistanceRow);

        final TextBox mateDistanceTextBox = new TextBox();
        panel.add(mateDistanceTextBox);
        panel.add(HtmlUtils.getHtml("&nbsp;", "text"));

        RoundedButton uploadButton = new RoundedButton("Create new node", new ClickListener() {
            public void onClick(Widget widget) {
                _logger.debug("UserNodeManagementFastqNodePopup: uploadButton pressed");
                UploadFastqDirectoryTask uploadTask = new UploadFastqDirectoryTask();
                uploadTask.setParameter(UploadFastqDirectoryTask.PARAM_SOURCE_DIR, unixPathTextBox.getText());
                uploadTask.setParameter(UploadFastqDirectoryTask.PARAM_MATE_MEAN_INNER_DISTANCE, mateDistanceTextBox.getText());
                uploadTask.setParameter(UploadFastqDirectoryTask.PARAM_NODE_NAME, nameTextBox.getText());
                uploadTask.setJobName("UploadFastqDirectoryTask");
                _logger.debug("UserNodeManagementFastqNodePopup: just before _dataservice.submitJob() taskName="+uploadTask.getTaskName());
                _dataservice.submitJob(uploadTask, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("An error occurred creating the Fastq node.")).showPopup(null);
                        thisPopup.hide();
                    }

                    public void onSuccess(Object o) {
                        new PopupCenteredLauncher(new InfoPopupPanel("Creating node...wait then refresh to access")).showPopup(null);
                        thisPopup.hide();
                    }
                });
                _logger.debug("UserNodeManagementFastqNodePopup: just after _dataservice.submitJob()");
            }
        });
        uploadButton.setWidth("130px");
        panel.add(uploadButton);

        panel.add(HtmlUtils.getHtml("&nbsp;", "text"));

        add(panel);
    }

}
