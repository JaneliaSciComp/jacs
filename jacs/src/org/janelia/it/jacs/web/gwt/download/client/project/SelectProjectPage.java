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

package org.janelia.it.jacs.web.gwt.download.client.project;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardPage;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

import java.util.Map;

/**
 * @author Michael Press
 */
public class SelectProjectPage extends WizardPage {
    private ProjectSelectedInTableListener _projectSelectionListener;
    private ProjectSelectionCancelledListener _cancelListener;
    private TitledBox _mainPanel;
    private SelectProjectPanel _selectProjectPanel;

    public SelectProjectPage(WizardController controller, ProjectSelectedInTableListener listener, ProjectSelectionCancelledListener cancelListener) {
        super(/*show buttons*/ true, controller);
        _projectSelectionListener = listener;
        _cancelListener = cancelListener;
        init();
    }

    private void init() {
        // Create the panel and register a callback to push the data to the panel when it's been retrieved
        _selectProjectPanel = new SelectProjectPanel(_projectSelectionListener, _cancelListener);
        getProjectsController().getDataManager().addDataRetrievedListener(new DataRetrievedListener() {
            public void onSuccess(Object projects) {
                _selectProjectPanel.setProjects((Map<String, Project>) projects);
            }

            public void onFailure(Throwable throwable) { /* TODO: notify user */ }

            public void onNoData() { /* TODO: notify user */ }
        });

        _mainPanel = new TitledBox("Select a Project", /*show action Links*/ false);
        _mainPanel.add(_selectProjectPanel);
    }

    public String getPageToken() {
        return "SelectProject";
    }

    public String getPageTitle() {
        return null;
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }


    protected void setupButtons() {
        getButtonManager().getBackButton().setVisible(false);
        getButtonManager().getNextButton().setVisible(false);
    }

    protected void preProcess(Integer priorPageNumber) {
        _selectProjectPanel.clearSelect();
    }

    public ProjectInTable getNextProjectInTable(Project currentProject) {
        return _selectProjectPanel.getNextProjectInTable(currentProject);
    }

    public ProjectInTable getPrevProjectInTable(Project currentProject) {
        return _selectProjectPanel.getPrevProjectInTable(currentProject);
    }

    public ProjectsWizardController getProjectsController() {
        return (ProjectsWizardController) getController();
    }
}
