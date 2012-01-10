
package org.janelia.it.jacs.web.gwt.download.client.project;

import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;

/**
 * @author Michael Press
 */
abstract public class ProjectsWizardController extends WizardController {
    private ProjectDataManager _dataManager;

    public static final int VIEW_PROJECT_PAGE = 0;
    public static final int SELECT_PROJECT_PAGE = 1;

    public ProjectsWizardController() {
        _dataManager = new ProjectDataManager();
    }

    public ProjectDataManager getDataManager() {
        return _dataManager;
    }

    public void setDataManager(ProjectDataManager dataManager) {
        _dataManager = dataManager;
    }
}
