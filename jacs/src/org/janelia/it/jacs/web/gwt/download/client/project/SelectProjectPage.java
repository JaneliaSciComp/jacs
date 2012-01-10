
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
