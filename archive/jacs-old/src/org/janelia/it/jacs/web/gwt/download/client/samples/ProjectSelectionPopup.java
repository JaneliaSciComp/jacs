
package org.janelia.it.jacs.web.gwt.download.client.samples;

import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.ResultReceiver;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.PublicationServiceHelper;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectSelectionCancelledListener;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectsSelectedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Press
 */
public class ProjectSelectionPopup extends BasePopupPanel {
    private MultiSelectProjectPanel _selectProjectPanel;
    private ProjectsSelectedListener _listener;
    private int _numProjects = 0;
    private int _numSelectedProjects = 0;
    private boolean _initialized = false;
    private List<String> _startingSelectedProjects;

    public ProjectSelectionPopup(String title, ProjectsSelectedListener listener) {
        super(title);
        _listener = listener;
    }

    protected void populateContent() {
        _selectProjectPanel = new MultiSelectProjectPanel(new OneProjectSelectedListener(),
                new ProjectsSelectionListener(), new PopupCancelledListener(), TitledBoxFactory.BoxType.SECONDARY_BOX);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(_selectProjectPanel);
    }

    public void show() {
        if (!_initialized) {
            // Retrieve the Projects
            _initialized = true;
            ResultReceiver rcv = new ResultReceiver() {
                public void setResult(Object result) {
                    Map<String, Project> projects = (Map<String, Project>) result;

                    _selectProjectPanel.setAvailableProjects(projects, /*refresh*/ false);
                    _numProjects = projects.size();

                    if (_startingSelectedProjects != null) {
                        _selectProjectPanel.setSelectedProjects(_startingSelectedProjects,  /*refresh*/ true);
                        _numSelectedProjects = _startingSelectedProjects.size();
                    }
                    else {
                        _selectProjectPanel.setSelectedProjects(projects,  /*refresh*/ true); // Set all projects as selected
                        _numSelectedProjects = projects.size(); // start with all selected unless previously specified
                        // Store the initial project list in case of cancel() - have to clone it or else it'll sync with user selections
                        _startingSelectedProjects = new ArrayList<String>(_selectProjectPanel.getSelectedProjects());
                    }

                    updateTitle();  // has to be in this method so updated only when async data retrieved
                }
            };
            PublicationServiceHelper.populateProjects(rcv);
        }
        else {
            // Store the initial project list in case of cancel() - have to clone it or else it'll sync with user selections
            _startingSelectedProjects = new ArrayList<String>(_selectProjectPanel.getSelectedProjects());
            _numSelectedProjects = _startingSelectedProjects.size();
            updateTitle();
        }

        super.show();
    }

    public void setSelectedProjects(List<String> projects) {
        _startingSelectedProjects = projects;
        _numSelectedProjects = projects.size();
        if (_initialized)
            _selectProjectPanel.setSelectedProjects(projects,  /*refresh*/ true); // Set all projects as selected
    }

    private class OneProjectSelectedListener implements SelectionListener {
        public void onSelect(String value) {
            ++_numSelectedProjects;
            updateTitle();
        }

        public void onUnSelect(String value) {
            --_numSelectedProjects;
            updateTitle();
        }
    }

    private void updateTitle() {
        //setText(_baseTitle + " - " + _numSelectedProjects + "  of " + _numProjects + " Projects Selected");
        setText(_numSelectedProjects + " of " + _numProjects + " Projects Selected");
    }

    private class ProjectsSelectionListener implements ProjectsSelectedListener {
        public void onSelect(List<String> projectNames) {
            _listener.onSelect(projectNames);
            hide();
        }
    }

    private class PopupCancelledListener implements ProjectSelectionCancelledListener {
        public void onCancel(int numProjectsInTable) {
            // Restore the projects from when the popup came up, and update the title for next show()
            _selectProjectPanel.setSelectedProjects(_startingSelectedProjects, /*refresh*/ true);
            hide();
        }
    }
}

