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

package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.user.client.ui.Label;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;

/**
 * @author Guy Cao
 */
public class ProjectTabPanel extends RoundedTabPanel {

    private ProjectDetailsPanel projectDetailPanel = null;
    private ProjectPreviewPanel projectPreviewPanel = null;
    private ProjectDescriptionPanel projectDescriptionPanel = null;
    private ProjectPublicationPanel projectPublicationPanel = null;

    public ProjectTabPanel() {

        // detail
        projectDetailPanel = new ProjectDetailsPanel(this);

        // description
        projectDescriptionPanel = new ProjectDescriptionPanel(this);

        // preview
        projectPreviewPanel = new ProjectPreviewPanel(this); //tabs.add(projectPreviewPanel, "Preview");


        // publication
        projectPublicationPanel = new ProjectPublicationPanel(this);


        // preferences
        selectTab(0);
        setWidth("100%");

    }

    public ProjectDetailsPanel getDetailPanel() {
        return projectDetailPanel;
    }

    public ProjectDescriptionPanel getDescriptionPanel() {
        return projectDescriptionPanel;
    }

    public ProjectPreviewPanel getPreviewPanel() {
        return projectPreviewPanel;
    }

    public ProjectPublicationPanel getPublicationPanel() {
        return projectPublicationPanel;
    }


    private void addDebugMessage(String message) {

        Label label = new Label(message);
        label.setStyleName("titledBoxLabel");
        this.add(label);

    }


}
