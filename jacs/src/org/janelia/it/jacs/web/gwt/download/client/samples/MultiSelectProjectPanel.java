
package org.janelia.it.jacs.web.gwt.download.client.samples;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBoxFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.list.MultiSelectList;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectSelectionCancelledListener;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectsSelectedListener;
import org.janelia.it.jacs.web.gwt.download.client.project.SelectProjectPanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Version of the SelectProjectPanel that allows multiple project selections
 *
 * @author Michael Press
 */
public class MultiSelectProjectPanel extends SelectProjectPanel {
    private MultiSelectList _multiSelectList;
    private SelectionListener _oneProjectSelectedListener;
    private ProjectsSelectedListener _projectsSelectedListener;

    public MultiSelectProjectPanel(SelectionListener oneProjectSelectedListener,
                                   ProjectsSelectedListener multiProjectsSelectedListener, ProjectSelectionCancelledListener cancelListener,
                                   TitledBoxFactory.BoxType searchOracleBoxType) {
        super(null, cancelListener, searchOracleBoxType);
        _oneProjectSelectedListener = oneProjectSelectedListener;  // fired on each project selection
        _projectsSelectedListener = multiProjectsSelectedListener; // fired on Apply button
        setApplyButtonEnabled(true);
    }

    protected Widget getTable() {
        _multiSelectList = new MultiSelectList("Projects", new SelectionListener() {
            public void onSelect(String value) {
                if (_oneProjectSelectedListener != null)
                    _oneProjectSelectedListener.onSelect(value);
            }

            public void onUnSelect(String value) {
                if (_oneProjectSelectedListener != null)
                    _oneProjectSelectedListener.onUnSelect(value);
            }
        });
        _multiSelectList.addStyleName("SamplesMultiSelectList");

        return _multiSelectList;
    }

    public void setOracleProjects(Collection<Project> projects) {
        populateSuggestOracle(projects);
    }

    /**
     * not applicable
     */
    //TODO: throw NotImplementedException?
    public void setProjects(Map<String, Project> projects) {
    }

    public void setAvailableProjects(Map<String, Project> projects) {
        _projects = projects;
        _multiSelectList.setAvailableList(getProjectNames(projects.values()));
        populateSuggestOracle(projects.values());
    }

    public void setAvailableProjects(Map<String, Project> projects, boolean refresh) {
        _projects = projects;
        _multiSelectList.setAvailableList(getProjectNames(projects.values()), refresh);
        populateSuggestOracle(projects.values());
    }

    public void setSelectedProjects(Map<String, Project> projectMap) {
        setSelectedProjects(getProjectNames(projectMap.values()), /*refresh*/ false);
    }

    public void setSelectedProjects(Map<String, Project> projectMap, boolean refresh) {
        setSelectedProjects(getProjectNames(projectMap.values()), refresh);
    }

    /**
     * Sets selected projects and refreshes table
     */
    public void setSelectedProjects(List<String> projectNames, boolean refresh) {
        setApplyButtonEnabled(true);
        if (_multiSelectList != null) // might not have been created yet
            _multiSelectList.setSelectedList(projectNames, refresh);
    }

    public void refresh() {
        _multiSelectList.refresh();
    }

    public List<String> getSelectedProjects() {
        return _multiSelectList.getSelectedItems();
    }

    private void setApplyButtonEnabled(boolean enable) {
        getApplyButton().setEnabled(enable);
    }

    /**
     * Called by the oracle to populate the Available table
     */
    protected void populateTable(Collection<Project> projects) {
        _multiSelectList.setAvailableList(getProjectNames(projects));
    }

    private List<String> getProjectNames(Collection<Project> projects) {
        List<String> projectNames = new ArrayList<String>();
        for (Project project : projects)
            projectNames.add(project.getProjectName());
        return projectNames;
    }

    /**
     * Called when Apply button is hit
     */
    protected void notifySelectionListener() {
        if (_projectsSelectedListener != null)
            _projectsSelectedListener.onSelect(_multiSelectList.getSelectedItems());
    }

    protected void notifyCancelListener() {
        if (getCancelListener() != null)
            getCancelListener().onCancel(0); //TODO: get real number
    }
}
