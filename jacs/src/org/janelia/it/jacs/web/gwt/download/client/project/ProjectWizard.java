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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

public class ProjectWizard extends ProjectsWizardController {
    private static Logger _logger = Logger.getLogger("");

    private ViewProjectPage _viewProjectPage;
    private SelectProjectPage _selectProjectPage;

    public static final String PROJECT_SYMBOL_PARAM = "projectSymbol";

    //TODO: handle initial project in URL and from pref
    public void onModuleLoad() {
        // Create and fade in the page contents
        try {
            // Setup the pages in the wizard
            _viewProjectPage = new ViewProjectPage(this, new NextProjectListener(), new PrevProjectListener());
            _selectProjectPage = new SelectProjectPage(this, new ProjectSelectionListener(), new CancelProjectSelectionListener());

            addPage(_viewProjectPage);
            addPage(_selectProjectPage);

            getDataManager().retrieveAllProjects(); // initiate data retrieval here since both pages need it and might be loaded first
        }
        catch (Throwable e) {
            _logger.error("Error onModuleLoad - ProjectWizard. " + e.getMessage());
        }

        // Show the wizard
        start();
    }

    /**
     * Notification that a project has been selected by the selection page.
     */
    private class ProjectSelectionListener implements ProjectSelectedInTableListener {
        public void onSelect(Project project, int currentProjectIndex, int numProjectsInTable) {
            getDataManager().setCurrentProject(project); // updates user pref in database
            getDataManager().setProjectSubsetValues(currentProjectIndex, numProjectsInTable);
            gotoPage(0); // display the project
        }

        public void onUnSelect(Project project) {
            getDataManager().setCurrentProject((Project) null);
        }
    }

    private class CancelProjectSelectionListener implements ProjectSelectionCancelledListener {
        public void onCancel(int totalRowsInTable) {
            getDataManager().setProjectSubsetValues(-1, totalRowsInTable);
            gotoPage(0); // redisplay the current project
        }
    }

    private class NextProjectListener implements ClickListener {
        public void onClick(Widget sender) {
            ProjectInTable nextProjectInTable = _selectProjectPage.getNextProjectInTable(getDataManager().getCurrentProject());
            getDataManager().setCurrentProject(nextProjectInTable);
            gotoPage(VIEW_PROJECT_PAGE);
        }
    }

    private class PrevProjectListener implements ClickListener {
        public void onClick(Widget sender) {
            ProjectInTable prevProjectInTable = _selectProjectPage.getPrevProjectInTable(getDataManager().getCurrentProject());
            getDataManager().setCurrentProject(prevProjectInTable);
            gotoPage(VIEW_PROJECT_PAGE);
        }
    }

    protected void processURLParam(String name, String value) {
        // Determine if a specific project is requested
        if (PROJECT_SYMBOL_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using project symbol " + value + " from URL");
            getDataManager().setInitialProjectSymbol(value);
        }
        else
            _logger.error("Got unknown param " + name + "=" + value + " from URL");
    }

    public Breadcrumb getBreadcrumbSection() {
        return new Breadcrumb(Constants.DATA_BROWSE_PROJECTS_LABEL, UrlBuilder.getProjectsUrl());
    }

    /**
     * Determine the start page:<ol>
     * <li>Display a project on the ViewProject page if it's specified in the URL</li>
     * <li>Else display a project on the ViewProject if the last-viewed project is specified in preferences</li>
     * <li>Otherwise, start on the project-selection page</li>
     */
    protected int getStartingPage(String startURL) {
        int startPage = SELECT_PROJECT_PAGE; // default if no project identified and no pref found

        if (getDataManager().getInitialProjectSymbol() != null) // specified in URL, stored during param processing
            startPage = VIEW_PROJECT_PAGE;
        else {
            // See if a (synchronous) preference has the last-viewed project
            UserPreference pref = Preferences.getUserPreference(ProjectDataManager.LAST_PROJECT_PREF, ProjectDataManager.LAST_PROJECT_PREF_CATEGORY);
            if (pref != null) {
                getDataManager().setInitialProjectSymbol(pref.getValue());
                startPage = VIEW_PROJECT_PAGE;
            }
        }

        return startPage;
    }
}